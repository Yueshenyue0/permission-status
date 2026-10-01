package com.permission.status.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 状态语义色 */
object StatusColors {
    val Granted = Color(0xFF2FA84F)
    val Denied = Color(0xFFF2A33C)
    val Permanently = Color(0xFFE5484D)
    val Unavailable = Color(0xFF9AA0A6)
}

/** 状态对应的中文标签与颜色 */
fun statusVisual(state: com.permission.status.PermState): Pair<String, Color> = when (state) {
    com.permission.status.PermState.GRANTED -> "已授权" to StatusColors.Granted
    com.permission.status.PermState.DENIED -> "未授权" to StatusColors.Denied
    com.permission.status.PermState.PERMANENTLY -> "已被拒绝" to StatusColors.Permanently
    com.permission.status.PermState.NOT_DECLARED -> "未声明" to StatusColors.Unavailable
    com.permission.status.PermState.NOT_APPLICABLE -> "系统不支持" to StatusColors.Unavailable
}

/**
 * 大标题状态卡片 —— 所有权限检测项统一使用该卡片展示。
 *
 * 结构：左侧圆形状态图标 + 大标题（权限名）+ 描述，底部为状态徽标与操作按钮。
 */
@Composable
fun StatusCard(
    title: String,
    state: com.permission.status.PermState,
    description: String,
    icon: ImageVector,
    detail: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (label, color) = statusVisual(state)

    val content: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 左侧圆形图标容器
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 大标题：权限名
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title3
                )
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MiuixTheme.textStyles.body2.copy(
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // 状态圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MiuixTheme.textStyles.subtitle.copy(color = color)
            )

            if (!detail.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = detail,
                    style = MiuixTheme.textStyles.footnote1.copy(
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (actionText != null && onAction != null) {
                TextButton(
                    text = actionText,
                    onClick = onAction,
                    colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }

    val cardModifier = modifier.fillMaxWidth()
    val inside = PaddingValues(horizontal = 16.dp, vertical = 16.dp)

    if (onAction != null) {
        Card(
            modifier = cardModifier,
            insideMargin = inside,
            pressFeedbackType = PressFeedbackType.Sink,
            showIndication = true,
            onClick = onAction,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            insideMargin = inside,
            content = content
        )
    }
}

/** 概览大卡片：整体评分 */
@Composable
fun OverviewCard(
    score: Int,
    granted: Int,
    total: Int,
    specialGranted: Int,
    specialTotal: Int,
    deviceModel: String,
    androidVersion: String,
    hyperOs: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = when {
        score >= 80 -> StatusColors.Granted
        score >= 40 -> StatusColors.Denied
        else -> StatusColors.Permanently
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "权限健康度",
                    style = MiuixTheme.textStyles.title2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hyperOs) "运行于 $deviceModel · HyperOS 风格" else "运行于 $deviceModel",
                    style = MiuixTheme.textStyles.footnote1.copy(
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$score",
                    style = MiuixTheme.textStyles.title1.copy(color = accent)
                )
                Text(
                    text = "分",
                    style = MiuixTheme.textStyles.footnote2.copy(
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier.fillMaxWidth(),
            colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
                .progressIndicatorColors(foregroundColor = accent)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OverviewStat(
                value = "$granted / $total",
                label = "运行时权限",
                modifier = Modifier.weight(1f)
            )
            OverviewStat(
                value = "$specialGranted / $specialTotal",
                label = "特殊权限",
                modifier = Modifier.weight(1f)
            )
            OverviewStat(
                value = androidVersion,
                label = "系统版本",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.title4
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2.copy(
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        )
    }
}

/** 分区标题 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.subtitle.copy(
            color = MiuixTheme.colorScheme.primary
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
    )
}