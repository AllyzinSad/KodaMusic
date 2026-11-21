package com.music.bitchord.tv

import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.playback.playSongs
import com.music.bitchord.playback.rememberMediaController
import com.music.bitchord.playback.rememberPlayerState
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.delay

private val canvas = Color(0xFF080610)
private val panel = Color(0xFF181124)
private val violet = Color(0xFF9635F5)
private val focusViolet = Color(0xFFD8AEFF)
private val secondaryText = Color(0xFFBEB0CC)

class TvActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TV's Back/close and Recents dismissal both end the playback session.
        AppSettings.setStopOnTaskRemoved(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            BoxWithConstraints(Modifier.fillMaxSize().background(canvas)) {
                val systemDensity = LocalDensity.current
                // Scale from the app window rather than the TV's advertised resolution.
                val scale = minOf(maxWidth / 960.dp, maxHeight / 540.dp).coerceIn(0.6f, 2f)
                val tvDensity = remember(systemDensity, scale) {
                    Density(systemDensity.density * scale, systemDensity.fontScale)
                }
                CompositionLocalProvider(LocalDensity provides tvDensity) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        TvApp(model)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvApp(model: MainViewModel) {
    val controller = rememberMediaController()
    val player = rememberPlayerState(controller)
    val home by model.home.collectAsStateWithLifecycle()
    val results by model.results.collectAsStateWithLifecycle()
    val explore by model.explore.collectAsStateWithLifecycle()
    val moodShelves by model.moodGenreShelves.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf("Início") }
    var fullPlayer by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<MoodGenre?>(null) }
    LaunchedEffect(page) { if (page == "Explorar" && explore !is UiState.Success) model.loadExplore() }
    var pendingSongs by remember { mutableStateOf<List<Song>?>(null) }
    var playbackMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingSongs, controller) {
        val songs = pendingSongs ?: return@LaunchedEffect
        if (controller == null) {
            playbackMessage = "Conectando ao player…"
            delay(8_000)
            playbackMessage = "Não foi possível conectar ao player. Tente novamente."
        } else {
            playbackMessage = "Carregando música…"
            try {
                controller.playSongs(songs, 0)
                delay(15_000)
                playbackMessage = if (controller.isPlaying) null else "A reprodução não iniciou. Tente outra faixa."
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                android.util.Log.w("KodaTV", "Could not start playback", error)
                playbackMessage = "Não foi possível iniciar a reprodução. Tente novamente."
            }
        }
        pendingSongs = null
    }
    LaunchedEffect(player.isPlaying, player.error, player.song?.videoId) {
        when {
            player.error != null -> {
                pendingSongs = null
                playbackMessage = "Não foi possível tocar esta faixa (${player.error})."
            }
            player.isPlaying && pendingSongs?.firstOrNull()?.videoId == player.song?.videoId -> {
                pendingSongs = null
                playbackMessage = null
            }
        }
    }
    val goPlay: (Song) -> Unit = { song -> pendingSongs = listOf(song) }
    val goPlayItem: (ShelfItem) -> Unit = { item ->
        when {
            item.videoId != null -> goPlay(Song(item.videoId, item.title, item.subtitle, item.thumbnailUrl))
            item.browseId != null -> model.collectSongs(item.browseId, item.thumbnailUrl) { result ->
                val songs = result.getOrNull()
                if (songs.isNullOrEmpty()) {
                    playbackMessage = "Não foi possível carregar as faixas deste álbum."
                } else {
                    pendingSongs = songs
                }
            }
        }
    }

    BackHandler(fullPlayer) { fullPlayer = false }
    val activity = androidx.compose.ui.platform.LocalContext.current as? TvActivity
    BackHandler(!fullPlayer && page == "Início") {
        controller?.stop()
        controller?.clearMediaItems()
        activity?.finish()
    }
    BackHandler(!fullPlayer && page != "Início") {
        page = "Início"
    }
    BackHandler(!fullPlayer && page == "Explorar" && selectedMood != null) {
        selectedMood = null
        model.closeMoodGenre()
    }
    MaterialTheme {
        if (fullPlayer && player.song != null) {
            FullPlayer(model, player.song!!, player.position.positionMs, player.durationMs,
                player.isPlaying, onBack = { fullPlayer = false }, onToggle = {
                    controller?.let { if (it.isPlaying) it.pause() else it.play() }
                }, onPrevious = { controller?.seekToPreviousMediaItem() },
                onNext = { controller?.seekToNextMediaItem() })
            return@MaterialTheme
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF21132F), canvas, canvas)))) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.width(150.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B153C), Color(0xFF100A1A))))
                    .padding(horizontal = 10.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KodaBrand()
                Spacer(Modifier.height(18.dp))
                listOf("Início" to Icons.Rounded.Home, "Buscar" to Icons.Rounded.Search,
                    "Explorar" to Icons.Rounded.Explore, "Biblioteca" to Icons.Rounded.LibraryMusic).forEach { (label, symbol) ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        FocusTile(selected = page == label, onClick = { page = label }, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(symbol, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 22.dp, end = 22.dp, top = 0.dp,
                bottom = if (player.song != null) 62.dp else 14.dp)) {
                when (page) {
                    "Buscar" -> {
                        Text("Buscar músicas", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(search, { search = it }, label = { Text("Música ou artista") }, singleLine = true,
                                modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            TvButton("Buscar") { if (search.isNotBlank()) model.searchFor(search.trim()) }
                        }
                        Spacer(Modifier.height(20.dp))
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (val state = results) {
                                is UiState.Success -> items(state.data) { result ->
                                    val song = when (result) { is SearchResult.Track -> result.song; is SearchResult.TopTrack -> result.song; else -> null }
                                    if (song != null) SongRow(song, goPlay)
                                }
                                is UiState.Error -> item { Text(state.message, color = Color.White) }
                                UiState.Loading -> item { Text("Buscando…", color = Color.White) }
                                null -> item { Text("Use o controle para procurar sua música.", color = Color.LightGray) }
                            }
                        }
                    }
                    "Explorar" -> {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val mood = selectedMood
                            if (mood == null) {
                                item {
                                    Text("Explorar", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                    Text("Encontre música para cada momento", color = Color(0xFFBBABC9), fontSize = 14.sp)
                                }
                                when (val state = explore) {
                                    is UiState.Success -> items(state.data) { section ->
                                        Text(section.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(9.dp))
                                        section.items.chunked(3).forEach { group ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                group.forEach { genre ->
                                                    GenreCard(genre, Modifier.weight(1f)) {
                                                        selectedMood = genre
                                                        model.openMoodGenre(genre)
                                                    }
                                                }
                                                repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                                            }
                                            Spacer(Modifier.height(10.dp))
                                        }
                                    }
                                    is UiState.Error -> item { Text(state.message, color = Color.LightGray) }
                                    UiState.Loading -> item { Text("Carregando estilos e momentos…", color = Color.LightGray) }
                                }
                            } else {
                                item {
                                    FocusTile(onClick = { selectedMood = null; model.closeMoodGenre() }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.ArrowBack, "Voltar", tint = Color.White)
                                            Spacer(Modifier.width(8.dp))
                                            Text(mood.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                when (val state = moodShelves) {
                                    is UiState.Success -> items(state.data) { shelf ->
                                        Text(shelf.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(shelf.items.filter { it.videoId != null || it.browseId != null }.take(14)) { item ->
                                                CoverCard(item) { goPlayItem(item) }
                                            }
                                        }
                                    }
                                    is UiState.Error -> item { Text(state.message, color = Color.LightGray) }
                                    UiState.Loading -> item { Text("Abrindo ${mood.title}…", color = Color.LightGray) }
                                }
                            }
                        }
                    }
                    "Biblioteca" -> Box(Modifier.weight(1f)) {
                        Placeholder("Sua biblioteca", "Entre na sua conta pelo celular para sincronizar suas músicas. A integração da biblioteca na TV está em preparação.")
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (val state = home) {
                                is UiState.Success -> {
                                    val featured = state.data.asSequence().flatMap { it.items.asSequence() }
                                        .firstOrNull { it.videoId != null && !it.thumbnailUrl.isNullOrBlank() }
                                    if (featured != null) item {
                                        HeroCard(featured) {
                                            goPlayItem(featured)
                                        }
                                    }
                                    items(state.data) { shelf ->
                                    val cards = shelf.items.filter { (it.videoId != null || it.browseId != null) &&
                                        !it.thumbnailUrl.isNullOrBlank() && it != featured }.take(14)
                                    if (cards.isNotEmpty()) {
                                        Text(shelf.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(cards) { item ->
                                                CoverCard(item) { goPlayItem(item) }
                                            }
                                        }
                                    }
                                    }
                                }
                                is UiState.Error -> item { Text(state.message, color = Color.LightGray) }
                                UiState.Loading -> item { Text("Preparando recomendações…", color = Color.LightGray) }
                            }
                        }
                    }
                }
            }
        }
        playbackMessage?.let { message ->
            Text(message, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = if (player.song != null) 62.dp else 10.dp)
                    .background(panel, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp))
        }
        player.song?.let { song ->
            TvMiniPlayer(song, player.position.positionMs, player.durationMs, player.isPlaying,
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                onOpen = { fullPlayer = true },
                onPrevious = { controller?.seekToPreviousMediaItem() },
                onToggle = { controller?.let { if (it.isPlaying) it.pause() else it.play() } },
                onNext = { controller?.seekToNextMediaItem() })
        }
        }
    }
}

