package uni.app.dondeestacionomobile.service.rest

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query
import uni.app.dondeestacionomobile.model.BlockRouteDto
import uni.app.dondeestacionomobile.model.RouteDto
import uni.app.dondeestacionomobile.util.WebClienteUtil

interface RouteService {
    @GET("route/filter")
    fun getByPosition(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): Observable<List<RouteDto>>

    @GET("route/filterByRadius")
    fun getByRadius(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double
    ): Observable<List<RouteDto>>

    @GET("route/block")
    fun getBlockRoute(): Observable<List<BlockRouteDto>>

    companion object {
        fun create(): RouteService {
            val retrofit = WebClienteUtil.client
            return retrofit.create(RouteService::class.java)
        }
    }
}