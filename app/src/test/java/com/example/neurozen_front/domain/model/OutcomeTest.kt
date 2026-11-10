package com.example.neurozen_front.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutcomeTest {

    @Test
    fun `map should transform success data`() {
        val success = Outcome.Success(10)
        val result = success.map { it * 2 }
        
        assertTrue(result is Outcome.Success)
        assertEquals(20, (result as Outcome.Success).data)
    }

    @Test
    fun `map should return same failure`() {
        val failure = Outcome.Failure(AppError.Network())
        val result = failure.map { "ignored" }
        
        assertTrue(result is Outcome.Failure)
        assertEquals(failure, result)
    }

    @Test
    fun `getOrNull should return data on Success`() {
        val data = "test"
        val outcome = Outcome.Success(data)
        assertEquals(data, outcome.getOrNull())
    }

    @Test
    fun `getOrNull should return null on Failure`() {
        val outcome = Outcome.Failure(AppError.Server("error"))
        assertNull(outcome.getOrNull())
    }
}
