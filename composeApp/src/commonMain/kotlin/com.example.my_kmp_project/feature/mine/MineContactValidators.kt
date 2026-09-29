package com.example.my_kmp_project.feature.mine

/** Flutter `WysContactValidators` — shared name / CN mobile rules. */
internal object MineContactValidators {
    const val MaxNameLength = 12
    const val MaxCnMobileLength = 11

    fun digitsOnly(input: String): String = input.filter { it.isDigit() }

    fun isNameValid(name: String): Boolean {
        val t = name.trim()
        return t.isNotEmpty() && t.length <= MaxNameLength
    }

    fun isCnMobile(phone: String): Boolean {
        val digits = digitsOnly(phone)
        return digits.length == MaxCnMobileLength && digits.startsWith('1')
    }

    fun maskPhone(phone: String?): String {
        val d = digitsOnly(phone.orEmpty())
        if (d.length < 7) return phone?.takeIf { it.isNotBlank() } ?: "—"
        return "${d.take(3)}****${d.takeLast(4)}"
    }
}
