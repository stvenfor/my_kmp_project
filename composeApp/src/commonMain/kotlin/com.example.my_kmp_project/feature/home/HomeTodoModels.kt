package com.example.my_kmp_project.feature.home

/**
 * Flutter `home_todo_models.dart` + `home_todo_packer.dart` port.
 * Card size is derived from [type] (ADR 0009).
 */
internal enum class HomeTodoSize(val cells: Int) {
    Small(1),
    Medium(2),
    Large(4),
    ;

    companion object {
        fun fromType(type: String): HomeTodoSize = when (type) {
            "partner_pending" -> Large
            "follow_up_customer", "after_sales_appointment" -> Medium
            else -> Small
        }
    }
}

internal data class HomeTodoCard(
    val type: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val actionRoute: String,
    val count: Int,
    val imageUrl: String? = null,
) {
    val size: HomeTodoSize get() = HomeTodoSize.fromType(type)
}

/** Flutter `HomeTodoJoinApplication` — partner pending row. */
internal data class HomeTodoJoinApplication(
    val applicationId: Int,
    val storeId: Int,
    val applicantUserId: String,
    val applicantName: String,
    /** 0 = pending, 1 = approved, 2 = rejected */
    val status: Int,
    val createdAtLabel: String,
    val roleLabel: String = "销售顾问",
) {
    val displayName: String
        get() = applicantName.trim().ifEmpty { applicantUserId }

    val isPending: Boolean get() = status == 0
}

/**
 * Pack ordered todo cards into 2×2 pages (capacity 4 cells).
 * Flutter `HomeTodoPacker`.
 */
internal object HomeTodoPacker {
    fun shouldWrapOnly(cards: List<HomeTodoCard>): Boolean {
        if (cards.isEmpty()) return true
        if (cards.size > 2) return false
        return cards.all { it.size == HomeTodoSize.Small }
    }

    fun packPages(cards: List<HomeTodoCard>): List<List<HomeTodoCard>> {
        if (cards.isEmpty()) return emptyList()
        val pages = mutableListOf<List<HomeTodoCard>>()
        var current = mutableListOf<HomeTodoCard>()
        var used = 0

        fun flush() {
            if (current.isEmpty()) return
            pages += current.toList()
            current = mutableListOf()
            used = 0
        }

        for (card in cards) {
            val need = card.size.cells
            if (need == 4 && used > 0) flush()
            if (used + need > 4) flush()
            current += card
            used += need
            if (used >= 4) flush()
        }
        flush()
        return pages
    }
}
