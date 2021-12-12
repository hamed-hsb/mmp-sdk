package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 12th October 2021
 */

enum class BackoffStrategy(
    var minRetries: Int,                // retries before starting backoff
    var milliSecondMultiplier: Long,
    var maxWait: Long,
    var minRange: Double,
    var maxRange: Double
) {

    LONG_WAIT(
        1,                                    // min retries
        2 * Constants.ONE_MINUTE,   // milliseconds multiplier
        24 * Constants.ONE_HOUR,              // max wait time
        0.5,                                // min jitter multiplier
        1.0                                // max jitter multiplier
    ),

    // 0.1-0.2, 0.2-0.4, 0.4-0.8, ... 1h
    SHORT_WAIT(
        1,                                    // min retries
        2 * Constants.ONE_MINUTE,   // milliseconds multiplier
        24 * Constants.ONE_HOUR,              // max wait time
        0.5,                                // min jitter multiplier
        1.0                                // max jitter multiplier
    ),


    // max jitter multiplier
    TEST_WAIT(
        1,                     // min retries
        200,         // milliseconds multiplier
        1000,                  // max wait time
        0.5,                 // min jitter multiplier
        1.0                 // max jitter multiplier
    ),

    NO_WAIT(
        100,                    // min retries
        1,            // milliseconds multiplier
        Constants.ONE_SECOND,             // max wait time
        1.0,                  // min jitter multiplier
        1.0                  // max jitter multiplier
    )
}