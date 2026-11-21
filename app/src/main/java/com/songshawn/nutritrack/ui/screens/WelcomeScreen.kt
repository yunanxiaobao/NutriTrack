package com.songshawn.nutritrack.ui.screens

import android.content.res.Configuration
import com.songshawn.nutritrack.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songshawn.nutritrack.theme.MediumGreen
import com.songshawn.nutritrack.viewmodel.WelcomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Welcome screen for the app - now with proper rotation support
 *
 * @param viewModel The welcome view model
 * @param onContinue Callback to navigate to login screen
 */
@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    onContinue: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val configuration = LocalConfiguration.current
    val scrollState = rememberScrollState()

    // Load strings
    val appName        = stringResource(R.string.app_name)
    val disclaimer     = stringResource(R.string.disclaimer)
    val disclaimerNote = stringResource(R.string.disclaimer_note)
    val clinicUrl      = stringResource(R.string.clinic_url)
    val authorName     = stringResource(R.string.author_name)
    val designedBy     = stringResource(R.string.designed_by, authorName)

    val shouldNavigate by viewModel.navigateToLogin.observeAsState(initial = false)

    LaunchedEffect(shouldNavigate) {
        if (shouldNavigate) {
            onContinue()
            viewModel.onNavigationComplete()
        }
    }

    // 检测是否是横屏模式
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) { innerPadding ->
        if (isLandscape) {
            LandscapeLayout(
                innerPadding = innerPadding,
                appName = appName,
                disclaimer = disclaimer,
                disclaimerNote = disclaimerNote,
                clinicUrl = clinicUrl,
                uriHandler = uriHandler,
                onLogin = { viewModel.onLoginClicked() },
                designedBy = designedBy
            )
        } else {
            PortraitLayout(
                innerPadding = innerPadding,
                scrollState = scrollState,
                appName = appName,
                disclaimer = disclaimer,
                disclaimerNote = disclaimerNote,
                clinicUrl = clinicUrl,
                uriHandler = uriHandler,
                onLogin = { viewModel.onLoginClicked() },
                designedBy = designedBy
            )
        }
    }
}

@Composable
private fun LandscapeLayout(
    innerPadding: PaddingValues,
    appName: String,
    disclaimer: String,
    disclaimerNote: String,
    clinicUrl: String,
    uriHandler: UriHandler,
    onLogin: () -> Unit,
    designedBy: String
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoSection(
                appName = appName,
                imageSize = 120.dp,
                fontSize = 24.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1.2f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DisclaimerSection(
                disclaimer = disclaimer,
                disclaimerNote = disclaimerNote,
                clinicUrl = clinicUrl,
                uriHandler = uriHandler,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            FooterSection(
                onLogin = onLogin,
                designedBy = designedBy,
                showDesignedBy = false
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    innerPadding: PaddingValues,
    scrollState: ScrollState,
    appName: String,
    disclaimer: String,
    disclaimerNote: String,
    clinicUrl: String,
    uriHandler: UriHandler,
    onLogin: () -> Unit,
    designedBy: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.1f))

        LogoSection(
            appName = appName,
            imageSize = 200.dp,
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.weight(0.3f))

        DisclaimerSection(
            disclaimer = disclaimer,
            disclaimerNote = disclaimerNote,
            clinicUrl = clinicUrl,
            uriHandler = uriHandler,
            showDivider = true
        )

        Spacer(modifier = Modifier.weight(0.1f))

        FooterSection(
            onLogin = onLogin,
            designedBy = designedBy,
            showDesignedBy = true
        )
    }
}

@Composable
private fun LogoSection(
    appName: String,
    imageSize: Dp = 200.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(imageSize)
                .padding(vertical = 16.dp)
        )
        Text(
            text = appName,
            fontFamily = FontFamily(Font(R.font.poppins)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4C7040),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DisclaimerSection(
    disclaimer: String,
    disclaimerNote: String,
    clinicUrl: String,
    uriHandler: UriHandler,
    showDivider: Boolean = true
) {
    val urlPrompt = stringResource(R.string.visit_clinic_prompt)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .alpha(0.7f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = disclaimer,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = disclaimerNote,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = buildAnnotatedString {
                append("$urlPrompt ")
                withStyle(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(stringResource(R.string.clinic_url))
                }
            },
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clickable { uriHandler.openUri(clinicUrl) }
        )
    }
}

@Composable
private fun FooterSection(
    onLogin: () -> Unit,
    designedBy: String,
    showDesignedBy: Boolean = true
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MediumGreen,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Explore",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.monaco)),
            )
        }

        if (showDesignedBy) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = designedBy,
                fontFamily = FontFamily(Font(R.font.poppins)),
                fontSize = 12.sp,
                color = Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}