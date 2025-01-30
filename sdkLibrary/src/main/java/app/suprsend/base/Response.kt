package app.suprsend.base

sealed class Response<T> {

    data class Success<T>(val model: T) : Response<T>()

    data class Error<T>(val ex: Exception) : Response<T>()

    fun isSuccess(): Boolean {
        return this is Success<*>
    }

    fun getData(): T? {
        return (this as? Success<T>)?.model
    }

    fun getException(): Exception? {
        return (this as? Error<*>)?.ex
    }
}