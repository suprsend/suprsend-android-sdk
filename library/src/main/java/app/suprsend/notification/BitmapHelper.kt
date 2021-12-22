package app.suprsend.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import app.suprsend.base.Logger
import java.net.URL

internal object BitmapHelper {

    fun getBitmapFromUrl(url: String): Bitmap? {
        Logger.i("bitmap", "Image Url : $url")
        return try {
            BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
        } catch (e: Exception) {
            Logger.e("bitmap", "Image Url : $url", e)
            null
        }
    }

    fun getBitmapFromRes(context: Context, res: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, res)
        } catch (e: Exception) {
            Logger.e("bitmap", "Resource Loading : $res", e)
            null
        }
    }

    fun toCircleBitmap(bitmap: Bitmap): Bitmap? {
        return try {
            val dstBmp: Bitmap = if (bitmap.width > bitmap.height) {
                Bitmap.createBitmap(
                    bitmap,
                    (bitmap.width - bitmap.height) / 2, // bitmap.getWidth()/2 - bitmap.getHeight()/2,
                    0,
                    bitmap.height,
                    bitmap.height
                )
            } else {
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    (bitmap.height - bitmap.width) / 2,
                    bitmap.width,
                    bitmap.width
                )
            }
            val output = Bitmap.createBitmap(
                dstBmp.width,
                dstBmp.height, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(output)
            val color = Color.BLACK
            val paint = Paint()
            val rect = Rect(0, 0, dstBmp.width, dstBmp.height)
            val rectF = RectF(rect)
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawOval(rectF, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(dstBmp, rect, rect, paint)
            output
        } catch (e: Exception) {
            Logger.e("bitmap", "Bitmap Failed", e)
            null
        }
    }
}
