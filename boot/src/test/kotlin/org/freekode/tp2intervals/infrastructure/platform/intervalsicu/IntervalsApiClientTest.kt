package org.freekode.tp2intervals.infrastructure.platform.intervalsicu

import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateEventRequestDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.cloud.openfeign.support.SpringMvcContract

class IntervalsApiClientTest {

    @Test
    fun `createEvents posts to the bulk endpoint with upsert=true so a repeated import updates instead of duplicating`() {
        // IntervalsApiClientMock#createEvents is unimplemented and cannot observe the URL
        // a real Feign call would build, so this parses the mapping through the same
        // SpringMvcContract Feign itself uses to build the request template — proving what
        // actually reaches the request, not just that the annotation text is present.
        //
        // Both halves are asserted because either one alone is inert: `upsert` is offered
        // only by `/events/bulk`, so the parameter on the single-event path would be
        // silently ignored, and the bulk path without the parameter creates duplicates.
        val method = IntervalsApiClient::class.java.getMethod(
            "createEvents",
            String::class.java,
            List::class.java,
        )
        val metadata = SpringMvcContract().parseAndValidateMetadata(IntervalsApiClient::class.java, method)

        assertEquals("/api/v1/athlete/{athleteId}/events/bulk", metadata.template().path())
        assertEquals("?upsert=true", metadata.template().queryLine())
    }

    @Test
    fun `no mapping posts a single event, because that endpoint cannot upsert on external_id`() {
        // A regression rail for the mechanism itself. intervals.icu's single-event
        // `POST /events` accepts `upsertOnUid` only — it has no `upsert` parameter — and
        // the live account returned our `external_id` but never our `uid` under API-key
        // authentication. Re-introducing a single-event create would restore duplicate
        // creation while every other test here stayed green.
        val singleEventPosts = IntervalsApiClient::class.java.methods
            .filter { method -> method.parameterTypes.any { it == CreateEventRequestDTO::class.java } }
            .map { it.name }
            .sorted()

        assertEquals(emptyList<String>(), singleEventPosts)
    }
}
