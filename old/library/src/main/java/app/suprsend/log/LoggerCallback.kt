package app.suprsend.log

interface LoggerCallback {

    fun i(tag: String, message: String)

    fun e(tag: String, message: String, throwable: Throwable? = null)
}