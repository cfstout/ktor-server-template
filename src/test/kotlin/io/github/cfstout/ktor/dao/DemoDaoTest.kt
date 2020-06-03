package io.github.cfstout.ktor.dao

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

abstract class DemoDaoTest {
    abstract val dao: DemoDao

    @Test
    fun getReturnsNull() {
        assertNull(dao.get(UserId(1)))
    }

    @Test
    fun setAndGet() {
        dao.set(UserId(1), Color("red"))
        assertEquals(Color("red"), dao.get(UserId(1)))
    }

    @Test
    fun setOverrides() {
        dao.set(UserId(1), Color("red"))
        dao.set(UserId(1), Color("blue"))
        assertEquals(Color("blue"), dao.get(UserId(1)))
    }

    @Test
    fun getColorsByCount() {
        dao.set(UserId(1), Color("red"))
        dao.set(UserId(2), Color("blue"))
        dao.set(UserId(3), Color("red"))

        val expected = mapOf(
            Color("red") to 2,
            Color("blue") to 1
        )
        assertEquals(expected, dao.getColorsByCount())
    }
}

class InMemoryDemoDaoTest : DemoDaoTest() {
    override val dao: DemoDao = InMemoryDemoDao()
}

class InMemoryDemoDao : DemoDao {
    private val db = mutableMapOf<UserId, Color>()

    override fun get(userId: UserId): Color? = db[userId]

    override fun set(userId: UserId, color: Color) {
        db[userId] = color
    }

    override fun getColorsByCount(): Map<Color, Int> = db.asSequence().groupingBy { it.value }.eachCount()
}
