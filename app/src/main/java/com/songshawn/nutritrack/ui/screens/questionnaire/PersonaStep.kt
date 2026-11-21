package com.songshawn.nutritrack.ui.screens.questionnaire

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.data.models.Persona
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.theme.MediumGreen

/**
 * Persona selection screen with adaptive layout for different orientations
 *
 * @param selectedPersona The currently selected persona
 * @param onPersonaSelected Callback when a persona is selected
 */
@Composable
fun PersonaPage(
    selectedPersona: Persona?,
    onPersonaSelected: (Persona?) -> Unit,
) {
    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Render different layouts based on orientation
    if (isLandscape) {
        LandscapePersonaLayout(
            selectedPersona = selectedPersona,
            onPersonaSelected = onPersonaSelected
        )
    } else {
        PortraitPersonaLayout(
            selectedPersona = selectedPersona,
            onPersonaSelected = onPersonaSelected
        )
    }
}

/**
 * Portrait layout for the persona screen - vertical arrangement
 */
@Composable
private fun PortraitPersonaLayout(
    selectedPersona: Persona?,
    onPersonaSelected: (Persona?) -> Unit
) {
    // Scroll state for the content
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .offset(y = 12.dp),
    ) {
        // Header section with title and description
        PersonaHeader()

        // Scrollable content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PersonaSelector(
                selectedPersona = selectedPersona,
                onPersonaSelected = onPersonaSelected
            )

            // Bottom space to prevent content from being covered by navigation
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Landscape layout - header on left, full surface selector on right
 */
@Composable
private fun LandscapePersonaLayout(
    selectedPersona: Persona?,
    onPersonaSelected: (Persona?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Persona header
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.personaScreen_title),
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.personaScreen_description),
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Right side: Full persona selector surface - entire screen height
        Surface(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            color = LightGreen,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                PersonaSelector(
                    selectedPersona = selectedPersona,
                    onPersonaSelected = onPersonaSelected,
                    isLandscape = true
                )
            }
        }
    }
}

/**
 * Header section for the persona page (portrait only)
 */
@Composable
private fun PersonaHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .offset(y = (-22).dp)  // Negative vertical offset to move up
    ) {
        Text(
            text = stringResource(R.string.personaScreen_title),
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            fontSize = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.personaScreen_description),
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily(Font(R.font.poppins)),
            fontSize = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Main persona selection component
 * Adapts layout based on orientation
 */
@Composable
fun PersonaSelector(
    selectedPersona: Persona?,
    onPersonaSelected: (Persona?) -> Unit,
    isLandscape: Boolean = false
) {
    // Get all persona options
    val personas = Persona.entries.toList()

    // Control card expansion state
    var expanded by remember { mutableStateOf(false) }

    // In landscape mode, don't wrap in a card since we're already in a surface
    if (isLandscape) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Title
            Text(
                text = stringResource(R.string.personaScreen_selectPersona),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Persona selection grid
            PersonaGrid(
                personas = personas,
                selectedPersona = selectedPersona,
                onPersonaClicked = { persona ->
                    if (selectedPersona == persona) {
                        // If clicking the already selected persona
                        if (expanded) {
                            // If expanded, collapse and deselect
                            expanded = false
                            onPersonaSelected(null)
                        } else {
                            // If collapsed, expand
                            expanded = true
                        }
                    } else {
                        // Clicking a new persona, select and expand
                        onPersonaSelected(persona)
                        expanded = true
                    }
                }
            )

            // Show details if a persona is selected and expanded
            if (selectedPersona != null && expanded) {
                Spacer(modifier = Modifier.height(32.dp))

                // Detail title
                Text(
                    text = selectedPersona.displayName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    color = MediumGreen,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Detail content card
                PersonaDetailCard(persona = selectedPersona)
            }
        }
    } else {
        // Portrait mode - wrap in card as before
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            colors = CardDefaults.cardColors(containerColor = LightGreen),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.personaScreen_selectPersona),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Persona selection grid
                PersonaGrid(
                    personas = personas,
                    selectedPersona = selectedPersona,
                    onPersonaClicked = { persona ->
                        if (selectedPersona == persona) {
                            // If clicking the already selected persona
                            if (expanded) {
                                // If expanded, collapse and deselect
                                expanded = false
                                onPersonaSelected(null)
                            } else {
                                // If collapsed, expand
                                expanded = true
                            }
                        } else {
                            // Clicking a new persona, select and expand
                            onPersonaSelected(persona)
                            expanded = true
                        }
                    }
                )

                // Show details if a persona is selected and expanded
                if (selectedPersona != null && expanded) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Detail title
                    Text(
                        text = selectedPersona.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily(Font(R.font.poppins)),
                        color = MediumGreen,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail content card
                    PersonaDetailCard(persona = selectedPersona)
                }
            }
        }
    }
}

/**
 * Grid of persona selection buttons
 */
@Composable
fun PersonaGrid(
    personas: List<Persona>,
    selectedPersona: Persona?,
    onPersonaClicked: (Persona) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp),) {
        // First row with 3 buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            personas.take(3).forEach { persona ->
                PersonaButton(
                    persona = persona,
                    isSelected = selectedPersona == persona,
                    onClick = { onPersonaClicked(persona) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                )
            }
        }

        // Second row with 3 buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            personas.drop(3).forEach { persona ->
                PersonaButton(
                    persona = persona,
                    isSelected = selectedPersona == persona,
                    onClick = { onPersonaClicked(persona) },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                )
            }
        }
    }
}

/**
 * Individual persona selection button
 */
@Composable
fun PersonaButton(
    persona: Persona,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MediumGreen else Color.LightGray
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = persona.displayName,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.poppins)),
                maxLines = 2,
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Detailed card showing information about the selected persona
 */
@Composable
fun PersonaDetailCard(persona: Persona) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image
        Image(
            painter = painterResource(id = persona.imageRes),
            contentDescription = persona.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = persona.description,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}