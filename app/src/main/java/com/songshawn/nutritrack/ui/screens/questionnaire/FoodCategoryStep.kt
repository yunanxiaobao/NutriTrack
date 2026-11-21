package com.songshawn.nutritrack.ui.screens.questionnaire

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.songshawn.nutritrack.data.models.FoodCategory
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.theme.MediumGreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable

/**
 * Food Categories Page with adaptive layout for different orientations
 * Supports both list view (portrait) and grid view (landscape)
 */
@Composable
fun FoodCategoriesPage(
    selected: Set<FoodCategory>,
    onToggleCategory: (FoodCategory) -> Unit,
) {
    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Render different layouts based on orientation
    if (isLandscape) {
        LandscapeFoodCategoriesLayout(
            selected = selected,
            onToggleCategory = onToggleCategory
        )
    } else {
        PortraitFoodCategoriesLayout(
            selected = selected,
            onToggleCategory = onToggleCategory
        )
    }
}

/**
 * Portrait layout - vertical list with full titles and descriptions
 */
@Composable
private fun PortraitFoodCategoriesLayout(
    selected: Set<FoodCategory>,
    onToggleCategory: (FoodCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title and description section
        FoodCategoriesHeader(isLandscape = false)

        // Food categories card with list view
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = LightGreen,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp
            ) {
                // Food categories list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(FoodCategory.entries.toList()) { category ->
                        FoodItemList(
                            category = category,
                            isSelected = selected.contains(category),
                            onToggle = { onToggleCategory(category) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Landscape layout - header on left, full surface list on right
 */
@Composable
private fun LandscapeFoodCategoriesLayout(
    selected: Set<FoodCategory>,
    onToggleCategory: (FoodCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left side: Food categories header
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
                text = stringResource(R.string.foodCategoriesScreen_title),
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.foodCategoriesScreen_description),
                fontSize = 13.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Right side: Full food categories surface - entire screen height
        Surface(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight(),
            color = LightGreen,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 2.dp
        ) {
            // Food categories list - single column, fills entire surface
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(FoodCategory.entries.toList()) { category ->
                    FoodItemList(
                        category = category,
                        isSelected = selected.contains(category),
                        onToggle = { onToggleCategory(category) }
                    )
                }
            }
        }
    }
}

/**
 * Header section with title and description
 * Adapts content based on orientation
 */
@Composable
private fun FoodCategoriesHeader(isLandscape: Boolean) {
    // Adjust sizing and alignment for orientation
    val titleFontSize = if (isLandscape) 18.sp else 20.sp
    val descriptionFontSize = if (isLandscape) 13.sp else 15.sp
    val alignment = if (isLandscape) TextAlign.Center else TextAlign.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = if (isLandscape) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = stringResource(R.string.foodCategoriesScreen_title),
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily(Font(R.font.poppins)),
            fontSize = titleFontSize,
            textAlign = alignment,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.foodCategoriesScreen_description),
            fontSize = descriptionFontSize,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily(Font(R.font.poppins)),
            textAlign = alignment,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Food item for list view (portrait mode)
 * Full-width cards with image, text, and checkmark
 */
@Composable
private fun FoodItemList(
    category: FoodCategory,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = category.imageRes),
                    contentDescription = category.displayName,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 12.dp)
                )

                Text(
                    text = category.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    color = if (isSelected) MediumGreen else Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Show checkmark if selected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MediumGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Food item for grid view (landscape mode)
 * Compact cards optimized for grid layout
 */
@Composable
private fun FoodItemGrid(
    category: FoodCategory,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f) // Slightly rectangular for better text visibility
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Image at the top
            Image(
                painter = painterResource(id = category.imageRes),
                contentDescription = category.displayName,
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 8.dp)
            )

            // Category name (multi-line support)
            Text(
                text = category.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily(Font(R.font.poppins)),
                color = if (isSelected) MediumGreen else Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )

            // Checkmark at the bottom (if selected)
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MediumGreen,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 4.dp)
                )
            } else {
                // Spacer to maintain consistent height
                Spacer(modifier = Modifier.size(18.dp).padding(top = 4.dp))
            }
        }
    }
}