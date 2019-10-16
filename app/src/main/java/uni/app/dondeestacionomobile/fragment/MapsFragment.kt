package uni.app.dondeestacionomobile.fragment

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import uni.app.dondeestacionomobile.R
import uni.app.dondeestacionomobile.databinding.FragmentMapsBinding
import uni.app.dondeestacionomobile.listener.INavigationDrawerAction
import uni.app.dondeestacionomobile.model.RouteDto
import uni.app.dondeestacionomobile.model.enumerate.RouteTypePermit
import uni.app.dondeestacionomobile.service.rest.RouteService

const val PATTERN_GAP_LENGTH_PX = 5.0f
const val ZOOM_MIN: Float = 15.0f
const val ZOOM_MAX: Float = 18.0f
const val REQUEST_CODE_AUTOCOMPLETE = 100
const val REQUEST_CODE_FINE_LOCATION = 200

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
    GoogleMap.OnPolygonClickListener, GoogleMap.OnCameraIdleListener {

    private lateinit var rutaService: RouteService
    private lateinit var mMap: GoogleMap
    private lateinit var intentSearch: Intent
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var navigationDrawerAction: INavigationDrawerAction

    private var disposable: Disposable? = null
    private var markers = arrayListOf<Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapsBinding.inflate(inflater, container, false)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.fragment_maps) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setUi(binding)
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        disposable?.dispose()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity as Activity)
        navigationDrawerAction = activity as INavigationDrawerAction
        if (!Places.isInitialized()) {
            val bundle = activity!!.packageManager.getApplicationInfo(
                activity!!.packageName,
                PackageManager.GET_META_DATA
            ).metaData
            Places.initialize(context, bundle["com.google.android.geo.API_KEY"] as String)
        }
