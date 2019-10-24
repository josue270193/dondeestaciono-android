package uni.app.dondeestacionomobile.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.bottom_sheet_maps.*
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
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

const val PATTERN_GAP_LENGTH_PX = 5.0F
const val ZOOM_MIN: Float = 15.5F
const val REQUEST_CODE_FINE_LOCATION = 100
const val ZOOM_DEFAULT: Float = 16F
const val WIDTH_LINE_DEFAULT: Float = 10F
const val RADIUS_DEFAULT: Float = 16F
const val LATITUDE_DEFAULT: Double = -34.6163605
const val LONGITUDE_DEFAULT: Double = -58.3805825
const val INTERVAL_LOCATION_MILISECOND: Long = 5000
const val TIME_TIMER_UPDATE: Long = 60000

const val WORD_COMA: String = ","
const val WORD_MONDAY: String = "MO"
const val WORD_TUESDAY: String = "TU"
const val WORD_WEDNESDAY = "WE"
const val WORD_THURSDAY = "TH"
const val WORD_FRIDAY = "FR"
const val WORD_SUNDAY = "SU"
const val WORD_SATURDAY = "SA"

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener,
    GoogleMap.OnPolygonClickListener, GoogleMap.OnCameraIdleListener,
    GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener {

    private lateinit var rutaService: RouteService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback
    private lateinit var timer: Timer

    private lateinit var mMap: GoogleMap
    private lateinit var actionNavigation: INavigationDrawerAction
    private lateinit var myLocationButton: FloatingActionButton
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var disposable: Disposable? = null
    private var lastLocation: Location? = null
    private var lastLocationMarker: Marker? = null
    private var isMyLocationEnabled: Boolean = false
    private var isShowDetails: Boolean = false
    private var mapEstacionamiento: HashMap<String, RouteDto> = hashMapOf()
    private var mapCorte: HashMap<String, BlockRouteDto> = hashMapOf()

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

    override fun onResume() {
        super.onResume()
        createTimerUpdate()
        if (isMyLocationEnabled) {
            setMyLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
        disposable?.dispose()
        stopLocationUpdates()

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
        mMap = googleMap
        mMap.setOnPolylineClickListener(this)
        mMap.setOnPolygonClickListener(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener(this)
        mMap.setOnCameraIdleListener(this)
        mMap.setOnCameraMoveStartedListener(this)
        mMap.uiSettings.isCompassEnabled = true
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.maps_style))

        setCiudad()
    }

    override fun onMapClick(position: LatLng?) {

    }

    override fun onCameraMoveStarted(reason: Int) {
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
        if (actionNavigation.isEnabledEstacionamiento()) {
            val latitude = mMap.cameraPosition.target.latitude
            val longitude = mMap.cameraPosition.target.longitude
            val radius = getMapVisibleRadius()

            if (mMap.cameraPosition.zoom > ZOOM_MIN) {
                getDataEstacionamiento(latitude, longitude, radius)
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

    private fun setUi(binding: FragmentMapsBinding) {

        rutaService = RouteService.create()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        locationRequest = LocationRequest()
        locationRequest.interval = INTERVAL_LOCATION_MILISECOND
        locationRequest.fastestInterval = INTERVAL_LOCATION_MILISECOND
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                if (locationResult.locations.isNotEmpty()) {
                    lastLocation = locationResult.lastLocation
                    setUiMyLocation(lastLocation!!)
                }
            }
        }

        actionNavigation = activity as INavigationDrawerAction

        bottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheetLayoutMaps.bottomSheetLayout)
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (!isShowDetails && newState == BottomSheetBehavior.STATE_DRAGGING) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        val bottomSheet = binding.bottomSheetLayoutMaps.bottomSheetLayout
        bottomSheet.setOnClickListener {

        }

        val searchBoxText = binding.textSearchBoxMaps
        searchBoxText.setOnClickListener {
            triggerSearch()
        }

        val openMenuButton = binding.buttonOpenMenuMaps
        openMenuButton.setOnClickListener {
            actionNavigation.openDrawer()
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

        if (actionNavigation.isEnabledCorte()) {
            getDataBlockRoute()
        }
        createTimerUpdate()
    }

    private fun createTimerUpdate() {
        timer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                Log.i("createTimerUpdate", "UPDATE")
                (context as Activity).runOnUiThread {
                    cleanMap()
                    val result = mapEstacionamiento.toMutableMap()
                    mapEstacionamiento.clear()
                    result.forEach { pintarEstacionamiento(it.value) }
                }
            }
        }
        timer.schedule(task, TIME_TIMER_UPDATE, TIME_TIMER_UPDATE)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }

    private fun disableMyLocation() {
        isMyLocationEnabled = false
        DrawableCompat.setTint(
            myLocationButton.drawable,
            ContextCompat.getColor(context!!, R.color.colorLineDefault)
        )
        stopLocationUpdates()
    }

    private fun getDataEstacionamiento(
        latitude: Double,
        longitude: Double,
        radius: Double
    ) {
        if (actionNavigation.isEnabledZona()) {
            disposable = rutaService.getByRadius(latitude, longitude, radius)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        cleanMap()
                        mapEstacionamiento.clear()
                        result.forEach { pintarEstacionamiento(it) }
                    },
                    { t -> Log.w("getDataEstacionamiento", t.message, t) })
        } else {
            disposable = rutaService.getByPosition(latitude, longitude)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result ->
                        cleanMap()
                        mapEstacionamiento.clear()
                        result.forEach { pintarEstacionamiento(it) }
                    },
                    { t -> Log.w("getDataEstacionamiento", t.message, t) })
        }
    }

    private fun getDataBlockRoute() {
        disposable = rutaService.getBlockRoute()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                mapCorte.clear()
                result.forEach { pintarCorte(it) }
            }, { t -> Log.w("getDataBlockRoute", t.message, t) })
    }

    private fun cleanMap() {
        mMap.clear()
        if (actionNavigation.isEnabledCorte()) {
            val result = mapCorte.toMutableMap()
            mapCorte.clear()
            result.forEach { pintarCorte(it.value) }
        }
        if (lastLocation != null) {
            lastLocationMarker = createMarker(
                LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
                bitmapDescriptorFromVector(context!!, R.drawable.ic_my_location, 1),
                100F
            )
        }
    }

    private fun triggerSearch() {

    }

    @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
    private fun setMyLocation() {
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(activity as Context, *perms)) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallBack,
                Looper.getMainLooper()
            )
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.app_name),
                REQUEST_CODE_FINE_LOCATION, *perms
            )
        }
    }

    private fun setUiMyLocation(location: Location) {
        Log.i("setMyLocation", location.toString())
        val position = LatLng(location.latitude, location.longitude)
        lastLocationMarker?.remove()
        lastLocationMarker = createMarker(
            position,
            bitmapDescriptorFromVector(context!!, R.drawable.ic_my_location, 1),
            100F
        )
        var zoom = mMap.cameraPosition.zoom
        if (zoom < ZOOM_DEFAULT) {
            zoom = ZOOM_DEFAULT
        }
        moveMapCamera(position, zoom)

        DrawableCompat.setTint(
            myLocationButton.drawable,
            ContextCompat.getColor(context!!, R.color.colorAccent)
        )
    }

    private fun moveMapCamera(latitude: Double, longitude: Double, zoom: Float) {
        this.moveMapCamera(LatLng(latitude, longitude), zoom)
    }

    private fun moveMapCamera(location: LatLng?) {
        this.moveMapCamera(location, ZOOM_DEFAULT)
    }

    private fun moveMapCamera(location: LatLng?, zoom: Float) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))
    }

    private fun pintarEstacionamiento(routeDto: RouteDto) {
        val lineWidth = WIDTH_LINE_DEFAULT
        val pattern = when (routeDto.schedule.permit) {
            TypeRoutePermit.PERMITIDO_ESTACIONAR_90_GRADO -> createPatternDot(1)
            TypeRoutePermit.PERMITIDO_ESTACIONAR_45_GRADO -> createPatternDot(5)
            else -> null
        }
        val idColor = evaluateSchedule(routeDto.schedule)
        val lineColor = ContextCompat.getColor(context!!, idColor)

        val polylineOptions = PolylineOptions()
            .clickable(true)
            .color(lineColor)
            .width(lineWidth)
            .pattern(pattern)

        routeDto.points.forEach {
            polylineOptions.add(LatLng(it.latitude!!, it.longitude!!))
        }

        val polyline = mMap.addPolyline(polylineOptions)
        mapEstacionamiento[polyline.id] = routeDto
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
            LatLng(block.point?.latitude!!, block.point?.longitude!!),
            drawable
        )
        mapCorte[marker.id] = block
    }

    private fun showInformationCorte(marker: Marker) {
        val corte = mapCorte[marker.id]
        if (corte != null) {
            var zoom = mMap.cameraPosition.zoom
            if (zoom < 17F) {
                zoom = 17F
            }
            moveMapCamera(marker.position, zoom)

            textTipo.text = corte.type?.value
            textCreado.text = calculateTimeLapse(corte.started!!)
            textTweet.text = corte.tweetData?.message

            layoutCorteDetails.visibility = View.VISIBLE
            layoutEstacionamientoDetails.visibility = View.GONE
            showDetails()
        }
    }

    private fun calculateTimeLapse(started: OffsetDateTime): String {
        val timeDifference = ""
        val now = OffsetDateTime.now()

        val duration = (now.toEpochSecond() - started.toEpochSecond()) * 1000

        val diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(duration)
        val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val diffInHours = TimeUnit.MILLISECONDS.toHours(duration)
        val diffInDays = TimeUnit.MILLISECONDS.toDays(duration)

        if (diffInDays > 365) {
            return String.format("hace %d año", diffInDays / 365)
        } else if (diffInDays > 1) {
            return String.format("hace %d dias", diffInDays)
        } else if (diffInHours > 1) {
            return String.format("hace %d horas", diffInHours)
        } else if (diffInMinutes > 1) {
            return String.format("hace %d dias", diffInMinutes)
        } else if (diffInSeconds > 1) {
            return String.format("hace %d segundos", diffInSeconds)
        }
        return timeDifference
    }

    private fun showInformationEstacionamiento(polyline: Polyline) {
        val route = mapEstacionamiento[polyline.id]
        if (route != null) {
            val center = getCenterOfPoints(polyline.points)
            var zoom = mMap.cameraPosition.zoom
            if (zoom < 17F) {
                zoom = 17F
            }
            moveMapCamera(center, zoom)

            textCalle.text = route.details.calle
            textAltura.text = route.details.altura
            textHorario.text = route.details.horario

            layoutCorteDetails.visibility = View.GONE
            layoutEstacionamientoDetails.visibility = View.VISIBLE
            showDetails()
        }
    }

    private fun evaluateSchedule(schedule: RouteScheduleDto): Int {
        val now = OffsetDateTime.now()
        var applyTime = false
        for (detail in schedule.details?: arrayListOf()) {
            var foundDayWeek = false
            val daysWeek = detail.weekday?.split(WORD_COMA) ?: arrayListOf()
            for (dayWeekString in daysWeek) {
                var dayWeek: DayOfWeek? = null
                if (dayWeekString.equals(WORD_MONDAY, true)) {
                    dayWeek = DayOfWeek.MONDAY
                } else if (dayWeekString.equals(WORD_TUESDAY, true)) {
                    dayWeek = DayOfWeek.TUESDAY
                } else if (dayWeekString.equals(WORD_WEDNESDAY, true)) {
                    dayWeek = DayOfWeek.WEDNESDAY
                } else if (dayWeekString.equals(WORD_THURSDAY, true)) {
                    dayWeek = DayOfWeek.THURSDAY
                } else if (dayWeekString.equals(WORD_FRIDAY, true)) {
                    dayWeek = DayOfWeek.FRIDAY
                } else if (dayWeekString.equals(WORD_SUNDAY, true)) {
                    dayWeek = DayOfWeek.SUNDAY
                } else if (dayWeekString.equals(WORD_SATURDAY, true)) {
                    dayWeek = DayOfWeek.SATURDAY
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
                    applyTime = now.toLocalTime().isAfter(detail.startTime.toLocalTime()) &&
                            now.toLocalTime().isBefore(detail.endTime.toLocalTime())
                }
            }
            if (applyTime) {
                break
            }
        }

        val idColor = when (schedule.permit) {
            TypeRoutePermit.PROHIBIDO_ESTACIONAR -> if (applyTime) R.color.colorLineNoHabilitado else R.color.colorLineHabilitado
            TypeRoutePermit.PROHIBIDO_ESTACIONAR_DETENERSE -> if (applyTime) R.color.colorLineNoHabilitado else R.color.colorLineHabilitado
            TypeRoutePermit.PERMITIDO_ESTACIONAR -> if (applyTime) R.color.colorLineHabilitado else R.color.colorLineNoHabilitado
            TypeRoutePermit.PERMITIDO_ESTACIONAR_90_GRADO -> if (applyTime) R.color.colorLineHabilitado else R.color.colorLineNoHabilitado
            TypeRoutePermit.PERMITIDO_ESTACIONAR_45_GRADO -> if (applyTime) R.color.colorLineHabilitado else R.color.colorLineNoHabilitado
            else -> R.color.colorLineDefault
        }
        return idColor
    }


    private fun showDetails() {
        if (!isShowDetails) {
            isShowDetails = true
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun collapseDetails() {
        if (isShowDetails) {
            isShowDetails = false
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun createMarker(
        punto: LatLng,
        resource: BitmapDescriptor
    ): Marker {
        return createMarker(punto, resource, 10F)
    }

    private fun createMarker(punto: LatLng,
                             resource: BitmapDescriptor,
                             zIndex: Float
    ): Marker {
        val markerOptions = MarkerOptions()
            .position(punto)
            .icon(resource)
            .zIndex(zIndex)
        return mMap.addMarker(markerOptions)
    }

    private fun setCiudad() {
        val latitude = LATITUDE_DEFAULT
        val longitude = LONGITUDE_DEFAULT
        val radius = RADIUS_DEFAULT

        moveMapCamera(latitude, longitude, radius)
    }

    private fun getCenterOfPoints(points: List<LatLng>): LatLng? {
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLon = Double.POSITIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY

        for (point in points) {
            maxLat = max(point.latitude, maxLat)
            minLat = min(point.latitude, minLat)
            maxLon = max(point.longitude, maxLon)
            minLon = min(point.longitude, minLon)
        }
        return LatLng((maxLat + minLat) / 2, (maxLon + minLon) / 2)
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

        return sqrt(
            (distanceWidth[0].toString().toDouble().pow(2.0))
                    + distanceHeight[0].toString().toDouble().pow(2.0)
        ) / 2
    }

    private fun bitmapDescriptorFromVector(
        context: Context,
        vectorResId: Int,
        multiple: Int
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth * multiple,
            vectorDrawable.intrinsicHeight * multiple
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth * multiple,
            vectorDrawable.intrinsicHeight * multiple,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
