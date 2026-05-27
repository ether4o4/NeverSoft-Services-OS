package com.ether4o4.morsvitaest.network.dtos.openaicompatible

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaRunningModelsResponseDto(
    val models: List<Model> = emptyList(),
) {
    @Serializable
    data class Model(
        val name: String? = null,
        val model: String? = null,
        val digest: String? = null,
        val size: Long? = null,
        @SerialName("size_vram")
        val sizeVram: Long? = null,
    ) {
        val effectiveId: String?
            get() = model?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() }
    }
}
