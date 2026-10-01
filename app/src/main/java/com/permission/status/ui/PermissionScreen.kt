package com.permission.status.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.permission.status.PermState
import com.permission.status.PermissionCatalog
import com.permission.status.PermissionChecker
import com.permission.status.PermissionGroup
import com.permission.status.PermissionItem
import com.permission.status.SpecialPermissionItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Mic
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 主界面：Miuix 大标题顶部栏 + 大标题状态卡片（所有检测项统一使用该卡片）。
 */
@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    val checker = remember(context) { PermissionChecker(context) }

    var refreshTick by remember { mutableIntStateOf(0) }
    val scrollBehavior = MiuixScrollBehavior()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    // 从系统设置页返回时自动重新检测
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openSpecialSettings(item: SpecialPermissionItem) {
        try {
            val intent = when (item.id) {
                "install_unknown" -> Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
                "battery_optimization" -> Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                )
                "overlay" -> Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                "write_settings" -> Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
                else -> Intent(item.action)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(context)
        }
    }

    val info = remember(refreshTick) { checker.deviceInfo() }
    val runtimeItems = PermissionCatalog.runtime
    val stateMap = remember(refreshTick) { runtimeItems.associateWith { checker.check(it) } }
    val specialMap = remember(refreshTick) {
        PermissionCatalog.special.associateWith { checker.checkSpecial(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "权限检测",
                largeTitle = "权限检测",
                subtitle = "${info.grantedCount}/${info.totalCount} 项运行时权限已授权",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "重新检测",
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 40.dp,
                start = 12.dp,
                end = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "overview") {
                OverviewCard(
                    score = info.score,
                    granted = info.grantedCount,
                    total = info.totalCount,
                    specialGranted = info.specialGranted,
                    specialTotal = info.specialTotal,
                    deviceModel = info.model,
                    androidVersion = info.androidVersion,
                    hyperOs = info.isHyperOs
                )
            }

            // 运行时权限：按分组展示，全部使用大标题状态卡片
            PermissionGroup.entries.forEach { group ->
                val groupItems = runtimeItems.filter { it.group == group }
                if (groupItems.isEmpty()) return@forEach

                item(key = "header_${group.name}") {
                    SectionTitle(group.label)
                }

                items(groupItems, key = { it.name }) { permission ->
                    val state = stateMap[permission] ?: PermState.NOT_APPLICABLE
                    StatusCard(
                        title = permission.label,
                        state = state,
                        description = permission.description,
                        icon = iconFor(permission),
                        detail = detailFor(state),
                        actionText = if (state == PermState.DENIED) "去授权" else null,
                        onAction = if (state == PermState.DENIED) {
                            {
                                val arr = permission.permissions.toTypedArray()
                                if (arr.isNotEmpty()) launcher.launch(arr) else openAppSettings(context)
                            }
                        } else null
                    )
                }
            }

            // 特殊权限
            item(key = "header_special") { SectionTitle("特殊权限") }
            items(PermissionCatalog.special, key = { it.id }) { special ->
                val state = specialMap[special] ?: PermState.DENIED
                StatusCard(
                    title = special.label,
                    state = state,
                    description = special.description,
                    icon = MiuixIcons.Lock,
                    detail = null,
                    actionText = if (state == PermState.GRANTED) null else "去开启",
                    onAction = if (state == PermState.GRANTED) null else {
                        { openSpecialSettings(special) }
                    }
                )
            }

            item(key = "footer") {
                StatusCard(
                    title = "检测说明",
                    state = PermState.GRANTED,
                    description = "本应用仅在本机读取权限状态，不采集、不上传任何数据。特殊权限需在系统设置中手动授予。",
                    icon = MiuixIcons.Info
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

/** 根据权限项匹配 Miuix 内置图标（均已核验存在于 miuix-icons 0.9.4） */
private fun iconFor(item: PermissionItem) = when (item.name) {
    "camera" -> MiuixIcons.Image
    "microphone" -> MiuixIcons.Mic
    "location_fine", "location_coarse" -> MiuixIcons.Location
    "contacts", "phone" -> MiuixIcons.Phone
    "sms" -> MiuixIcons.Messages
    "storage", "media_images", "media_video", "media_audio", "media_location" -> MiuixIcons.Folder
    "nearby" -> MiuixIcons.Link
    "notification" -> MiuixIcons.Email
    "body_sensors" -> MiuixIcons.Tasks
    "calendar" -> MiuixIcons.WorldClock
    else -> MiuixIcons.Lock
}

private fun detailFor(state: PermState): String? = when (state) {
    PermState.NOT_APPLICABLE -> "当前系统无需该权限"
    PermState.NOT_DECLARED -> "应用未声明"
    else -> null
}