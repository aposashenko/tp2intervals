package org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout

import org.freekode.tp2intervals.domain.ExternalData
import org.freekode.tp2intervals.domain.TrainingType
import org.freekode.tp2intervals.domain.workout.Workout
import org.freekode.tp2intervals.domain.workout.WorkoutDetails
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ToIntervalsWorkoutConverterTest {
    private val converter = ToIntervalsWorkoutConverter()

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
        assertEquals("tp2intervals:trainingPeaks:42:2026-08-19", request.external_id)
        assertEquals("tp2intervals:trainingPeaks:42:2026-08-19", request.uid)
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
        assertEquals("tp2intervals:trainerRoad:7:2026-08-19", trainerRoadRequest.uid)
        assertEquals("tp2intervals:intervals:999:2026-08-19", intervalsRequest.uid)
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
        assertEquals("tp2intervals:trainingPeaks:42:2026-08-19", firstRequest.uid)
        assertEquals("tp2intervals:trainingPeaks:42:2026-08-26", secondRequest.uid)
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