@Composable
private fun KodaBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.foundation.Image(painterResource(R.drawable.koda_mark), null, Modifier.size(34.dp))
        Text("Koda Music", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FullPlayer(
    model: MainViewModel,
    song: Song,
    positionMs: Long,
    durationMs: Long,
    playing: Boolean,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val palette = rememberArtworkPalette(song.artworkAt(440))
    val lyrics by model.lyrics.collectAsStateWithLifecycle()
    val checked by model.lyricsChecked.collectAsStateWithLifecycle()
    LaunchedEffect(song.videoId, durationMs) {
        model.loadLyrics(song.videoId, song.title, song.artist, durationMs, song.albumName)
    }
    val visible = lyrics.orEmpty().filter { !it.isGap }
    val current = visible.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
    BoxWithConstraints(Modifier.fillMaxSize().background(palette.background)) {
        val artSize = (maxHeight * .43f).coerceIn(100.dp, 260.dp)
        val artColumnWidth = (maxWidth * .35f).coerceAtLeast(artSize)
        AsyncImage(song.artworkAt(360), null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, alpha = .38f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
            Color(0xC0080610), Color(0xDC1B102E), Color(0xED080610)))))
        Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KodaBrand()
                Spacer(Modifier.weight(1f))
                TvIconButton(Icons.Rounded.ArrowBack, "Voltar", onClick = onBack)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                Column(Modifier.width(artColumnWidth)) {
                    AsyncImage(song.artworkAt(480), null,
                        Modifier.size(artSize).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(8.dp))
                    Text(song.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = Color(0xFFCCBDD8), fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    if (visible.isEmpty()) {
                        Text(if (checked) "Letras indisponíveis para esta música" else "Buscando letras…",
                            color = Color(0xFFD3C4DA), fontSize = 22.sp)
                    } else {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Letra sincronizada", color = Color(0xFFC993FF),
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            for (offset in -1..2) {
                                val line = visible.getOrNull(current + offset)
                                Row(Modifier.fillMaxWidth().height(if (offset == 0) 74.dp else 45.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp))
                                        .background(if (offset == 0 && line != null) focusViolet else Color.Transparent))
                                    Spacer(Modifier.width(12.dp))
                                    Text(line?.text.orEmpty(),
                                    color = if (offset == 0) Color.White else Color(0xFF9E90AB),
                                    fontSize = if (offset == 0) 31.sp else 21.sp,
                                    fontWeight = if (offset == 0) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    lineHeight = if (offset == 0) 35.sp else 23.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xE9110B1B)).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(formatTime(positionMs), color = secondaryText, fontSize = 13.sp)
                ProgressTrack(positionMs, durationMs, Modifier.weight(1f))
                Text(formatTime(durationMs), color = secondaryText, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                TvIconButton(Icons.Rounded.SkipPrevious, "Anterior", onClick = onPrevious)
                Spacer(Modifier.width(15.dp))
                TvIconButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (playing) "Pausar" else "Reproduzir", selected = true, size = 52.dp, onClick = onToggle)
                Spacer(Modifier.width(15.dp))
                TvIconButton(Icons.Rounded.SkipNext, "Próxima", onClick = onNext)
            }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}

