package uni.app.dondeestacionomobile.model

class Ruta {

    var id: String? = null
    var nombre: String? = null
    var puntos: List<Coordenada> = arrayListOf()
    var linea = LineaEstilo()
}
