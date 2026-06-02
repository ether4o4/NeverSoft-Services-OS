package com.ether4o4.morsvitaest.data

import com.ether4o4.morsvitaest.data.AppSettings.Companion.KEY_ACTIVE_PROJECT_ID
import com.ether4o4.morsvitaest.data.AppSettings.Companion.KEY_CONFIGURED_SERVICES
import com.ether4o4.morsvitaest.data.AppSettings.Companion.KEY_CURRENT_SERVICE_ID
import com.ether4o4.morsvitaest.data.AppSettings.Companion.KEY_PROJECTS
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

// Service selection
fun AppSettings.selectService(service: Service) {
    settings.putString(KEY_CURRENT_SERVICE_ID, service.id)
}

fun AppSettings.currentService(): Service {
    val id = settings.getString(KEY_CURRENT_SERVICE_ID, Service.Free.id)
    return Service.fromId(id)
}

// API Keys
fun AppSettings.getApiKey(service: Service): String = if (service.requiresApiKey || service.supportsOptionalApiKey) {
    settings.getString(service.apiKeyKey, "")
} else {
    ""
}

fun AppSettings.setApiKey(service: Service, apiKey: String) {
    if (service.requiresApiKey || service.supportsOptionalApiKey) {
        settings.putString(service.apiKeyKey, apiKey)
    }
}

// Model selection
fun AppSettings.getSelectedModelId(service: Service): String = settings.getString(service.modelIdKey, service.defaultModel ?: "")

// Base URL (for self-hosted services like OpenAI-compatible APIs)
fun AppSettings.getBaseUrl(service: Service): String = when (service) {
    Service.OpenAICompatible -> settings.getString(service.baseUrlKey, Service.DEFAULT_OPENAI_COMPATIBLE_BASE_URL)
    else -> ""
}

fun AppSettings.setBaseUrl(service: Service, baseUrl: String) {
    if (service == Service.OpenAICompatible) {
        settings.putString(service.baseUrlKey, baseUrl)
    }
}

// Configured services (ordered list of service instances)
fun AppSettings.getConfiguredServiceInstances(): List<ServiceInstance> {
    val json = settings.getString(KEY_CONFIGURED_SERVICES, "")
    if (json.isBlank()) return emptyList()
    return try {
        val array = Json.parseToJsonElement(json).jsonArray
        array.map { element ->
            if (element is JsonObject) {
                ServiceInstance(
                    instanceId = element["instanceId"]?.jsonPrimitive?.content ?: "",
                    serviceId = element["serviceId"]?.jsonPrimitive?.content ?: "",
                )
            } else {
                val id = element.jsonPrimitive.content
                ServiceInstance(instanceId = id, serviceId = id)
            }
        }.filter { it.instanceId.isNotBlank() && it.serviceId.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

fun AppSettings.setConfiguredServiceInstances(instances: List<ServiceInstance>) {
    val jsonArray = kotlinx.serialization.json.JsonArray(
        instances.map { instance ->
            JsonObject(
                mapOf(
                    "instanceId" to JsonPrimitive(instance.instanceId),
                    "serviceId" to JsonPrimitive(instance.serviceId),
                ),
            )
        },
    )
    settings.putString(KEY_CONFIGURED_SERVICES, jsonArray.toString())
}

// Per-instance settings (API key, model, base URL)
fun AppSettings.getInstanceApiKey(instanceId: String): String = settings.getString("instance_${instanceId}_api_key", "")

// Per-instance enabled state. Defaults to true for backward compatibility:
// any existing instance without this key was implicitly enabled before the
// feature was added. Disable hides the instance from the chat service
// picker but leaves its config (api key, model, etc.) intact.
fun AppSettings.getInstanceEnabled(instanceId: String): Boolean =
    settings.getBoolean("instance_${instanceId}_enabled", true)

fun AppSettings.setInstanceEnabled(instanceId: String, enabled: Boolean) {
    settings.putBoolean("instance_${instanceId}_enabled", enabled)
}

// Projects (app-owned context containers — see Project.kt for the architecture rationale).
fun AppSettings.getProjects(): List<Project> {
    val json = settings.getString(KEY_PROJECTS, "")
    if (json.isBlank()) return emptyList()
    return try {
        val array = Json.parseToJsonElement(json).jsonArray
        array.mapNotNull { element ->
            if (element !is JsonObject) return@mapNotNull null
            val id = element["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = element["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val instructions = element["instructions"]?.jsonPrimitive?.content ?: ""
            val createdAt = element["createdAt"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            Project(id = id, name = name, instructions = instructions, createdAt = createdAt)
        }.filter { it.id.isNotBlank() && it.name.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

fun AppSettings.setProjects(projects: List<Project>) {
    val jsonArray = kotlinx.serialization.json.JsonArray(
        projects.map { project ->
            JsonObject(
                mapOf(
                    "id" to JsonPrimitive(project.id),
                    "name" to JsonPrimitive(project.name),
                    "instructions" to JsonPrimitive(project.instructions),
                    "createdAt" to JsonPrimitive(project.createdAt.toString()),
                ),
            )
        },
    )
    settings.putString(KEY_PROJECTS, jsonArray.toString())
}

fun AppSettings.getActiveProjectId(): String = settings.getString(KEY_ACTIVE_PROJECT_ID, Project.NONE_ID)

fun AppSettings.setActiveProjectId(id: String) {
    settings.putString(KEY_ACTIVE_PROJECT_ID, id)
}

fun AppSettings.setInstanceApiKey(instanceId: String, apiKey: String) {
    settings.putString("instance_${instanceId}_api_key", apiKey)
}

fun AppSettings.getInstanceModelId(instanceId: String): String = settings.getString("instance_${instanceId}_model_id", "")

fun AppSettings.setInstanceModelId(instanceId: String, modelId: String) {
    settings.putString("instance_${instanceId}_model_id", modelId)
}

fun AppSettings.getInstanceBaseUrl(instanceId: String): String = settings.getString("instance_${instanceId}_base_url", "")

fun AppSettings.setInstanceBaseUrl(instanceId: String, baseUrl: String) {
    settings.putString("instance_${instanceId}_base_url", baseUrl)
}

fun AppSettings.removeInstanceSettings(instanceId: String) {
    settings.remove("instance_${instanceId}_api_key")
    settings.remove("instance_${instanceId}_model_id")
    settings.remove("instance_${instanceId}_base_url")
}

fun AppSettings.generateInstanceId(serviceId: String): String {
    val existing = getConfiguredServiceInstances()
    val existingIds = existing.map { it.instanceId }.toSet()
    if (serviceId !in existingIds) return serviceId
    var counter = 2
    while ("${serviceId}_$counter" in existingIds) counter++
    return "${serviceId}_$counter"
}