@Composable
private fun ProgressTrack(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    Box(modifier.height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .24f))) {
        Box(Modifier.fillMaxWidth((positionMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f))
            .fillMaxHeight().background(violet))
    }
}

@Composable
private fun TvMiniPlayer(
    song: Song, positionMs: Long, durationMs: Long, playing: Boolean,
    modifier: Modifier = Modifier, onOpen: () -> Unit, onPrevious: () -> Unit,
    onToggle: () -> Unit, onNext: () -> Unit,
) {
    val shape = RoundedCornerShape(13.dp)
    Row(modifier.height(48.dp).clip(shape).background(Color(0xF0171125))
        .border(1.dp, Color(0xFF33203F), shape).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(song.artworkAt(160), song.title, Modifier.size(37.dp).clip(RoundedCornerShape(7.dp)),
                contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(song.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color(0xFFBBAFC9), fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TvIconButton(Icons.Rounded.SkipPrevious, "Anterior", size = 33.dp, onClick = onPrevious)
            Spacer(Modifier.width(7.dp))
            TvIconButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (playing) "Pausar" else "Reproduzir", selected = true, size = 37.dp, onClick = onToggle)
            Spacer(Modifier.width(7.dp))
            TvIconButton(Icons.Rounded.SkipNext, "Próxima", size = 33.dp, onClick = onNext)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End) {
            Text(formatTime(positionMs), color = Color(0xFFD7CEDF), fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            ProgressTrack(positionMs, durationMs, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(formatTime(durationMs), color = Color(0xFFD7CEDF), fontSize = 11.sp)
            Spacer(Modifier.width(12.dp))
            TvIconButton(Icons.Rounded.Fullscreen, "Tela cheia e letras", size = 32.dp, onClick = onOpen)
        }
    }
}

@Composable
private fun TvIconButton(icon: ImageVector, label: String, selected: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 42.dp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(13.dp)
    Box(Modifier.size(size).onFocusChanged { focused = it.isFocused }.clip(shape)
        .background(if (selected) Color(0xFF6B2BAA) else if (focused) Color(0xFF452661) else Color.Transparent)
        .then(if (focused) Modifier.border(2.dp, Color(0xFFC993FF), shape) else Modifier)
        .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = if (selected || focused) Color.White else Color(0xFFD4C3E7),
            modifier = Modifier.size(if (size > 45.dp) 26.dp else 23.dp))
    }
}

@Composable
private fun GenreCard(item: MoodGenre, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(15.dp)
    Box(modifier.height(100.dp).onFocusChanged { focused = it.isFocused }
        .clip(shape).background(Color(0xFF241436))
        .then(if (focused) Modifier.border(2.dp, Color(0xFFC993FF), shape) else Modifier)
        .clickable(onClick = onClick)) {
        AsyncImage(item.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .83f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
            Color(0xEE130B1D), Color(0x77130B1D), Color.Transparent))))
        Text(item.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
    }
}

