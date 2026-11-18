package com.example.campushub.domain

import com.example.campushub.isFinished
import com.example.campushub.model.Event
import com.example.campushub.normalized

enum class AccessFilter { ALL, PUBLIC_ONLY, MY_COURSE }

enum class SituationFilter { ALL, UPCOMING, FINISHED }

data class EventFilters(
    val query: String = "",
    val course: String? = null,
    val access: AccessFilter = AccessFilter.ALL,
    val category: String? = null,
    val situation: SituationFilter = SituationFilter.ALL,
    val myCourse: String = ""
)

object EventFilter {

    /**
     * Aplica busca e filtros. Próximos eventos vêm primeiro (do mais perto ao mais longe),
     * depois os encerrados (do mais recente ao mais antigo).
     */
    fun apply(
        events: List<Event>,
        filters: EventFilters,
        now: Long = System.currentTimeMillis()
    ): List<Event> {
        val query = filters.query.trim().normalized()

        return events
            .filter { query.isEmpty() || it.title.normalized().contains(query) }
            .filter { filters.course == null || it.hostCourse == filters.course }
            .filter {
                when (filters.access) {
                    AccessFilter.ALL -> true
                    AccessFilter.PUBLIC_ONLY -> it.isPublic
                    AccessFilter.MY_COURSE -> it.hostCourse == filters.myCourse
                }
            }
            .filter { filters.category == null || it.category == filters.category }
            .filter {
                when (filters.situation) {
                    SituationFilter.ALL -> true
                    SituationFilter.UPCOMING -> !it.isFinished(now)
                    SituationFilter.FINISHED -> it.isFinished(now)
                }
            }
            .sortedWith(
                compareBy<Event>(
                    { it.isFinished(now) },
                    { if (it.isFinished(now)) -it.dateMillis else it.dateMillis }
                )
            )
    }
}