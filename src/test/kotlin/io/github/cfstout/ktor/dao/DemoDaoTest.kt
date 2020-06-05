package io.github.cfstout.ktor.dao

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cfstout.ktor.jooq.tables.FavoriteColors
import java.io.Closeable
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class DemoDaoTest {
    abstract fun withDao(block: (DemoDao) -> Unit)

    @Test
    fun getReturnsNull() {
        withDao {
            assertNull(it.get(UserId(1)))
        }
    }

    @Test
    fun setAndGet() {
        withDao {
            it.set(UserId(1), Color("red"))
            assertEquals(Color("red"), it.get(UserId(1)))
        }
    }

    @Test
    fun setOverrides() {
        withDao {
            it.set(UserId(1), Color("red"))
            it.set(UserId(1), Color("blue"))
            assertEquals(Color("blue"), it.get(UserId(1)))
        }
    }

    @Test
    fun getColorsByCount() {
        withDao {
            it.set(UserId(1), Color("red"))
            it.set(UserId(2), Color("blue"))
            it.set(UserId(3), Color("red"))

            val expected = mapOf(
                Color("red") to 2,
                Color("blue") to 1
            )
            assertEquals(expected, it.getColorsByCount())
        }
    }
}

class InMemoryDemzoDaoTest : DemoDaoTest() {
    override fun withDao(block: (DemoDao) -> Unit) = block(InMemoryDemoDao())
}

class InMemoryDemoDao : DemoDao {
    private val db = mutableMapOf<UserId, Color>()

    override fun get(userId: UserId): Color? = db[userId]

    override fun set(userId: UserId, color: Color) {
        db[userId] = color
    }

    override fun getColorsByCount(): Map<Color, Int> = db.asSequence().groupingBy { it.value }.eachCount()
}

class SqlDemoDaoTest : DemoDaoTest() {
    private val dbHelpers = DbHelpers()

    override fun withDao(block: (DemoDao) -> Unit) {
        dbHelpers.txnContext.transaction { t ->
            block(SqlDemoDao(t.dsl()))
        }
    }

    @BeforeEach
    fun cleanDb() {
        dbHelpers.cleanupDb()
    }

    @AfterEach
    fun cleanupDbHelpers() {
        dbHelpers.close()
    }
}

class DbHelpers : Closeable {
    private val config = HikariConfig().apply {
        addDataSourceProperty("serverName", "localhost")
        addDataSourceProperty("portNumber", 5432)
        addDataSourceProperty("databaseName", "cfstout")
        isAutoCommit = false
        username = "cfstout"
        password = "password"
        connectionInitSql = "SET TIME ZONE 'UTC'"
        dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
        maximumPoolSize = 2
    }

    private val dataSource: HikariDataSource = HikariDataSource(config)
    val txnContext: DSLContext = DSL.using(dataSource, SQLDialect.POSTGRES)

    fun cleanupDb() {
        txnContext.transaction { t ->
            t.dsl().deleteFrom(FavoriteColors.FAVORITE_COLORS).execute()
        }
    }

    override fun close() {
        dataSource.close()
        txnContext.close()
    }
}
