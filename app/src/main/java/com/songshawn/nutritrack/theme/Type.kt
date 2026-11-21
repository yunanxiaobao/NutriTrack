package com.songshawn.nutritrack.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.songshawn.nutritrack.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val poppinsGoogleFont = GoogleFont("Lobster TwoLobster Two")

val fontFamily = FontFamily(
    Font(
        googleFont    = poppinsGoogleFont,
        fontProvider  = googleFontProvider,
        weight        = FontWeight.Normal
    ),
    Font(
        googleFont    = poppinsGoogleFont,
        fontProvider  = googleFontProvider,
        weight        = FontWeight.Medium
    ),
    Font(
        googleFont    = poppinsGoogleFont,
        fontProvider  = googleFontProvider,
        weight        = FontWeight.Bold
    )
)


val MyTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 22.sp
    )
)