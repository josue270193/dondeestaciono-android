package uni.app.dondeestacionomobile.model

import org.threeten.bp.OffsetDateTime

class RouteScheduleDetailDto {

    val weekday: String? = null
    val startTime: OffsetDateTime? = null
    val endTime: OffsetDateTime? = null
    val exceptions: List<String>? = arrayListOf()
    val isAllDay: Boolean? = null
}
