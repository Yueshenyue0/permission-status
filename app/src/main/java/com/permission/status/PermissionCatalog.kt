package com.permission.status

/** 权限分组，用于「全部权限」页面的分区展示 */
enum class PermissionGroup(val label: String) {
    CAMERA("相机与麦克风"),
    LOCATION("位置信息"),
    CONTACTS("通讯录与电话"),
    MESSAGES("短信"),
    STORAGE("存储与媒体"),
    NEARBY("附近的设备"),
    NOTIFICATION("通知"),
    SENSORS("身体传感器"),
    CALENDAR("日历")
}

/** 权限状态 */
enum class PermState {
    GRANTED,          // 已授权
    DENIED,           // 已拒绝（可再次申请）
    PERMANENTLY,      // 被永久拒绝（需去设置页手动开启）
    NOT_DECLARED,     // 未在清单中声明
    NOT_APPLICABLE    // 当前系统版本不适用
}

/** 单条权限检测项 */
data class PermissionItem(
    val name: String,
    val label: String,
    val description: String,
    val group: PermissionGroup,
    val permissions: List<String>,
    val minSdk: Int = 0,
    val maxSdk: Int = Int.MAX_VALUE
) {
    /** 该条目在当前系统版本是否适用 */
    fun appliesOn(sdk: Int): Boolean = sdk in minSdk..maxSdk

    /** 用于 requestPermissions 的权限数组 */
    fun requestArray(sdk: Int): Array<String> = permissions.toTypedArray()

    val isRuntime: Boolean get() = permissions.isNotEmpty()
}

/** 特殊权限（需跳转系统设置授予） */
data class SpecialPermissionItem(
    val id: String,
    val label: String,
    val description: String,
    val action: String
)

/** 当前系统可用的运行时权限全集（含版本适配） */
object PermissionCatalog {

    const val SDK_TIRAMISU = 33
    const val SDK_UPSIDE_DOWN_CAKE = 34

