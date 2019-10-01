package uni.app.dondeestacionomobile.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import uni.app.dondeestacionomobile.R
import uni.app.dondeestacionomobile.databinding.FragmentMapsBinding
import uni.app.dondeestacionomobile.model.RouteDto
import uni.app.dondeestacionomobile.service.rest.RouteService

const val PATTERN_GAP_LENGTH_PX = 5.0f

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
    GoogleMap.OnPolygonClickListener {

    private val dot = Dot()
    private val gap = Gap(PATTERN_GAP_LENGTH_PX)
    private val patternPolyneDotted = arrayListOf(gap, dot)
    private val rutaService by lazy {
        RouteService.create()
    }

    private lateinit var mMap: GoogleMap
    private var disposable: Disposable? = null
    private var markers = arrayListOf<Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapsBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(ZOOM_MAX)
        mMap.setOnPolylineClickListener(this)
        mMap.setOnPolygonClickListener(this)

        setCiudad()
        testRoute()
    }

    private fun pintarLinea() {
        val polyline1 = mMap.addPolyline(
            PolylineOptions()
                .clickable(true)
                .add(
                    LatLng(-34.6163605, -58.3805825),
                    LatLng(-35.016, 143.321),
                    LatLng(-34.747, 145.592),
                    LatLng(-34.364, 147.891),
                    LatLng(-33.501, 150.217),
                    LatLng(-32.306, 149.248),
                    LatLng(-32.491, 147.309)
                )
        )
        polyline1.tag = "1"
    }

    private fun pintaRuta(routeDto: RouteDto) {
        Log.i("PintaRuta", "Id: ${routeDto.id} - Nombre: ${routeDto.nombre}")

        val polylineOptions = PolylineOptions()
            .clickable(true)
            .color(Color.parseColor(routeDto.linea.color))
            .width(routeDto.linea.ancho!!)

        routeDto.puntos.forEach {
            Log.i("PintaRuta", "${it.latitud} - ${it.longitud}")
            polylineOptions.add(LatLng(it.latitud!!, it.longitud!!))
        }

        val polyline1 = mMap.addPolyline(polylineOptions)
        polyline1.tag = routeDto.id

        // Se crear marker invisible
        val markerOptions = MarkerOptions()
            .position(polyline1.points.first())
            .title(routeDto.nombre?.split("-")?.first())
            .snippet(routeDto.nombre?.substring(routeDto.nombre?.indexOf("-")!!))
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.punto))
        val marker1 = mMap.addMarker(markerOptions)
        marker1.tag = routeDto.id
        markers.add(marker1)
    }

    private fun testRoute() {
        disposable = rutaService.getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.i("PruebaRest", "Total: ${result.size}")
                    result.forEach(this::pintaRuta)
                },
                { error ->
                    Log.w("PruebaRest", error.message, error)
                    Toast.makeText(activity, error.message, Toast.LENGTH_SHORT).show()
                }
            )
    }

    private fun setCiudad() {
//        val zoom = 11.5f
//        val point = LatLng(-34.5963605, -58.4235825)
        val zoom = 15.5f
        val point = LatLng(-34.6163605, -58.3805825)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, zoom))
    }

    override fun onPolygonClick(polygon: Polygon?) {

    }

    override fun onPolylineClick(polyline: Polyline) {
        if ((polyline.pattern == null) || (!polyline.pattern!!.contains(dot))) {
            polyline.pattern = patternPolyneDotted
            showInformationRuta(polyline)
        } else {
            polyline.pattern = null
        }
    }

    private fun showInformationRuta(polyline: Polyline) {
        val marker = markers.find { it.tag?.equals(polyline.tag)!! }
        marker?.showInfoWindow()
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    companion object {
        const val ZOOM_MAX: Float = 12.0f
    }
}