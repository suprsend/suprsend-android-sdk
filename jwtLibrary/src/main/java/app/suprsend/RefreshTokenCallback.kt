package app.suprsend

interface RefreshTokenCallback {
    fun getToken(distinctId: String): String
}