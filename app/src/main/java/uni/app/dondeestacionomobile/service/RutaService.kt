package uni.app.dondeestacionomobile.service

import io.reactivex.Observable
import retrofit2.http.GET
import uni.app.dondeestacionomobile.model.Ruta
import uni.app.dondeestacionomobile.util.WebClienteUtil

interface RutaService {
    @GET("ruta/")
    fun getAll(): Observable<List<Ruta>>

    companion object {
        fun create(): RutaService {
            val retrofit = WebClienteUtil.client
            return retrofit.create(RutaService::class.java)
        }
    }
}