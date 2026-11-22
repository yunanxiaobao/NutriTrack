package com.songshawn.nutritrack.domain.rag

import kotlin.math.ln
import kotlin.math.round
import kotlin.math.sqrt

/**
 * A single chunk from the Australian Dietary Guidelines (ADG 2013) & HEIFA-2013 reference manual.
 */
data class GuidelineChunk(
    val chunkId: String,
    val sectionTitle: String,
    val citationTag: String,
    val keywords: List<String>,
    val content: String,
)

/**
 * Ranked retrieval result pairing a [GuidelineChunk] with its hybrid cosine/BM25 relevance score.
 */
data class RetrievedGuideline(
    val chunk: GuidelineChunk,
    val score: Double,
)

/**
 * Pure-Kotlin on-device Retrieval-Augmented Generation (RAG) engine over the Australian Dietary
 * Guidelines (ADG 2013) and HEIFA-2013 scoring manual.
 *
 * Combines:
 * 1. **64-dimensional hashed TF-IDF embedding vectors** with L2-normalized **Cosine Similarity**;
 * 2. **BM25-style lexical & domain-keyword boosting** for exact nutrition terminology.
 *
 * Returns grounded guideline snippets with formal citation identifiers (e.g. `[ADG §2.1]`).
 */
object DietaryGuidelinesRagEngine {

    private const val EMBEDDING_DIM = 64
    private const val KEYWORD_BOOST = 0.35

