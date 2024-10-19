import androidx.compose.runtime.*
import androidx.compose.ui.window.application
import config.AppConfig
import v2.BufferManager
import v2.window.MainWindow
import v2.window.MainWindowController
import java.nio.file.Path
import kotlin.system.exitProcess

fun main() = application {
    val coScope = rememberCoroutineScope()
    val bufferManager = remember { BufferManager() }


    var controller = remember {
        val currentDir = Path.of("")
        val configPath = currentDir.resolve("config.toml")
        val config = AppConfig.load(configPath)
        if (config == null) {
            println("Config not found at $configPath")
            exitProcess(1)
        }
        val roots = config.roots.map {
            Root(it.name, it.path)
        }
        MainWindowController(bufferManager, roots, coScope, LauncherViewModel())
    }

    MainWindow(controller,
        onCloseRequest = ::exitApplication)
}

