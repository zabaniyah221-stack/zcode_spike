import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import dev.datlag.kcef.KCEF
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Spike Fase 0: Compose Desktop + KCEF memuat bundle CM6 ZCODE apa adanya.
 * Uji: onEditorReady handshake + round-trip setCode/getCode + RAM idle.
 */
fun main() = application {
    val windowState = rememberWindowState()
    var kcefReady by remember { mutableStateOf(false) }
    var roundTrip by remember { mutableStateOf("BELUM") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            KCEF.init(builder = {
                installDir(File("kcef-bundle"))
                progress { onDownloading { println("[SPIKE] KCEF download $it%") } }
            })
        }
        kcefReady = true
        println("[SPIKE] KCEF_INIT_OK")
    }

    Window(onCloseRequest = ::exitApplication, state = windowState, title = "ZCODE Spike") {
        if (!kcefReady) return@Window
        val page = File("zcode-www/index.html")
        val state = rememberWebViewState("file://${page.absolutePath}")
        val navigator = rememberWebViewNavigator()
        var fired by remember { mutableStateOf(false) }
        WebView(state, Modifier.fillMaxSize(), navigator = navigator)
        LaunchedEffect(state.loadingState) {
            if (!fired && state.loadingState != null) {
                fired = true
                println("[SPIKE] PAGE_LOADED")
                // Round-trip: tulis lalu baca kembali via bridge 12 fungsi
                navigator.evaluateJavaScript("setCode('print(42)')") {}
                kotlinx.coroutines.delay(1500)
                navigator.evaluateJavaScript("getCode()") { got ->
                    roundTrip = got.toString()
                    println("[SPIKE] ROUNDTRIP_RESULT=$roundTrip")
                }
            }
        }
    }
}
