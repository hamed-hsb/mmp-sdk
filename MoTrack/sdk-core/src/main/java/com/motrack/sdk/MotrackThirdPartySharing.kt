package com.motrack.sdk

/**
 * @author yaya (@yahyalmh)
 * @since 06th October 2021
 */

class MotrackThirdPartySharing(val isEnabled: Boolean?) {
    var granularOptions: HashMap<String, HashMap<String, String>> = HashMap()

    public fun addGranularOption(partnerName: String?, key: String?, value: String?) {
        if (partnerName.isNullOrEmpty() || key.isNullOrEmpty() || value.isNullOrEmpty()) {
            MotrackFactory.getLogger().error("Can not add granular option with any null value")
            return
        }
        var partnerOptions = granularOptions[partnerName]
        if (partnerOptions == null) {
            partnerOptions = HashMap()
            granularOptions[partnerName] = partnerOptions
        }

        partnerOptions[key] = value
    }
}