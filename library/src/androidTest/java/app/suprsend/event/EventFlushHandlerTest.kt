package app.suprsend.event

import app.suprsend.base.BaseTest
import app.suprsend.base.SSConstants
import app.suprsend.config.ConfigHelper
import app.suprsend.database.Event_Model
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class EventFlushHandlerTest: BaseTest() {

    @Test
    fun eventFlush() {

        val events = arrayListOf<Event_Model>()

        events.add(
            Event_Model(
                "{}",
                1,
                System.currentTimeMillis(),
                UUID.randomUUID().toString(),
                1
            )
        )
        val response = EventFlushHandler.flushEvents(events)

        Assert.assertEquals(202,response.statusCode)
    }
}