# Integration

## Requirements

| Item                     | Version           |
| ------------------------ | ----------------- |
| Min SDK                  | 19                |
| Target / Compile SDK     | 33                |
| Kotlin                   | 1.3.72+           |
| JVM target               | 1.8               |
| Firebase Cloud Messaging | Required for push |

**Package:** `app.suprsend`  
**Maven coordinates:** `com.suprsend:native:<version>` (latest version: `2.0.3`)

## Installation

Add the SuprSend SDK artifact to your app `build.gradle` or `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.suprsend:native:X.X.X")
}
```

## Integration

### 1. Initialize SDK

Call `SuprSend.initialize()` in your `Application.onCreate()`.

```kotlin
import app.suprsend.SuprSend

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SuprSend.initialize(
            context = this,
            publicApiKey = "YOUR_PUBLIC_API_KEY"
        )
    }
}
```

| Params         | Description                                                                                                                    |
| -------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| context\*      | Application `Context`. SDK stores `applicationContext`.                                                                        |
| publicApiKey\* | This is public Key used to authenticate API calls to SuprSend. Get it in SuprSend dashboard **ApiKeys -> Public Keys** section |

### 2.Authenticate User

Authenticate user so that all the actions performed after authenticating will be w.r.t that user. This is mandatory step and need to be called before using any other method. This is usually performed after successful login and on reopening app to re-authenticate user.

```kotlin
// async variant
SuprSend.getInstance().identityAsync(
    distinctId = "YOUR_USER_ID",
    userToken = userTokenData,
    tenantId = "YOUR_TENANT_ID", // only needed in multi-tenant workspaces
    refreshUserToken = object : RefreshUserTokenCallback {
          override fun getToken(distinctId: String): String {
              return yourBackend.getSubscriberJwt(distinctId)
          }
      },
    actionStatusCallback = object : ActionStatusCallback {
        override fun onComplete(response: ApiResponse) {
            if (response.isSuccess()) {
                // user has logged in
            }
        }
    }
)

// sync variant — do not call on the main thread
val response = SuprSend.getInstance().identify(
    distinctId = "YOUR_USER_ID",
    userToken = userTokenData,
    tenantId = "YOUR_TENANT_ID",
    refreshUserToken = refreshUserToken
)
```

| Properties           | Description                                                                                                                                                                                                                               |
| -------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| distinctId\*         | Unique identifier to identify a user across platform.                                                                                                                                                                                     |
| userToken            | Mandatory when enhanced security mode is on. This is ES256 JWT token generated in your server-side. Refer [docs](https://docs.suprsend.com/docs/client-authentication#enhanced-security-mode-with-signed-user-token) to create userToken. |
| tenantId             | Needed only when your workspace has multiple tenants/brands. Scopes the identified users activity to that tenant. Its value must match `scope.tenant_id` in the `userToken` payload, else it raises a scoping error.                      |
| refreshUserToken     | This function is called by SDK internally to get new userToken before existing token is expired. The returned string is used as the new userToken.                                                                                        |
| actionStatusCallback | **Async only.** `ActionStatusCallback` invoked on the **main thread** with the `ApiResponse` from `identify`.                                                                                                                             |

#### 2.1 Check if user is authenticated

This method will check if user is authenticated i.e. distinctId is attached to SuprSend instance. To check for userToken also pass checkUserToken flag true.

```kotlin
SuprSend.getInstance().isIdentified(checkUserToken = true)
```

### 3. Reset user

This will remove user data from SuprSend instance. This is usually called on logout action.

```kotlin
// async variant
SuprSend.getInstance().resetAsync(
    unSubscribeNotification = true,
    actionStatusCallback = myCallback
)

// sync variant — do not call on the main thread
SuprSend.getInstance().reset(unSubscribeNotification = true)
```

| Params                  | Description                                                            |
| ----------------------- | ---------------------------------------------------------------------- |
| unSubscribeNotification | When `true`, removes the device FCM token from the subscriber profile. |

## Change active tenant

Use the below method to switch the active tenant of identified user.

```kotlin
SuprSend.changeTenant("YOUR_TENANT_ID")
```

## Response Structure

```kotlin
data class ApiResponse(
    val status: ResponseStatus,       // SUCCESS or ERROR
    val statusCode: Int? = null,
    val body: String? = null,
    val errorType: ErrorType? = null, // e.g. VALIDATION_ERROR, NETWORK_ERROR etc
    val exception: Exception? = null,
    val message: String? = null
) {
    fun isSuccess(): Boolean
}
```

### 4 ProGuard / R8 configuration

    If your release build uses code shrinking (`minifyEnabled true`):

    - **SDK version above 2.0.1** — ProGuard/R8 rules are bundled in the AAR via `consumerProguardFiles` and merged by Gradle automatically. No manual setup required.
    - **SDK version 2.0.1 or below** — add the following to your app's `proguard-rules.pro`:

    <CodeGroup>
      ```proguard proguard-rules.pro theme={"system"}
      # SuprSend SDK
      -dontwarn app.suprsend.**
      -keep class app.suprsend.** { *; }

      # JWT dependencies
      -keep class com.auth0.android.jwt.** { *; }
      -keep class sun.misc.** { *; }
      -keep class com.google.gson.** { *; }
      ```
    </CodeGroup>
