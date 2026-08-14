package org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.freekode.tp2intervals.domain.ExternalData
import org.freekode.tp2intervals.domain.TrainingType
import org.freekode.tp2intervals.domain.workout.Workout
import org.freekode.tp2intervals.domain.workout.WorkoutDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ToIntervalsWorkoutConverterTest {
    private val converter = ToIntervalsWorkoutConverter()
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `sets external_id and uid from the trainingPeaks id and the workout date`() {
        // given
        val workout = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = "42", intervalsId = null, trainerRoadId = null),
        )

        // when
        val request = converter.createEventRequestDTO(workout)

        // then
        assertEquals("tp2intervals:workout:trainingPeaks:42:2026-08-19", request.external_id)
        assertEquals("tp2intervals:workout:trainingPeaks:42:2026-08-19", request.uid)
    }

    @Test
    fun `prefers trainingPeaks over trainerRoad when a workout carries both ids`() {
        // given
        val workout = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = "42", intervalsId = null, trainerRoadId = "7"),
        )

        // when
        val request = converter.createEventRequestDTO(workout)

        // then
        assertEquals("tp2intervals:workout:trainingPeaks:42:2026-08-19", request.uid)
    }

    @Test
    fun `falls back to trainerRoad then intervals ids in that order`() {
        // given
        val trainerRoadOnly = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = null, intervalsId = "999", trainerRoadId = "7"),
        )
        val intervalsOnly = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = null, intervalsId = "999", trainerRoadId = null),
        )

        // when
        val trainerRoadRequest = converter.createEventRequestDTO(trainerRoadOnly)
        val intervalsRequest = converter.createEventRequestDTO(intervalsOnly)

        // then
        assertEquals("tp2intervals:workout:trainerRoad:7:2026-08-19", trainerRoadRequest.uid)
        assertEquals("tp2intervals:workout:intervals:999:2026-08-19", intervalsRequest.uid)
    }

    @Test
    fun `leaves external_id and uid null when the workout carries no source id`() {
        // given
        val workout = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = null, intervalsId = null, trainerRoadId = null),
        )

        // when
        val request = converter.createEventRequestDTO(workout)

        // then
        assertNull(request.external_id)
        assertNull(request.uid)
    }

    @Test
    fun `keys two occurrences on different dates differently`() {
        // given
        val externalData = ExternalData(trainingPeaksId = "42", intervalsId = null, trainerRoadId = null)
        val firstOccurrence = workoutWith(date = LocalDate.parse("2026-08-19"), externalData = externalData)
        val secondOccurrence = workoutWith(date = LocalDate.parse("2026-08-26"), externalData = externalData)

        // when
        val firstRequest = converter.createEventRequestDTO(firstOccurrence)
        val secondRequest = converter.createEventRequestDTO(secondOccurrence)

        // then
        assertEquals("tp2intervals:workout:trainingPeaks:42:2026-08-19", firstRequest.uid)
        assertEquals("tp2intervals:workout:trainingPeaks:42:2026-08-26", secondRequest.uid)
    }

    @Test
    fun `keys a note and a workout differently even when they share a source id and date`() {
        // given: TrainingPeaks assigns notes and workouts independent id sequences that
        // can coincide on the same numeric value.
        val externalData = ExternalData(trainingPeaksId = "42", intervalsId = null, trainerRoadId = null)
        val date = LocalDate.parse("2026-08-19")
        val workout = workoutWith(date = date, externalData = externalData)
        val note = Workout.note(date, "a note", null, externalData)

        // when
        val workoutRequest = converter.createEventRequestDTO(workout)
        val noteRequest = converter.createEventRequestDTO(note)

        // then
        assertEquals("tp2intervals:workout:trainingPeaks:42:2026-08-19", workoutRequest.uid)
        assertEquals("tp2intervals:note:trainingPeaks:42:2026-08-19", noteRequest.uid)
    }

    @Test
    fun `omits external_id and uid from the serialized JSON when the workout carries no source id`() {
        // given
        val keyless = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = null, intervalsId = null, trainerRoadId = null),
        )
        val keyed = workoutWith(
            date = LocalDate.parse("2026-08-19"),
            externalData = ExternalData(trainingPeaksId = "42", intervalsId = null, trainerRoadId = null),
        )

        // when
        val keylessJson = objectMapper.writeValueAsString(converter.createEventRequestDTO(keyless))
        val keyedJson = objectMapper.writeValueAsString(converter.createEventRequestDTO(keyed))

        // then
        assertFalse(keylessJson.contains("external_id"), "keyless body must not send external_id at all: $keylessJson")
        assertFalse(keylessJson.contains("\"uid\""), "keyless body must not send uid at all: $keylessJson")
        assertTrue(keyedJson.contains("\"external_id\":\"tp2intervals:workout:trainingPeaks:42:2026-08-19\""))
        assertTrue(keyedJson.contains("\"uid\":\"tp2intervals:workout:trainingPeaks:42:2026-08-19\""))
    }

    private fun workoutWith(date: LocalDate, externalData: ExternalData): Workout {
        val details = WorkoutDetails(
            type = TrainingType.BIKE,
            name = "test workout",
            description = null,
            duration = null,
            load = null,
            externalData = externalData,
        )
        return Workout(details, date, null)
    }
}
