package org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout

import java.time.LocalDate
import org.freekode.tp2intervals.domain.ExternalData
import org.freekode.tp2intervals.domain.Platform
import org.freekode.tp2intervals.domain.librarycontainer.LibraryContainer
import org.freekode.tp2intervals.domain.workout.Workout
import org.freekode.tp2intervals.domain.workout.WorkoutDetails
import org.freekode.tp2intervals.domain.workout.WorkoutRepository
import org.freekode.tp2intervals.infrastructure.PlatformException
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.IntervalsApiClient
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.configuration.IntervalsConfigurationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class IntervalsWorkoutRepository(
    private val intervalsApiClient: IntervalsApiClient,
    private val intervalsConfigurationRepository: IntervalsConfigurationRepository,
) : WorkoutRepository {

    private val log = LoggerFactory.getLogger(this.javaClass)
    private val maxWorkoutsToSave = 10

    override fun platform() = Platform.INTERVALS

    override fun saveWorkoutsToCalendar(workouts: List<Workout>) {
        if (workouts.isEmpty()) {
            // The bulk endpoint is the only one that upserts on external_id, and an
            // empty import must stay a no-op rather than become an empty POST.
            return
        }
        val toIntervalsWorkoutConverter = ToIntervalsWorkoutConverter()
        val athleteId = intervalsConfigurationRepository.getConfiguration().athleteId
        val requests = workouts.map { toIntervalsWorkoutConverter.createEventRequestDTO(it) }
        // A workout with no source id has no external_id, so it has nothing to
        // upsert on. Asking the server to match on an absent key has no safe
        // answer; these are created outright, as they were before this fix.
        val (keyed, keyless) = requests.partition { it.external_id != null }
        keyed.chunked(maxWorkoutsToSave)
            .forEach { batch -> send(batch) { intervalsApiClient.upsertEvents(athleteId, it) } }
        keyless.chunked(maxWorkoutsToSave)
            .forEach { batch -> send(batch) { intervalsApiClient.createEvents(athleteId, it) } }
    }

    private fun send(
        requests: List<CreateEventRequestDTO>,
        post: (List<CreateEventRequestDTO>) -> List<CreateEventResponseDTO>?,
    ) {
        // The Feign client sets dismiss404 = true, so a 404 on this path would
        // otherwise return normally and a calendar copy that created nothing
        // would be reported as a success.
        val response = post(requests)
        if (response == null || response.size != requests.size) {
            throw PlatformException(
                Platform.INTERVALS,
                "intervals.icu confirmed ${response?.size ?: 0} of ${requests.size} calendar events",
            )
        }
        val sent = requests.mapNotNull { it.external_id }.toSet()
        val echoed = response.mapNotNull { it.external_id }.toSet()
        if (!echoed.containsAll(sent)) {
            // The exact symptom that made the previous `uid`-based fix fail
            // silently in production: the field is accepted on write and absent
            // from every read, so the next import cannot match on it.
            log.warn(
                "intervals.icu did not return external_id for {} of {} events; " +
                    "repeated imports of this window will duplicate",
                sent.size - echoed.intersect(sent).size, requests.size,
            )
        }
    }

    override fun saveWorkoutsToLibrary(libraryContainer: LibraryContainer, workouts: List<Workout>) {
        val toIntervalsWorkoutConverter = ToIntervalsWorkoutConverter()
        for (fromIndex in workouts.indices step maxWorkoutsToSave) {
            val toIndex =
                if (fromIndex + maxWorkoutsToSave >= workouts.size) workouts.size else fromIndex + maxWorkoutsToSave

            val workoutsToSave = workouts.subList(fromIndex, toIndex)
            val requests =
                workoutsToSave.map { toIntervalsWorkoutConverter.createWorkoutRequestDTO(libraryContainer, it) }
            intervalsApiClient.createWorkouts(intervalsConfigurationRepository.getConfiguration().athleteId, requests)
        }
    }

    override fun getWorkoutsFromCalendar(startDate: LocalDate, endDate: LocalDate): List<Workout> {
        val configuration = intervalsConfigurationRepository.getConfiguration()
        val events = intervalsApiClient.getEvents(
            configuration.athleteId,
            startDate.toString(),
            endDate.toString(),
            configuration.powerRange,
            configuration.hrRange,
            configuration.paceRange,
        )
        return events
            .filter { it.isWorkout() }
            .mapNotNull { toWorkout(it) }
    }

    override fun getWorkoutFromLibrary(externalData: ExternalData): Workout {
        TODO("Not yet implemented")
    }

    override fun findWorkoutsFromLibraryByName(name: String): List<WorkoutDetails> {
        TODO("Not yet implemented")
    }

    override fun getWorkoutsFromLibrary(libraryContainer: LibraryContainer): List<Workout> {
        TODO("Not yet implemented")
    }

    override fun deleteWorkoutsFromCalendar(startDate: LocalDate, endDate: LocalDate) {
        TODO("Not yet implemented")
    }

    private fun toWorkout(eventDTO: IntervalsEventDTO): Workout? {
        return try {
            FromIntervalsWorkoutConverter(eventDTO).toWorkout()
        } catch (e: PlatformException) {
            log.warn("Can't convert a workout ${eventDTO.name} on ${eventDTO.start_date_local}, skipping...", e)
            return null
        }
    }
}
