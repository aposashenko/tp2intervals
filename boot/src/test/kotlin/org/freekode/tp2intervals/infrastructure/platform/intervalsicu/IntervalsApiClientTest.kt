package org.freekode.tp2intervals.infrastructure.platform.intervalsicu

import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateEventRequestDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.cloud.openfeign.support.SpringMvcContract

class IntervalsApiClientTest {

    @Test
    fun `createEvent's Feign request template carries upsertOnUid=true so a repeated import updates instead of duplicating`() {
        // IntervalsApiClientMock#createEvent is unimplemented and cannot observe the query
        // string a real Feign call would send, so this parses the mapping through the same
        // SpringMvcContract Feign itself uses to build the request template — proving what
        // actually reaches the request, not just that the annotation text is present.
        val method = IntervalsApiClient::class.java.getMethod(
            "createEvent",
            String::class.java,
            CreateEventRequestDTO::class.java,
        )
        val metadata = SpringMvcContract().parseAndValidateMetadata(IntervalsApiClient::class.java, method)

        assertEquals("?upsertOnUid=true", metadata.template().queryLine())
    }
}
