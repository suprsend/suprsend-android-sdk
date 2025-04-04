package app.suprsend.base

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object TokenGenerator {

    fun generateToken(time: Long = System.currentTimeMillis() + (1000 * 60 * 60 * 48)): String {
        val expiresAt = Date(time)
        val secretKey = "supr"

        val token = JWT.create()
            .withHeader(mapOf("alg" to "HS256", "typ" to "JWT"))
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(secretKey))
        return token
    }

}