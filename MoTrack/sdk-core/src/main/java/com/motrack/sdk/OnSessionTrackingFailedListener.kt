package com.motrack.sdk

import com.motrack.sdk.MotrackSessionFailure

/**
 * @author yaya (@yahyalmh)
 * @since 17th October 2021
 */

interface OnSessionTrackingFailedListener {
    fun onFinishedSessionTrackingFailed(failureResponseData: MotrackSessionFailure)

}