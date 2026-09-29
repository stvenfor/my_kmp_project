package com.example.my_kmp_project.core.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.geist_variable
import org.jetbrains.compose.resources.Font

/**
 * Flutter SoT uses VercelTypography.fontFamily = Geist (variable).
 * Bundle the same TTF so text-heavy Android Screenshot Diff Gates can converge.
 */
internal object DemoFonts {
    val Geist: FontFamily
        @Composable
        get() = FontFamily(
            Font(Res.font.geist_variable, weight = FontWeight.Normal),
            Font(Res.font.geist_variable, weight = FontWeight.Medium),
            Font(Res.font.geist_variable, weight = FontWeight.SemiBold),
            Font(Res.font.geist_variable, weight = FontWeight.Bold),
        )
}
