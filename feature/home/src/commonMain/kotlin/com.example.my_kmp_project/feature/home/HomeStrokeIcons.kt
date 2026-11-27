package com.example.my_kmp_project.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 首页用**描边**风格图标（对齐 Flutter 侧 `Icons.*_outlined` 的视觉）。
 *
 * 与 [HomeIcons]（Material 实心路径）分开的原因：
 * 1. 描边图标直接用 `stroke` 画开放路径，不必处理「外轮廓 + 反向内轮廓」的
 *    NonZero 挖洞问题，24×24 视口下像素位置可控；
 * 2. Flutter SoT 的首页服务九宫格、门店选择行用的都是 outlined 变体，
 *    实心 Material 路径视觉偏重，与截图对不上。
 *
 * 约束沿用 [HomeIcons]：全部手写 `ImageVector`，不引入 material-icons-extended，
 * 保证 OHOS（ohosArm64/ohosX64）可编译。
 */
object HomeStrokeIcons {
    /** 门店图标（`Icons.storefront_rounded` 的描边等价物）。 */
    val Storefront: ImageVector by lazy { storefront() }

    /** 下拉箭头（`Icons.keyboard_arrow_down_rounded`）。 */
    val ArrowDown: ImageVector by lazy { arrowDown() }

    /** 换门店（`Icons.swap_horiz_rounded`）。 */
    val SwapHoriz: ImageVector by lazy { swapHoriz() }

    /** 朋友圈（`Icons.photo_camera_outlined`）。 */
    val PhotoCamera: ImageVector by lazy { photoCamera() }

    /** 视频号（`Icons.videocam_outlined`）。 */
    val Videocam: ImageVector by lazy { videocam() }

    /** 直播（`Icons.live_tv_outlined`）。 */
    val LiveTv: ImageVector by lazy { liveTv() }

    /** 素材库（`Icons.photo_library_outlined`）。 */
    val PhotoLibrary: ImageVector by lazy { photoLibrary() }

    /** 培训（`Icons.school_outlined`）。 */
    val School: ImageVector by lazy { school() }

    /** 竞品分析（`Icons.insights_outlined`）。 */
    val Insights: ImageVector by lazy { insights() }

    /** 更多 / 全部服务（`Icons.apps_rounded` 的描边等价物）。 */
    val Apps: ImageVector by lazy { apps() }

    /** 车辆（首页「二手车 / 新车成交」）。 */
    val Car: ImageVector by lazy { car() }

    /** 代码调试（首页「H5 调试」）。 */
    val Code: ImageVector by lazy { code() }

    /** 按后端 `icon` key 取服务图标；未知 key 回退到按文案映射，仍未知用「更多」。 */
    fun forServiceIcon(icon: String, label: String): ImageVector = when (icon) {
        "moments" -> PhotoCamera
        "channels" -> Videocam
        "live" -> LiveTv
        "material" -> PhotoLibrary
        "scripts" -> HomeIcons.ChatBubble
        "training" -> School
        "competitor" -> Insights
        "more" -> Apps
        else -> forServiceLabel(label)
    }

    /** 按首页功能图标 key 取矢量图（服务端下发 icon 字段）。 */
    fun forFeatureIcon(icon: String, label: String): ImageVector = when (icon) {
        "h5" -> Code
        "life" -> Storefront
        "used_car", "new_car_deal" -> Car
        "new_car_follow" -> HomeIcons.Phone
        "ai_stone" -> HomeIcons.ChatBubble
        "club" -> HomeIcons.PersonOutline
        "live" -> LiveTv
        "more" -> Apps
        else -> forFeatureLabel(label)
    }

    /** 按功能名兜底（顺序与首页宫格一致）。 */
    fun forFeatureLabel(label: String): ImageVector = when (label) {
        "H5 调试" -> Code
        "生活服务" -> Storefront
        "二手车", "新车成交" -> Car
        "新车跟进" -> HomeIcons.Phone
        "AI小石头" -> HomeIcons.ChatBubble
        "Club" -> HomeIcons.PersonOutline
        "直播带货" -> LiveTv
        else -> Apps
    }

    /** 按首页服务名取图标（Flutter `_ServiceGridItem._icon` 的 switch 同序）。 */
    fun forServiceLabel(label: String): ImageVector = when (label) {
        "朋友圈" -> PhotoCamera
        "视频号" -> Videocam
        "直播" -> LiveTv
        "素材库" -> PhotoLibrary
        "话术库" -> HomeIcons.ChatBubble
        "培训" -> School
        "竞品分析" -> Insights
        else -> Apps
    }

