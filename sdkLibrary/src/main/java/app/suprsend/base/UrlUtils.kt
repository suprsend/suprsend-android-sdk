package app.suprsend.base

internal object UrlUtils {

    fun createNotificationBannerImage(imagePath: String, widthPixels: Int, quality: Int): String {
        return if (imagePath.contains("http")) {
            imagePath
        } else {
            "${SSConstants.IMAGE_KIT_BASE_PATH}/tr:ar-2-1,q-$quality,fo-auto,w-$widthPixels/$imagePath"
        }
    }

    fun createNotificationLogoImage(imagePath: String, size: Int, quality: Int): String {
        return if (imagePath.contains("http")) {
            imagePath
        } else {
            "${SSConstants.IMAGE_KIT_BASE_PATH}/tr:ar-1-1,q-$quality,r-max,w-$size/$imagePath"
        }
    }

    fun calculateQuality(networkType: NetworkType): Int {
        return when (networkType) {
            NetworkType.WIFI,
            NetworkType.G4,
            NetworkType.G5 -> 80
            NetworkType.G3 -> 60
            NetworkType.G2 -> 40
            NetworkType.UNKNOWN -> 60
        }
    }
}
