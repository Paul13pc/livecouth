package com.example.livecouth.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InputValidationTest {

    @Test
    fun ageRequiresAnIntegerBetweenFiveAndOneHundredTwenty() {
        listOf("5", "120").forEach { assertNull(InputValidation.validateAge(it)) }
        listOf("", "abc", "-5", "5.5", "4", "121").forEach {
            assertEquals(
                "Input should be rejected: $it",
                false,
                InputValidation.validateAge(it) == null,
            )
        }
    }

    @Test
    fun weightAcceptsOneDecimalWithCommaOrPointAndEnforcesRange() {
        listOf("20", "20.1", "20,1", "300", "300.0").forEach {
            assertNull(InputValidation.validateWeight(it))
        }
        listOf("", "text", "-20", "19.9", "300.1", "70.25", "70,2.").forEach {
            assertEquals(
                "Input should be rejected: $it",
                false,
                InputValidation.validateWeight(it) == null,
            )
        }
    }

    @Test
    fun waterAmountRequiresAnIntegerBetweenTenAndThreeThousand() {
        listOf("10", "3000").forEach { assertNull(InputValidation.validateWaterAmount(it)) }
        listOf("", "text", "0", "-10", "9", "3001", "10.5").forEach {
            assertEquals(
                "Input should be rejected: $it",
                false,
                InputValidation.validateWaterAmount(it) == null,
            )
        }
    }

    @Test
    fun namesAreTrimmedForValidationAndLimitedToThirtyCharacters() {
        assertNull(InputValidation.validateName("  Ana  "))
        assertEquals(ValidationError.REQUIRED, InputValidation.validateName("   "))
        assertEquals(ValidationError.TOO_LONG, InputValidation.validateName("a".repeat(31)))
        assertEquals("Ana", InputValidation.normalizeName("  Ana  "))
    }

    @Test
    fun numericInputFiltersKeepOnlySupportedCharacters() {
        assertEquals("123", InputValidation.filterIntegerInput("1a23"))
        assertEquals("", InputValidation.filterIntegerInput("-123"))
        assertEquals("", InputValidation.filterIntegerInput("12.3"))
        assertEquals("70,5", InputValidation.filterWeightInput("70,5.2x"))
        assertEquals("70.5", InputValidation.filterWeightInput("70.5,2"))
    }
}
