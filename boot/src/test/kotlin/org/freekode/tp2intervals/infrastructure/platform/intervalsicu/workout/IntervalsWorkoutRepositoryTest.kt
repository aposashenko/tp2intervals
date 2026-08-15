package org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout

import config.TestUtils
import config.mock.IntervalsApiClientMock
import config.mock.ObjectMapperFactory
import org.freekode.tp2intervals.domain.ExternalData
import org.freekode.tp2intervals.domain.TrainingType
import org.freekode.tp2intervals.domain.workout.Workout
import org.freekode.tp2intervals.domain.workout.WorkoutDetails
import org.freekode.tp2intervals.domain.workout.structure.SingleStep
import org.freekode.tp2intervals.domain.workout.structure.StepLength
import org.freekode.tp2intervals.domain.workout.structure.WorkoutStructure
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.IntervalsActivityDTO
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.IntervalsApiClient
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.activity.CreateActivityResponseDTO
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.configuration.IntervalsConfiguration
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.configuration.IntervalsConfigurationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.util.ResourceUtils
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.time.LocalDate

class IntervalsWorkoutRepositoryTest {
    private val objectMapper = ObjectMapperFactory.objectMapper()

    private val intervalsApiClient: IntervalsApiClient = IntervalsApiClientMock(
        objectMapper,
        ResourceUtils.getFile("classpath:intervals-events-response.json").inputStream()
    )

    private val intervalsConfigurationRepository: IntervalsConfigurationRepository =
        getIntervalsConfigurationRepository()

    private val intervalsWorkoutRepository =
        IntervalsWorkoutRepository(intervalsApiClient, intervalsConfigurationRepository)

    @Test
    fun `should parse hr workout`() {
        // when
        val workouts = intervalsWorkoutRepository.getWorkoutsFromCalendar(LocalDate.now(), LocalDate.now())

        // then
        assertTrue(workouts.isNotEmpty())

        val workout = findWorkoutWithName("hr test", workouts)
        val structure = workout.structure!!

        assertEquals(TrainingType.BIKE, workout.details.type)
        assertEquals(WorkoutStructure.TargetUnit.LTHR_PERCENTAGE, structure.target)
        assertEquals(5, structure.steps.size)
        // 10m 50-70% LTHR
        assertEquals(600.toLong(), (structure.steps[0] as SingleStep).length.value)
        assertEquals(50, (structure.steps[0] as SingleStep).target.start)
        assertEquals(70, (structure.steps[0] as SingleStep).target.end)
        // 10m 80% LTHR
        assertEquals(78, (structure.steps[1] as SingleStep).target.start)
        assertEquals(82, (structure.steps[1] as SingleStep).target.end)
        // 10m ramp 45-60% LTHR
        assertEquals(45, (structure.steps[2] as SingleStep).target.start)
        assertEquals(60, (structure.steps[2] as SingleStep).target.end)
        (structure.steps[2] as SingleStep).ramp
        // 10m Z2 HR
        assertEquals(68, (structure.steps[3] as SingleStep).target.start)
        assertEquals(82, (structure.steps[3] as SingleStep).target.end)
        // 10m Z3-Z4 HR
        assertEquals(83, (structure.steps[4] as SingleStep).target.start)
        assertEquals(105, (structure.steps[4] as SingleStep).target.end)
    }

