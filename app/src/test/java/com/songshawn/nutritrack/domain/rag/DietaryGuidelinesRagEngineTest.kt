package com.songshawn.nutritrack.domain.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DietaryGuidelinesRagEngineTest {

    @Test
    fun `retrieve ranks sodium guideline ADG-3_2 first for salt and blood pressure query`() {
        val hits = DietaryGuidelinesRagEngine.retrieve("daily sodium salt limit blood pressure mg", topK = 2)
        assertTrue(hits.isNotEmpty())
        assertEquals("ADG-3.2", hits.first().chunk.chunkId)
        assertTrue(hits.first().chunk.citationTag.contains("ADG §3.2"))
    }

    @Test
    fun `retrieve ranks wholegrain guideline ADG-2_3 first for oats and wholegrain query`() {
        val hits = DietaryGuidelinesRagEngine.retrieve("why should I eat wholegrain oats and brown rice cereal", topK = 2)
        assertTrue(hits.isNotEmpty())
        assertEquals("ADG-2.3", hits.first().chunk.chunkId)
    }

    @Test
    fun `retrieve ranks vegetable diversity guideline ADG-2_1 first for vegetable sub-group query`() {
        val hits = DietaryGuidelinesRagEngine.retrieve("vegetable serves variety dark green legumes spinach", topK = 2)
        assertTrue(hits.isNotEmpty())
        assertEquals("ADG-2.1", hits.first().chunk.chunkId)
    }

    @Test
    fun `cosineSimilarity returns 1 for identical vectors and 0 for orthogonal vectors`() {
        val v1 = doubleArrayOf(0.6, 0.8, 0.0)
        val v2 = doubleArrayOf(0.0, 0.0, 1.0)
        assertEquals(1.0, DietaryGuidelinesRagEngine.cosineSimilarity(v1, v1), 0.001)
        assertEquals(0.0, DietaryGuidelinesRagEngine.cosineSimilarity(v1, v2), 0.001)
    }
}
