package io.github.cfstout.ktor.dao

interface DemoDao {
    fun get(userId: UserId): Color?
    fun set(userId: UserId, color: Color)
    fun getColorsByCount(): Map<Color, Int>
}

data class Color(val name: String)
data class UserId(val id: Int)
