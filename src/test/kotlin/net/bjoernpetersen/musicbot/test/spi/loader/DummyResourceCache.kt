package net.bjoernpetersen.musicbot.test.spi.loader

import com.google.inject.AbstractModule
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.loader.NoResource
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache

class DummyResourceCache : ResourceCache {
    private val logger = KotlinLogging.logger {}
    override suspend fun get(song: Song): Resource {
        logger.debug { "get Resource for song ${song.id}" }
        return NoResource
    }

    override suspend fun close() {
        logger.debug { "closing" }
    }

    companion object : AbstractModule() {
        override fun configure() {
            bind(ResourceCache::class.java).to(DummyResourceCache::class.java)
        }
    }
}
