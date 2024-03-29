package uni.app.dondeestacionomobile.service.rest

import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST
import uni.app.dondeestacionomobile.model.PhoneRegistrationDto
import uni.app.dondeestacionomobile.util.WebClienteUtil

interface NotificationService {
    @POST("notification/register")
    fun register(@Body phoneRegistrationDto: PhoneRegistrationDto): Observable<Void>

    companion object {
        fun create(): NotificationService {
            val retrofit = WebClienteUtil.client
            return retrofit.create(NotificationService::class.java)
        }
    }
}