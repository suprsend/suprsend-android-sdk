package app.suprsend

import android.app.Activity
import android.app.Application
import android.os.Bundle
import app.suprsend.base.Logger

internal class ActivityLifecycleCallbackHandler(
) : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.i("flush", "Lifecycle flush")
        SSApiInternal.flush()
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    companion object {
    }
}
