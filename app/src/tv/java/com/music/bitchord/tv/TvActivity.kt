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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.launch

private val canvas = Color(0xFF080610)
private val panel = Color(0xFF181124)
private val violet = Color(0xFF9635F5)

class TvActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { TvApp(model) }
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
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("Início") }
    var fullPlayer by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<MoodGenre?>(null) }
    LaunchedEffect(page) { if (page == "Explorar" && explore !is UiState.Success) model.loadExplore() }
    val goPlay: (Song) -> Unit = { song -> controller?.let { scope.launch { it.playSongs(listOf(song), 0) } } }
    val goPlayItem: (ShelfItem) -> Unit = { item ->
        when {
            item.videoId != null -> goPlay(Song(item.videoId, item.title, item.subtitle, item.thumbnailUrl))
            item.browseId != null -> model.collectSongs(item.browseId, item.thumbnailUrl) { result ->
                result.onSuccess { songs ->
                    if (songs.isNotEmpty()) controller?.let { scope.launch { it.playSongs(songs, 0) } }
                }
            }
        }
    }

    BackHandler(fullPlayer) { fullPlayer = false }
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
        Row(Modifier.fillMaxSize().background(canvas)) {
            Column(
                Modifier.width(68.dp).fillMaxHeight()
                    .background(Color(0xFF0D0919)).padding(horizontal = 9.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.Image(painterResource(R.drawable.koda_mark), "Koda Music", Modifier.size(46.dp))
                Spacer(Modifier.height(18.dp))
                listOf("Início" to Icons.Rounded.Home, "Explorar" to Icons.Rounded.Explore,
                    "Buscar" to Icons.Rounded.Search, "Biblioteca" to Icons.Rounded.LibraryMusic).forEach { (label, symbol) ->
                    TvIconButton(symbol, label, selected = page == label, size = 48.dp) { page = label }
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp)) {
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
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            when (val state = home) {
                                is UiState.Success -> {
                                    val featured = state.data.asSequence().flatMap { it.items.asSequence() }
                                        .firstOrNull { it.videoId != null || it.browseId != null }
                                    if (featured != null) item {
                                        HeroCard(featured) {
                                            goPlayItem(featured)
                                        }
                                    }
                                    items(state.data) { shelf ->
                                    val cards = shelf.items.filter { it.videoId != null || it.browseId != null }.take(14)
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
                player.song?.let { song ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(0xE3171024)).border(1.dp, Color(0xFF4A3063), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(song.artworkAt(120), null, Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1.1f).clickable { fullPlayer = true }) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(song.artist, maxLines = 1, color = Color(0xFFBBAFC9), fontSize = 11.sp)
                        }
                        TvIconButton(Icons.Rounded.SkipPrevious, "Anterior", size = 34.dp) { controller?.seekToPreviousMediaItem() }
                        TvIconButton(if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (player.isPlaying) "Pausar" else "Reproduzir", selected = true, size = 38.dp) {
                            controller?.let { if (it.isPlaying) it.pause() else it.play() }
                        }
                        TvIconButton(Icons.Rounded.SkipNext, "Próxima", size = 34.dp) { controller?.seekToNextMediaItem() }
                        Spacer(Modifier.width(8.dp))
                        Text(formatTime(player.position.positionMs), color = Color.LightGray, fontSize = 11.sp)
                        Spacer(Modifier.width(7.dp))
                        ProgressTrack(player.position.positionMs, player.durationMs, Modifier.weight(.55f))
                        Spacer(Modifier.width(7.dp))
                        Text(formatTime(player.durationMs), color = Color.LightGray, fontSize = 11.sp)
                        TvIconButton(Icons.Rounded.Fullscreen, "Tela cheia e letras", size = 34.dp) { fullPlayer = true }
                    }
                }
            }
        }
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
        val artSize = (maxHeight * .31f).coerceIn(100.dp, 260.dp)
        AsyncImage(song.artworkAt(360), null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, alpha = .25f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
            Color(0xD9080610), Color(0xC7080610), Color(0xE6080610)))))
        Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(painterResource(R.drawable.koda_mark), "Koda Music", Modifier.size(36.dp))
                Spacer(Modifier.weight(1f))
                TvIconButton(Icons.Rounded.ArrowBack, "Voltar", onClick = onBack)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                Column(Modifier.width((maxWidth * .28f).coerceAtLeast(artSize))) {
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
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (offset in -2..2) {
                                val line = visible.getOrNull(current + offset)
                                Text(line?.text.orEmpty(),
                                    color = if (offset == 0) Color.White else Color(0xFF9E90AB),
                                    fontSize = if (offset == 0) 26.sp else 20.sp,
                                    fontWeight = if (offset == 0) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.height(48.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack(positionMs, durationMs, Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), color = Color(0xFFD5C8DD), fontSize = 12.sp)
                Text(formatTime(durationMs), color = Color(0xFFD5C8DD), fontSize = 12.sp)
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
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().height(178.dp).clip(shape).background(panel)) {
        AsyncImage(item.thumbnailUrl, null, Modifier.fillMaxWidth(.62f).fillMaxHeight()
            .align(Alignment.CenterEnd), contentScale = ContentScale.Crop, alpha = .78f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(
            Color(0xFA0D071B), Color(0xC50D071B), Color(0x220D071B)))))
        Column(Modifier.fillMaxHeight().fillMaxWidth(.72f).padding(20.dp),
            verticalArrangement = Arrangement.Center) {
            Text("KODA MUSIC", color = Color(0xFFC478FF),
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(item.title, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, color = Color(0xFFE0D6E7), fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            TvButton("▶  Reproduzir", onPlay)
        }
    }
}

@Composable
private fun FocusTile(selected: Boolean = false, onClick: () -> Unit, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.clip(shape)
        .background(if (selected || focused) Color(0xFF53317B) else Color.Transparent)
        .border(if (focused) 2.dp else 0.dp, violet, shape)
        .onFocusChanged { focused = it.isFocused }
        .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 12.dp)) { content() }
}

@Composable
private fun CoverCard(item: ShelfItem, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.width(132.dp).onFocusChanged { focused = it.isFocused }
        .clip(shape).then(if (focused) Modifier.border(2.dp, violet, shape) else Modifier)
        .clickable(onClick = onClick).padding(2.dp)) {
        Box(Modifier.size(128.dp).clip(RoundedCornerShape(11.dp)).background(panel)) {
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
