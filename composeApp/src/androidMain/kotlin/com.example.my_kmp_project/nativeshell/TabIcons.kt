package com.example.my_kmp_project.nativeshell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Cupertino-ish tab glyphs (OHOS-safe ImageVector — no material-icons-extended).
 * Matches Flutter: house / chat_bubble / person_2 / person.
 */
internal object TabIcons {
    val Home: ImageVector by lazy { home() }
    val Chat: ImageVector by lazy { chat() }
    val Community: ImageVector by lazy { community() }
    val Mine: ImageVector by lazy { mine() }

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

    /** CupertinoIcons.house-ish outline. */
    private fun home() = solid {
        moveTo(12f, 3.5f)
        lineTo(4f, 10.2f)
        verticalLineTo(20f)
        horizontalLineTo(9.5f)
        verticalLineTo(14.5f)
        horizontalLineTo(14.5f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(10.2f)
        close()
        moveTo(12f, 5.8f)
        lineTo(18.2f, 11f)
        verticalLineTo(18.2f)
        horizontalLineTo(16.2f)
        verticalLineTo(12.7f)
        horizontalLineTo(7.8f)
        verticalLineTo(18.2f)
        horizontalLineTo(5.8f)
        verticalLineTo(11f)
        close()
    }

    /** CupertinoIcons.chat_bubble-ish. */
    private fun chat() = solid {
        moveTo(12f, 3f)
        curveTo(7.03f, 3f, 3f, 6.58f, 3f, 11f)
        curveTo(3f, 13.4f, 4.2f, 15.55f, 6.1f, 16.95f)
        lineTo(5f, 21f)
        lineTo(9.4f, 18.9f)
        curveTo(10.2f, 19.1f, 11.1f, 19.2f, 12f, 19.2f)
        curveTo(16.97f, 19.2f, 21f, 15.62f, 21f, 11.2f)
        reflectiveCurveTo(16.97f, 3f, 12f, 3f)
        close()
        moveTo(12f, 5f)
        curveTo(15.87f, 5f, 19f, 7.8f, 19f, 11.2f)
        reflectiveCurveTo(15.87f, 17.2f, 12f, 17.2f)
        curveTo(11.2f, 17.2f, 10.45f, 17.1f, 9.75f, 16.9f)
        lineTo(9.4f, 16.75f)
        lineTo(7.15f, 17.8f)
        lineTo(7.7f, 15.75f)
        lineTo(7.5f, 15.4f)
        curveTo(5.95f, 14.25f, 5f, 12.8f, 5f, 11.2f)
        curveTo(5f, 7.8f, 8.13f, 5f, 12f, 5f)
        close()
    }

    /** CupertinoIcons.person_2-ish. */
    private fun community() = solid {
        // back person
        moveTo(15.5f, 8f)
        curveTo(16.88f, 8f, 18f, 9.12f, 18f, 10.5f)
        reflectiveCurveTo(16.88f, 13f, 15.5f, 13f)
        reflectiveCurveTo(13f, 11.88f, 13f, 10.5f)
        reflectiveCurveTo(14.12f, 8f, 15.5f, 8f)
        close()
        moveTo(15.5f, 14f)
        curveTo(17.67f, 14f, 21f, 15.08f, 21f, 17.25f)
        verticalLineTo(19f)
        horizontalLineTo(17.5f)
        verticalLineTo(17.25f)
        curveTo(17.5f, 16.35f, 16.7f, 15.55f, 15.35f, 15f)
        curveTo(15.4f, 14.7f, 15.45f, 14.35f, 15.5f, 14f)
        close()
        // front person
        moveTo(9f, 8f)
        curveTo(10.66f, 8f, 12f, 9.34f, 12f, 11f)
        reflectiveCurveTo(10.66f, 14f, 9f, 14f)
        reflectiveCurveTo(6f, 12.66f, 6f, 11f)
        reflectiveCurveTo(7.34f, 8f, 9f, 8f)
        close()
        moveTo(9f, 15.5f)
        curveTo(11.67f, 15.5f, 16f, 16.83f, 16f, 19.5f)
        verticalLineTo(21f)
        horizontalLineTo(2f)
        verticalLineTo(19.5f)
        curveTo(2f, 16.83f, 6.33f, 15.5f, 9f, 15.5f)
        close()
    }

    /** CupertinoIcons.person-ish. */
    private fun mine() = solid {
        moveTo(12f, 5f)
        curveTo(13.66f, 5f, 15f, 6.34f, 15f, 8f)
        reflectiveCurveTo(13.66f, 11f, 12f, 11f)
        reflectiveCurveTo(9f, 9.66f, 9f, 8f)
        reflectiveCurveTo(10.34f, 5f, 12f, 5f)
        close()
        moveTo(12f, 12.5f)
        curveTo(14.67f, 12.5f, 20f, 13.83f, 20f, 16.5f)
        verticalLineTo(19f)
        horizontalLineTo(4f)
        verticalLineTo(16.5f)
        curveTo(4f, 13.83f, 9.33f, 12.5f, 12f, 12.5f)
        close()
    }
}
