package window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState

data class NoteWindowState(
    val roots: List<RootState>,
    val activeRootIndex: Int = 0,
    val windowState: WindowState = WindowState(size = DpSize(1000.dp, 800.dp)),
) {
    val currentRootName get() =  roots[activeRootIndex].name
    val workspace get() =  roots[activeRootIndex].workspace
    val root get() =  roots[activeRootIndex].root

    init {
        assert(roots.isNotEmpty()) { "At least one root must be provided" }
    }
}