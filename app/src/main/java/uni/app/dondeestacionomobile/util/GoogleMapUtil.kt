package uni.app.dondeestacionomobile.util

import android.location.Location
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.VisibleRegion
import java.util.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

const val PATTERN_GAP_LENGTH_PX = 5.0F

class GoogleMapUtil {
    companion object {
        fun getVisibleRadius(map: GoogleMap): Double {
            val visibleRegion: VisibleRegion? = map.projection?.visibleRegion

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

        fun getCenter(points: List<LatLng>): LatLng? {
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

        fun patternDot(multiple: Int): ArrayList<PatternItem> {
            val dot = Dot()
            val gap = Gap(PATTERN_GAP_LENGTH_PX * multiple)
            return arrayListOf(gap, dot)
        }

        fun createMarker(
            map: GoogleMap,
            punto: LatLng,
            resource: BitmapDescriptor
        ): Marker {
            return createMarker(map, punto, resource, 10F)
        }

        fun createMarker(
            map: GoogleMap,
            punto: LatLng,
            resource: BitmapDescriptor,
            zIndex: Float
        ): Marker {
            val markerOptions = MarkerOptions()
                .position(punto)
                .icon(resource)
                .zIndex(zIndex)
            return map.addMarker(markerOptions)
        }

    }
}