package app.suprsend.base

internal object EventSanitation {

    fun processKey(value: String): String {
        return if (value.length > SSConstants.EVENT_KEY_MAX_LENGTH) {
            value.substring(0, SSConstants.EVENT_KEY_MAX_LENGTH - 1)
        } else {
            value
        }
    }

    fun processValue(value: String): String {
        return if (value.length > SSConstants.EVENT_VALUE_MAX_LENGTH) {
            value.substring(0, SSConstants.EVENT_VALUE_MAX_LENGTH - 1)
        } else {
            value
        }
    }
}
