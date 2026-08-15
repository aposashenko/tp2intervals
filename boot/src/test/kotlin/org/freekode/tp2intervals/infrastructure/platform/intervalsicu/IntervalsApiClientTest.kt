package org.freekode.tp2intervals.infrastructure.platform.intervalsicu

import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateEventRequestDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.cloud.openfeign.support.SpringMvcContract
import org.springframework.web.bind.annotation.PostMapping

class IntervalsApiClientTest {

    @Test
    fun `upsertEvents posts to the bulk endpoint with upsert=true so a repeated import updates instead of duplicating`() {
        // IntervalsApiClientMock#upsertEvents cannot observe the URL a real Feign call
        // would build, so this parses the mapping through the same SpringMvcContract
        // Feign itself uses to build the request template — proving what actually
        // reaches the request, not just that the annotation text is present.
        //
        // Both halves are asserted because either one alone is inert: `upsert` is
        // offered only by `/events/bulk`, so the parameter on the single-event path
        // would be silently ignored, and the bulk path without the parameter creates
        // duplicates.
        val metadata = parse("upsertEvents")

        assertEquals("/api/v1/athlete/{athleteId}/events/bulk", metadata.template().path())
        assertEquals("?upsert=true", metadata.template().queryLine())
    }

    @Test
    fun `createEvents posts to the same bulk endpoint without upsert, for workouts that carry no external_id`() {
        // Asking a server to upsert on an absent key has no defined answer, and one
        // plausible answer collapses every keyless workout onto one event. These are
        // created outright instead.
        val metadata = parse("createEvents")

        assertEquals("/api/v1/athlete/{athleteId}/events/bulk", metadata.template().path())
        assertEquals("", metadata.template().queryLine())
    }

    @Test
    fun `the only event mappings are the two bulk ones, because the single-event endpoint cannot upsert on external_id`() {
        // A regression rail for the mechanism itself. intervals.icu's single-event
        // `POST /events` accepts `upsertOnUid` only — it has no `upsert` parameter —
        // and the live account returned our `external_id` but never our `uid` under
        // API-key authentication. Re-introducing a single-event create would restore
        // duplicate creation while every other test here stayed green.
        //
        // Asserted on the mapping paths rather than on parameter types: type erasure
        // makes `List<CreateEventRequestDTO>` indistinguishable from any other list,
        // so a parameter-type rail passes for a single-event method taking a list.
        val eventPostPaths = IntervalsApiClient::class.java.methods
            .mapNotNull { it.getAnnotation(PostMapping::class.java) }
            .flatMap { it.value.toList() }
            .filter { it.contains("/events") }
            .sorted()

        assertEquals(
            listOf(
                "/api/v1/athlete/{athleteId}/events/bulk",
                "/api/v1/athlete/{athleteId}/events/bulk?upsert=true",
            ),
            eventPostPaths,
        )
    }

    private fun parse(methodName: String) = SpringMvcContract().parseAndValidateMetadata(
        IntervalsApiClient::class.java,
        IntervalsApiClient::class.java.getMethod(methodName, String::class.java, List::class.java),
    )
}
