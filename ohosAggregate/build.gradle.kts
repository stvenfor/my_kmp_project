/**
 * Placeholder for Spike II OHOS fat-link entry.
 *
 * Today `:composeApp` still owns `libkn.so` linking + Harmony publish tasks.
 * When ready, move OHOS sharedLib / cinterop (resource, avplayer) + export filter
 * here and have this module `api`-depend on `:core:network`, `:core:account`,
 * and remaining feature/component modules.
 *
 * See `docs/architecture/ohos-aggregate.md`.
 */
plugins {
    // Empty placeholder — no targets yet (avoids duplicate OHOS link entry).
}

tasks.register("ohosAggregateStatus") {
    group = "harmony"
    description = "Prints current OHOS aggregate status (placeholder)."
    doLast {
        println(
            """
            :ohosAggregate is a placeholder.
            Active OHOS link entry: :composeApp (publish*BinariesToHarmonyApp)
            Core libs already extractable: :core:network, :core:account
            """.trimIndent(),
        )
    }
}
