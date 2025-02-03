package app.suprsend.log

interface LoggerCallback {

    fun v(tag: String, message: String)

    fun i(tag: String, message: String)

    fun e(tag: String, message: String, throwable: Throwable? = null)
}