# suprsend-android-sdk

#Release Notes

```
1.0.1
- Logging Support

0.1.10
- Compile with API 33
- Xiaomi sdk updated from 4.8.3 to 5.1.1
- Push Notification permission implemented
- remove push token on reset call
- Unsubscribe flag in reset

Developer Change
- Developers will have to add xiaomi sdk aar manually to the project
- Update proguard rules of app

0.1.9
- Set Preferred Language
- Tracking notification if deeplink is not provided(Notification without deeplink)

0.1.8
- Icon drawable (Small Icon & Action Icon) - If icon do not exist in drawable folder then notification was not getting shown

0.1.7
- Notification group issue fixed

0.1.4
- 0.1.3 - native sdk was having bug on xiaomi device app_launched was getting tracked only once

0.1.3
- Flutter sdk released

0.1.1
Commit - 75c794f1d0aeaf4f59ed8d94fecd259b8e91ce0b
- com.suprsend:android0.1Beta10 published on mavenCenteral()
- Fixed bug - Super properties not clear on reset

com.github.suprsend:suprsend-kmm-sdk:0.1Beta10
- Notification dismissed not getting tracked when app is in background
Commit -f97f43acb68ab458cc46df3d06b6730330cd0251

com.github.suprsend:suprsend-kmm-sdk:0.1Beta9
- Created separate method to init url for react native sdk

com.github.suprsend:suprsend-kmm-sdk:0.1Beta8
- Ignoring identify event if user identify id is same
https://github.com/suprsend/suprsend-android-sdk/pull/8

com.github.suprsend:suprsend-kmm-sdk:0.1Beta7
- One instance support
https://github.com/suprsend/suprsend-android-sdk/pull/7/files

com.github.suprsend:suprsend-kmm-sdk:0.1Beta6
- Fix app launch & app install were not getting tracked in beta 5 due to bug that $ was getting filtered

com.github.suprsend:suprsend-kmm-sdk:0.1Beta3
- Xiaomi Messaging Support
- commit 14020eb5be5d8be69e2cdd84e8be965421f1be4f

com.github.suprsend:suprsend-kmm-sdk:0.1Beta2
- Kotlin 1.3.72 support

com.github.suprsend:suprsend-kmm-sdk:0.1Beta1
- KMM Sdk compilation issue of kotlin 1.3.72
```
