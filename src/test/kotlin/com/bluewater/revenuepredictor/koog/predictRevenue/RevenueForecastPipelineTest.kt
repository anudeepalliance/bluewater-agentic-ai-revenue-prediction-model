package com.bluewater.revenuepredictor.koog.predictRevenue

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevenueForecastPipelineTest {
    @Test
    fun `parse forecast input uses explicit fields`() {
        val input = parseForecastInput(
            """
            Goal: Increase qualified pipeline for AI platform offers
            Audience: Marketing leaders in mid-market SaaS
            Ideas: 4
            Solutions: GPUs, App Runtime
            """.trimIndent(),
        )

        assertEquals("Increase qualified pipeline for AI platform offers", input.goal)
        assertEquals("Marketing leaders in mid-market SaaS", input.audience)
        assertEquals(4, input.numberOfIdeas)
        assertEquals(2, input.solutionLines.size)
    }

    @Test
    fun `parse forecast input falls back to defaults`() {
        val input = parseForecastInput("Boost adoption in technical buying accounts")
        assertEquals(3, input.numberOfIdeas)
        assertTrue(input.solutionLines.isNotEmpty())
    }
}
