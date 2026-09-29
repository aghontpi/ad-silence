package bluepie.ad_silence

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatchingTest {
    @Test
    fun matchesOnlyWhenAllNotificationFieldsAreEmpty() {
        assertTrue(matchesEmptyKeyword("allow-keyword-empty", "", "", ""))
        assertFalse(matchesEmptyKeyword("allow-keyword-empty", "Song", "", ""))
        assertFalse(matchesEmptyKeyword("allow-keyword-empty", "", "Artist", ""))
        assertFalse(matchesEmptyKeyword("allow-keyword-empty", "", "", "Album"))
    }

    @Test
    fun quotedOrDifferentKeywordsDoNotActivateEmptyMatching() {
        assertTrue(matchesEmptyKeyword("ALLOW-KEYWORD-EMPTY", "", "", ""))
        assertFalse(matchesEmptyKeyword("'allow-keyword-empty'", "", "", ""))
        assertFalse(matchesEmptyKeyword("ad", "", "", ""))
    }
}
