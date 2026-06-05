package com.bluewater.revenuepredictor.domain

import kotlinx.serialization.Serializable

@Serializable
enum class SolutionLine(val displayName: String, val docsUrl: String) {
    GPUS("GPUs", "https://docs.bluewater.example/gpus"),
    OBJECT_STORAGE("Object Storage", "https://docs.bluewater.example/object-storage"),
    APP_RUNTIME("App Runtime", "https://docs.bluewater.example/app-runtime"),
    CONTAINER_PLATFORM("Container Platform", "https://docs.bluewater.example/container-platform"),
    ANALYTICS_STREAMS("Analytics Streams", "https://docs.bluewater.example/analytics-streams"),
    ;

    companion object {
        fun parseMany(raw: String?): List<SolutionLine> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(',', ';')
                .mapNotNull { token ->
                    val cleaned = token.trim()
                    values().firstOrNull { value ->
                        value.displayName.equals(cleaned, ignoreCase = true) ||
                            value.name.equals(cleaned.replace(' ', '_').uppercase(), ignoreCase = true)
                    }
                }
                .distinct()
        }
    }
}

