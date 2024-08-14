import androidx.compose.ui.input.key.*

enum class KeyModifier {
    Shift, Ctrl, Alt, Meta;

    fun isPressed(event: KeyEvent): Boolean {
        return when(this) {
            Shift -> event.isShiftPressed
            Ctrl -> event.isCtrlPressed
            Alt -> event.isAltPressed
            Meta -> event.isMetaPressed
        }
    }
}

data class Shortcut(
    val key: Key,
    val modifiers: Set<KeyModifier>
) {

    constructor(key: Key, vararg mods: KeyModifier) :
        this(key, mods.toSet())


    fun shouldFire(event: KeyEvent): Boolean {
        val pressedModifiers = setOf(
            event.isShiftPressed to KeyModifier.Shift,
            event.isCtrlPressed to KeyModifier.Ctrl,
            event.isAltPressed to KeyModifier.Alt,
            event.isMetaPressed to KeyModifier.Meta
        ).filter { it.first }.map { it.second }.toSet()
        return key == event.key && pressedModifiers == modifiers
    }
}

class Shortcuts {
    private val shortcuts = mutableListOf<Pair<Shortcut, Action>>()

    fun exists(shortcut: Shortcut): Boolean =
        shortcuts.find { it.first == shortcut } != null

    fun add(shortcut: Shortcut, action: Action): Boolean {
        if (!exists(shortcut)) {
            shortcuts.add(shortcut to action)
            return true
        }
        return false
    }

    fun remove(shortcut: Shortcut) {
        shortcuts.removeIf { it.first == shortcut }
    }

    fun handle(event: KeyEvent): Boolean {
        if (!event.isCtrlPressed
            && !event.isMetaPressed
            && !event.isAltPressed
            && !event.isShiftPressed) {
            return false
        }
        shortcuts.forEach {
            if (it.first.shouldFire(event)) {
                if (event.type == KeyEventType.KeyDown) {
                    it.second.call()
                }
                return true
            }
        }
        return false
    }
}