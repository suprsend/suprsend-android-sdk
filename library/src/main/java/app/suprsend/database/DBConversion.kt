package app.suprsend.database

internal object DBConversion {
    fun longToBoolean(value: Long): Boolean {
        return value == 1L
    }

    fun booleanToLong(value: Boolean): Int {
        return if (value)
            1
        else
            0
    }
}
