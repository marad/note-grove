package v2

import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class Buffer(val title: String,
             val path: Path,
             initialContent: AnnotatedString) {
    private val _content = MutableStateFlow(initialContent)
    val content = _content.asStateFlow()

    fun updateContent(annotatedString: AnnotatedString) {
        _content.tryEmit(annotatedString)
    }

    fun reload() {
        _content.tryEmit(AnnotatedString(Files.readString(path)))
    }
}


class BufferManager {
    private val buffers = mutableMapOf<Path, WeakReference<Buffer>>()

    fun openBuffer(file: Path, defaultContent: () -> String = {""}): Buffer {
        val buffer = buffers[file]?.get()
        if (buffer != null) {
            return buffer
        } else {
            val content = if (Files.exists(file)) Files.readString(file) else defaultContent()
            val buffer = Buffer(file.nameWithoutExtension, file, AnnotatedString(content))
            buffers[file] = WeakReference(buffer)
            return buffer
        }
    }

    fun reloadBuffer(noteName: NoteName) {
        buffers.mapNotNull { it.value.get() }
            .filter { it.title == noteName.name }
            .forEach {
                it.reload()
            }
    }

    fun removeBuffer(file: Path) {
        if (buffers.containsKey(file)) {
            buffers.remove(file)
        }
    }
}