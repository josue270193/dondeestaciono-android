package uni.app.dondeestacionomobile.service.rest

import io.reactivex.Observable
import retrofit2.http.GET
import uni.app.dondeestacionomobile.model.RouteDto
import uni.app.dondeestacionomobile.util.WebClienteUtil

interface RouteService {
    @GET("route/")
    fun getAll(): Observable<List<RouteDto>>

    companion object {
        fun create(): RouteService {
            val retrofit = WebClienteUtil.client
            return retrofit.create(RouteService::class.java)
        }
    }
}