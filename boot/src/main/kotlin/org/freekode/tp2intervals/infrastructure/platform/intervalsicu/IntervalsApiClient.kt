package org.freekode.tp2intervals.infrastructure.platform.intervalsicu

import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.activity.CreateActivityResponseDTO
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateEventRequestDTO
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateEventResponseDTO
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.CreateWorkoutRequestDTO
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.IntervalsEventDTO
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@FeignClient(
    value = "IntervalsApiClient",
    url = "\${app.intervals.api-url}",
    dismiss404 = true,
    primary = false,
    configuration = [IntervalsApiClientConfig::class]
)
interface IntervalsApiClient {

    @PostMapping("/api/v1/athlete/{athleteId}/workouts/bulk")
    fun createWorkouts(
        @PathVariable athleteId: String,
        @RequestBody requests: List<CreateWorkoutRequestDTO>
    )

    /**
     * Creates calendar events, updating any event that already carries the same
     * `external_id` instead of adding a second copy.
     *
     * `upsert` is offered only by the **bulk** endpoint; the single-event
     * `POST /events` offers `upsertOnUid` and nothing else. Sending one event at
     * a time therefore cannot be made idempotent on `external_id` at all, which
     * is why the calendar path posts a list even for a single workout.
     *
     * Every event sent here carries an `external_id`. Sending one that does not
     * would ask the server to match on an absent key, which is not a question
     * with a safe answer — see [createEvents].
     */
    @PostMapping("/api/v1/athlete/{athleteId}/events/bulk?upsert=true")
    fun upsertEvents(
        @PathVariable athleteId: String,
        @RequestBody createEventRequestDTOs: List<CreateEventRequestDTO>
    ): List<CreateEventResponseDTO>?

    /**
     * Creates calendar events unconditionally, for workouts that carry no source
     * id and therefore no `external_id`.
     *
     * These cannot go through [upsertEvents]: what a server does when asked to
     * upsert on an absent key is undefined, and one plausible answer — collapse
     * them onto each other — silently loses planned workouts. Creating them is
     * exactly what this importer did before the upsert existed, so it is the
     * behaviour with a known failure mode rather than an unknown one.
     */
    @PostMapping("/api/v1/athlete/{athleteId}/events/bulk")
    fun createEvents(
        @PathVariable athleteId: String,
        @RequestBody createEventRequestDTOs: List<CreateEventRequestDTO>
    ): List<CreateEventResponseDTO>?

    @GetMapping(
        "/api/v1/athlete/{athleteId}/events?" +
                "oldest={startDate}&" +
                "newest={endDate}&" +
                "resolve=true&" +
                "powerRange={powerRange}&" +
                "hrRange={hrRange}&" +
                "paceRange={paceRange}"
    )
    fun getEvents(
        @PathVariable athleteId: String,
        @PathVariable startDate: String,
        @PathVariable endDate: String,
        @PathVariable powerRange: Float,
        @PathVariable hrRange: Float,
        @PathVariable paceRange: Float,
    ): List<IntervalsEventDTO>

    @GetMapping("/api/v1/athlete/{athleteId}/activities?oldest={startDate}&newest={endDate}")
    fun getActivities(
        @PathVariable("athleteId") athleteId: String,
        @PathVariable("startDate") startDate: String,
        @PathVariable("endDate") endDate: String,
    ): List<IntervalsActivityDTO>

    @PostMapping("/api/v1/athlete/{athleteId}/activities?name={name}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createActivity(
        @PathVariable athleteId: String,
        @PathVariable name: String,
        @RequestPart("file") file: MultipartFile
    ): CreateActivityResponseDTO
}
