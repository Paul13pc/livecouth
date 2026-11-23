package com.example.livecouth.domain.model

enum class ValidationError {
    REQUIRED,
    INVALID_FORMAT,
    OUT_OF_RANGE,
    TOO_LONG,
}

object InputValidation {
    const val MIN_AGE = 5
    const val MAX_AGE = 120
    const val MIN_WEIGHT_KG = 20f
    const val MAX_WEIGHT_KG = 300f
    const val MIN_WATER_ML = 10
    const val MAX_WATER_ML = 3000
    const val MAX_NAME_LENGTH = 30

    fun validateName(value: String): ValidationError? {
        val name = value.trim()
        return when {
            name.isEmpty() -> ValidationError.REQUIRED
            name.length > MAX_NAME_LENGTH -> ValidationError.TOO_LONG
            else -> null
        }
    }

    fun normalizeName(value: String): String = value.trim()

    fun validateAge(value: String): ValidationError? {
        val ageText = value.trim()
        if (ageText.isEmpty()) return ValidationError.REQUIRED
        if (!ageText.matches(Regex("[0-9]+"))) return ValidationError.INVALID_FORMAT
        val age = ageText.toIntOrNull() ?: return ValidationError.OUT_OF_RANGE
        return if (age in MIN_AGE..MAX_AGE) null else ValidationError.OUT_OF_RANGE
    }

    fun validateWeight(value: String): ValidationError? {
        val weightText = value.trim()
        if (weightText.isEmpty()) return ValidationError.REQUIRED
        if (!weightText.matches(Regex("[0-9]+(?:[.,][0-9])?"))) {
            return ValidationError.INVALID_FORMAT
        }
        val weight = weightText.replace(',', '.').toFloatOrNull()
            ?: return ValidationError.INVALID_FORMAT
        return if (weight.isFinite() && weight in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            null
        } else {
            ValidationError.OUT_OF_RANGE
        }
    }

    fun validateWaterAmount(value: String): ValidationError? {
        val amountText = value.trim()
        if (amountText.isEmpty()) return ValidationError.REQUIRED
        if (!amountText.matches(Regex("[0-9]+"))) return ValidationError.INVALID_FORMAT
        val amount = amountText.toIntOrNull() ?: return ValidationError.OUT_OF_RANGE
        return if (amount in MIN_WATER_ML..MAX_WATER_ML) null else ValidationError.OUT_OF_RANGE
    }

    fun isValidWaterAmount(amountMl: Int): Boolean = amountMl in MIN_WATER_ML..MAX_WATER_ML

    fun filterIntegerInput(value: String): String {
        if (value.any { it == '-' || it == '.' || it == ',' }) return ""
        return value.filter(Char::isDigit)
    }

    fun filterWeightInput(value: String): String {
        if ('-' in value) return ""
        val filtered = StringBuilder()
        var hasSeparator = false
        var decimalDigits = 0
        value.forEach { character ->
            when {
                character.isDigit() && (!hasSeparator || decimalDigits == 0) -> {
                    filtered.append(character)
                    if (hasSeparator) decimalDigits++
                }
                (character == '.' || character == ',') &&
                    !hasSeparator &&
                    filtered.isNotEmpty() -> {
                    filtered.append(character)
                    hasSeparator = true
                }
            }
        }
        return filtered.toString()
    }
}
