package io.github.mg138.indicator

import eu.pb4.polymer.api.resourcepack.PolymerRPUtils
import net.fabricmc.api.DedicatedServerModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("UNUSED")
object Main : DedicatedServerModInitializer {
    const val modId = "health_indicator"
    val logger: Logger = LogManager.getLogger(modId)

    override fun onInitializeServer() {
        PolymerRPUtils.addAssetSource(modId)
        logger.info("Registered health indicator.")

    }
}