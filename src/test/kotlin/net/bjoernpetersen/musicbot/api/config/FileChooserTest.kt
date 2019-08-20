package net.bjoernpetersen.musicbot.api.config

import net.bjoernpetersen.musicbot.test.api.config.ConfigExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ConfigExtension::class)
class FileChooserTest {
    @Test
    fun `isDirectory and isFile`(config: Config) {
        FileChooser(true, true)
    }

    @Test
    fun `not isDirectory and isFile`(config: Config) {
        FileChooser(false, true)
    }

    @Test
    fun `isDirectory and not isFile`(config: Config) {
        assertThrows<IllegalArgumentException> {
            FileChooser(true, false)
        }
    }

    @Test
    fun `not isDirectory and not isFile`(config: Config) {
        FileChooser(false, false)
    }
}
