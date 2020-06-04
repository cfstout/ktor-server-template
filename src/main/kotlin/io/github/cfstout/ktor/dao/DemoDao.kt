package io.github.cfstout.ktor.dao

import io.github.cfstout.ktor.jooq.tables.FavoriteColors
import io.github.cfstout.ktor.jooq.tables.FavoriteColors.FAVORITE_COLORS
import org.jooq.DSLContext
import org.jooq.impl.DSL

interface DemoDao {
    fun get(userId: UserId): Color?
    fun set(userId: UserId, color: Color)
    fun getColorsByCount(): Map<Color, Int>
}

data class Color(val name: String)
data class UserId(val id: Int)


class SqlDemoDao(private val txnContext: DSLContext) : DemoDao {
    override fun get(userId: UserId): Color? {
        return txnContext.selectFrom(FAVORITE_COLORS)
            .where(FAVORITE_COLORS.USER_ID.eq(userId.id))
            .fetchOne()
            ?.let { Color(it.color) }
    }

    override fun set(userId: UserId, color: Color) {
        txnContext.insertInto(FAVORITE_COLORS)
            .values(userId.id, color.name)
            .onDuplicateKeyUpdate()
            .set(FAVORITE_COLORS.COLOR, color.name)
            .execute()
    }

    override fun getColorsByCount(): Map<Color, Int> {
        val countField = DSL.count().`as`("count")
        return txnContext.select(FAVORITE_COLORS.COLOR, countField)
            .from(FAVORITE_COLORS)
            .groupBy(FAVORITE_COLORS.COLOR)
            .fetch()
            .associate { Color(it.get(FAVORITE_COLORS.COLOR)) to it.get(countField) }
    }
}
