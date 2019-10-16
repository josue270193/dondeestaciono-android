package uni.app.dondeestacionomobile.model

class RouteDto {

    var id: String? = null
    var points: List<PointDto> = arrayListOf()
    var schedule = RouteScheduleDto()
}
