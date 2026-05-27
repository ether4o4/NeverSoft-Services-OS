package com.ether4o4.morsvitaest.network.dtos.openaicompatible

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class OllamaRunningModelsResponseDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    @Test
    fun `parses Ollama ps response and keeps exact running model id`() {
        val responseJson = """
            {
                "models": [
                    {
                        "name": "llama3.2:latest",
                        "model": "llama3.2:latest",
                        "digest": "sha256:abc123",
                        "size": 2019393189,
                        "size_vram": 2019393189,
                        "expires_at": "2026-05-27T12:00:00Z"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString(OllamaRunningModelsResponseDto.serializer(), responseJson)

        assertEquals(1, response.models.size)
        assertEquals("llama3.2:latest", response.models[0].effectiveId)
        assertEquals(2019393189L, response.models[0].sizeVram)
    }

    @Test
    fun `falls back to name when model field is absent`() {
        val responseJson = """
            {
                "models": [
                    {
                        "name": "hf.co/example/model:Q4_K_M"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString(OllamaRunningModelsResponseDto.serializer(), responseJson)

        assertEquals("hf.co/example/model:Q4_K_M", response.models[0].effectiveId)
    }
}
