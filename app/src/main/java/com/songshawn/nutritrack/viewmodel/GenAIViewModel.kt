package com.songshawn.nutritrack.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.songshawn.nutritrack.data.repository.GenAIRepository
import com.songshawn.nutritrack.data.repository.NutriCoachTipRepository
import com.songshawn.nutritrack.data.repository.PatientRepository
import com.songshawn.nutritrack.data.repository.QuestionnaireInfoRepository
import com.songshawn.nutritrack.data.repository.StreamResult
import com.songshawn.nutritrack.data.room.entity.NutriCoachTipEntity
import com.songshawn.nutritrack.data.room.entity.PatientEntity
import com.songshawn.nutritrack.data.room.entity.QuestionnaireInfoEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for managing GenAI interactions and storing nutrition tips.
 * Handles communication with the LLM API and local tip storage.
 */
@HiltViewModel
class GenAIViewModel @Inject constructor(
    private val repo: GenAIRepository,
    private val tipRepository: NutriCoachTipRepository,
    private val patientRepository: PatientRepository,
    private val questionnaireRepository: QuestionnaireInfoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ApiResult<String>>(ApiResult.Initial)
    val state: StateFlow<ApiResult<String>> = _state.asStateFlow()

    private val _ui = MutableStateFlow<GenAiUiState>(GenAiUiState.Idle)
    val ui: StateFlow<GenAiUiState> = _ui
    
    private val _identifiedFruit = MutableStateFlow<String?>(null)
    val identifiedFruit: StateFlow<String?> = _identifiedFruit.asStateFlow()

    // Streaming text accumulator
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    // Current prompt for database storage
    private var currentPrompt: String = ""
    
    // Current streaming job for cancellation
    private var streamingJob: Job? = null

    /**
     * Gets all tips for a specific user
     * @param userId ID of the user
     * @return Flow of tips for the user
     */
    fun getTipsForUser(userId: String) = tipRepository.getTipsForUser(userId)

    /**
     * Identifies a fruit from an image URI
     * @param uri The URI of the image to identify
     */
    fun identifyFruit(uri: Uri) {
        viewModelScope.launch {
            _ui.value = GenAiUiState.Loading("Identifying Fruit...")
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                }

                if (bitmap != null) {
                    when (val result = repo.identifyFruitFromImage(bitmap)) {
                        is ApiResult.Success -> {
                            val fruitName = result.data
                            if (fruitName.contains("Not a fruit", ignoreCase = true)) {
                                _ui.value = GenAiUiState.Error("Could not identify a fruit in this image.")
                            } else {
                                _identifiedFruit.value = fruitName
                                _ui.value = GenAiUiState.Idle // Reset UI state as we handled it
                            }
                        }
                        is ApiResult.Error -> {
                            _ui.value = GenAiUiState.Error("Failed to identify fruit: ${result}")
                        }
                        else -> {
                            _ui.value = GenAiUiState.Error("Unknown error during identification")
                        }
                    }
                } else {
                    _ui.value = GenAiUiState.Error("Failed to load image")
                }
            } catch (e: Exception) {
                _ui.value = GenAiUiState.Error("Error processing image: ${e.message}")
            }
        }
    }
    
    fun clearIdentifiedFruit() {
        _identifiedFruit.value = null
    }

    /**
     * Generates insights based on population data
     * @param maleCount Number of male users
     * @param femaleCount Number of female users
     * @param maleAvg Average HEIFA score for male users
     * @param femaleAvg Average HEIFA score for female users
     */
    fun generateInsights(
        maleCount: Int,
        femaleCount: Int,
        maleAvg: Double,
        femaleAvg: Double
    ) {
        val total = maleCount + femaleCount
        if (total == 0) {
            _ui.value = GenAiUiState.Error("No population data.")
            return
        }

        val prompt = buildInsightPrompt(
            maleCount = maleCount,
            femaleCount = femaleCount,
            maleAvg = maleAvg,
            femaleAvg = femaleAvg,
            total = total
        )

        currentPrompt = prompt
        sendPrompt(prompt)
    }

    /**
     * Builds a prompt for population health insights
     */
    private fun buildInsightPrompt(
        maleCount: Int,
        femaleCount: Int,
        maleAvg: Double,
        femaleAvg: Double,
        total: Int
    ): String = buildString {
        appendLine("As a data analyst, please analyze the following nutritional health data for this population and identify 3 interesting data patterns or insights:")
        appendLine("- Male users: $maleCount people, average HEIFA score: $maleAvg/100")
        appendLine("- Female users: $femaleCount people, average HEIFA score: $femaleAvg/100")
        appendLine("- Total users: $total people")
        appendLine("The HEIFA score is a healthy eating index assessment, with a perfect score of 100.")
        appendLine("Please list 3 insights directly in this format:")
        appendLine("1. ...")
        appendLine("2. ...")
        appendLine("3. ...")
    }

    /**
     * Sends a custom prompt to the AI using streaming
     * @param prompt The prompt to send
     */
    fun sendCustomPrompt(prompt: String) {
        if (prompt.isBlank()) {
            _ui.value = GenAiUiState.Validation("Prompt cannot be empty")
            return
        }

        // Ensure prompt is about fruits and nutrition
        val enhancedPrompt = ensureNutritionContext(prompt)

        currentPrompt = enhancedPrompt
        sendPromptStream(enhancedPrompt)  // Use streaming
    }

    /**
     * Sends a personalized nutrition advice request using RAG approach with streaming.
     * Combines user's nutrition data, questionnaire info, problem areas, and recent tips
     * to provide context-aware AI responses with real-time streaming output.
     *
     * @param userId ID of the user
     * @param userQuestion The user's question
     */
    fun sendPersonalizedAdviceRequest(userId: String, userQuestion: String) {
        if (userQuestion.isBlank()) {
            _ui.value = GenAiUiState.Validation("Question cannot be empty")
            return
        }

        viewModelScope.launch {
            _ui.value = GenAiUiState.Loading("Analyzing your nutrition profile...")
            _state.value = ApiResult.Loading

            try {
                // Retrieve user data for RAG context
                val patient = patientRepository.getPatientById(userId)
                val questionnaireInfo = questionnaireRepository.getInfoForUser(userId)
                val recentTips = tipRepository.getTipsForUser(userId).first().take(3)

                // Build personalized prompt with context
                val ragPrompt = buildRAGPrompt(
                    userQuestion = userQuestion,
                    patient = patient,
                    questionnaireInfo = questionnaireInfo,
                    recentTips = recentTips
                )

                currentPrompt = ragPrompt
                
                // Use streaming for real-time output
                sendPromptStream(ragPrompt)
                
            } catch (e: Exception) {
                _state.value = ApiResult.Error.Unknown(e)
                _ui.value = GenAiUiState.Error("Failed to load user data: ${e.message}")
            }
        }
    }

    /**
     * Builds a RAG-enhanced prompt with user context
     */
    private fun buildRAGPrompt(
        userQuestion: String,
        patient: PatientEntity?,
        questionnaireInfo: QuestionnaireInfoEntity?,
        recentTips: List<NutriCoachTipEntity>
    ): String = buildString {
        appendLine("You are a professional nutrition coach assistant. Please provide personalized advice based on the user's nutrition profile.")
        appendLine()

        // Section 1: User Nutrition Profile
        if (patient != null) {
            appendLine("=== USER NUTRITION PROFILE ===")
            appendLine("Overall HEIFA Score: ${String.format("%.1f", patient.heifaTotalScore)}/100")
            appendLine()
            appendLine("Detailed Scores (out of 10):")
            appendLine("• Fruits: ${String.format("%.1f", patient.fruitScore)} | Variations: ${String.format("%.1f", patient.fruitVariantionsScore)} | Serve Size: ${String.format("%.1f", patient.fruitServeSize)}")
            appendLine("• Vegetables: ${String.format("%.1f", patient.vegetableScore)} | Variations: ${String.format("%.1f", patient.vegetablesVariantionsScore)}")
            appendLine("• Grains & Cereals: ${String.format("%.1f", patient.grainsScore)}")
            appendLine("• Whole Grains: ${String.format("%.1f", patient.wholegrainsScore)}")
            appendLine("• Meat & Alternatives: ${String.format("%.1f", patient.meatAndAlternativeScore)}")
            appendLine("• Dairy & Alternatives: ${String.format("%.1f", patient.dairyScore)}")
            appendLine("• Water: ${String.format("%.1f", patient.waterScore)}")
            appendLine("• Sodium: ${String.format("%.1f", patient.sodiumScore)}")
            appendLine("• Sugar: ${String.format("%.1f", patient.sugarScore)}")
            appendLine("• Saturated Fat: ${String.format("%.1f", patient.saturatedFatScore)}")
            appendLine("• Unsaturated Fat: ${String.format("%.1f", patient.unsaturatedFatScore)}")
            appendLine("• Alcohol: ${String.format("%.1f", patient.alcoholScore)}")
            appendLine("• Discretionary Foods: ${String.format("%.1f", patient.discretionaryScore)}")
            appendLine()

            // Section 2: Identify Problem Areas (scores below 5.0)
            val problemAreas = identifyProblemAreas(patient)
            if (problemAreas.isNotEmpty()) {
                appendLine("⚠️ AREAS NEEDING IMPROVEMENT (score < 5.0):")
                problemAreas.forEach { (area, score) ->
                    appendLine("• $area: ${String.format("%.1f", score)}/10 - Needs attention")
                }
                appendLine()
            }
        } else {
            appendLine("=== USER NUTRITION PROFILE ===")
            appendLine("No nutrition data available for this user.")
            appendLine()
        }

        // Section 3: User Persona and Preferences
        if (questionnaireInfo != null) {
            appendLine("=== USER PREFERENCES ===")
            appendLine("Persona Type: ${questionnaireInfo.persona.displayName}")
            appendLine("Persona Description: ${questionnaireInfo.persona.description}")
            appendLine("Biggest Meal Time: ${questionnaireInfo.biggestMealTime}")
            appendLine("Sleep Time: ${questionnaireInfo.sleepTime}")
            appendLine("Wake Time: ${questionnaireInfo.wakeTime}")
            if (questionnaireInfo.selectedCategories.isNotEmpty()) {
                appendLine("Interested Food Categories: ${questionnaireInfo.selectedCategories.joinToString(", ") { it.displayName }}")
            }
            appendLine()
        }

        // Section 4: Recent AI Tips (to avoid repetition)
        if (recentTips.isNotEmpty()) {
            appendLine("=== RECENT ADVICE GIVEN (avoid repeating) ===")
            recentTips.forEachIndexed { index, tip ->
                appendLine("${index + 1}. ${tip.tipContent.take(150)}...")
            }
            appendLine()
        }

        // Section 5: User Question
        appendLine("=== USER'S QUESTION ===")
        appendLine(userQuestion)
        appendLine()

        // Section 6: Instructions for AI
        appendLine("=== INSTRUCTIONS ===")
        appendLine("1. Provide personalized advice based on the user's actual nutrition scores")
        appendLine("2. Focus especially on the problem areas identified above")
        appendLine("3. Consider the user's persona type when giving advice")
        appendLine("4. Avoid repeating advice that was recently given")
        appendLine("5. Be concise, practical, and encouraging")
        appendLine("6. Include specific food recommendations when appropriate")
    }

    /**
     * Identifies nutrition areas that need improvement (score < 5.0)
     * @return List of problem areas with their scores
     */
    private fun identifyProblemAreas(patient: PatientEntity): List<Pair<String, Double>> {
        val threshold = 5.0
        val areas = mutableListOf<Pair<String, Double>>()

        if (patient.fruitScore < threshold) areas.add("Fruits" to patient.fruitScore)
        if (patient.vegetableScore < threshold) areas.add("Vegetables" to patient.vegetableScore)
        if (patient.grainsScore < threshold) areas.add("Grains & Cereals" to patient.grainsScore)
        if (patient.wholegrainsScore < threshold) areas.add("Whole Grains" to patient.wholegrainsScore)
        if (patient.meatAndAlternativeScore < threshold) areas.add("Meat & Alternatives" to patient.meatAndAlternativeScore)
        if (patient.dairyScore < threshold) areas.add("Dairy & Alternatives" to patient.dairyScore)
        if (patient.waterScore < threshold) areas.add("Water" to patient.waterScore)
        if (patient.sodiumScore < threshold) areas.add("Sodium" to patient.sodiumScore)
        if (patient.sugarScore < threshold) areas.add("Sugar" to patient.sugarScore)
        if (patient.saturatedFatScore < threshold) areas.add("Saturated Fat" to patient.saturatedFatScore)
        if (patient.unsaturatedFatScore < threshold) areas.add("Unsaturated Fat" to patient.unsaturatedFatScore)
        if (patient.alcoholScore < threshold) areas.add("Alcohol" to patient.alcoholScore)
        if (patient.discretionaryScore < threshold) areas.add("Discretionary Foods" to patient.discretionaryScore)

        // Sort by score (lowest first) to prioritize worst areas
        return areas.sortedBy { it.second }
    }

    /**
     * Ensures the prompt is related to nutrition and health
     */
    private fun ensureNutritionContext(prompt: String): String {
        val nutritionKeywords = listOf("fruit", "nutrition", "diet", "health")
        val containsNutritionContext = nutritionKeywords.any {
            prompt.contains(it, ignoreCase = true)
        }

        return if (!containsNutritionContext) {
            "Generate a response about fruits and nutrition based on this prompt: $prompt"
        } else {
            prompt
        }
    }

    /**
     * Sends a prompt to the AI and updates state with the result
     * @param prompt The prompt to send
     */
    private fun sendPrompt(prompt: String) {
        viewModelScope.launch {
            _ui.value = GenAiUiState.Loading("Generating Tip...")
            _state.value = ApiResult.Loading
            
            val r = repo.generateText(prompt)
            _state.value = r
            
            when (r) {
                is ApiResult.Success -> _ui.value = GenAiUiState.Content(r.data)
                is ApiResult.Error.Network -> _ui.value = GenAiUiState.Error("Network issue, try again.")
                is ApiResult.Error.Http -> _ui.value = GenAiUiState.Error("Server error: ${r.code}")
                is ApiResult.Error.Parsing -> _ui.value = GenAiUiState.Error(r.toString())
                else -> _ui.value = GenAiUiState.Error("Unknown error")
            }
        }
    }

    /**
     * Sends a prompt to the AI using SSE streaming for real-time text output.
     * This significantly reduces TTFT (Time To First Token) and improves user experience.
     *
     * @param prompt The prompt to send
     */
    private fun sendPromptStream(prompt: String) {
        // Cancel any existing streaming job
        streamingJob?.cancel()
        
        streamingJob = viewModelScope.launch {
            _streamingText.value = ""
            _ui.value = GenAiUiState.Streaming("", isComplete = false)
            _state.value = ApiResult.Loading

            repo.streamText(prompt).collect { result ->
                when (result) {
                    is StreamResult.Chunk -> {
                        // Append new chunk to accumulated text
                        _streamingText.value += result.text
                        _ui.value = GenAiUiState.Streaming(_streamingText.value, isComplete = false)
                    }
                    is StreamResult.Complete -> {
                        // Stream completed successfully
                        val finalText = _streamingText.value
                        _state.value = ApiResult.Success(finalText)
                        _ui.value = GenAiUiState.Streaming(finalText, isComplete = true)
                    }
                    is StreamResult.Error -> {
                        _state.value = ApiResult.Error.Unknown(Exception(result.message))
                        _ui.value = GenAiUiState.Error(result.message)
                    }
                }
            }
        }
    }

    /**
     * Cancels the current streaming operation if any
     */
    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        if (_ui.value is GenAiUiState.Streaming) {
            val currentText = (_ui.value as GenAiUiState.Streaming).text
            if (currentText.isNotEmpty()) {
                // Keep partial content if user cancels mid-stream
                _ui.value = GenAiUiState.Streaming(currentText, isComplete = true)
                _state.value = ApiResult.Success(currentText)
            } else {
                _ui.value = GenAiUiState.Idle
                _state.value = ApiResult.Initial
            }
        }
    }

    /**
     * Saves the current AI response as a tip in the database
     * @param userId ID of the user to associate with the tip
     */
    fun saveTipToDatabase(userId: String) {
        val currentResult = _state.value
        if (currentResult is ApiResult.Success) {
            val tipContent = currentResult.data

            viewModelScope.launch {
                try {
                    tipRepository.saveTip(
                        userId = userId,
                        content = tipContent,
                        prompt = currentPrompt
                    )
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    // Silent error handling
                }
            }
        }
    }

    /**
     * Deletes a specific tip by its ID
     * @param tipId The ID of the tip to delete
     */
    fun deleteTip(tipId: Long) {
        viewModelScope.launch {
            try {
                tipRepository.deleteTipById(tipId)
            } catch (e: Exception) {
                // Silent error handling
            }
        }
    }

    /**
     * Resets the AI state
     */
    fun reset() {
        streamingJob?.cancel()
        streamingJob = null
        _state.value = ApiResult.Initial
        _streamingText.value = ""
        currentPrompt = ""
    }

    /**
     * Resets the UI state
     */
    fun resetUiState() {
        _ui.value = GenAiUiState.Idle
    }
}