    val corpus: List<GuidelineChunk> = listOf(
        GuidelineChunk(
            chunkId = "ADG-2.1",
            sectionTitle = "Guideline 2.1 — Vegetables, Legumes & Sub-group Diversity",
            citationTag = "[ADG §2.1: Vegetables & Variety]",
            keywords = listOf("vegetable", "vegetables", "variety", "legumes", "lentils", "spinach", "broccoli", "greens", "serves"),
            content = "Australian adults should consume 5 serves/day (women) or 6 serves/day (men) of vegetables. " +
                "One standard serve is 75g cooked vegetables (1/2 cup) or 1 cup raw leafy greens. " +
                "HEIFA awards 5 points for quantity and 5 points for variety across dark-green, cruciferous, " +
                "orange/red, starchy, and legume sub-groups.",
        ),
        GuidelineChunk(
            chunkId = "ADG-2.2",
            sectionTitle = "Guideline 2.2 — Fresh Fruit Intake & Whole Fruit Preference",
            citationTag = "[ADG §2.2: Fruit Intake]",
            keywords = listOf("fruit", "fruits", "berries", "citrus", "apple", "banana", "orange", "fiber"),
            content = "Adults should eat 2 standard serves of fruit per day (150g fresh fruit such as 1 medium apple/banana " +
                "or 1 cup berries). Whole fruit is strongly preferred over fruit juice to preserve dietary fiber and " +
                "limit free sugar absorption. HEIFA awards 5 points for 2+ serves and 5 points for sub-group variety.",
        ),
        GuidelineChunk(
            chunkId = "ADG-2.3",
            sectionTitle = "Guideline 2.3 — Grain (Cereal) Foods & Wholegrain Ratio",
            citationTag = "[ADG §2.3: Wholegrains & Cereals]",
            keywords = listOf("grain", "grains", "wholegrain", "wholegrains", "oats", "quinoa", "brown", "rice", "cereal", "bread"),
            content = "Adults require 6 serves of grain foods daily, with at least 50% (3+ serves) coming from wholegrain " +
                "or high-fiber varieties such as rolled oats, brown rice, quinoa, or wholegrain sourdough. " +
                "HEIFA splits grain scoring into 5 points for total grains and 5 points for wholegrain proportion.",
        ),
        GuidelineChunk(
            chunkId = "ADG-2.4",
            sectionTitle = "Guideline 2.4 — Lean Meats, Poultry, Fish, Eggs, Tofu, Nuts & Seeds",
            citationTag = "[ADG §2.4: Lean Protein & Alternatives]",
            keywords = listOf("meat", "protein", "salmon", "fish", "seafood", "chicken", "poultry", "eggs", "tofu", "almonds", "nuts"),
            content = "Recommended daily intake is 2.5 serves (women) or 3.0 serves (men) of lean meats and alternatives. " +
                "One serve equals 65g cooked lean red meat, 80g cooked poultry, 100g cooked fish fillet, 2 large eggs, " +
                "170g firm tofu, or 30g raw nuts/seeds. Include omega-3 rich oily fish (e.g. salmon) at least twice weekly.",
        ),
        GuidelineChunk(
            chunkId = "ADG-2.5",
            sectionTitle = "Guideline 2.5 — Milk, Yogurt, Cheese & Calcium-Fortified Alternatives",
            citationTag = "[ADG §2.5: Dairy & Calcium Alternatives]",
            keywords = listOf("dairy", "milk", "yogurt", "cheese", "soy", "calcium", "reduced-fat"),
            content = "Adults aged 19–50 should consume 2.5 serves/day of mostly reduced-fat dairy or calcium-fortified " +
                "alternatives (1 cup 250mL milk or soy milk containing >=100mg calcium/100mL, 200g Greek yogurt, or 40g cheese) " +
                "for bone mineral density and protein.",
        ),
        GuidelineChunk(
            chunkId = "ADG-2.6",
            sectionTitle = "Guideline 2.6 — Water & Hydration",
            citationTag = "[ADG §2.6: Water & Hydration]",
            keywords = listOf("water", "hydration", "fluid", "drink", "tea", "thirst"),
            content = "Plain water should be the primary beverage. HEIFA awards the full 5 points when daily water and " +
                "unsweetened fluid intake reaches at least 1,500 mL (3 large 500mL glasses) and displaces sugar-sweetened drinks.",
        ),
        GuidelineChunk(
            chunkId = "ADG-3.1",
            sectionTitle = "Guideline 3.1 — Saturated vs. Unsaturated Fat Balance",
            citationTag = "[ADG §3.1: Dietary Fat Quality]",
            keywords = listOf("fat", "saturated", "unsaturated", "olive", "oil", "avocado", "cholesterol", "lipid"),
            content = "Replace saturated fats (butter, fatty processed meats, palm oil) with monounsaturated and polyunsaturated " +
                "fats (extra-virgin olive oil, avocado, nuts, oily fish). HEIFA awards 5 points when saturated fat is <=10% " +
                "of total energy (0 points at >=15%) and 5 points for 2–4 serves of healthy unsaturated fats.",
        ),
        GuidelineChunk(
            chunkId = "ADG-3.2",
            sectionTitle = "Guideline 3.2 — Limiting Sodium (Salt) Intake",
            citationTag = "[ADG §3.2: Sodium Moderation]",
            keywords = listOf("sodium", "salt", "blood", "pressure", "hypertension", "mg"),
            content = "Keep daily sodium intake at or below the Suggested Dietary Target of 2,000 mg/day (~5g salt) to support " +
                "cardiovascular and blood-pressure health. In HEIFA-2013, <=2,000 mg earns 10/10 points, scaling linearly " +
                "down to 0 points at >=3,500 mg/day.",
        ),
        GuidelineChunk(
            chunkId = "ADG-3.3",
            sectionTitle = "Guideline 3.3 — Limiting Added Sugars",
            citationTag = "[ADG §3.3: Added Sugar Limit]",
            keywords = listOf("sugar", "sugars", "added", "sweet", "cola", "soda", "cookie", "dessert"),
            content = "Limit foods and drinks containing added sugars (soft drinks, confectionery, sweet bakery items). " +
                "WHO and HEIFA-2013 award 10/10 points when added sugar contributes <=5% of total daily energy, " +
                "dropping to 0 points at >=15% of total energy.",
        ),
        GuidelineChunk(
            chunkId = "ADG-3.4",
            sectionTitle = "Guideline 3.4 — Discretionary Foods & Alcohol Moderation",
            citationTag = "[ADG §3.4: Discretionary & Alcohol]",
            keywords = listOf("discretionary", "treats", "fries", "fast", "junk", "alcohol", "wine", "beer"),
            content = "Discretionary foods (energy-dense, nutrient-poor items ~600 kJ/serve such as fries, cookies, and sugary soda) " +
                "should be kept to <=1 serve/day for full 10/10 HEIFA points. Alcohol intake should not exceed 2 standard " +
                "drinks per day (5/5 HEIFA points).",
        ),
    )

