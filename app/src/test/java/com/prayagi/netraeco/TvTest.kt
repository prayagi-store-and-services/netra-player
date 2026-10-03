package com.prayagi.netraeco

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvTest {
    @Test fun playerIsTallerOnTv() {
        assertEquals(240, playerHeightDp(false))
        assertEquals(360, playerHeightDp(true))
    }

    @Test fun noPickerMessageIsReadable() = assertTrue(NO_PICKER_MESSAGE.isNotBlank())
}
