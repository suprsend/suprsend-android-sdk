package app.suprsend.android;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacGeneratation {
    String hmacRawURLSafeBase64String(String distinctId, String secret) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac sha256mac = getSha256macInstance(secret);
        byte[] macData = sha256mac.doFinal(distinctId.getBytes(StandardCharsets.UTF_8));
        String hmacString = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            hmacString = Base64.getUrlEncoder().withoutPadding().encodeToString(macData);
        }
        return hmacString;
    }

    private Mac getSha256macInstance(String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        final byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
        Mac sha256mac;
        try {
            sha256mac = Mac.getInstance("HmacSHA256");
            sha256mac.init(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (InvalidKeyException e) {
            throw e;
        }
        return sha256mac;
    }
}