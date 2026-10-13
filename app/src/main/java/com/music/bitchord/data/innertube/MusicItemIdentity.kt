package com.music.bitchord.data.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Resolves only the identity of this item, never arbitrary descendants/menus. */
internal object MusicItemIdentity {
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")

    private fun JsonElement?.objectAt(key: String): JsonObject? =
        (this as? JsonObject)?.get(key) as? JsonObject

    private fun JsonElement?.stringAt(key: String): String? =
        ((this as? JsonObject)?.get(key) as? JsonPrimitive)?.contentOrNull

    private fun validId(value: String?): String? = value?.takeIf(videoIdPattern::matches)

    fun browseId(renderer: JsonObject): String? = renderer
        .objectAt("navigationEndpoint").objectAt("browseEndpoint").stringAt("browseId")
        ?.takeIf { it.isNotBlank() && !it.startsWith("MPED") }

    fun videoId(renderer: JsonObject): String? {
        // An album/artist/playlist card may offer an unrelated preview in its
        // overlay; its own browse destination wins over any playback shortcut.
        if (browseId(renderer) != null) return null

        validId(renderer.objectAt("playlistItemData").stringAt("videoId"))?.let { return it }

        val title = renderer.objectAt("title")
            ?: ((renderer["flexColumns"] as? JsonArray)?.firstOrNull())
                .objectAt("musicResponsiveListItemFlexColumnRenderer").objectAt("text")
        val runs = title?.get("runs") as? JsonArray
        runs?.forEach { run ->
            endpointVideoId(run.objectAt("navigationEndpoint"))?.let { return it }
        }
        endpointVideoId(renderer.objectAt("navigationEndpoint"))?.let { return it }
        endpointVideoId(renderer.objectAt("onTap"))?.let { return it }

        val play = renderer.objectAt("overlay")
            .objectAt("musicItemThumbnailOverlayRenderer").objectAt("content")
            .objectAt("musicPlayButtonRenderer").objectAt("playNavigationEndpoint")
        return endpointVideoId(play)
    }

    private fun endpointVideoId(endpoint: JsonObject?): String? =
        validId(endpoint.objectAt("watchEndpoint").stringAt("videoId"))
            ?: validId(endpoint.objectAt("browseEndpoint").stringAt("browseId")
                ?.takeIf { it.startsWith("MPED") }?.removePrefix("MPED"))
}