@Composable
private fun HeroCard(item: ShelfItem, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(166.dp).clip(RoundedCornerShape(16.dp)).background(panel)) {
        AsyncImage(item.thumbnailUrl, item.title, Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.62f),
            contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
            Color(0xFF231232), Color(0xF01A1026), Color(0x55130D20), Color(0x08130D20)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
            Color.Transparent, Color.Transparent, canvas))))
        Column(Modifier.fillMaxHeight().fillMaxWidth(.56f).padding(start = 24.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.Center) {
            Text(item.title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 34.sp)
            Text(item.subtitle, color = Color(0xFFE0D6E7), fontSize = 18.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            var focused by remember { mutableStateOf(false) }
            val buttonShape = RoundedCornerShape(24.dp)
            Button(
                onClick = onPlay,
                modifier = Modifier.onFocusChanged {
                    focused = it.hasFocus
                }.then(if (focused) Modifier.border(3.dp, Color(0xFFC993FF), buttonShape) else Modifier),
                shape = buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = canvas),
            ) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Reproduzir", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FocusTile(
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.clip(shape)
        .background(if (selected || focused) Color(0xFF53317B) else Color.Transparent)
        .then(if (focused) Modifier.border(2.dp, Color(0xFFC993FF), shape) else Modifier)
        .onFocusChanged { focused = it.isFocused }
        .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 12.dp)) { content() }
}

@Composable
private fun CoverCard(item: ShelfItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.width(110.dp).onFocusChanged { focused = it.isFocused }
        .clip(shape).then(if (focused) Modifier.border(2.dp, focusViolet, shape) else Modifier)
        .clickable(onClick = onClick).padding(2.dp)) {
        Box(Modifier.fillMaxWidth().height(82.dp).clip(RoundedCornerShape(11.dp)).background(panel)) {
            AsyncImage(item.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(item.subtitle, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SongRow(song: Song, onClick: (Song) -> Unit) {
    FocusTile(onClick = { onClick(song) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(song.artworkAt(120), null, Modifier.size(56.dp), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(song.artist, color = Color.LightGray)
            }
        }
    }
}

@Composable
private fun TvButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = violet)) { Text(label) }
}

@Composable
private fun Placeholder(title: String, description: String) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF24103E), canvas))).padding(35.dp),
        verticalArrangement = Arrangement.Center) {
        Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(description, color = Color(0xFFCCBEDD), fontSize = 18.sp)
    }
}
