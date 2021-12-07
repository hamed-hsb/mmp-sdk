package com.motrack.sdk.imei

/**
 * @author yaya (@yahyalmh)
 * @since 07th December 2021
 */

class MotrackImei {
    companion object{
        var isImeiToBeRead = false

        fun readImei() {
            isImeiToBeRead = true
        }

        fun doNotReadImei() {
            isImeiToBeRead = false
        }
    }
}