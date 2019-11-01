package uni.app.dondeestacionomobile.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_details_maps.layout_corte_details
import kotlinx.android.synthetic.main.layout_details_maps.layout_estacionamiento_details
import kotlinx.android.synthetic.main.layout_details_maps.text_calle
import kotlinx.android.synthetic.main.layout_details_maps.text_creado
import kotlinx.android.synthetic.main.layout_details_maps.text_horario
import kotlinx.android.synthetic.main.layout_details_maps.text_tipo
import kotlinx.android.synthetic.main.layout_details_maps.text_tipo_estacionamiento
import kotlinx.android.synthetic.main.layout_details_maps.text_tweet
import org.threeten.bp.DayOfWeek
import org.threeten.bp.OffsetDateTime
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import uni.app.dondeestacionomobile.R
import uni.app.dondeestacionomobile.databinding.FragmentMapsBinding
import uni.app.dondeestacionomobile.listener.INavigationDrawerAction
import uni.app.dondeestacionomobile.model.BlockRouteDto
import uni.app.dondeestacionomobile.model.RouteDto
import uni.app.dondeestacionomobile.model.RouteScheduleDto
import uni.app.dondeestacionomobile.model.enumerate.TypeRouteBlock
import uni.app.dondeestacionomobile.model.enumerate.TypeRoutePermit
import uni.app.dondeestacionomobile.service.rest.RouteService
import uni.app.dondeestacionomobile.util.GoogleMapUtil
import uni.app.dondeestacionomobile.util.GoogleMapUtil.Companion.createMarker
import uni.app.dondeestacionomobile.util.GoogleMapUtil.Companion.getCenter
import uni.app.dondeestacionomobile.util.GoogleMapUtil.Companion.patternDot
import uni.app.dondeestacionomobile.util.ImageUtil.Companion.bitmapDescriptorFromVector
import uni.app.dondeestacionomobile.util.KeyboardUtil.Companion.hideSoftKeyboard
import uni.app.dondeestacionomobile.util.TimeUtil.Companion.calculateTimeLapse
import java.util.HashMap
import java.util.Timer
import java.util.TimerTask

const val REQUEST_CODE_FINE_LOCATION = 100
const val MAP_ESTACIONAMIENTO_LINE_WIDTH: Float = 10F
const val MAP_RADIUS_DEFAULT: Float = 16F
const val MAP_LATITUDE_DEFAULT: Double = -34.6163605
const val MAP_LONGITUDE_DEFAULT: Double = -58.3805825
const val MAP_INTERVAL_LOCATION: Long = 5000
const val MAP_SMALL_DISPLACEMENT: Float = 100F
const val MAP_MY_LOCATION_ZINDEX: Float = 100F
const val MAP_ZOOM_MIN: Float = 15.5F
const val MAP_ZOOM_ESTACIONAMIENTO: Float = 17F
const val MAP_ZOOM_MY_LOCATION: Float = 16F
const val TIMER_UPDATE: Long = 60000

const val WORD_COMA: String = ","
const val WORD_MONDAY: String = "MO"
const val WORD_TUESDAY: String = "TU"
const val WORD_WEDNESDAY = "WE"
const val WORD_THURSDAY = "TH"
const val WORD_FRIDAY = "FR"
const val WORD_SUNDAY = "SU"
const val WORD_SATURDAY = "SA"

