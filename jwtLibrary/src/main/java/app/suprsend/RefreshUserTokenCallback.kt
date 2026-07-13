package app.suprsend

interface RefreshUserTokenCallback {
    fun getToken(distinctId: String): String
}
