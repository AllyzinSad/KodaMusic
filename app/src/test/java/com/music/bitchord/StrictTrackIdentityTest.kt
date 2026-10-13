package com.music.bitchord

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.sources.TrackMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StrictTrackIdentityTest {
    private val target = TrackMatcher.Target("Midnight", "Original Artist", 210)
    private fun candidate(artist: String, duration: String = "3:30") =
        Song("other000001", "Midnight", artist, null, duration)

    @Test fun differentArtistCannotPassWithAnIdenticalRuntime() {
        assertNull(TrackMatcher.best(listOf(candidate("Unrelated Artist")), target))
    }
    @Test fun differentArtistCannotPassWithinTwoSeconds() {
        assertNull(TrackMatcher.best(listOf(candidate("Unrelated Artist", "3:29")), target))
    }
    @Test fun correctArtistAndRecordingStillMatch() {
        val correct = candidate("Original Artist")
        assertEquals(correct, TrackMatcher.best(listOf(candidate("Unrelated Artist"), correct), target))
    }
    @Test fun featuredCreditsKeepMatching() {
        val correct = candidate("Original Artist, Featured Artist")
        assertEquals(correct, TrackMatcher.best(listOf(correct), target))
    }
}
