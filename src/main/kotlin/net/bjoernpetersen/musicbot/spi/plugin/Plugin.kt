package net.bjoernpetersen.musicbot.spi.plugin

import com.google.common.annotations.Beta
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.plugin.ActiveBase
import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.spi.plugin.management.InitStateWriter
import java.io.IOException
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

/**
 * Base interface for plugins. This interface isn't meant to be directly implemented, but to be
 * extended by a specialized interface first.
 *
 * Every plugin MUST be annotated with [Bases] and [IdBase].
 *
 * ## Dependencies
 *
 * A plugin can request dependencies (typically other plugins) by marking fields with the
 * [Inject] annotation. Please note that dependency injection in constructors is not possible.
 *
 * ## Lifecycle
 *
 * A plugin goes through several different lifecycle phases due to the way it is integrated in the
 * MusicBot.
 *
 * ### Creation
 *
 * The plugin is loaded from an external Jar-File and instantiated using a no-args constructor.
 * In this phase, no actual work should be done by the plugin and no resources should be allocated.
 *
 * ### Dependency injection
 *
 * In a second, **separate** step, the dependencies requested by the plugin are resolved
 * and injected. There is no notification for the plugin about this.
 *
 * ### Configuration
 *
 * The plugin is asked to provide its configuration entries by [createConfigEntries] and
 * [createSecretEntries]. The returned entries are shown to the user and their value may change
 * any number of times during this phase.
 *
 * ### Initialization
 *
 * The [initialize] method is called after configuration is done and the plugin may allocate
 * additional resources for the time it is running.
 *
 * ### Destruction
 *
 * The [close] method is called and all resources should be released.
 * The plugin instance will never be used again at this point.
 *
 */
interface Plugin {

    /**
     * An arbitrary plugin name. Keep it short, but descriptive.
     *
     * The name will never be shown without the user knowing the specialized plugin interface
     * this plugin implements (Provider, Suggester, ...), so please don't include that part in the
     * name.
     *
     * This value should be static, i.e. not dependent on config or state.
     */
    val name: String

    /**
     * A one or two sentence description of the plugin.
     */
    val description: String

    fun createConfigEntries(config: Config): List<Config.Entry<*>>
    fun createSecretEntries(secrets: Config): List<Config.Entry<*>>
    fun createStateEntries(state: Config)

    @Throws(InitializationException::class)
    fun initialize(initStateWriter: InitStateWriter)

    @Throws(IOException::class)
    fun close()
}

interface UserFacing {
    /**
     * The subject of the content provided by this plugin.
     *
     * This will be shown to end users, together with the type of plugin, this should give users
     * an idea of they will be getting.
     *
     * Note that the value of this may even change during runtime, especially during configuration.
     *
     * Some good examples:
     * - For providers: "Spotify" or "YouTube"
     * - For suggesters: "Random MP3s", a playlist name, "Based on last played song"
     */
    val subject: String
}

/**
 * An exception during plugin initialization.
 */
open class InitializationException : Exception {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * An exception thrown by Plugins if they are misconfigured.
 */
class ConfigurationException : InitializationException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

/**
 * Thrown if a plugin's declaration is invalid.
 */
class DeclarationException : RuntimeException {

    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}

private val KClass<*>.isBase: Boolean
    get() = findAnnotation<Base>() != null
        || annotations.any { it.annotationClass.isBase }

val KClass<*>.isActiveBase: Boolean
    get() = findAnnotation<ActiveBase>() != null

val KClass<*>.isIdBase: Boolean
    get() = findAnnotation<IdBase>() != null

val KClass<*>.hasActiveBase: Boolean
    get() = findAnnotation<ActiveBase>() != null
        || superclasses.any { it.hasActiveBase }

val Plugin.bases: List<KClass<out Plugin>>
    get() {
        val specs = mutableListOf<KClass<out Plugin>>()
        val type = this::class
        if (type.isBase) specs.add(type)
        type.allSuperclasses.asSequence()
            .filter { it.isBase }
            .filter {
                it.isSubclassOf(Plugin::class).also { isSubclass ->
                    if (!isSubclass) {
                        DeclarationException("Base ${it.qualifiedName} is not a plugin subtype")
                    }
                }
            }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as KClass<out Plugin>
            }
            .forEach { specs.add(it) }

        return specs
    }

val Plugin.id: KClass<out Plugin>
    @Beta
    get() {
        var foundActive = false
        val ids: MutableList<KClass<out Plugin>> = ArrayList(bases.size)
        bases.forEach {
            if (it.isIdBase) {
                ids.add(it)
            }
            if (it.isActiveBase) {
                foundActive = true
            }
        }
        if (!foundActive) {
            throw IllegalStateException(
                "Plugin does not implement an active base: ${this::class.qualifiedName}")
        }
        if (ids.isEmpty()) {
            throw DeclarationException(
                "No ID base on plugin with active base: ${this::class.qualifiedName}")
        }
        if (ids.size > 1) {
            // TODO should this happen?
            throw DeclarationException()
        }
        return ids.first()
    }

val Plugin.category: KClass<out Plugin>
    get() = this::class.pluginCategory

val KClass<*>.pluginCategory: KClass<out Plugin>
    get() {
        val specs = mutableListOf<KClass<out Plugin>>()
        sequenceOf(GenericPlugin::class, PlaybackFactory::class, Provider::class, Suggester::class)
            .filter { this.isSubclassOf(it) }
            .forEach { specs.add(it) }
        return if (specs.size == 1) specs.first()
        else throw ConfigurationException()
    }
