package org.freekode.tp2intervals.infrastructure.platform.intervalsicu

import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateEventRequestDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.PostMapping

class IntervalsApiClientTest {

    @Test
    fun `createEvent posts with upsertOnUid=true so a repeated import updates instead of duplicating`() {
        // IntervalsApiClientMock#createEvent is unimplemented and cannot observe the query
        // string a real Feign call would send, so this reads the mapping the Feign proxy is
        // built from directly — a test that fails if the parameter is dropped or changed.
        val method = IntervalsApiClient::class.java.getMethod(
            "createEvent",
            String::class.java,
            CreateEventRequestDTO::class.java,
        )
        val mapping = method.getAnnotation(PostMapping::class.java)

        assertEquals("/api/v1/athlete/{athleteId}/events?upsertOnUid=true", mapping.value.single())
    }
}
