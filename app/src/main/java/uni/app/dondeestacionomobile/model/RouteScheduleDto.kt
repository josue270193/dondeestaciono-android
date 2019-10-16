package uni.app.dondeestacionomobile.model

import uni.app.dondeestacionomobile.model.enumerate.RouteTypePermit

class RouteScheduleDto {

    var permit: RouteTypePermit? = null
    var details: List<RouteScheduleDetailDto> = arrayListOf()
}
