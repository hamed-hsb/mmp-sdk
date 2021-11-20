package com.motrack.sdk

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author yaya (@yahyalmh)
 * @since 20th November 2021
 */
@RunWith(AndroidJUnit4::class)
class ApplicationTest  {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.motrack.sdk.test.test", appContext.packageName)
    }
}