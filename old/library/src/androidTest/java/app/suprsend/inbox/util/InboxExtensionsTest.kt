package app.suprsend.inbox.util

import org.junit.Assert
import org.junit.Test

class InboxExtensionsTest {

    @Test
    fun testReadableTime() {

        val currentTime = System.currentTimeMillis()

        val testCases = listOf(
            Pair(currentTime + 1_000L, "1 sec"),  // 1 sec in the future
            Pair(currentTime + 2_000L, "2 sec(s)"),  // 2 secs in the future
            Pair(currentTime + 60_000L, "1 min"), // 1 min in the future
            Pair(currentTime + 120_000L, "2 min(s)"), // 2 mins in the future
            Pair(currentTime + 3_600_000L, "1 hour"), // 1 hour in the future
            Pair(currentTime + 7_200_000L, "2 hour(s)"), // 2 hours in the future
            Pair(currentTime + 86_400_000L, "1 day"), // 1 day in the future
            Pair(currentTime + 172_800_000L, "2 day(s)"), // 2 days in the future
            Pair(currentTime + 2_592_000_000L, "1 month"), // 1 month in the future
            Pair(currentTime + 5_184_000_000L, "2 month(s)"), // 2 months in the future
            Pair(currentTime + 31_536_000_000L, "1 year"), // 1 year in the future
            Pair(currentTime + 63_072_000_000L, "2 year(s)"), // 2 years in the future

            Pair(currentTime - 1_000L, "1 sec ago"),  // 1 sec in the past
            Pair(currentTime - 2_000L, "2 sec(s) ago"),  // 2 secs in the past
            Pair(currentTime - 60_000L, "1 min ago"), // 1 min in the past
            Pair(currentTime - 120_000L, "2 min(s) ago"), // 2 mins in the past
            Pair(currentTime - 3_600_000L, "1 hour ago"), // 1 hour in the past
            Pair(currentTime - 7_200_000L, "2 hour(s) ago"), // 2 hours in the past
            Pair(currentTime - 86_400_000L, "1 day ago"), // 1 day in the past
            Pair(currentTime - 172_800_000L, "2 day(s) ago"), // 2 days in the past
            Pair(currentTime - 2_592_000_000L, "1 month ago"), // 1 month in the past
            Pair(currentTime - 5_184_000_000L, "2 month(s) ago"), // 2 months in the past
            Pair(currentTime - 31_536_000_000L, "1 year ago"), // 1 year in the past
            Pair(currentTime - 63_072_000_000L, "2 year(s) ago") // 2 years in the past
        )

        for ((timestamp, expected) in testCases) {
            val result = getReadableTime(timestamp, currentTime)
            Assert.assertEquals(
                expected,
                result
            )
        }
    }
}