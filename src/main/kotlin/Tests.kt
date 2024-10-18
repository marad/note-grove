import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.launch
import v2.Buffer
import v2.notestream.NoteCardState
import v2.notestream.NoteStream
import v2.notestream.NoteStreamState
import java.nio.file.Paths


fun main() {
    singleWindowApplication {
        val root = Root("", "")
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus ultricies aliquam egestas. Etiam tempus in enim quis vulputate. Cras orci arcu, porttitor vitae tellus sed, ullamcorper auctor lorem. Morbi erat justo, fringilla in purus at, hendrerit molestie enim. Cras eget magna leo. Nulla posuere ut sapien in tristique. Curabitur sodales maximus tempor. Nunc sit amet tempor mi, vitae imperdiet ligula. Proin tristique, enim vel vestibulum feugiat, quam odio ullamcorper nibh, eget efficitur augue felis eu tortor. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. In nisl metus, venenatis a massa sed, ullamcorper ultrices elit."
        val cardA = NoteCardState(Buffer(NoteName("title a"), Paths.get(""), root, AnnotatedString(text)))
        val cardB = NoteCardState(Buffer(NoteName("title b"), Paths.get(""), root, AnnotatedString("Donec efficitur elementum sem ut tempor. Ut sit amet arcu sit amet dui fringilla tincidunt vel quis erat. In eget diam efficitur turpis pulvinar sollicitudin eget nec diam. Fusce non tincidunt diam. Pellentesque a mauris eu eros pretium consectetur eu sit amet velit. Suspendisse augue metus, euismod eu tincidunt sed, auctor sit amet nibh. Etiam sed metus sollicitudin, placerat dolor a, porta leo. Cras in volutpat erat.")))

        val sharedBuffer = Buffer(NoteName("title c"), Paths.get(""), root, AnnotatedString("shared content"))
        val cardC = NoteCardState(sharedBuffer)
        val cardD = NoteCardState(sharedBuffer)

        val state = remember { mutableStateOf(NoteStreamState(
            cards = listOf(cardA, cardB, cardC, cardD)
        ))}

        val lazyListState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        MaterialTheme {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                NoteStream(
                    state.value,
                    lazyListState = lazyListState,
                    onUpdate = { new -> state.value = new },
                    modifier = Modifier.weight(0.5f)
                )
                Button(
                    onClick = {
                        val card = NoteCardState(Buffer(NoteName("generated"), Paths.get(""), root, AnnotatedString(text)))
                        state.value = state.value.prependCard(card)
                        scope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        "Add another card"
                    )
                }
            }
        }
    }
}
