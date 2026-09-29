package com.example.my_kmp_project.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * OHOS-safe Home icons (custom ImageVector — no material-icons-extended).
 * Aligns Flutter `HomeDashboardWidgets` / `_StrategyEntry` / `HomeContactList`.
 */
internal object HomeIcons {
    val NotificationsNone: ImageVector by lazy { notificationsNone() }
    val Search: ImageVector by lazy { search() }
    val QrScan: ImageVector by lazy { qrScan() }
    val GridView: ImageVector by lazy { gridView() }
    val Analytics: ImageVector by lazy { analytics() }
    val PersonOutline: ImageVector by lazy { personOutline() }
    val ChatBubble: ImageVector by lazy { chatBubble() }
    val Phone: ImageVector by lazy { phone() }
    val ChevronRight: ImageVector by lazy { chevronRight() }
    val Article: ImageVector by lazy { article() }

    private fun solid(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.NonZero,
                pathBuilder = block,
            )
        }.build()

    /** Material Icons.notifications_none_rounded */
    private fun notificationsNone() = solid {
        moveTo(12f, 22f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        horizontalLineToRelative(-4f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        close()
        moveTo(18f, 16f)
        verticalLineToRelative(-5f)
        curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
        verticalLineTo(4f)
        curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
        reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
        verticalLineToRelative(0.68f)
        curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
        verticalLineToRelative(5f)
        lineToRelative(-2f, 2f)
        verticalLineToRelative(1f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(-1f)
        lineToRelative(-2f, -2f)
        close()
        moveTo(16f, 17f)
        horizontalLineTo(8f)
        verticalLineToRelative(-6f)
        curveToRelative(0f, -2.48f, 1.51f, -4.5f, 4f, -4.5f)
        reflectiveCurveToRelative(4f, 2.02f, 4f, 4.5f)
        verticalLineToRelative(6f)
        close()
    }

    /** Material Icons.search_rounded */
    private fun search() = solid {
        moveTo(15.5f, 14f)
        horizontalLineToRelative(-0.79f)
        lineToRelative(-0.28f, -0.27f)
        curveToRelative(1.2f, -1.4f, 1.82f, -3.31f, 1.48f, -5.34f)
        curveToRelative(-0.47f, -2.78f, -2.79f, -5f, -5.59f, -5.34f)
        curveToRelative(-4.23f, -0.52f, -7.79f, 3.04f, -7.27f, 7.27f)
        curveToRelative(0.34f, 2.8f, 2.56f, 5.12f, 5.34f, 5.59f)
        curveToRelative(2.03f, 0.34f, 3.94f, -0.28f, 5.34f, -1.48f)
        lineToRelative(0.27f, 0.28f)
        verticalLineToRelative(0.79f)
        lineToRelative(4.25f, 4.25f)
        curveToRelative(0.41f, 0.41f, 1.08f, 0.41f, 1.49f, 0f)
        curveToRelative(0.41f, -0.41f, 0.41f, -1.08f, 0f, -1.49f)
        lineTo(15.5f, 14f)
        close()
        moveTo(9.5f, 14f)
        curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        reflectiveCurveTo(7.01f, 5f, 9.5f, 5f)
        reflectiveCurveTo(14f, 7.01f, 14f, 9.5f)
        reflectiveCurveTo(11.99f, 14f, 9.5f, 14f)
        close()
    }

    /** Material Icons.qr_code_scanner_rounded — corner brackets + center. */
    private fun qrScan() = solid {
        moveTo(9.5f, 6.5f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-3f)
        verticalLineToRelative(-3f)
        horizontalLineToRelative(3f)
        close()
        moveTo(11f, 5f)
        horizontalLineTo(5f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(6f)
        verticalLineTo(5f)
        close()
        moveTo(9.5f, 14.5f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-3f)
        verticalLineToRelative(-3f)
        horizontalLineToRelative(3f)
        close()
        moveTo(11f, 13f)
        horizontalLineTo(5f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-6f)
        close()
        moveTo(17.5f, 6.5f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-3f)
        verticalLineToRelative(-3f)
        horizontalLineToRelative(3f)
        close()
        moveTo(19f, 5f)
        horizontalLineToRelative(-6f)
        verticalLineToRelative(6f)
        horizontalLineToRelative(6f)
        verticalLineTo(5f)
        close()
        moveTo(13f, 13f)
        horizontalLineToRelative(1.5f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(13f)
        verticalLineTo(13f)
        close()
        moveTo(14.5f, 14.5f)
        horizontalLineTo(16f)
        verticalLineTo(16f)
        horizontalLineToRelative(-1.5f)
        verticalLineToRelative(-1.5f)
        close()
        moveTo(16f, 13f)
        horizontalLineToRelative(1.5f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(16f)
        verticalLineTo(13f)
        close()
        moveTo(13f, 16f)
        horizontalLineToRelative(1.5f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(13f)
        verticalLineTo(16f)
        close()
        moveTo(14.5f, 17.5f)
        horizontalLineTo(16f)
        verticalLineTo(19f)
        horizontalLineToRelative(-1.5f)
        verticalLineToRelative(-1.5f)
        close()
        moveTo(16f, 16f)
        horizontalLineToRelative(1.5f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(16f)
        verticalLineTo(16f)
        close()
        moveTo(17.5f, 14.5f)
        horizontalLineTo(19f)
        verticalLineTo(16f)
        horizontalLineToRelative(-1.5f)
        verticalLineToRelative(-1.5f)
        close()
        moveTo(17.5f, 17.5f)
        horizontalLineTo(19f)
        verticalLineTo(19f)
        horizontalLineToRelative(-1.5f)
        verticalLineToRelative(-1.5f)
        close()
        moveTo(22f, 7f)
        horizontalLineToRelative(-2f)
        verticalLineTo(4f)
        horizontalLineToRelative(-3f)
        verticalLineTo(2f)
        horizontalLineToRelative(5f)
        verticalLineToRelative(5f)
        close()
        moveTo(22f, 22f)
        verticalLineToRelative(-5f)
        horizontalLineToRelative(-2f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(-3f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(5f)
        close()
        moveTo(2f, 22f)
        horizontalLineToRelative(5f)
        verticalLineToRelative(-2f)
        horizontalLineTo(4f)
        verticalLineToRelative(-3f)
        horizontalLineTo(2f)
        verticalLineToRelative(5f)
        close()
        moveTo(2f, 2f)
        verticalLineToRelative(5f)
        horizontalLineToRelative(2f)
        verticalLineTo(4f)
        horizontalLineToRelative(3f)
        verticalLineTo(2f)
        horizontalLineTo(2f)
        close()
    }

    /** Material Icons.grid_view_rounded */
    private fun gridView() = solid {
        moveTo(3f, 3f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(8f)
        horizontalLineTo(3f)
        close()
        moveTo(13f, 3f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(-8f)
        close()
        moveTo(3f, 13f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(8f)
        horizontalLineTo(3f)
        close()
        moveTo(13f, 13f)
        horizontalLineToRelative(8f)
        verticalLineToRelative(8f)
        horizontalLineToRelative(-8f)
        close()
    }

    /** Material Icons.analytics_outlined — bar chart. */
    private fun analytics() = solid {
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(19f, 19f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(14f)
        verticalLineToRelative(14f)
        close()
        moveTo(7f, 12f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(5f)
        horizontalLineTo(7f)
        close()
        moveTo(11f, 7f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(10f)
        horizontalLineToRelative(-2f)
        close()
        moveTo(15f, 10f)
        horizontalLineToRelative(2f)
        verticalLineToRelative(7f)
        horizontalLineToRelative(-2f)
        close()
    }

    /** Material Icons.person_outline */
    private fun personOutline() = solid {
        moveTo(12f, 5.9f)
        curveToRelative(1.16f, 0f, 2.1f, 0.94f, 2.1f, 2.1f)
        reflectiveCurveToRelative(-0.94f, 2.1f, -2.1f, 2.1f)
        reflectiveCurveToRelative(-2.1f, -0.94f, -2.1f, -2.1f)
        reflectiveCurveToRelative(0.94f, -2.1f, 2.1f, -2.1f)
        moveToRelative(0f, 9f)
        curveToRelative(2.97f, 0f, 6.1f, 1.46f, 6.1f, 2.1f)
        verticalLineToRelative(1.1f)
        horizontalLineTo(5.9f)
        verticalLineTo(17f)
        curveToRelative(0f, -0.64f, 3.13f, -2.1f, 6.1f, -2.1f)
        moveTo(12f, 4f)
        curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
        reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
        reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
        reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
        close()
        moveTo(12f, 13f)
        curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(-3f)
        curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
        close()
    }

    /** Material Icons.chat_bubble_outline */
    private fun chatBubble() = solid {
        moveTo(20f, 2f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(18f)
        lineToRelative(4f, -4f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(4f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(20f, 16f)
        horizontalLineTo(5.17f)
        lineTo(4f, 17.17f)
        verticalLineTo(4f)
        horizontalLineToRelative(16f)
        verticalLineToRelative(12f)
        close()
    }

    /** Material Icons.phone_outlined */
    private fun phone() = solid {
        moveTo(6.54f, 5f)
        curveToRelative(0.06f, 0.89f, 0.21f, 1.76f, 0.45f, 2.59f)
        lineToRelative(-1.2f, 1.2f)
        curveToRelative(-0.41f, -1.2f, -0.67f, -2.47f, -0.76f, -3.79f)
        horizontalLineToRelative(1.51f)
        close()
        moveTo(16.4f, 17.02f)
        curveToRelative(0.85f, 0.24f, 1.72f, 0.39f, 2.6f, 0.45f)
        verticalLineToRelative(1.49f)
        curveToRelative(-1.32f, -0.09f, -2.59f, -0.35f, -3.8f, -0.75f)
        lineToRelative(1.2f, -1.19f)
        close()
        moveTo(7.5f, 3f)
        horizontalLineTo(4f)
        curveToRelative(-0.55f, 0f, -1f, 0.45f, -1f, 1f)
        curveToRelative(0f, 9.39f, 7.61f, 17f, 17f, 17f)
        curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
        verticalLineToRelative(-3.49f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        curveToRelative(-1.24f, 0f, -2.45f, -0.2f, -3.57f, -0.57f)
        curveToRelative(-0.1f, -0.04f, -0.21f, -0.05f, -0.31f, -0.05f)
        curveToRelative(-0.26f, 0f, -0.51f, 0.1f, -0.71f, 0.29f)
        lineToRelative(-2.2f, 2.2f)
        curveToRelative(-2.83f, -1.45f, -5.15f, -3.76f, -6.59f, -6.59f)
        lineToRelative(2.2f, -2.2f)
        curveToRelative(0.28f, -0.28f, 0.36f, -0.67f, 0.25f, -1.02f)
        curveTo(8.7f, 6.45f, 8.5f, 5.25f, 8.5f, 4f)
        curveToRelative(0f, -0.55f, -0.45f, -1f, -1f, -1f)
        close()
    }

    /** Material Icons.chevron_right_rounded */
    private fun chevronRight() = solid {
        moveTo(9.29f, 6.71f)
        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
        lineTo(13.17f, 12f)
        lineToRelative(-3.88f, 3.88f)
        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0f, 1.41f)
        curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
        lineToRelative(4.59f, -4.59f)
        curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0f, -1.41f)
        lineTo(10.7f, 6.7f)
        curveToRelative(-0.38f, -0.38f, -1.02f, -0.38f, -1.41f, 0.01f)
        close()
    }

    /** Material Icons.article_outlined — news leading. */
    private fun article() = solid {
        moveTo(19f, 5f)
        verticalLineToRelative(14f)
        horizontalLineTo(5f)
        verticalLineTo(5f)
        horizontalLineToRelative(14f)
        moveToRelative(0f, -2f)
        horizontalLineTo(5f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(14f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(14f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(5f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        close()
        moveTo(7f, 7f)
        horizontalLineToRelative(5f)
        verticalLineToRelative(5f)
        horizontalLineTo(7f)
        close()
        moveTo(14f, 7f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(1.5f)
        horizontalLineToRelative(-3f)
        close()
        moveTo(14f, 10.5f)
        horizontalLineToRelative(3f)
        verticalLineTo(12f)
        horizontalLineToRelative(-3f)
        close()
        moveTo(7f, 14f)
        horizontalLineToRelative(10f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(7f)
        close()
        moveTo(7f, 17f)
        horizontalLineToRelative(10f)
        verticalLineToRelative(1.5f)
        horizontalLineTo(7f)
        close()
    }
}
