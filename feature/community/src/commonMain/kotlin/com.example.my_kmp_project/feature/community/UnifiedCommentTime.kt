package com.example.my_kmp_project.feature.community

/**
 * 评论相对时间。
 *
 * 只接受服务端解析出的 epoch 毫秒；[createdAtMillis] 为 null（缺键 / 非法时间）时返回 null，
 * 调用方**不展示时间**，绝不回落到「现在」。未来时间按 0 处理（只影响相对展示，不伪造日期）。
 */
internal fun formatCommentRelativeTime(createdAtMillis: Long?, nowMillis: Long): String? {
    if (createdAtMillis == null) return null
    val diff = (nowMillis - createdAtMillis).coerceAtLeast(0L)
    val seconds = diff / 1000
    return when {
        seconds < 60 -> "刚刚"
        seconds < 60 * 60 -> "${seconds / 60}分钟前"
        seconds < 24 * 60 * 60 -> "${seconds / (60 * 60)}小时前"
        seconds < 30L * 24 * 60 * 60 -> "${seconds / (24 * 60 * 60)}天前"
        seconds < 365L * 24 * 60 * 60 -> "${seconds / (30L * 24 * 60 * 60)}个月前"
        else -> "${seconds / (365L * 24 * 60 * 60)}年前"
    }
}