    @Test
    fun `should parse power workout`() {
        // when
        val workouts = intervalsWorkoutRepository.getWorkoutsFromCalendar(LocalDate.now(), LocalDate.now())

        // then
        assertTrue(workouts.isNotEmpty())


        val workout = findWorkoutWithName("power test", workouts)
        val structure = workout.structure!!

        assertEquals(TrainingType.BIKE, workout.details.type)
        assertEquals(WorkoutStructure.TargetUnit.FTP_PERCENTAGE, structure.target)
        assertEquals(5, structure.steps.size)
        // 10m 10-30%
        assertEquals(600.toLong(), (structure.steps[0] as SingleStep).length.value)
        assertEquals(10, (structure.steps[0] as SingleStep).target.start)
        assertEquals(30, (structure.steps[0] as SingleStep).target.end)
        // 10m 40% 30-90rpm
        assertEquals(39, (structure.steps[1] as SingleStep).target.start)
        assertEquals(41, (structure.steps[1] as SingleStep).target.end)
        assertEquals(30, (structure.steps[1] as SingleStep).cadence!!.start)
        assertEquals(90, (structure.steps[1] as SingleStep).cadence!!.end)
        // 10m ramp 10-60%
        assertEquals(10, (structure.steps[2] as SingleStep).target.start)
        assertEquals(60, (structure.steps[2] as SingleStep).target.end)
        (structure.steps[2] as SingleStep).ramp
        // 10m Z2 85rpm
        assertEquals(56, (structure.steps[3] as SingleStep).target.start)
        assertEquals(75, (structure.steps[3] as SingleStep).target.end)
        assertEquals(85, (structure.steps[3] as SingleStep).cadence!!.start)
        assertEquals(85, (structure.steps[3] as SingleStep).cadence!!.end)
        // 10m Z3-Z4
        assertEquals(76, (structure.steps[4] as SingleStep).target.start)
        assertEquals(105, (structure.steps[4] as SingleStep).target.end)
    }

    @Test
    fun `should parse pace workout`() {
        // when
        val workouts = intervalsWorkoutRepository.getWorkoutsFromCalendar(LocalDate.now(), LocalDate.now())

        // then
        assertTrue(workouts.isNotEmpty())


        val workout = findWorkoutWithName("pace test", workouts)
        val structure = workout.structure!!

        assertEquals(TrainingType.RUN, workout.details.type)
        assertEquals(WorkoutStructure.TargetUnit.PACE_PERCENTAGE, structure.target)
        assertEquals(4, structure.steps.size)
        // 10m 70% Pace
        assertEquals(600.toLong(), (structure.steps[0] as SingleStep).length.value)
        assertEquals(68, (structure.steps[0] as SingleStep).target.start)
        assertEquals(72, (structure.steps[0] as SingleStep).target.end)
        // 10m 80-110% Pace
        assertEquals(80, (structure.steps[1] as SingleStep).target.start)
        assertEquals(110, (structure.steps[1] as SingleStep).target.end)
        // 10m Z2 Pace
        assertEquals(79, (structure.steps[2] as SingleStep).target.start)
        assertEquals(88, (structure.steps[2] as SingleStep).target.end)
        // 10m Z3-Z5 Pace
        assertEquals(89, (structure.steps[3] as SingleStep).target.start)
        assertEquals(103, (structure.steps[3] as SingleStep).target.end)
    }

    @Test
    fun `should parse pace workout with distance based steps`() {
        // when
        val workouts = intervalsWorkoutRepository.getWorkoutsFromCalendar(LocalDate.now(), LocalDate.now())

        // then
        assertTrue(workouts.isNotEmpty())

        val workout = findWorkoutWithName("distance test", workouts)
        val structure = workout.structure!!

        assertEquals(TrainingType.SWIM, workout.details.type)
        assertEquals(WorkoutStructure.TargetUnit.PACE_PERCENTAGE, structure.target)
        assertEquals(4, structure.steps.size)

        TestUtils.assertStep(structure.steps[0], 800, StepLength.LengthUnit.METERS, 20, 20)
        TestUtils.assertStep(structure.steps[1], 500, StepLength.LengthUnit.METERS, 70, 70)
        TestUtils.assertStep(structure.steps[2], 600, StepLength.LengthUnit.SECONDS, 20, 20)
        TestUtils.assertStep(structure.steps[3], 300, StepLength.LengthUnit.METERS, 100, 100)
    }

    @Test
    fun `should parse virtual ride workout`() {
        // when
        val workouts = intervalsWorkoutRepository.getWorkoutsFromCalendar(LocalDate.now(), LocalDate.now())

        // then
        assertTrue(workouts.isNotEmpty())


        val workout = findWorkoutWithName("virtual ride test", workouts)
        assertEquals(TrainingType.VIRTUAL_BIKE, workout.details.type)
        assertEquals(WorkoutStructure.TargetUnit.FTP_PERCENTAGE, workout.structure!!.target)
        assertEquals(5, workout.structure!!.steps.size)
    }

