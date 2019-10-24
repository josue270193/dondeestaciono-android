package uni.app.dondeestacionomobile.model

import uni.app.dondeestacionomobile.model.enumerate.TypeRoutePermit

class RouteScheduleDto {

    var permit: TypeRoutePermit? = null
    var details: List<RouteScheduleDetailDto> = arrayListOf()
}