class MapsFragment : Fragment(),
    OnMapReadyCallback,
    GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private lateinit var routeService: RouteService
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var timerDrawing: Timer
    private lateinit var map: GoogleMap
    private lateinit var navigationAction: INavigationDrawerAction

    private lateinit var myLocationButton: FloatingActionButton
    private lateinit var layoutDetails: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var searchBox: EditText

    private var disposable: Disposable? = null
    private var lastLocation: Location? = null
    private var lastLocationMarker: Marker? = null
    private var isMyLocationEnabled: Boolean = false
    private var isShowDetails: Boolean = false
    private var estacionamientos: HashMap<String, RouteDto> = hashMapOf()
    private var cortes: HashMap<String, BlockRouteDto> = hashMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMapsBinding.inflate(inflater, container, false)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.fragment_maps) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setUiConfiguration(binding)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        createTimerDrawing()
        if (isMyLocationEnabled) {
            setMyLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        timerDrawing.cancel()
        disposable?.dispose()
        stopMyLocationUpdates()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnPolylineClickListener(this)
        map.setOnPolygonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.setOnMapClickListener(this)
        map.setOnCameraIdleListener(this)
        map.setOnCameraMoveStartedListener(this)
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.maps_style))
        setMyLocation()
    }

    override fun onMapClick(position: LatLng?) {
        hideKeyboard()
    }

    override fun onCameraMoveStarted(reason: Int) {
        hideKeyboard()
        when (reason) {
            GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                collapseDetails()
                disableMyLocation()
            }
            GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> {
                collapseDetails()
            }
            GoogleMap.OnCameraMoveStartedListener.REASON_API_ANIMATION -> {

            }
        }
    }

    override fun onCameraIdle() {
        if (navigationAction.isEnabledEstacionamiento()) {
            val latitude = map.cameraPosition.target.latitude
            val longitude = map.cameraPosition.target.longitude
            val radius = GoogleMapUtil.getVisibleRadius(map)

            if (map.cameraPosition.zoom > MAP_ZOOM_MIN) {
                getEstacionamientos(latitude, longitude, radius)
            }
        }
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        if (marker != null) {
            disableMyLocation()
            showInformationCorte(marker)
        }
        return true
    }

    override fun onPolygonClick(polygon: Polygon?) {
        disableMyLocation()
    }

    override fun onPolylineClick(polyline: Polyline) {
        disableMyLocation()
        showInformationEstacionamiento(polyline)
    }

    @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
    private fun setMyLocation() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(activity as Context, *perms)) {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.app_name),
                REQUEST_CODE_FINE_LOCATION, *perms
            )
            setCiudad()
        }
    }

    private fun setUiConfiguration(binding: FragmentMapsBinding) {
        configurateServices()
        configurateMyLocation()

        navigationAction = activity as INavigationDrawerAction

        layoutDetails = binding.layoutDetailsMaps.layoutDetails
        bottomSheetBehavior =
            BottomSheetBehavior.from(layoutDetails)
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (!isShowDetails && newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        searchBox = binding.textSearchBoxMaps
        searchBox.setOnClickListener {
            triggerSearch()
        }

        val openMenuButton = binding.buttonMenuMaps
        openMenuButton.setOnClickListener {
            hideKeyboard()
            navigationAction.openDrawer()
        }

        myLocationButton = binding.buttonMyLocationMaps
        myLocationButton.setOnClickListener {
            isMyLocationEnabled = !isMyLocationEnabled
            if (isMyLocationEnabled) {
                setMyLocation()
            } else {
                disableMyLocation()
            }
        }

        if (navigationAction.isEnabledCorte()) {
            getCortes()
        }
        createTimerDrawing()
    }

    private fun configurateServices() {
        routeService = RouteService.create()
    }

    private fun configurateMyLocation() {
        locationClient = LocationServices.getFusedLocationProviderClient(context!!)
        locationRequest = LocationRequest()
        locationRequest.interval = MAP_INTERVAL_LOCATION
        locationRequest.fastestInterval = MAP_INTERVAL_LOCATION
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.smallestDisplacement = MAP_SMALL_DISPLACEMENT
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    lastLocation = locationResult.lastLocation
                    setUiMyLocation(lastLocation!!)
                }
            }
        }
    }

    private fun createTimerDrawing() {
        timerDrawing = Timer()
        val task = object : TimerTask() {
            override fun run() {
                (context as Activity).runOnUiThread {
                    cleanMap()
                    val result = estacionamientos.toMutableMap()
                    estacionamientos.clear()
                    result.forEach { pintarEstacionamiento(it.value) }
                }
            }
        }
        timerDrawing.schedule(task, TIMER_UPDATE, TIMER_UPDATE)
    }

    private fun stopMyLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    private fun disableMyLocation() {
        isMyLocationEnabled = false
        DrawableCompat.setTint(
            myLocationButton.drawable,
            ContextCompat.getColor(context!!, R.color.color_line_default)
        )
        stopMyLocationUpdates()
    }

    private fun getEstacionamientos(
        latitude: Double,
        longitude: Double,
        radius: Double
    ) {
        timerDrawing.cancel()
        if (navigationAction.isEnabledZona()) {
            disposable = routeService.getByRadius(latitude, longitude, radius)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        cleanMap()
                        estacionamientos.clear()
                        result.forEach { pintarEstacionamiento(it) }
                        createTimerDrawing()
                    },
                    { t -> Log.w("getDataEstacionamiento", t.message, t) })
        } else {
            disposable = routeService.getByPosition(latitude, longitude)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        cleanMap()
                        estacionamientos.clear()
                        result.forEach { pintarEstacionamiento(it) }
                        createTimerDrawing()
                    },
                    { t -> Log.w("Estacionamientos", t.message, t) })
        }
    }

    private fun getCortes() {
        disposable = routeService.getBlockRoute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                cortes.clear()
                result.forEach { pintarCorte(it) }
            }, { t -> Log.w("Cortes", t.message, t) })
    }

    private fun cleanMap() {
        map.clear()
        if (navigationAction.isEnabledCorte()) {
            val result = cortes.toMutableMap()
            cortes.clear()
            result.forEach { pintarCorte(it.value) }
        }
        if (lastLocation != null) {
            lastLocationMarker = createMarker(
                map,
                LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
                bitmapDescriptorFromVector(context!!, R.drawable.ic_my_location, 1),
                MAP_MY_LOCATION_ZINDEX
            )
        }
    }

    private fun triggerSearch() {

    }

    private fun setUiMyLocation(location: Location) {
        Log.i("MyLocation", location.toString())
        val position = LatLng(location.latitude, location.longitude)
        lastLocationMarker?.remove()
        lastLocationMarker = createMarker(
            map,
            position,
            bitmapDescriptorFromVector(context!!, R.drawable.ic_my_location, 1),
            MAP_MY_LOCATION_ZINDEX
        )
        var zoom = map.cameraPosition.zoom
        if (zoom < MAP_ZOOM_MY_LOCATION) {
            zoom = MAP_ZOOM_MY_LOCATION
        }
        moveMapCamera(position, zoom)

        DrawableCompat.setTint(
            myLocationButton.drawable,
            ContextCompat.getColor(context!!, R.color.color_accent)
        )
    }

    private fun moveMapCamera(location: LatLng?, zoom: Float) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))
    }

    private fun pintarEstacionamiento(routeDto: RouteDto) {
        val lineWidth = MAP_ESTACIONAMIENTO_LINE_WIDTH
        val pattern = when (routeDto.schedule.permit) {
            TypeRoutePermit.PERMITIDO_ESTACIONAR_90_GRADO -> patternDot(1)
            TypeRoutePermit.PERMITIDO_ESTACIONAR_45_GRADO -> patternDot(5)
            else -> null
        }
        val idColor = getColorBySchedule(routeDto.schedule)
        val lineColor = ContextCompat.getColor(context!!, idColor)

        val polylineOptions = PolylineOptions()
            .clickable(true)
            .color(lineColor)
            .width(lineWidth)
            .pattern(pattern)

        routeDto.points.forEach {
            polylineOptions.add(LatLng(it.latitude!!, it.longitude!!))
        }

        val polyline = map.addPolyline(polylineOptions)
        estacionamientos[polyline.id] = routeDto
    }

    private fun pintarCorte(block: BlockRouteDto) {
        val drawable = when (block.type) {
            TypeRouteBlock.MANIFESTACION -> bitmapDescriptorFromVector(
                context!!,
                R.drawable.ic_alerta_amarilla,
                2
            )
            TypeRouteBlock.OPERATIVO -> bitmapDescriptorFromVector(
                context!!,
                R.drawable.ic_alerta_amarilla,
                2
            )
            TypeRouteBlock.INCIDENTE -> bitmapDescriptorFromVector(
                context!!,
                R.drawable.ic_alerta_amarilla,
                2
            )
            else -> bitmapDescriptorFromVector(context!!, R.drawable.ic_block_black, 1)
        }
        val marker = createMarker(
            map,
            LatLng(block.point?.latitude!!, block.point?.longitude!!),
            drawable
        )
        cortes[marker.id] = block
    }

    private fun showInformationCorte(marker: Marker) {
        val corte = cortes[marker.id]
        if (corte != null) {
            var zoom = map.cameraPosition.zoom
            if (zoom < 17F) {
                zoom = 17F
            }
            moveMapCamera(marker.position, zoom)

            text_tipo.text = corte.type?.value
            text_creado.text = calculateTimeLapse(corte.started!!)
            text_tweet.text = corte.tweetData?.message

            layout_corte_details.visibility = View.VISIBLE
            layout_estacionamiento_details.visibility = View.GONE
            showDetails()
        }
    }

    private fun showInformationEstacionamiento(polyline: Polyline) {
        val route = estacionamientos[polyline.id]
        if (route != null) {
            val center = getCenter(polyline.points)
            var zoom = map.cameraPosition.zoom
            if (zoom < MAP_ZOOM_ESTACIONAMIENTO) {
                zoom = MAP_ZOOM_ESTACIONAMIENTO
            }
            moveMapCamera(center, zoom)

            text_calle.text =
                getString(R.string.text_numero_calle, route.details.altura, route.details.calle)
            text_horario.text = route.details.horario
            text_tipo_estacionamiento.text = route.schedule.permit?.value
            layoutDetails.background = when (getColorBySchedule(route.schedule)) {
                R.color.color_line_habilitado -> ContextCompat.getDrawable(
                    context!!,
                    R.drawable.shape_bottom_sheet_active
                )
                R.color.color_line_no_habilitado -> ContextCompat.getDrawable(
                    context!!,
                    R.drawable.shape_bottom_sheet_desactive
                )
                else -> ContextCompat.getDrawable(context!!, R.drawable.shape_bottom_sheet)
            }

            layout_corte_details.visibility = View.GONE
            layout_estacionamiento_details.visibility = View.VISIBLE
            showDetails()
        }
    }

    private fun getColorBySchedule(schedule: RouteScheduleDto): Int {
        val now = OffsetDateTime.now()
        var applyTime = false
        for (detail in schedule.details) {
            var foundDayWeek = false
            val daysWeek = detail.weekday?.split(WORD_COMA) ?: arrayListOf()
            for (dayWeekString in daysWeek) {
                var dayWeek: DayOfWeek? = null
                when {
                    dayWeekString.equals(WORD_MONDAY, true) -> dayWeek = DayOfWeek.MONDAY
                    dayWeekString.equals(WORD_TUESDAY, true) -> dayWeek = DayOfWeek.TUESDAY
                    dayWeekString.equals(WORD_WEDNESDAY, true) -> dayWeek = DayOfWeek.WEDNESDAY
                    dayWeekString.equals(WORD_THURSDAY, true) -> dayWeek = DayOfWeek.THURSDAY
                    dayWeekString.equals(WORD_FRIDAY, true) -> dayWeek = DayOfWeek.FRIDAY
                    dayWeekString.equals(WORD_SUNDAY, true) -> dayWeek = DayOfWeek.SUNDAY
                    dayWeekString.equals(WORD_SATURDAY, true) -> dayWeek = DayOfWeek.SATURDAY
                }
                if (dayWeek != null && now.dayOfWeek == dayWeek) {
                    foundDayWeek = true
                    break
                }
            }
            if (foundDayWeek) {
                if (detail.isAllDay != null) {
                    applyTime = detail.isAllDay
                }
                if (!applyTime && detail.startTime != null && detail.endTime != null) {
                    val day = OffsetDateTime.of(
                        detail.startTime.toLocalDate(),
                        now.toLocalTime(),
                        now.offset
                    )
                    applyTime = day.isAfter(detail.startTime) && day.isBefore(detail.endTime)
                }
            }
            if (applyTime) {
                break
            }
        }
        return when (schedule.permit) {
            TypeRoutePermit.PROHIBIDO_ESTACIONAR -> if (applyTime) R.color.color_line_no_habilitado else R.color.color_line_habilitado
            TypeRoutePermit.PROHIBIDO_ESTACIONAR_DETENERSE -> if (applyTime) R.color.color_line_no_habilitado else R.color.color_line_habilitado
            TypeRoutePermit.PERMITIDO_ESTACIONAR -> if (applyTime) R.color.color_line_habilitado else R.color.color_line_no_habilitado
            TypeRoutePermit.PERMITIDO_ESTACIONAR_90_GRADO -> if (applyTime) R.color.color_line_habilitado else R.color.color_line_no_habilitado
            TypeRoutePermit.PERMITIDO_ESTACIONAR_45_GRADO -> if (applyTime) R.color.color_line_habilitado else R.color.color_line_no_habilitado
            else -> R.color.color_line_default
        }
    }

    private fun showDetails() {
        if (!isShowDetails) {
            isShowDetails = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun collapseDetails() {
        if (isShowDetails) {
            layoutDetails.background =
                ContextCompat.getDrawable(context!!, R.drawable.shape_bottom_sheet)
            isShowDetails = false
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setCiudad() {
        val latitude = MAP_LATITUDE_DEFAULT
        val longitude = MAP_LONGITUDE_DEFAULT
        val radius = MAP_RADIUS_DEFAULT

        moveMapCamera(LatLng(latitude, longitude), radius)
    }

    private fun hideKeyboard() {
        hideSoftKeyboard(searchBox, activity)
    }
}
