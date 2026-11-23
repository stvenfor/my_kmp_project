package com.example.my_kmp_project.feature.community

/**
 * 统计 Unicode 码点（Go `utf8.RuneCountInString` 口径），用于 1..2000 的评论长度校验。
 *
 * Kotlin `String.length` 是 UTF-16 单元数：一个 emoji 记 2，会在 1000 字时误判超限。
 * 这里只把高位代理项与其后的低位代理项合并成 1 个码点，不引入字素簇依赖。
 */
public fun countCommentCodePoints(text: String): Int {
    var count = 0
    var index = 0
    while (index < text.length) {
        val char = text[index]
        val isHighSurrogate = char in '\uD800'..'\uDBFF'
        if (isHighSurrogate &&
            index + 1 < text.length &&
            text[index + 1] in '\uDC00'..'\uDFFF'
        ) {
            index += 2
        } else {
            index += 1
        }
        count += 1
    }
    return count
}
