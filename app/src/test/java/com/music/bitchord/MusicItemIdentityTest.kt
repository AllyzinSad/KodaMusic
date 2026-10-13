package com.music.bitchord

import com.music.bitchord.data.innertube.MusicItemIdentity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class MusicItemIdentityTest {
    private fun item(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test fun playlistIdentityWinsOverMenuAndPreview() {
        val row = item("""{
            "menu":{"watchEndpoint":{"videoId":"wrong000001"}},
            "overlay":{"musicItemThumbnailOverlayRenderer":{"content":{
                "musicPlayButtonRenderer":{"playNavigationEndpoint":{"watchEndpoint":{"videoId":"wrong000002"}}}}}},
            "playlistItemData":{"videoId":"right000001"}
        }""")
        assertEquals("right000001", MusicItemIdentity.videoId(row))
    }

    @Test fun titleIdentityWinsOverMenuEvenWhenMenuAppearsFirst() {
        val row = item("""{
            "menu":{"watchEndpoint":{"videoId":"wrong000001"}},
            "flexColumns":[{"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[
                {"text":"Minha musica","navigationEndpoint":{"watchEndpoint":{"videoId":"right000002"}}}
            ]}}}]
        }""")
        assertEquals("right000002", MusicItemIdentity.videoId(row))
    }

    @Test fun directNavigationWinsOverMenu() {
        val row = item("""{"menu":{"watchEndpoint":{"videoId":"wrong000001"}},
            "navigationEndpoint":{"watchEndpoint":{"videoId":"right000003"}}}""")
        assertEquals("right000003", MusicItemIdentity.videoId(row))
    }

    @Test fun exactPlayOverlayWorksWithoutDirectNavigation() {
        val row = item("""{"menu":{"watchEndpoint":{"videoId":"wrong000001"}},
            "overlay":{"musicItemThumbnailOverlayRenderer":{"content":{
                "musicPlayButtonRenderer":{"playNavigationEndpoint":{"watchEndpoint":{"videoId":"right000004"}}}}}}}""")
        assertEquals("right000004", MusicItemIdentity.videoId(row))
    }

    @Test fun menuOnlyAndUnrelatedNestedVideosAreNotPlayable() {
        assertNull(MusicItemIdentity.videoId(item("""{"menu":{"watchEndpoint":{"videoId":"wrong000001"}}}""")))
        assertNull(MusicItemIdentity.videoId(item("""{"recommendations":[{"watchEndpoint":{"videoId":"wrong000001"}}]}""")))
    }

    @Test fun browseCardsRemainBrowseCardsDespitePlayableOverlays() {
        for (id in listOf("MPRE_album", "UC_artist", "VL_playlist")) {
            val row = item("""{"navigationEndpoint":{"browseEndpoint":{"browseId":"$id"}},
                "overlay":{"musicItemThumbnailOverlayRenderer":{"content":{
                    "musicPlayButtonRenderer":{"playNavigationEndpoint":{"watchEndpoint":{"videoId":"wrong000001"}}}}}}}""")
            assertEquals(id, MusicItemIdentity.browseId(row))
            assertNull(MusicItemIdentity.videoId(row))
        }
    }

    @Test fun legacyVideoBrowseIdsMustHaveAValidVideoId() {
        assertEquals("right000005", MusicItemIdentity.videoId(item("""{
            "navigationEndpoint":{"browseEndpoint":{"browseId":"MPEDright000005"}}}""")))
        assertNull(MusicItemIdentity.videoId(item("""{
            "navigationEndpoint":{"browseEndpoint":{"browseId":"MPEDinvalid"}}}""")))
    }

    @Test fun onTapAndTwoRowTitleEndpointsAreSupported() {
        assertEquals("right000006", MusicItemIdentity.videoId(item("""{
            "onTap":{"watchEndpoint":{"videoId":"right000006"}}}""")))
        assertEquals("right000007", MusicItemIdentity.videoId(item("""{
            "title":{"runs":[{"text":"Faixa","navigationEndpoint":{"watchEndpoint":{"videoId":"right000007"}}}]}}""")))
    }

    @Test fun malformedIdsCannotBecomeTracks() {
        for (value in listOf("", "short", "bad id!!!!!", "https://youtube.com/watch?v=right000001")) {
            assertNull(MusicItemIdentity.videoId(item("""{"playlistItemData":{"videoId":"$value"}}""")))
        }
    }
}
