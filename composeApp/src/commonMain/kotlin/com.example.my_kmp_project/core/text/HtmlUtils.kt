package com.example.my_kmp_project.core.text

private val imgSrcRegex = Regex(
    """<img[^>]+src\s*=\s*["']([^"']+)["']""",
    RegexOption.IGNORE_CASE,
)

fun stripHtmlRough(html: String): String =
    html
        .replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
        .replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()

fun extractImageUrlsFromHtml(html: String): List<String> =
    imgSrcRegex.findAll(html)
        .mapNotNull { it.groupValues.getOrNull(1)?.trim()?.takeIf { u -> u.isNotEmpty() } }
        .toList()
