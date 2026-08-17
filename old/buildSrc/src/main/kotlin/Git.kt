import java.io.File

object Git {
    fun headCommitHash(projectDir: File): String {
        return try {
            val process = ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotEmpty()) output else ""
        } catch (_: Exception) {
            ""
        }
    }

}
