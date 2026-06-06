# 🎵 Integración de Lyrics con Karaoke Effect - Soundly

## ✅ Lo que se implementó

### 1. **Parser LRC** (`LyricsParser.kt`)
- Parsea archivos `.lrc` con timestamps sincronizados
- Soporta formato karaoke con sílabas explícitas: `<Hel>lo <world>`
- Convierte lyrics sincronizadas a `KaraokeTrack` para animación palabra por palabra
- Método principal: `parseLrcWithKaraoke()` → devuelve `LyricsUiState` con `track` listo para karaoke

### 2. **Repository** (`LyricsRepository.kt`)
- Carga lyrics desde múltiples fuentes:
  1. Archivos `.lrc` locales (misma carpeta que el audio)
  2. Carpeta dedicada `files/lyrics/`
  3. **API LRCLIB** (fetch online con OkHttp)
- Cachea lyrics descargadas para uso offline
- Prioriza synced lyrics sobre plain text

### 3. **Dependencias Agregadas**
```kotlin
// OkHttp para fetch de lyrics desde APIs
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

### 4. **Modelos Existentes** (`PlayerUiState.kt`)
- `LyricsUiState`: Estado completo de lyrics
- `LyricLine`: Línea con timestamp
- `KaraokeSyllable`: Sílaba/palabra con start/end
- `KaraokeLine`: Línea karaoke con múltiples syllables
- `KaraokeTrack`: Track completo con todas las líneas

---

## 🎯 Cómo usar el Karaoke Effect

### Opción A: Lyrics desde API (LRCLIB)

```kotlin
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val lyricsRepository: LyricsRepository // <-- Inyectar
) : ViewModel() {

    fun loadSongWithLyrics(song: Song, queue: List<Song>) {
        viewModelScope.launch {
            // 1. Reproducir canción
            playbackManager.play(song, queue)
            
            // 2. Cargar lyrics automáticamente
            val lyrics = lyricsRepository.loadLyrics(
                audioFile = File(song.path),
                audioUri = Uri.parse(song.path),
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration
            )
            
            // 3. Actualizar UI state con lyrics
            // (El PlaybackManager ya hace esto internamente)
        }
    }
}
```

### Opción B: Lyrics desde archivo .lrc local

1. **Coloca el archivo** junto al audio:
   ```
   /sdcard/Music/
   ├── mi_cancion.mp3
   └── mi_cancion.lrc    <-- Mismo nombre, extensión .lrc
   ```

2. **Formato del archivo .lrc**:
   ```lrc
   [ti:Nombre de la canción]
   [ar:Artista]
   [al:Álbum]
   
   [00:00.00] Primera línea de la letra
   [00:05.50] Segunda línea sincronizada
   [00:10.00] Tercera línea...
   ```

3. **El sistema lo detecta automáticamente** y carga las lyrics con karaoke.

---

## 🎨 UI: LyricsContainer con Karaoke

El componente `LyricsContainer` ya está configurado para usar `KaraokeTrack`:

```kotlin
LyricsContainer(
    lyricsState = state.lyrics,  // Tiene .track con KaraokeTrack
    currentPositionMs = state.positionMs,
    onColor = MaterialTheme.colorScheme.onSurface,
    onSeekTo = { positionMs -> onSeek(positionMs) },
    onShowFullLyrics = { showLyricsSheet = true }
)
```

### ¿Cómo funciona el efecto visual?

1. **`ProgressText`** dibuja cada carácter individualmente
2. Un **shader/ola** se mueve de izquierda a derecha en cada línea
3. Donde pasa el shader:
   - **Shadow blanco** sutil (`Color.White.copy(alpha = 0.25)`)
   - **Aumento de tamaño** 8% (`scaleBoost = 0.08`)
   - **Más brillante** (`alpha` de 0.35 a 1.0)
4. El efecto es **por línea** (estilo Apple Music), no continuo entre líneas

---

## 📁 Estructura de Archivos

```
app/src/main/java/com/soundly/player/
├── LyricsParser.kt          # Parsea LRC → KaraokeTrack
├── LyricsRepository.kt      # Carga lyrics (local + API)
├── PlayerUiState.kt         # Modelos de datos
├── PlaybackManager.kt       # Gestiona reproducción + lyrics
└── PlaybackViewModel.kt     # ViewModel

app/src/main/java/com/soundly/ui/componentes/
├── LyricsContainer-ArtistInfoContainer.kt  # UI de lyrics con efectos
└── FullPlayerScreen.kt                     # Pantalla completa del player
```

---

## 🔧 Personalización

### Ajustar el efecto del shader

En `ProgressText()` dentro de `LyricsContainer-ArtistInfoContainer.kt`:

```kotlin
val shaderWidthPx = 60f        // Ancho del "foco" del shader
val scaleBoost = 0.08f         // 8% aumento de tamaño
val shadowAlpha = 0.25f        // Intensidad del shadow blanco
```

### Cambiar colores

```kotlin
val textColorAlpha = (0.35f + 0.65f * intensity).coerceAtMost(1f)
// 0.35 = alpha base (texto inactivo)
// 0.65 = delta máximo (texto en el centro del shader)
```

### Usar syllables explícitos (karaoke real)

Si tienes archivos LRC con syllables:

```lrc
[00:10.00]<Hel>lo <world> test
```

El parser detecta automáticamente los syllables y crea animación palabra por palabra precisa.

---

## 🚀 Próximos Pasos (Opcional)

1. **Agregar letra desde APIs alternativas** (Genius, Musixmatch)
2. **Soporte para traducciones** (ya está en el modelo `translation`)
3. **Animación de entrada/salida** de líneas (fade + slide)
4. **Modo sing-along** con detección de pitch (más avanzado)

---

## 📝 Ejemplo de Archivo LRC Completo

```lrc
[ti:Bohemian Rhapsody]
[ar:Queen]
[al:A Night at the Opera]
[by:LyricFind]

[00:00.00]
[00:05.50]Is this the real life?
[00:09.00]Is this just fantasy?
[00:12.50]Caught in a landslide
[00:15.00]No escape from reality
```

El sistema:
- Parsea cada línea con su timestamp
- Distribuye el tiempo entre palabras
- Crea `KaraokeTrack` con syllables para animación

---

**¡Listo! Ya tienes un sistema de lyrics tipo Apple Music/Spotify con efectos karaoke hermosos. 🎵✨**
