package com.matchbar.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografía de la app. Sin fuentes externas: ajustamos pesos y tracking sobre
 * la familia del sistema para conseguir títulos rotundos y un cuerpo legible,
 * con aire minimalista.
 */
private val base = Typography()

val AppTypography = base.copy(
    displayLarge = base.displayLarge.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displayMedium = base.displayMedium.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    displaySmall = base.displaySmall.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),

    headlineLarge = base.headlineLarge.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.25).sp),
    headlineSmall = base.headlineSmall.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),

    titleLarge = base.titleLarge.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),

    labelLarge = base.labelLarge.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
    labelMedium = base.labelMedium.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    labelSmall = base.labelSmall.copy(
        fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)
