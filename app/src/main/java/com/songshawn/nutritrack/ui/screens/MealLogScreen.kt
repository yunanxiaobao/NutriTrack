package com.songshawn.nutritrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.songshawn.nutritrack.data.models.HeifaFoodGroup
import com.songshawn.nutritrack.data.models.MealType
import com.songshawn.nutritrack.data.room.entity.DailyScoreEntity
import com.songshawn.nutritrack.data.room.entity.FoodItemEntity
import com.songshawn.nutritrack.data.room.entity.MealItemWithFood
import com.songshawn.nutritrack.data.room.entity.MealWithItems
import com.songshawn.nutritrack.theme.MediumGreen
import com.songshawn.nutritrack.ui.components.BottomNavigationBar
import com.songshawn.nutritrack.util.getScoreColor
import com.songshawn.nutritrack.viewmodel.MealLogUiState
import com.songshawn.nutritrack.viewmodel.MealLogViewModel
import kotlin.math.roundToInt

@Composable
fun MealLogScreen(
    userId: String,
    navController: NavController,
    viewModel: MealLogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.bindUser(userId)
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = navController.currentBackStackEntry?.destination?.route ?: "",
                navController = navController,
            )
        },
    ) { innerPadding ->
        MealLogContent(
            innerPadding = innerPadding,
            uiState = uiState,
            onShiftDate = viewModel::shiftDateByDays,
            onSelectMealType = viewModel::selectMealType,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSelectCategory = viewModel::selectCategory,
            onAddFood = { foodId -> viewModel.addFoodToSelectedMeal(foodId, 1.0) },
            onAdjustServings = viewModel::adjustItemServings,
            onRemoveItem = viewModel::removeItem,
        )
    }
}

@Composable
private fun MealLogContent(
    innerPadding: PaddingValues,
    uiState: MealLogUiState,
    onShiftDate: (Long) -> Unit,
    onSelectMealType: (MealType) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (HeifaFoodGroup?) -> Unit,
    onAddFood: (String) -> Unit,
    onAdjustServings: (Long, Double) -> Unit,
    onRemoveItem: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            DateSelectorHeader(
                date = uiState.date,
                onPreviousDay = { onShiftDate(-1L) },
                onNextDay = { onShiftDate(1L) },
            )
        }

        item {
            DailyScoreSummaryBanner(dailyScore = uiState.dailyScore)
        }

        item {
            MealSlotSelector(
                selected = uiState.selectedMealType,
                onSelect = onSelectMealType,
            )
        }

        if (uiState.mealsForDay.any { it.items.isNotEmpty() }) {
            item {
                LoggedMealsCard(
                    meals = uiState.mealsForDay,
                    onAdjustServings = onAdjustServings,
                    onRemoveItem = onRemoveItem,
                )
            }
        }

        item {
            FoodSearchHeader(
                selectedMealType = uiState.selectedMealType,
                searchQuery = uiState.searchQuery,
                selectedCategory = uiState.selectedCategory,
                onSearchQueryChange = onSearchQueryChange,
                onSelectCategory = onSelectCategory,
            )
        }

        items(
            items = uiState.catalogResults,
            key = { it.id },
        ) { food ->
            FoodCatalogItemCard(
                food = food,
                mealType = uiState.selectedMealType,
                onAdd = { onAddFood(food.id) },
            )
        }
    }
}

@Composable
private fun DateSelectorHeader(
    date: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Daily Food Log",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Log meals to compute your live HEIFA nutrition score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousDay) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Text(
                text = date,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNextDay) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
            }
        }
    }
}

@Composable
private fun DailyScoreSummaryBanner(dailyScore: DailyScoreEntity?) {
    val score = dailyScore?.heifaTotalScore ?: 0.0
    val calories = dailyScore?.totalCaloriesKcal?.roundToInt() ?: 0
    val protein = dailyScore?.totalProteinG?.roundToInt() ?: 0
    val carbs = dailyScore?.totalCarbsG?.roundToInt() ?: 0
    val fat = dailyScore?.totalFatG?.roundToInt() ?: 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(getScoreColor(score.toFloat() / 100f).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${score.roundToInt()}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = getScoreColor(score.toFloat() / 100f),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HEIFA Score / 100",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "$calories kcal • ${protein}g P • ${carbs}g C • ${fat}g F",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MealSlotSelector(
    selected: MealType,
    onSelect: (MealType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MealType.entries.forEach { mealType ->
            FilterChip(
                selected = mealType == selected,
                onClick = { onSelect(mealType) },
                label = { Text(mealType.displayName) },
            )
        }
    }
}

@Composable
private fun LoggedMealsCard(
    meals: List<MealWithItems>,
    onAdjustServings: (Long, Double) -> Unit,
    onRemoveItem: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Logged Today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))

            meals.filter { it.items.isNotEmpty() }.forEachIndexed { index, mealWithItems ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Text(
                    text = "${mealWithItems.meal.mealType.displayName} (${mealWithItems.totalCaloriesKcal.roundToInt()} kcal)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MediumGreen,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                mealWithItems.items.forEach { entry ->
                    LoggedFoodItemRow(
                        entry = entry,
                        onAdjustServings = onAdjustServings,
                        onRemoveItem = onRemoveItem,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoggedFoodItemRow(
    entry: MealItemWithFood,
    onAdjustServings: (Long, Double) -> Unit,
    onRemoveItem: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.food.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${entry.item.servings}x (${entry.food.servingSizeDesc}) • ${entry.totalCaloriesKcal.roundToInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = { onAdjustServings(entry.item.id, entry.item.servings - 0.5) },
                label = { Text("-0.5") },
            )
            Spacer(modifier = Modifier.width(4.dp))
            AssistChip(
                onClick = { onAdjustServings(entry.item.id, entry.item.servings + 0.5) },
                label = { Text("+0.5") },
            )
            IconButton(onClick = { onRemoveItem(entry.item.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun FoodSearchHeader(
    selectedMealType: MealType,
    searchQuery: String,
    selectedCategory: HeifaFoodGroup?,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (HeifaFoodGroup?) -> Unit,
) {
    Column {
        Text(
            text = "Add to ${selectedMealType.displayName} (FTS4 Search)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search food catalog (e.g. salmon, oats, spinach)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") },
            )
            HeifaFoodGroup.entries.forEach { group ->
                FilterChip(
                    selected = selectedCategory == group,
                    onClick = { onSelectCategory(group) },
                    label = { Text(group.displayName) },
                )
            }
        }
    }
}

@Composable
private fun FoodCatalogItemCard(
    food: FoodItemEntity,
    mealType: MealType,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${food.category.displayName} • ${food.servingSizeDesc}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${food.caloriesKcal.roundToInt()} kcal • " +
                        "P ${food.proteinG}g • C ${food.carbsG}g • F ${food.fatG}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGreen,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = MediumGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to ${mealType.displayName}",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log")
            }
        }
    }
}
