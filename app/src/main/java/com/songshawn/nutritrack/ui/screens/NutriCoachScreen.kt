package com.songshawn.nutritrack.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Search
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.songshawn.nutritrack.R
import com.songshawn.nutritrack.data.network.fruityvice.FruitDto
import com.songshawn.nutritrack.theme.LightGreen
import com.songshawn.nutritrack.ui.components.BottomNavigationBar
import com.songshawn.nutritrack.ui.components.GenAIAdviceComponent
import com.songshawn.nutritrack.ui.components.NutriCoachAgentSection
import com.songshawn.nutritrack.ui.components.TipHistoryDialog
import com.songshawn.nutritrack.util.getFruitEmoji
import com.songshawn.nutritrack.viewmodel.ApiResult
import com.songshawn.nutritrack.viewmodel.FruitUiState
import com.songshawn.nutritrack.viewmodel.GenAIViewModel
import com.songshawn.nutritrack.viewmodel.GenAiUiState
import com.songshawn.nutritrack.viewmodel.NutriCoachAgentViewModel
import com.songshawn.nutritrack.viewmodel.NutriCoachViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Objects

/**
 * Main NutriCoach screen with fruit information and GenAI nutrition tips
 * Features adaptive layout for different orientations
 *
 * @param userId ID of the current user
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel for fruit-related data
 * @param genAiViewModel ViewModel for GenAI interactions
 */
