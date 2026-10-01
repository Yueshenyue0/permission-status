package com.permission.status

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 权限状态检测引擎。
 * 所有判断均基于 PackageManager / AppOpsManager 的真实结果。
 */
class PermissionChecker(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val sdk: Int = Build.VERSION.SDK_INT

    /** 检测单个权限项 */
    fun check(item: PermissionItem): PermState {
        if (!item.appliesOn(sdk)) return PermState.NOT_APPLICABLE

        val declared = item.permissions.filter { isDeclared(it) }
        if (declared.isEmpty()) return PermState.NOT_DECLARED

        val grantedCount = declared.count { isGranted(it) }
        return if (grantedCount == declared.size) PermState.GRANTED else PermState.DENIED
    }

    fun isDeclared(permission: String): Boolean = try {
        pm.getPermissionInfo(permission, 0)
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.contains(permission) == true
    } catch (e: Exception) {
        false
    }

    fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * 判断是否被永久拒绝：
     * 已声明、未授权，且系统不再展示申请弹窗（shouldShowRequestPermissionRationale == false）
     * 对于从未申请过的权限，需要结合「是否申请过」的判断，这里采用保守策略：
     * 只要 shouldShow 为 false 且未授权，即提示用户可前往设置页开启。
     */
    private fun isPermanentlyDenied(permissions: List<String>): Boolean {
        val activity = context as? androidx.activity.ComponentActivity
        if (activity == null) return false
        return permissions.none { perm ->
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }
    }

    /** 特殊权限：使用情况访问 */
    fun hasUsageAccess(): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }

    /** 特殊权限：悬浮窗 */
    fun hasOverlay(): Boolean = Settings.canDrawOverlays(context)

    /** 特殊权限：修改系统设置 */
    fun hasWriteSettings(): Boolean = Settings.System.canWrite(context)

    /** 特殊权限：通知使用权 */
    fun hasNotificationListener(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabled.contains(context.packageName)
    }

    /** 特殊权限：安装未知应用（未声明该权限时系统会抛 SecurityException，必须兜底） */
    fun hasInstallUnknownApps(): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pm.canRequestPackageInstalls()
        } else {
            true
        }
    } catch (t: Throwable) {
        false
    }

    /** 特殊权限：是否已忽略电池优化 */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE)
                as? android.os.PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** 特殊权限：无障碍服务 */
    fun hasAccessibility(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(context.packageName)
    }

    /** 检测特殊权限项（整体兜底：任何系统异常都降级为未授权，绝不崩溃） */
    fun checkSpecial(item: SpecialPermissionItem): PermState = try {
        when (item.id) {
            "usage_access" -> boolToState(hasUsageAccess())
            "overlay" -> boolToState(hasOverlay())
            "write_settings" -> boolToState(hasWriteSettings())
            "notification_listener" -> boolToState(hasNotificationListener())
            "install_unknown" -> boolToState(hasInstallUnknownApps())
            "battery_optimization" -> boolToState(isIgnoringBatteryOptimizations())
            "accessibility" -> boolToState(hasAccessibility())
            else -> PermState.NOT_APPLICABLE
        }
    } catch (t: Throwable) {
        PermState.DENIED
    }

    private fun boolToState(value: Boolean): PermState =
        if (value) PermState.GRANTED else PermState.DENIED

    /** 通知是否可用（额外校验通知渠道未被关闭） */
    fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** 设备基础信息 */
    fun deviceInfo(): DeviceInfo {
        val runtimeItems = PermissionCatalog.runtime.filter { it.appliesOn(sdk) }
        val granted = runtimeItems.count { check(it) == PermState.GRANTED }
        val total = runtimeItems.size
        val specialItems = PermissionCatalog.special
        val specialGranted = specialItems.count { checkSpecial(it) == PermState.GRANTED }

        return DeviceInfo(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            sdkInt = sdk,
            isHyperOs = isHyperOs(),
            grantedCount = granted,
            totalCount = total,
            specialGranted = specialGranted,
            specialTotal = specialItems.size,
            score = if (total == 0) 0 else (granted * 100 / total)
        )
    }

    private fun isHyperOs(): Boolean {
        val osName = readSystemProperty("ro.mi.os.version.name")
        val uiName = readSystemProperty("ro.miui.ui.version.name")
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val isXiaomiFamily =
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
        return osName.isNotEmpty() || uiName.isNotEmpty() || isXiaomiFamily
    }

    private fun readSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getter = clazz.getMethod("get", String::class.java)
            val raw = getter.invoke(null, key)
            if (raw is String) raw else ""
        } catch (t: Throwable) {
            ""
        }
    }

    /** 系统信息附件，用于「设备信息」卡片 */
    fun storageSummary(): String {
        return try {
            val stat = android.os.StatFs(Environment.getDataDirectory().path)
            val totalGb = stat.blockCountLong * stat.blockSizeLong / 1024.0 / 1024.0 / 1024.0
            String.format("%.1f GB 可用空间", totalGb)
        } catch (t: Throwable) {
            "未知"
        }
    }
}

/** 设备与权限概览数据 */
data class DeviceInfo(
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val isHyperOs: Boolean,
    val grantedCount: Int,
    val totalCount: Int,
    val specialGranted: Int,
    val specialTotal: Int,
    val score: Int
)