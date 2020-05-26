package io.github.cfstout.ktor.config

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EmptyConfiguration
import com.natpryce.konfig.Override
import java.nio.file.Files
import java.nio.file.Path

/**
 * Load properties files in the directory in sorted order
 *
 * Later files overwrite previous files
 */
fun ConfigurationProperties.Companion.fromDirectory(configDir: Path): Configuration =
    Files.newDirectoryStream(configDir)
        .filter { it.fileName.toString().endsWith("properties") }
        .asSequence()
        .sorted()
        .map { fromFile(it.toFile()) }
        .fold(EmptyConfiguration as Configuration) {acc, configurationProperties ->
            Override(configurationProperties, acc)
        }