@Composable
fun NutriCoachScreen(
    userId: String,
    navController: NavController,
    viewModel: NutriCoachViewModel = hiltViewModel(),
    genAiViewModel: GenAIViewModel = hiltViewModel(),
    agentViewModel: NutriCoachAgentViewModel = hiltViewModel(),
) {
    // State collection
    val fruitScore by viewModel.getFruitScore(userId).collectAsState(initial = 100.0)
    val fruitUiState by viewModel.fruitUiState.collectAsStateWithLifecycle()
    val aiResult by genAiViewModel.state.collectAsState()
    val tipHistory by genAiViewModel.getTipsForUser(userId).collectAsState(initial = emptyList())
    val agentUiState by agentViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        agentViewModel.bindUser(userId)
    }

    val needsImprovement by viewModel.checkIfFruitScoreNeedsImprovement(userId)
        .collectAsState(initial = false)

    // UI state variables
    var showTipHistory by remember { mutableStateOf(false) }
    var fruitCardExpanded by remember { mutableStateOf(false) }
    var localQuery by remember { mutableStateOf("") }

    // Default prompt and suggested prompts
    val defaultPrompt = stringResource(R.string.nutriCoachScreen_defaultPrompt)
    val suggestedPromptsShort = stringArrayResource(id = R.array.fruit_prompts_short).toList()
    val suggestedPromptsFull = stringArrayResource(id = R.array.fruit_prompts_full).toList()

    // Focus and scroll state management
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Check current screen orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Camera integration
    val context = LocalContext.current
    var currentPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isMealPlateCameraCapture by remember { mutableStateOf(false) }

    val identifiedFruit by genAiViewModel.identifiedFruit.collectAsState()
    val genAiUiState by genAiViewModel.ui.collectAsState()

    // Handle GenAI UI States (Loading, Error)
    when (val state = genAiUiState) {
        is GenAiUiState.Error -> {
            AlertDialog(
                onDismissRequest = { genAiViewModel.resetUiState() },
                title = { Text("Error") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { genAiViewModel.resetUiState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is GenAiUiState.Validation -> {
             AlertDialog(
                onDismissRequest = { genAiViewModel.resetUiState() },
                title = { Text("Input Required") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { genAiViewModel.resetUiState() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }

    // Auto-fill search when fruit is identified
    LaunchedEffect(identifiedFruit) {
        identifiedFruit?.let { fruitName ->
            localQuery = fruitName
            viewModel.updateQuery(fruitName)
            viewModel.searchFruit()
            fruitCardExpanded = true
            genAiViewModel.clearIdentifiedFruit()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            currentPhotoUri?.let { uri ->
                if (isMealPlateCameraCapture) {
                    agentViewModel.analyzeMealPlatePhoto(uri)
                } else {
                    genAiViewModel.identifyFruit(uri)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = createImageFile(context)
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = photoUri
            cameraLauncher.launch(photoUri)
        }
    }

    val launchCameraWithMode: (Boolean) -> Unit = { mealPlateMode ->
        isMealPlateCameraCapture = mealPlateMode
        val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            val photoFile = createImageFile(context)
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = photoUri
            cameraLauncher.launch(photoUri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    val onCameraClick = { launchCameraWithMode(false) }
    val onMealPlateCameraClick = { launchCameraWithMode(true) }

    // Reset GenAI state when screen is disposed
    DisposableEffect(Unit) {
        onDispose { genAiViewModel.reset() }
    }

    // Auto-scroll to bottom when AI result changes
    LaunchedEffect(aiResult) {
        if (aiResult is ApiResult.Success || aiResult is ApiResult.Error) {
            delay(100) // Give UI time to update
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    // Show tip history dialog when requested
    if (showTipHistory) {
        TipHistoryDialog(
            tips = tipHistory,
            onDismiss = { showTipHistory = false },
            onDeleteTip = { tipId ->
                genAiViewModel.deleteTip(tipId)
            }
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = navController.currentBackStackEntry?.destination?.route ?: "",
                navController = navController
            )
        }
    ) { innerPadding ->
        // Render different layouts based on orientation
        var showClassicTools by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
        ) {
            NutriCoachHeader()
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showClassicTools,
                    onClick = { showClassicTools = false },
                    label = { Text("ReAct Agent & Meal Vision") },
                )
                FilterChip(
                    selected = showClassicTools,
                    onClick = { showClassicTools = true },
                    label = { Text("Fruit Guide & Tips") },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (!showClassicTools) {
                NutriCoachAgentSection(
                    uiState = agentUiState,
                    onSendPrompt = agentViewModel::sendMessage,
                    onConfirmWrite = agentViewModel::confirmPendingMealWrite,
                    onDismissWrite = agentViewModel::dismissPendingMealWrite,
                    onLaunchMealCamera = onMealPlateCameraClick,
                    onSimulateMealPlateVision = agentViewModel::analyzeSampleMealPlate,
                    onCancelRun = agentViewModel::cancelActiveRun,
                    onClearHistory = agentViewModel::clearHistory,
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else if (isLandscape) {
                LandscapeNutriCoachLayout(
                    innerPadding = PaddingValues(0.dp),
                    fruitUiState = fruitUiState,
                    needsImprove = needsImprovement,
                    aiResult = aiResult,
                    genAiViewModel = genAiViewModel,
                    userId = userId,
                    localQuery = localQuery,
                    onQueryChange = { localQuery = it },
                    fruitCardExpanded = fruitCardExpanded,
                    onExpandedChange = { fruitCardExpanded = it },
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    viewModel = viewModel,
                    defaultPrompt = defaultPrompt,
                    suggestedPromptsShort = suggestedPromptsShort,
                    suggestedPromptsFull = suggestedPromptsFull,
                    onShowHistory = { showTipHistory = true },
                    scrollState = rememberScrollState(),
                    onCameraClick = onCameraClick
                )
            } else {
                PortraitNutriCoachLayout(
                    innerPadding = PaddingValues(0.dp),
                    needsImprove = needsImprovement,
                    fruitUiState = fruitUiState,
                    aiResult = aiResult,
                    genAiViewModel = genAiViewModel,
                    userId = userId,
                    localQuery = localQuery,
                    onQueryChange = { localQuery = it },
                    fruitCardExpanded = fruitCardExpanded,
                    onExpandedChange = { fruitCardExpanded = it },
                    focusRequester = focusRequester,
                    focusManager = focusManager,
                    viewModel = viewModel,
                    defaultPrompt = defaultPrompt,
                    suggestedPromptsShort = suggestedPromptsShort,
                    suggestedPromptsFull = suggestedPromptsFull,
                    onShowHistory = { showTipHistory = true },
                    scrollState = rememberScrollState(),
                    onCameraClick = onCameraClick
                )
            }
        }
    }
}

/**
 * Portrait layout for the NutriCoach screen - vertical arrangement
 */
@Composable
private fun PortraitNutriCoachLayout(
    innerPadding: PaddingValues,
    needsImprove: Boolean,
    fruitUiState: FruitUiState,
    aiResult: ApiResult<String>,
    genAiViewModel: GenAIViewModel,
    userId: String,
    localQuery: String,
    onQueryChange: (String) -> Unit,
    fruitCardExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    viewModel: NutriCoachViewModel,
    defaultPrompt: String,
    suggestedPromptsShort: List<String>,
    suggestedPromptsFull: List<String>,
    onShowHistory: () -> Unit,
    scrollState: ScrollState,
    onCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        NutriCoachHeader()

        Spacer(modifier = Modifier.height(20.dp))

        if (needsImprove) {
            FruitSearchCard(
                query = localQuery,
                onQueryChange = onQueryChange,
                onSearch = {
                    viewModel.updateQuery(localQuery)
                    viewModel.searchFruit()
                    onExpandedChange(true)
                    focusManager.clearFocus()
                },
                expanded = fruitCardExpanded,
                onExpandedChange = onExpandedChange,
                focusRequester = focusRequester,
                fruitUiState = fruitUiState,
                onCloseCard = {
                    onExpandedChange(false)
                    viewModel.resetState()
                    onQueryChange("")
                },
                onCameraClick = onCameraClick
            )
        } else {
            GoodScoreCard()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GenAI Advice Component
        GenAIAdviceComponent(
            aiResult = aiResult,
            genAiViewModel = genAiViewModel,
            userId = userId,
            title = "Need a tip?",
            suggestedPromptsShort = suggestedPromptsShort,
            suggestedPromptsFull = suggestedPromptsFull,
            defaultPrompt = defaultPrompt,
            showHistory = true,
            onShowHistory = onShowHistory
        )

        // Bottom padding to ensure content isn't obscured
        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * Landscape layout for the NutriCoach screen - two-column arrangement
 */
@Composable
private fun LandscapeNutriCoachLayout(
    innerPadding: PaddingValues,
    needsImprove: Boolean,
    fruitUiState: FruitUiState,
    aiResult: ApiResult<String>,
    genAiViewModel: GenAIViewModel,
    userId: String,
    localQuery: String,
    onQueryChange: (String) -> Unit,
    fruitCardExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager,
    viewModel: NutriCoachViewModel,
    defaultPrompt: String,
    suggestedPromptsShort: List<String>,
    suggestedPromptsFull: List<String>,
    onShowHistory: () -> Unit,
    scrollState: ScrollState,
    onCameraClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Left side: Header and fruit search/score card
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with adjusted sizing for landscape
            NutriCoachHeader(isLandscape = true)

            Spacer(modifier = Modifier.height(16.dp))

            if (needsImprove) {
                FruitSearchCard(
                    query = localQuery,
                    onQueryChange = onQueryChange,
                    onSearch = {
                        viewModel.updateQuery(localQuery)
                        viewModel.searchFruit()
                        onExpandedChange(true)
                        focusManager.clearFocus()
                    },
                    expanded = fruitCardExpanded,
                    onExpandedChange = onExpandedChange,
                    focusRequester = focusRequester,
                    fruitUiState = fruitUiState,
                    onCloseCard = {
                        onExpandedChange(false)
                        viewModel.resetState()
                        onQueryChange("")
                    },
                    isLandscape = true,
                    onCameraClick = onCameraClick
                )
            } else {
                GoodScoreCard(isLandscape = true)
            }
        }

        // Right side: GenAI Advice section
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .verticalScroll(scrollState)
        ) {
            // GenAI Advice Component with landscape-specific styling
            GenAIAdviceComponent(
                aiResult = aiResult,
                genAiViewModel = genAiViewModel,
                userId = userId,
                title = "Need a tip?",
                suggestedPromptsShort = suggestedPromptsShort,
                suggestedPromptsFull = suggestedPromptsFull,
                defaultPrompt = defaultPrompt,
                showHistory = true,
                onShowHistory = onShowHistory
            )

            // Bottom padding
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

/**
 * Header section for NutriCoach screen
 * Adapts font size based on orientation
 */
@Composable
private fun NutriCoachHeader(isLandscape: Boolean = false) {
    // Adjust font size and padding for landscape
    val fontSize = if (isLandscape) 24.sp else 28.sp
    val verticalPadding = if (isLandscape) 8.dp else 16.dp

    Text(
        stringResource(R.string.nutriCoachScreen_header),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily(Font(R.font.poppins)),
        fontSize = fontSize,
        modifier = Modifier.padding(vertical = verticalPadding)
    )
}

/**
 * Card for searching and displaying fruit information
 * Adapts layout based on orientation
 */
@Composable
fun FruitSearchCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    fruitUiState: FruitUiState,
    onCloseCard: () -> Unit,
    isLandscape: Boolean = false,
    onCameraClick: () -> Unit
) {
    // Adjust padding and spacing for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp
    val topPadding = if (isLandscape) 8.dp else 16.dp
    val titleFontSize = if (isLandscape) 18.sp else 20.sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { onExpandedChange(!expanded) },
        colors = CardDefaults.cardColors(containerColor = LightGreen)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding)
        ) {
            // Card title
            Text(
                stringResource(R.string.clinicianScreen_fruitSeachTitle),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 8.dp))

            // Search field and button
            SearchInputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                focusRequester = focusRequester,
                placeholder = stringResource(R.string.nutriCoachScreen_searchPlaceholder),
                isLandscape = isLandscape,
                onCameraClick = onCameraClick
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 6.dp else 8.dp))

            // Conditional content - only shown when expanded
            if (expanded) {
                FruitResultContent(
                    fruitUiState = fruitUiState,
                    onClose = onCloseCard,
                    isLandscape = isLandscape
                )
            }
        }
    }
}

/**
 * Search input field with send button
 * Adapts sizing based on orientation
 */
@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
    placeholder: String,
    isLandscape: Boolean = false,
    onCameraClick: () -> Unit
) {
    // Adjust sizes for landscape
    val cornerRadius = if (isLandscape) 12.dp else 16.dp
    val fieldPadding = if (isLandscape) 6.dp else 8.dp
    val textPadding = if (isLandscape) 10.dp else 12.dp
    val fontSize = if (isLandscape) 14.sp else 16.sp
    val buttonSize = if (isLandscape) 40.dp else 48.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
            .padding(fieldPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = textPadding, vertical = if (isLandscape) 6.dp else 8.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(fontSize = fontSize, color = Color.Black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotBlank()) {
                        onSearch()
                    }
                }
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            color = Color.Gray,
                            fontSize = fontSize,
                            fontFamily = FontFamily(Font(R.font.monaco)),
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Camera button
        IconButton(
            onClick = onCameraClick,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Camera",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
            )
        }

        // Send button
        IconButton(
            onClick = {
                if (query.isNotBlank()) {
                    onSearch()
                }
            },
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
            )
        }
    }
}

/**
 * Content section showing fruit search results
 * Adapts content layout based on orientation
 */
@Composable
fun FruitResultContent(
    fruitUiState: FruitUiState,
    onClose: () -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust padding for landscape
    val verticalPadding = if (isLandscape) 12.dp else 16.dp

    when (fruitUiState) {
        is FruitUiState.Initial -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = verticalPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.initialStateMsg),
                    color = Color.DarkGray,
                    fontSize = if (isLandscape) 12.sp else 14.sp,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    textAlign = TextAlign.Center
                )
            }
        }
        is FruitUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(verticalPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                )
            }
        }
        is FruitUiState.Success -> {
            val fruit = fruitUiState.fruit
            FruitDetailCard(fruit = fruit, onClose = onClose, isLandscape = isLandscape)
        }
        is FruitUiState.Error -> {
            ErrorCard(
                message = fruitUiState.message,
                onClose = onClose,
                isLandscape = isLandscape
            )
        }
    }
}

