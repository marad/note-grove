package config

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import java.nio.file.Files
import java.nio.file.Path

data class AppConfig(
    val roots: List<RootConfig>
) {
    companion object {
        fun load(file: Path): AppConfig? {
            if (Files.notExists(file)) return null
            val mapper = tomlMapper {  }
            return mapper.decode(file)
        }
    }
}

data class RootConfig(
    val name: String,
    val path: String
)