    // ── 构造器 ────────────────────────────────────────────────────────────────

    private const val STROKE = 1.7f

    /** 单条描边路径 → 24×24 ImageVector。 */
    private fun stroked(block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                fillAlpha = 1f,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 1f,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4f,
                pathBuilder = block,
            )
        }.build()

    /** 圆角矩形轮廓（描边不能用 arcTo 的简写，这里用四段 curve 近似 r 半径）。 */
    private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
        moveTo(l + rad, t)
        horizontalLineTo(r - rad)
        curveToRelative(rad, 0f, rad, 0f, rad, rad)
        verticalLineTo(b - rad)
        curveToRelative(0f, rad, 0f, rad, -rad, rad)
        horizontalLineTo(l + rad)
        curveToRelative(-rad, 0f, -rad, 0f, -rad, -rad)
        verticalLineTo(t + rad)
        curveToRelative(0f, -rad, 0f, -rad, rad, -rad)
        close()
    }

    // ── 图标 ──────────────────────────────────────────────────────────────────

    private fun storefront() = stroked {
        // 遮阳棚
        moveTo(3.5f, 9.5f)
        verticalLineTo(8f)
        lineTo(5f, 4.5f)
        horizontalLineTo(19f)
        lineTo(20.5f, 8f)
        verticalLineTo(9.5f)
        // 棚下三个波浪
        moveTo(3.5f, 9.5f)
        curveToRelative(0f, 1.4f, 1.1f, 2.2f, 2.4f, 2.2f)
        curveToRelative(1.3f, 0f, 2.4f, -0.8f, 2.4f, -2.2f)
        curveToRelative(0f, 1.4f, 1.1f, 2.2f, 2.4f, 2.2f)
        curveToRelative(1.3f, 0f, 2.4f, -0.8f, 2.4f, -2.2f)
        curveToRelative(0f, 1.4f, 1.1f, 2.2f, 2.4f, 2.2f)
        curveToRelative(1.3f, 0f, 2.4f, -0.8f, 2.4f, -2.2f)
        curveToRelative(0f, 1.4f, 1.1f, 2.2f, 2.4f, 2.2f)
        // 店体
        moveTo(4.8f, 12.4f)
        verticalLineTo(19.5f)
        horizontalLineTo(19.2f)
        verticalLineTo(12.4f)
        // 门
        moveTo(10f, 19.5f)
        verticalLineTo(15f)
        horizontalLineTo(14f)
        verticalLineTo(19.5f)
    }

    private fun arrowDown() = stroked {
        moveTo(7.5f, 10f)
        lineTo(12f, 14.5f)
        lineTo(16.5f, 10f)
    }

    private fun swapHoriz() = stroked {
        moveTo(4f, 9f)
        horizontalLineTo(17f)
        moveTo(14f, 6f)
        lineTo(17f, 9f)
        lineTo(14f, 12f)
        moveTo(20f, 15f)
        horizontalLineTo(7f)
        moveTo(10f, 12f)
        lineTo(7f, 15f)
        lineTo(10f, 18f)
    }

    private fun photoCamera() = stroked {
        // 机身
        roundRect(2.5f, 7f, 21.5f, 19.5f, 2.6f)
        // 顶部取景凸台
        moveTo(8.6f, 7f)
        lineTo(9.8f, 4.8f)
        horizontalLineTo(14.2f)
        lineTo(15.4f, 7f)
        // 镜头
        moveTo(16f, 13.2f)
        curveToRelative(0f, 2.2f, -1.8f, 4f, -4f, 4f)
        curveToRelative(-2.2f, 0f, -4f, -1.8f, -4f, -4f)
        curveToRelative(0f, -2.2f, 1.8f, -4f, 4f, -4f)
        curveToRelative(2.2f, 0f, 4f, 1.8f, 4f, 4f)
        close()
    }

    private fun videocam() = stroked {
        roundRect(2.5f, 7f, 15.5f, 17.5f, 2.6f)
        moveTo(15.5f, 11.4f)
        lineTo(21.5f, 8.4f)
        verticalLineTo(16.1f)
        lineTo(15.5f, 13.1f)
    }

    private fun liveTv() = stroked {
        roundRect(2.5f, 7.5f, 21.5f, 19f, 2.4f)
        // 直播信号点
        moveTo(14.2f, 13.25f)
        curveToRelative(0f, 1.2f, -1f, 2.2f, -2.2f, 2.2f)
        curveToRelative(-1.2f, 0f, -2.2f, -1f, -2.2f, -2.2f)
        curveToRelative(0f, -1.2f, 1f, -2.2f, 2.2f, -2.2f)
        curveToRelative(1.2f, 0f, 2.2f, 1f, 2.2f, 2.2f)
        close()
        // 天线
        moveTo(7f, 7.5f)
        lineTo(9.6f, 4.4f)
        moveTo(17f, 7.5f)
        lineTo(14.4f, 4.4f)
    }

    private fun photoLibrary() = stroked {
        // 后面那张
        moveTo(7.4f, 3.5f)
        horizontalLineTo(19.4f)
        curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
        verticalLineTo(17.4f)
        // 前面那张
        roundRect(2.6f, 7.2f, 16.6f, 20.5f, 2f)
        // 山水
        moveTo(4.6f, 17.6f)
        lineTo(8.4f, 13.6f)
        lineTo(11.4f, 16.6f)
        lineTo(13.2f, 14.8f)
        lineTo(14.8f, 17.6f)
    }

    private fun school() = stroked {
        // 学位帽
        moveTo(12f, 4.2f)
        lineTo(21.5f, 8.6f)
        lineTo(12f, 13f)
        lineTo(2.5f, 8.6f)
        close()
        // 帽下袍体
        moveTo(6.6f, 10.6f)
        verticalLineTo(14.8f)
        curveToRelative(0f, 1.9f, 2.4f, 3.4f, 5.4f, 3.4f)
        curveToRelative(3f, 0f, 5.4f, -1.5f, 5.4f, -3.4f)
        verticalLineTo(10.6f)
        // 流苏
        moveTo(21.5f, 8.6f)
        verticalLineTo(13.6f)
    }

    private fun insights() = stroked {
        // 折线
        moveTo(3f, 17.6f)
        lineTo(9f, 11.6f)
        lineTo(12.6f, 15.2f)
        lineTo(20.4f, 7.4f)
        // 箭头
        moveTo(15.6f, 7.4f)
        horizontalLineTo(20.4f)
        verticalLineTo(12.2f)
        // 基线
        moveTo(3f, 20.6f)
        horizontalLineTo(21f)
    }

    private fun apps() = stroked {
        roundRect(3.2f, 3.2f, 9.2f, 9.2f, 1.6f)
        roundRect(14.8f, 3.2f, 20.8f, 9.2f, 1.6f)
        roundRect(3.2f, 14.8f, 9.2f, 20.8f, 1.6f)
        roundRect(14.8f, 14.8f, 20.8f, 20.8f, 1.6f)
    }

    private fun car() = stroked {
        // 车身
        moveTo(3.2f, 16.2f)
        verticalLineTo(12.4f)
        lineTo(6.2f, 12.4f)
        lineTo(8.4f, 7.8f)
        lineTo(15.6f, 7.8f)
        lineTo(17.8f, 12.4f)
        horizontalLineTo(20.8f)
        verticalLineTo(16.2f)
        // 车窗
        moveTo(9f, 12.1f)
        lineTo(10.3f, 9.2f)
        horizontalLineTo(13.7f)
        lineTo(15f, 12.1f)
        // 前轮
        moveTo(6.4f, 16.6f)
        curveToRelative(0f, 1.1f, -0.9f, 1.9f, -1.9f, 1.9f)
        curveToRelative(-1f, 0f, -1.9f, -0.8f, -1.9f, -1.9f)
        // 后轮
        moveTo(21.4f, 16.6f)
        curveToRelative(0f, 1.1f, -0.9f, 1.9f, -1.9f, 1.9f)
        curveToRelative(-1f, 0f, -1.9f, -0.8f, -1.9f, -1.9f)
    }

    private fun code() = stroked {
        // 显示器
        roundRect(3f, 4.5f, 21f, 16.5f, 2.2f)
        // 底座
        moveTo(12f, 16.5f)
        verticalLineTo(20.2f)
        moveTo(8.5f, 20.2f)
        horizontalLineTo(15.5f)
        // 尖括号
        moveTo(9.6f, 8.6f)
        lineTo(7.2f, 10.8f)
        lineTo(9.6f, 13f)
        moveTo(14.4f, 8.6f)
        lineTo(16.8f, 10.8f)
        lineTo(14.4f, 13f)
    }
}
