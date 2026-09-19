package com.semyqofu.aliasyou.data

import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    val words: List<String>,
    val teams: List<String>
)
