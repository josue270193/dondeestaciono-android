package uni.app.dondeestacionomobile.model

class RouteDto {

    var id: String? = null
    var nombre: String? = null
    var puntos: List<PointDto> = arrayListOf()
    var linea = LineStyleDto()
}