//        val placesClient = Places.createClient(context)
        val fields = listOf(Place.Field.ID, Place.Field.NAME)
        val countryArgentina = "AR"
        val northEast = LatLng(-34.53, -58.35)
        val southWest = LatLng(-34.7, -58.55)
        val rectangle = RectangularBounds.newInstance(southWest, northEast)

        intentSearch =
            Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry(countryArgentina)
                .setLocationBias(rectangle)
                .build(context)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                Log.i("INFORMACION", "Place: " + place.name + ", " + place.id)
                if (place.latLng != null) {
                    moveMapCamera(place.latLng)
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                val status = Autocomplete.getStatusFromIntent(data!!)
                Log.i("INFORMACION", status.statusMessage)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMinZoomPreference(ZOOM_MIN)
        mMap.setMaxZoomPreference(ZOOM_MAX)
        mMap.setOnPolylineClickListener(this)
        mMap.setOnPolygonClickListener(this)
        mMap.setOnCameraIdleListener(this)

        rutaService = RouteService.create()
        setCiudad()
//        testRoute()
    }

    override fun onCameraIdle() {
        val latitude = mMap.cameraPosition.target.latitude
        val longitude = mMap.cameraPosition.target.longitude
        val radius = getMapVisibleRadius()
        Log.i("onCameraIdle", String.format("latitude: %s longitude: %s radius: %s", latitude, longitude, radius))
        cleanMap()
        disposable = rutaService.getByRadius(latitude, longitude,radius)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.i(
                        "onCameraIdle",
                        result.toString()
                    )
                    result.forEach { pintaRuta(it) }
                },
                { t -> Log.w("onCameraIdle", t.message, t) })
    }

    private fun cleanMap() {
        mMap.clear()
        markers.clear()
    }

    override fun onPolygonClick(polygon: Polygon?) {

    }

    override fun onPolylineClick(polyline: Polyline) {
//        if ((polyline.pattern == null) || (!polyline.pattern!!.contains(dot))) {
//            polyline.pattern = patternPolyneDotted
        showInformationRuta(polyline)
//        } else {
//            polyline.pattern = null
//        }
    }

    private fun setUi(binding: FragmentMapsBinding) {
        val searchBoxText = binding.textSearchBoxMaps
        searchBoxText.setOnClickListener {
            triggerSearch()
        }

        val openMenuButton = binding.buttonOpenMenuMaps
        openMenuButton.setOnClickListener {
            navigationDrawerAction.openDrawer()
        }

        val myLocationButton = binding.buttonMyLocationMaps
        myLocationButton.setOnClickListener {
            setMyLocation()
//            myLocationButton.backgroundTintList =
//                ColorStateList.valueOf(getColor(activity as Context, R.color.colorAccent))
        }
    }

    private fun triggerSearch() {
        startActivityForResult(intentSearch, REQUEST_CODE_AUTOCOMPLETE)
    }

    @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
    private fun setMyLocation() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(activity as Context, *perms)) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.i("INFORMACION", location.toString())
                        moveMapCamera(location.latitude, location.longitude, 15.0F)
                    }
                }
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.app_name),
                REQUEST_CODE_FINE_LOCATION, *perms
            )
        }
    }

    private fun moveMapCamera(latitude: Double, longitude: Double, zoom: Float) {
        this.moveMapCamera(LatLng(latitude, longitude), zoom)
    }

    private fun moveMapCamera(location: LatLng?) {
        this.moveMapCamera(location, 15.0F)
    }

    private fun moveMapCamera(location: LatLng?, zoom: Float) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))
    }

    private fun pintaRuta(routeDto: RouteDto) {
        Log.i("PintaRuta", "Id: ${routeDto.id}")

        val lineWidth = 10.0F
        val pattern = when (routeDto.schedule.permit) {
            RouteTypePermit.PERMITIDO_ESTACIONAR_90_GRADO -> createPatternDot(1)
            RouteTypePermit.PERMITIDO_ESTACIONAR_45_GRADO -> createPatternDot(3)
            else -> null
        }
        val idColor = when (routeDto.schedule.permit) {
            RouteTypePermit.PROHIBIDO_ESTACIONAR -> R.color.colorLineProhibidoEstacionar
            RouteTypePermit.PROHIBIDO_ESTACIONAR_DETENERSE -> R.color.colorLineProhibidoDetenerse
            RouteTypePermit.PERMITIDO_ESTACIONAR -> R.color.colorLineHabilitado
            RouteTypePermit.PERMITIDO_ESTACIONAR_90_GRADO -> R.color.colorLineHabilitado2
            RouteTypePermit.PERMITIDO_ESTACIONAR_45_GRADO -> R.color.colorLineHabilitado3
            else -> R.color.colorLineDefault
        }
        val lineColor = ContextCompat.getColor(context!!, idColor)

        val polylineOptions = PolylineOptions()
            .clickable(true)
            .color(lineColor)
            .width(lineWidth)
            .pattern(pattern)

        routeDto.points.forEach {
            polylineOptions.add(LatLng(it.latitude!!, it.longitude!!))
        }

        val polyline1 = mMap.addPolyline(polylineOptions)
        polyline1.tag = routeDto.id

        // Se crear marker invisible
        val markerOptions = MarkerOptions()
            .position(polyline1.points.first())
            .title(routeDto.id)
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
        moveMapCamera(-34.6163605, -58.3805825, 15.5f)
    }

    private fun showInformationRuta(polyline: Polyline) {
        val marker = markers.find { it.tag?.equals(polyline.tag)!! }
        marker?.showInfoWindow()
    }

    private fun createPatternDot(multiple: Int): ArrayList<PatternItem> {
        val dot = Dot()
        val gap = Gap(PATTERN_GAP_LENGTH_PX * multiple)
        return arrayListOf(gap, dot)
    }

    private fun getMapVisibleRadius(): Double {
        val visibleRegion: VisibleRegion? = mMap.projection?.visibleRegion

        val distanceWidth = FloatArray(1)
        val distanceHeight = FloatArray(1)

        val farRight: LatLng? = visibleRegion?.farRight
        val farLeft: LatLng? = visibleRegion?.farLeft
        val nearRight: LatLng? = visibleRegion?.nearRight
        val nearLeft: LatLng? = visibleRegion?.nearLeft

        Location.distanceBetween(
            (farLeft!!.latitude + nearLeft!!.latitude) / 2,
            farLeft.longitude,
            (farRight!!.latitude + nearRight!!.latitude) / 2,
            farRight.longitude, distanceWidth
        )

        Location.distanceBetween(
            farRight.latitude,
            (farRight.longitude + farLeft.longitude) / 2,
            nearRight.latitude,
            (nearRight.longitude + nearLeft.longitude) / 2,
            distanceHeight
        )

        return Math.sqrt(
            (Math.pow(distanceWidth[0].toString().toDouble(), 2.0))
                    + Math.pow(distanceHeight[0].toString().toDouble(), 2.0)
        ) / 2
    }
}