    private val idfByBucket: DoubleArray by lazy { computeIdfWeights() }
    private val chunkEmbeddings: Map<String, DoubleArray> by lazy {
        corpus.associate { chunk ->
            chunk.chunkId to embedText("${chunk.sectionTitle} ${chunk.keywords.joinToString(" ")} ${chunk.content}")
        }
    }

    /**
     * Retrieves the top [topK] most relevant dietary guideline chunks for [query] using
     * L2-normalized TF-IDF vector cosine similarity plus domain-keyword recall boosting.
     */
    fun retrieve(query: String, topK: Int = 3): List<RetrievedGuideline> {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()

        val queryVec = embedTokens(tokens)
        val tokenSet = tokens.toSet()

        return corpus.map { chunk ->
            val docVec = chunkEmbeddings.getValue(chunk.chunkId)
            val cosine = cosineSimilarity(queryVec, docVec)
            val keywordHits = chunk.keywords.count { kw ->
                tokenSet.any { tok -> tok == kw || (tok.length >= 4 && (kw.startsWith(tok) || tok.startsWith(kw))) }
            }
            val combinedScore = round3(cosine + (keywordHits * KEYWORD_BOOST))
            RetrievedGuideline(chunk = chunk, score = combinedScore)
        }
            .filter { it.score > 0.05 }
            .sortedByDescending { it.score }
            .take(topK.coerceIn(1, 5))
    }

    fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size) { "Vector dimensions must match" }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom <= 1e-12) 0.0 else (dot / denom).coerceIn(0.0, 1.0)
    }

    internal fun embedText(text: String): DoubleArray = embedTokens(tokenize(text))

    private fun embedTokens(tokens: List<String>): DoubleArray {
        val vec = DoubleArray(EMBEDDING_DIM)
        if (tokens.isEmpty()) return vec

        for (token in tokens) {
            val stem = stemToken(token)
            val bucket = (stem.hashCode() and Int.MAX_VALUE) % EMBEDDING_DIM
            vec[bucket] += idfByBucket[bucket]
        }

        val norm = sqrt(vec.sumOf { it * it })
        if (norm > 1e-12) {
            for (i in vec.indices) {
                vec[i] /= norm
            }
        }
        return vec
    }

    private fun computeIdfWeights(): DoubleArray {
        val docFreq = IntArray(EMBEDDING_DIM)
        for (chunk in corpus) {
            val seenBuckets = mutableSetOf<Int>()
            val tokens = tokenize("${chunk.sectionTitle} ${chunk.keywords.joinToString(" ")} ${chunk.content}")
            for (token in tokens) {
                val bucket = (stemToken(token).hashCode() and Int.MAX_VALUE) % EMBEDDING_DIM
                seenBuckets.add(bucket)
            }
            for (b in seenBuckets) docFreq[b]++
        }
        val n = corpus.size.toDouble()
        return DoubleArray(EMBEDDING_DIM) { b ->
            1.0 + ln((n + 1.0) / (docFreq[b].toDouble() + 1.0))
        }
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in STOP_WORDS }

    private fun stemToken(token: String): String = when {
        token.endsWith("ies") && token.length > 5 -> token.dropLast(3) + "y"
        token.endsWith("es") && token.length > 5 -> token.dropLast(2)
        token.endsWith("s") && token.length > 4 -> token.dropLast(1)
        else -> token
    }

    private fun round3(v: Double): Double = round(v * 1000.0) / 1000.0

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "that", "this", "from", "are", "was", "were",
        "what", "how", "why", "can", "should", "into", "your", "per", "day",
    )
}
