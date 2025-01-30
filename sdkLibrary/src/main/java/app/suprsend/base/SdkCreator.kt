package app.suprsend.base

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal val sdkExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
internal val appExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }