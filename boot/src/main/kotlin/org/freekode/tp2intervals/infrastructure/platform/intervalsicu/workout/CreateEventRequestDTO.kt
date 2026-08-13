package org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class CreateEventRequestDTO(
    val start_date_local: String,
    val name: String,
    val type: String,
    val category: String,
    val description: String,
    val external_id: String?,
    val uid: String?,
)
