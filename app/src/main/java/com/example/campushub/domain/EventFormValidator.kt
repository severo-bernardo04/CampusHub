package com.example.campushub.domain

import com.example.campushub.model.Event

enum class FormField { TITLE, DESCRIPTION, CATEGORY, DATE, ADDRESS, CAPACITY, MAP_LOCATION }

data class EventFormInput(
    val title: String,
    val description: String,
    val category: String,
    val dateMillis: Long,
    val address: String,
    val capacityText: String,
    val latitude: Double,
    val longitude: Double,
    val confirmedCount: Int = 0
)

object EventFormValidator {
    const val MIN_TITLE_LENGTH = 3
    const val MAX_CAPACITY = 10_000

    fun parseCapacity(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in 1..MAX_CAPACITY }

    fun inputFrom(event: Event) = EventFormInput(
        title = event.title,
        description = event.description,
        category = event.category,
        dateMillis = event.dateMillis,
        address = event.address,
        capacityText = event.capacity.toString(),
        latitude = event.latitude,
        longitude = event.longitude,
        confirmedCount = event.confirmedCount
    )

    /** Rascunho aceita data passada e sem mapa; publicar exige data futura e local confirmado. */
    fun validate(
        input: EventFormInput,
        publishing: Boolean,
        now: Long = System.currentTimeMillis()
    ): Map<FormField, String> {
        val errors = linkedMapOf<FormField, String>()

        if (input.title.trim().length < MIN_TITLE_LENGTH) {
            errors[FormField.TITLE] = "Informe um título com pelo menos $MIN_TITLE_LENGTH caracteres"
        }
        if (input.description.isBlank()) errors[FormField.DESCRIPTION] = "Informe a descrição"
        if (input.category.isBlank()) errors[FormField.CATEGORY] = "Escolha a categoria"
        if (input.address.isBlank()) errors[FormField.ADDRESS] = "Informe o endereço"

        val capacity = parseCapacity(input.capacityText)
        if (capacity == null) {
            errors[FormField.CAPACITY] = "Informe a capacidade (de 1 a $MAX_CAPACITY)"
        } else if (capacity < input.confirmedCount) {
            errors[FormField.CAPACITY] =
                "A capacidade não pode ser menor que os ${input.confirmedCount} inscritos confirmados"
        }

        if (input.dateMillis <= 0) {
            errors[FormField.DATE] = "Escolha a data e a hora"
        } else if (publishing && input.dateMillis <= now) {
            errors[FormField.DATE] = "Para publicar, a data precisa estar no futuro"
        }

        if (publishing && input.latitude == 0.0 && input.longitude == 0.0) {
            errors[FormField.MAP_LOCATION] = "Use \"Buscar no mapa\" para confirmar o endereço"
        }
        return errors
    }
}