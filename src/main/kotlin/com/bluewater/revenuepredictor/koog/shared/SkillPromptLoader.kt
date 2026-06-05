package com.bluewater.revenuepredictor.koog.shared

object SkillPromptLoader {
    private const val CLASSPATH_BASE = "bluewater-skills"
    private val cache = mutableMapOf<String, String>()

    fun load(fileName: String): String = cache.getOrPut(fileName) {
        val path = "/$CLASSPATH_BASE/${fileName.trim().removePrefix("/")}"
        val stream = SkillPromptLoader::class.java.getResourceAsStream(path)
            ?: error("Skill file not found at $path")
        stream.bufferedReader().use { it.readText() }
    }
}

