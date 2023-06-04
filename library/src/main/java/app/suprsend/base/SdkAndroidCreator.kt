package app.suprsend.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import app.suprsend.database.SQLDataHelper
import app.suprsend.event.EventLocalDatasource
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("StaticFieldLeak")
internal object SdkAndroidCreator {

    // Keeping only application context here
    lateinit var context: Context

    fun isContextInitialized(): Boolean {
        return this::context.isInitialized
    }

    val deviceInfo: DeviceInfo by lazy { DeviceInfo() }

    val networkInfo: NetworkInfo by lazy { NetworkInfo() }
    val phoneNumberUtils: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    val eventLocalDatasource: EventLocalDatasource by lazy { EventLocalDatasource() }
    val sqlDataHelper: SQLDataHelper by lazy { SQLDataHelper(context) }

    internal fun getSharedPreference(key:String): SharedPreferences {
        return context.getSharedPreferences(key,Context.MODE_PRIVATE)
    }
}

@SuppressLint("SimpleDateFormat")
fun getReadableDate(date: Date = Date()): String {
    return SimpleDateFormat("dd-MM-yyyy").format(date)
}
