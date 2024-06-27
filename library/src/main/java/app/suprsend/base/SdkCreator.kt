package app.suprsend.base

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal val executorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
internal val flushExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
internal val appExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }
internal val inboxExecutorService: ExecutorService by lazy { Executors.newFixedThreadPool(1) }