/**
 * Card showing detailed fruit information
 * Adapts content layout based on orientation
 */
@Composable
fun FruitDetailCard(
    fruit: FruitDto,
    onClose: () -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust sizing for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp
    val emojiSize = if (isLandscape) 20.sp else 24.sp
    val detailFontSize = if (isLandscape) 14.sp else 16.sp
    val nutritionHeaderSize = if (isLandscape) 18.sp else 20.sp
    val spacing = if (isLandscape) 6.dp else 8.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fruit name and icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        getFruitEmoji(fruit.name),
                        fontSize = emojiSize,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily(Font(R.font.poppins)),
                    )
                    Spacer(modifier = Modifier.width(if (isLandscape) 6.dp else 8.dp))
                    Text(
                        fruit.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (isLandscape) 18.sp else 22.sp
                        )
                    )
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(if (isLandscape) 18.dp else 24.dp)
                    )
                }
            }

            // Fruit details
            Text(
                text = "Genus: ${fruit.genus}",
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "Family: ${fruit.family}",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "Order: ${fruit.order}",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )

            Spacer(modifier = Modifier.height(spacing))

            // Nutrition information
            Text(
                text = stringResource(R.string.clinicianScreen_nutritionHeader),
                fontWeight = FontWeight.Bold,
                fontSize = nutritionHeaderSize,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "• Calories: ${fruit.nutritions.calories}",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "• Sugar: ${fruit.nutritions.sugar}g",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "• Carbs: ${fruit.nutritions.carbohydrates}g",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "• Protein: ${fruit.nutritions.protein}g",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
            Text(
                text = "• Fat: ${fruit.nutritions.fat}g",
                fontSize = detailFontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily(Font(R.font.poppins)),
            )
        }
    }
}

/**
 * Error card shown when fruit search fails
 * Adapts sizing based on orientation
 */
@Composable
fun ErrorCard(
    message: String,
    onClose: () -> Unit,
    isLandscape: Boolean = false
) {
    // Adjust sizing for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp
    val errorTitleSize = if (isLandscape) 14.sp else 16.sp
    val errorMessageSize = if (isLandscape) 12.sp else 14.sp
    val buttonSize = if (isLandscape) 28.dp else 32.dp
    val iconSize = if (isLandscape) 14.dp else 16.dp
    val spacing = if (isLandscape) 6.dp else 8.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(if (isLandscape) 12.dp else 16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(cardPadding)
            ) {
                Text(
                    text = stringResource(R.string.fruit_error_msg),
                    fontSize = errorTitleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily(Font(R.font.poppins)),
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(spacing))

                Text(
                    text = message,
                    fontSize = errorMessageSize,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily(Font(R.font.poppins))
                )
            }
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(buttonSize)
                    .background(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Card shown when user has a good fruit score
 * Adapts sizing based on orientation
 */
@Composable
fun GoodScoreCard(isLandscape: Boolean = false) {
    // Adjust sizing for landscape
    val cardPadding = if (isLandscape) 12.dp else 16.dp
    val topPadding = if (isLandscape) 8.dp else 16.dp
    val titleSize = if (isLandscape) 18.sp else 20.sp
    val spacing = if (isLandscape) 6.dp else 8.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8F5E9))
            .padding(cardPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.nutriCoachScreen_goodScore),
                fontWeight = FontWeight.Bold,
                fontSize = titleSize,
                fontFamily = FontFamily(Font(R.font.poppins))
            )
        }
    }
}

private fun createImageFile(context: android.content.Context): File {
    val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
    val storageDir = context.cacheDir
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}