package logiviz

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.net.URI
import java.awt.Desktop

fun openGraphInBrowser(mermaidCode: String) {
    if (mermaidCode.isBlank()) {
        println("Error: Mermaid code is empty.")
        return
    }

    try {
        val input = mermaidCode.toByteArray(Charsets.UTF_8)
        val output = ByteArrayOutputStream()

        // Use BEST_COMPRESSION as Kroki expects standard zlib/deflate
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(input)
        deflater.finish()

        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            output.write(buffer, 0, count)
        }
        deflater.end()

        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(output.toByteArray())

        val url = "https://kroki.io/mermaid/svg/$encoded"

        println("Graph API URL: $url")

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            println("Automatic browsing not supported. Please copy the URL manually.")
        }
    } catch (e: Exception) {
        println("Failed to generate or open graph: ${e.message}")
    }
}