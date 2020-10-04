package edu.gatech.cog.script

data class Script(
    val script: List<String>,
    @Transient
    var currentIndex: Int = 0
)