package app.suprsend.base

enum class LogLevel(val num: Int) {
    VERBOSE(101),
    DEBUG(102),
    INFO(103),
    ERROR(104),
    OFF(Int.MAX_VALUE)
}
