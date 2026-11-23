package com.example.livecouth.data.remote

import com.example.livecouth.data.local.entity.QuickAddButton
import com.example.livecouth.data.local.entity.WaterLog
import com.example.livecouth.domain.model.ActivityLevel
import com.example.livecouth.domain.model.CityLocation
import com.example.livecouth.domain.model.DrinkType
import com.example.livecouth.domain.model.InputValidation
import com.example.livecouth.domain.model.SyncableUserProfile
import com.example.livecouth.domain.model.UserProfile

class SyncDataException(message: String) : Exception(message)

fun WaterLog.toFirestoreMap(): Map<String, Any> = mapOf(
    "syncId" to syncId,
    "cantidadMl" to cantidadMl,
    "fechaHora" to fechaHora,
    "drinkType" to drinkType.name,
    "hydrationFactor" to hydrationFactor.toDouble(),
    "updatedAt" to updatedAt,
    "sincronizado" to true,
    "eliminado" to eliminado,
)

fun Map<String, Any?>.toWaterLog(documentId: String): WaterLog {
    val drinkTypeName = requiredString("drinkType")
    val drinkType = DrinkType.entries.firstOrNull { it.name == drinkTypeName }
        ?: throw SyncDataException("Tipo de bebida desconocido.")
    val factorValue = requiredNumber("hydrationFactor").toDouble()
    if (!factorValue.isFinite() || factorValue !in 0.0..2.0) {
        throw SyncDataException("Factor de hidratación inválido.")
    }
    val factor = factorValue.toFloat()
    if (!factor.isFinite() || factor !in 0f..2f) {
        throw SyncDataException("Factor de hidratación inválido.")
    }

    val cantidadMl = requiredInt("cantidadMl")
    if (!InputValidation.isValidWaterAmount(cantidadMl)) {
        throw SyncDataException("Cantidad de agua inválida.")
    }
    return WaterLog(
        cantidadMl = cantidadMl,
        fechaHora = requiredNumber("fechaHora").toLong(),
        drinkType = drinkType,
        hydrationFactor = factor,
        syncId = (this["syncId"] as? String)?.takeIf(String::isNotBlank) ?: documentId,
        updatedAt = requiredNumber("updatedAt").toLong(),
        sincronizado = true,
        eliminado = requiredBoolean("eliminado"),
    )
}

fun QuickAddButton.toFirestoreMap(): Map<String, Any> = mapOf(
    "syncId" to syncId,
    "name" to name,
    "amountMl" to amountMl,
    "sortOrder" to sortOrder,
    "updatedAt" to updatedAt,
    "sincronizado" to true,
    "eliminado" to eliminado,
)

fun Map<String, Any?>.toQuickAddButton(documentId: String): QuickAddButton {
    val name = InputValidation.normalizeName(requiredString("name"))
    val amountMl = requiredInt("amountMl")
    val sortOrder = requiredInt("sortOrder")
    if (
        InputValidation.validateName(name) != null ||
        !InputValidation.isValidWaterAmount(amountMl) ||
        sortOrder < 0
    ) {
        throw SyncDataException("Botón rápido inválido.")
    }
    return QuickAddButton(
        name = name,
        amountMl = amountMl,
        sortOrder = sortOrder,
        syncId = (this["syncId"] as? String)?.takeIf(String::isNotBlank) ?: documentId,
        updatedAt = requiredNumber("updatedAt").toLong(),
        sincronizado = true,
        eliminado = requiredBoolean("eliminado"),
    )
}

fun SyncableUserProfile.toFirestoreMap(): Map<String, Any> = with(profile) {
    mapOf(
        "name" to name,
        "weightKg" to weightKg.toDouble(),
        "age" to age,
        "activityLevel" to activityLevel.name,
        "isOnboardingCompleted" to isOnboardingCompleted,
        "reminderEnabled" to reminderEnabled,
        "reminderStartHour" to reminderStartHour,
        "reminderEndHour" to reminderEndHour,
        "cityName" to city.name,
        "updatedAt" to updatedAt,
        "sincronizado" to true,
        "eliminado" to eliminado,
    )
}

fun Map<String, Any?>.toSyncableUserProfile(): SyncableUserProfile {
    val activityName = requiredString("activityLevel")
    val activityLevel = ActivityLevel.entries.firstOrNull { it.name == activityName }
        ?: throw SyncDataException("Nivel de actividad desconocido.")
    val name = InputValidation.normalizeName(requiredString("name"))
    val weight = requiredNumber("weightKg").toFloat()
    val age = requiredInt("age")
    val cityName = requiredString("cityName")
    val city = CityLocation.supportedCity(cityName)
        ?: throw SyncDataException("La ciudad del perfil remoto no es compatible.")
    val profile = UserProfile(
        name = name,
        weightKg = weight,
        age = age,
        activityLevel = activityLevel,
        isOnboardingCompleted = requiredBoolean("isOnboardingCompleted"),
        reminderEnabled = requiredBoolean("reminderEnabled"),
        reminderStartHour = requiredInt("reminderStartHour"),
        reminderEndHour = requiredInt("reminderEndHour"),
        city = city,
    )
    if (
        InputValidation.validateName(profile.name) != null ||
        !profile.weightKg.isFinite() ||
        profile.weightKg !in InputValidation.MIN_WEIGHT_KG..InputValidation.MAX_WEIGHT_KG ||
        profile.age !in InputValidation.MIN_AGE..InputValidation.MAX_AGE ||
        profile.city.name.isBlank() ||
        !profile.city.hasValidCoordinates() ||
        profile.reminderStartHour !in 0..23 ||
        profile.reminderEndHour !in 0..23 ||
        profile.reminderEndHour <= profile.reminderStartHour
    ) {
        throw SyncDataException("Perfil remoto inválido.")
    }
    return SyncableUserProfile(
        profile = profile,
        updatedAt = requiredNumber("updatedAt").toLong(),
        sincronizado = true,
        eliminado = requiredBoolean("eliminado"),
    )
}

private fun Map<String, Any?>.requiredString(key: String): String =
    this[key] as? String ?: throw SyncDataException("Falta el campo $key.")

private fun Map<String, Any?>.requiredNumber(key: String): Number =
    this[key] as? Number ?: throw SyncDataException("Falta el campo numérico $key.")

private fun Map<String, Any?>.requiredInt(key: String): Int {
    val value = requiredNumber(key).toDouble()
    if (
        !value.isFinite() ||
        value % 1.0 != 0.0 ||
        value !in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()
    ) {
        throw SyncDataException("El campo $key debe ser un entero válido.")
    }
    return value.toInt()
}

private fun Map<String, Any?>.requiredBoolean(key: String): Boolean =
    this[key] as? Boolean ?: throw SyncDataException("Falta el campo lógico $key.")
