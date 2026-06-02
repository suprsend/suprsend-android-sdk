package app.suprsend.base

import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader

object AssetHelper {

    fun readAssetFileToString(fileName: String): String {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(fileName)
        return inputStream.bufferedReader().use(BufferedReader::readText)
    }
}