    val runtime: List<PermissionItem> = listOf(
        // 相机与麦克风
        PermissionItem(
            name = "camera",
            label = "相机",
            description = "拍摄照片与视频，支持扫码、音视频通话",
            group = PermissionGroup.CAMERA,
            permissions = listOf("android.permission.CAMERA")
        ),
        PermissionItem(
            name = "microphone",
            label = "麦克风",
            description = "录制音频、语音通话与语音输入",
            group = PermissionGroup.CAMERA,
            permissions = listOf("android.permission.RECORD_AUDIO")
        ),

        // 位置
        PermissionItem(
            name = "location_fine",
            label = "精确位置",
            description = "获取 GPS 级别的精确定位信息",
            group = PermissionGroup.LOCATION,
            permissions = listOf("android.permission.ACCESS_FINE_LOCATION")
        ),
        PermissionItem(
            name = "location_coarse",
            label = "大致位置",
            description = "仅获取基站 / WiFi 级别的模糊位置",
            group = PermissionGroup.LOCATION,
            permissions = listOf("android.permission.ACCESS_COARSE_LOCATION")
        ),

        // 通讯录与电话
        PermissionItem(
            name = "contacts",
            label = "通讯录",
            description = "读取联系人列表，用于来电识别与社交推荐",
            group = PermissionGroup.CONTACTS,
            permissions = listOf("android.permission.READ_CONTACTS")
        ),
        PermissionItem(
            name = "phone",
            label = "电话",
            description = "拨打电话与读取设备通话状态",
            group = PermissionGroup.CONTACTS,
            permissions = listOf(
                "android.permission.CALL_PHONE",
                "android.permission.READ_PHONE_STATE"
            )
        ),

        // 短信
        PermissionItem(
            name = "sms",
            label = "短信",
            description = "读取与发送短信，用于验证码自动填充",
            group = PermissionGroup.MESSAGES,
            permissions = listOf(
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS"
            )
        ),

        // 存储与媒体
        PermissionItem(
            name = "storage",
            label = "存储空间",
            description = "读取设备上的图片、视频与音频文件",
            group = PermissionGroup.STORAGE,
            permissions = listOf("android.permission.READ_EXTERNAL_STORAGE"),
            maxSdk = SDK_TIRAMISU - 1
        ),
        PermissionItem(
            name = "media_images",
            label = "图片与照片",
            description = "读取相册中的图片文件（Android 13+）",
            group = PermissionGroup.STORAGE,
            permissions = listOf("android.permission.READ_MEDIA_IMAGES"),
            minSdk = SDK_TIRAMISU
        ),
        PermissionItem(
            name = "media_video",
            label = "视频",
            description = "读取相册中的视频文件（Android 13+）",
            group = PermissionGroup.STORAGE,
            permissions = listOf("android.permission.READ_MEDIA_VIDEO"),
            minSdk = SDK_TIRAMISU
        ),
        PermissionItem(
            name = "media_audio",
            label = "音频",
            description = "读取设备上的音乐与录音文件（Android 13+）",
            group = PermissionGroup.STORAGE,
            permissions = listOf("android.permission.READ_MEDIA_AUDIO"),
            minSdk = SDK_TIRAMISU
        ),
        PermissionItem(
            name = "media_location",
            label = "照片位置信息",
            description = "读取照片 EXIF 中的地理位置信息",
            group = PermissionGroup.STORAGE,
            permissions = listOf("android.permission.ACCESS_MEDIA_LOCATION"),
            minSdk = 29
        ),

        // 附近的设备
        PermissionItem(
            name = "nearby",
            label = "附近的设备",
            description = "扫描并连接蓝牙设备（Android 12+）",
            group = PermissionGroup.NEARBY,
            permissions = listOf(
                "android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH_CONNECT"
            ),
            minSdk = 31
        ),

        // 通知
        PermissionItem(
            name = "notification",
            label = "通知",
            description = "向通知栏推送消息提醒（Android 13+）",
            group = PermissionGroup.NOTIFICATION,
            permissions = listOf("android.permission.POST_NOTIFICATIONS"),
            minSdk = SDK_TIRAMISU
        ),

        // 传感器
        PermissionItem(
            name = "body_sensors",
            label = "身体传感器",
            description = "读取心率、步数等健康传感器数据",
            group = PermissionGroup.SENSORS,
            permissions = listOf("android.permission.BODY_SENSORS")
        ),

        // 日历
        PermissionItem(
            name = "calendar",
            label = "日历",
            description = "读取与写入日程安排",
            group = PermissionGroup.CALENDAR,
            permissions = listOf(
                "android.permission.READ_CALENDAR",
                "android.permission.WRITE_CALENDAR"
            )
        )
    )

    val special: List<SpecialPermissionItem> = listOf(
        SpecialPermissionItem(
            id = "usage_access",
            label = "使用情况访问",
            description = "查看应用的启动记录与使用时长（应用锁 / 数字健康依赖）",
            action = "android.settings.USAGE_ACCESS_SETTINGS"
        ),
        SpecialPermissionItem(
            id = "overlay",
            label = "显示在其他应用上层",
            description = "悬浮窗权限，用于来电阻止、游戏助手等场景",
            action = "android.settings.action.MANAGE_OVERLAY_PERMISSION"
        ),
        SpecialPermissionItem(
            id = "write_settings",
            label = "修改系统设置",
            description = "用于自动化工具修改系统配置项",
            action = "android.settings.ACTION_MANAGE_WRITE_SETTINGS"
        ),
        SpecialPermissionItem(
            id = "notification_listener",
            label = "通知使用权",
            description = "读取通知内容，用于通知过滤与消息同步",
            action = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
        ),
        SpecialPermissionItem(
            id = "install_unknown",
            label = "安装未知应用",
            description = "允许安装第三方来源的 APK 安装包",
            action = "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
        ),
        SpecialPermissionItem(
            id = "battery_optimization",
            label = "电池优化白名单",
            description = "忽略电池优化，保证后台任务稳定运行",
            action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
        ),
        SpecialPermissionItem(
            id = "accessibility",
            label = "无障碍服务",
            description = "辅助功能与自动化操作依赖的高危权限",
            action = "android.settings.ACCESSIBILITY_SETTINGS"
        )
    )
}