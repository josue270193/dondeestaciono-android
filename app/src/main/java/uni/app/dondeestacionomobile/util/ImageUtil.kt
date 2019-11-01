package uni.app.dondeestacionomobile.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class ImageUtil {
    companion object {
        fun bitmapDescriptorFromVector(
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
}
