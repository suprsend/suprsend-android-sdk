package app.suprsend

interface UserTokenFetcher {
    fun getToken(distinctId:String):String
}