package org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * The bulk event endpoint's response, read for two things only: how many events
 * intervals.icu confirmed, and whether it echoed back the `external_id` the
 * upsert matches on.
 *
 * Every field is nullable and unknown properties are ignored, so this can never
 * fail to deserialize and turn a working import into an error. It exists
 * because the client sets `dismiss404 = true`: without a response to count, a
 * 404 on the bulk path would be swallowed and a calendar copy that created
 * nothing would report success.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class CreateEventResponseDTO(
    val id: Long? = null,
    val external_id: String? = null,
)
