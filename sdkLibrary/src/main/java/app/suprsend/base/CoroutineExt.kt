package app.suprsend.base

import app.suprsend.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

val jobsMap = ConcurrentHashMap<String, Pair<Long, Job>>()

fun Job.executeWithThrottleLast(
    key: String,
    throttleTimeMillis: Long,
    info: String
): Job {
    val currentTime = System.currentTimeMillis()
    val (lastExecutionTime, existingJob) = jobsMap[key] ?: Pair(0L, null)

    Logger.i(SSConstants.TAG_SUPRSEND, "Job Scheduling : $info")
    val newJob = CoroutineScope(Dispatchers.IO).launch {
        Logger.i(SSConstants.TAG_SUPRSEND, "Job executing : $info")
        delay(throttleTimeMillis)
        start()
        runBlocking {
            join()
        }
        Logger.i(SSConstants.TAG_SUPRSEND, "Job completed : $info")
    }
    jobsMap[key] = Pair(currentTime, newJob)

    // Check if enough time has passed since the last execution
    if ((currentTime - lastExecutionTime) < throttleTimeMillis) {
        existingJob?.cancel() // Cancel any ongoing job for the key
        if (existingJob != null)
            Logger.i(SSConstants.TAG_SUPRSEND, "Canceling job : $info")
    }
    runBlocking {
        Logger.i(SSConstants.TAG_SUPRSEND, "Job waiting : $info")
        newJob.join()
    }
    return newJob
}