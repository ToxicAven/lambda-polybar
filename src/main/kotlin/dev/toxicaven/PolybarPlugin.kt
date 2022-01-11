package dev.toxicaven

import com.lambda.client.plugin.api.Plugin

internal object PolybarPlugin: Plugin() {

    override fun onLoad() {
        // Load any modules, commands, or HUD elements here
        modules.add(PolybarLink)
    }

    override fun onUnload() {
        // Here you can unregister threads etc...
    }
}