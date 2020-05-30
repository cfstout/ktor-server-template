package io.github.cfstout.ktor.hikari

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.Key
import com.natpryce.konfig.booleanType
import com.natpryce.konfig.intType
import com.natpryce.konfig.stringType
import com.zaxxer.hikari.HikariConfig

/**
 * Load a [HikariConfig] from a [Configuration].
 *
 * This assumes that the following properties are available (when prefixed with the supplied `prefix`):
 *
 * - `HOST`
 * - `PORT`
 * - `USERNAME`
 * - `PASSWORD`
 * - `NAME` - the database name to connect to
 * - `DATA_SOURCE_CLASS` - the class name of the `DataSource` implementation, e.g. [`org.postgresql.ds.PGSimpleDataSource`](https://jdbc.postgresql.org/documentation/head/ds-ds.html)
 * - `MAX_POOL_SIZE` - see https://github.com/brettwooldridge/HikariCP#frequently-used. if unsure, 10 is a good default.
 * - `CONN_INIT_SQL` - SQL to run for each new connection created. A good place to set the timezone, e.g. `SET TIME ZONE 'UTC'` for Postgres.
 * - `AUTO_COMMIT` - should be false, but may have to be set to true for legacy apps that don't manage transactions properly.
 *
 * The provided `prefix` will be stitched together with the above property names, so prefix = "PRIMARY_DB_" would lead to the property `PRIMARY_DB_HOST` being used for the hostname to connect to.
 *
 * `HOST`, `NAME`, `USERNAME`, and `PASSWORD` are provided by ECS task definitions created by deployer, which is why we use those names.
 * You can of course set them manually as well if desired.
 *
 * The returned `HikariConfig` can be further configured if need be (e.g. adding additional datasource properties); the
 * things set here are simply the ones that all applications should set at a minimum.
 */
fun buildHikariConfig(config: Configuration, prefix: String, dbVariant: DbVariant = PostgresVariant): HikariConfig {
    return HikariConfig().apply {
        // shared config for all drivers
        dataSourceClassName = config[Key("${prefix}DATA_SOURCE_CLASS", stringType)]
        username = config[Key("${prefix}USERNAME", stringType)]
        password = config[Key("${prefix}PASSWORD", stringType)]
        maximumPoolSize = config[Key("${prefix}MAX_POOL_SIZE", intType)]
        connectionInitSql = config[Key("${prefix}CONN_INIT_SQL", stringType)]
        isAutoCommit = config[Key("${prefix}AUTO_COMMIT", booleanType)]

        // vendor-specific config
        dbVariant.applyProperties(config, prefix, this)
    }
}

/**
 * Create space for vendor-specific setup so that we can eventually use this for MySQL, etc, as well.
 */
sealed class DbVariant {
    abstract fun applyProperties(config: Configuration, prefix: String, dsConfig: HikariConfig)
}

/**
 * PostgreSQL can be configured just via datasource properties, so we don't need a jdbc url at all.
 */
object PostgresVariant : DbVariant() {
    override fun applyProperties(config: Configuration, prefix: String, dsConfig: HikariConfig) {
        dsConfig.apply {
            // properties as per https://jdbc.postgresql.org/documentation/head/ds-ds.html
            addDataSourceProperty("serverName", config[Key("${prefix}HOST", stringType)])
            addDataSourceProperty("portNumber", config[Key("${prefix}PORT", stringType)])
            addDataSourceProperty("databaseName", config[Key("${prefix}NAME", stringType)])
        }
    }
}