    @Test
    fun `should parse other workout`() {
        // when
        val workouts = intervalsWorkoutRepository.getWorkoutsFromCalendar(LocalDate.now(), LocalDate.now())

        // then
        assertTrue(workouts.isNotEmpty())


        val workout = findWorkoutWithName("other test", workouts)
        assertEquals(TrainingType.UNKNOWN, workout.details.type)
        assertEquals(Duration.ofMinutes(45), workout.details.duration)
        assertEquals(32, workout.details.load)
        assertEquals(null, workout.structure)
    }

    @Test
    fun `saves a calendar copy through the bulk endpoint, carrying every workout's external_id`() {
        // given
        val recorder = RecordingIntervalsApiClient()
        val repository = IntervalsWorkoutRepository(recorder, intervalsConfigurationRepository)

        // when
        repository.saveWorkoutsToCalendar(listOf(calendarWorkout("11"), calendarWorkout("22")))

        // then: one request, not one per workout — the single-event endpoint cannot upsert
        // on external_id at all, so batching is what makes the mechanism reachable.
        assertEquals(1, recorder.calls.size)
        assertEquals(
            listOf(
                "tp2intervals:workout:trainingPeaks:11:2026-08-19",
                "tp2intervals:workout:trainingPeaks:22:2026-08-19",
            ),
            recorder.calls[0].map { it.external_id },
        )
    }

    @Test
    fun `chunks a long calendar copy into bounded bulk batches`() {
        // given
        val recorder = RecordingIntervalsApiClient()
        val repository = IntervalsWorkoutRepository(recorder, intervalsConfigurationRepository)

        // when
        repository.saveWorkoutsToCalendar((1..23).map { calendarWorkout(it.toString()) })

        // then
        assertEquals(listOf(10, 10, 3), recorder.calls.map { it.size })
        assertEquals(23, recorder.calls.flatten().size)
    }

    @Test
    fun `sends no request at all when there is nothing to import`() {
        // given
        val recorder = RecordingIntervalsApiClient()
        val repository = IntervalsWorkoutRepository(recorder, intervalsConfigurationRepository)

        // when
        repository.saveWorkoutsToCalendar(emptyList())

        // then: an empty import must stay a no-op rather than POST an empty array
        assertEquals(emptyList<List<CreateEventRequestDTO>>(), recorder.calls)
    }

    private fun calendarWorkout(trainingPeaksId: String): Workout {
        val details = WorkoutDetails(
            type = TrainingType.BIKE,
            name = "workout $trainingPeaksId",
            description = null,
            duration = null,
            load = null,
            externalData = ExternalData(trainingPeaksId = trainingPeaksId, intervalsId = null, trainerRoadId = null),
        )
        return Workout(details, LocalDate.parse("2026-08-19"), null)
    }

    private class RecordingIntervalsApiClient : IntervalsApiClient {
        val calls = mutableListOf<List<CreateEventRequestDTO>>()

        override fun createEvents(athleteId: String, createEventRequestDTOs: List<CreateEventRequestDTO>) {
            calls.add(createEventRequestDTOs)
        }

        override fun createWorkouts(athleteId: String, requests: List<CreateWorkoutRequestDTO>) =
            throw UnsupportedOperationException()

        override fun getEvents(
            athleteId: String,
            startDate: String,
            endDate: String,
            powerRange: Float,
            hrRange: Float,
            paceRange: Float,
        ): List<IntervalsEventDTO> = throw UnsupportedOperationException()

        override fun getActivities(
            athleteId: String,
            startDate: String,
            endDate: String,
        ): List<IntervalsActivityDTO> = throw UnsupportedOperationException()

        override fun createActivity(
            athleteId: String,
            name: String,
            file: MultipartFile,
        ): CreateActivityResponseDTO = throw UnsupportedOperationException()
    }

    private fun findWorkoutWithName(name: String, workouts: List<Workout>): Workout {
        return workouts.find { it.details.name == name }!!
    }

    private fun getIntervalsConfigurationRepository(): IntervalsConfigurationRepository {
        val repo = mock(IntervalsConfigurationRepository::class.java)
        `when`(repo.getConfiguration()).thenReturn(IntervalsConfiguration("apiKey", "athleteId", 0.1f, 0.2f, 0.3f))
        return repo
    }
}
