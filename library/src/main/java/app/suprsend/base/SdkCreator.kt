package app.suprsend.base

import android.annotation.SuppressLint
import android.content.Context
import app.suprsend.database.SQLDataHelper
import app.suprsend.event.EventLocalDatasource
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal object SdkCreator

@SuppressLint("StaticFieldLeak")
internal lateinit var context: Context

internal val executorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
internal val flushExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
internal val appExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
