package com.music.bitchord.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

private val canvas = Color(0xFF0B0913)
private val panel = Color(0xFF171225)
private val violet = Color(0xFFA65AFF)

class TvActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvApp(model) }
    }
}

@Composable
private fun TvApp(model: MainViewModel) {
    val controller = rememberMediaController()
    val player = rememberPlayerState(controller)
    val home by model.home.collectAsStateWithLifecycle()
    val results by model.results.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf("Início") }
    var fullPlayer by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val goPlay: (Song) -> Unit = { song -> controller?.let { scope.launch { it.playSongs(listOf(song), 0) } } }

    BackHandler(fullPlayer) { fullPlayer = false }
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
                Modifier.width(208.dp).fillMaxHeight().background(Color(0xFF100D1A)).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(painterResource(R.drawable.koda_mark), null, Modifier.size(44.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Koda Music", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Spacer(Modifier.height(24.dp))
                listOf("Início", "Explorar", "Buscar", "Biblioteca").forEach { label ->
                    FocusTile(selected = page == label, onClick = { page = label }) {
                        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = if (page == label) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("KODA MUSIC  •  TV", color = Color(0xFFAD9FBE), fontSize = 11.sp)
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 16.dp)) {
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
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    "Biblioteca" -> Placeholder("Sua biblioteca", "Entre na sua conta pelo celular para sincronizar suas músicas. A integração da biblioteca na TV está em preparação.")
                    else -> {
                        val title = if (page == "Explorar") "Explore novas músicas" else "Sua música na tela grande"
                        Text("KODA MUSIC TV", color = violet, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Escolha uma capa com o controle remoto e dê o play.", color = Color(0xFFBBAFC9), fontSize = 15.sp)
                        Spacer(Modifier.height(20.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            when (val state = home) {
                                is UiState.Success -> items(state.data) { shelf ->
                                    val playable = shelf.items.filter { it.videoId != null }.take(14)
                                    if (playable.isNotEmpty()) {
                                        Text(shelf.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(10.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                            items(playable) { item ->
                                                CoverCard(item) {
                                                    goPlay(Song(item.videoId!!, item.title, item.subtitle, item.thumbnailUrl))
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
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(panel).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(song.artworkAt(120), null, Modifier.size(52.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(song.artist, maxLines = 1, color = Color.LightGray)
                        }
                        TvButton("Anterior") { controller?.seekToPreviousMediaItem() }
                        Spacer(Modifier.width(8.dp))
                        TvButton(if (player.isPlaying) "Pausar" else "Reproduzir") {
                            controller?.let { if (it.isPlaying) it.pause() else it.play() }
                        }
                        Spacer(Modifier.width(8.dp))
                        TvButton("Próxima") { controller?.seekToNextMediaItem() }
                        Spacer(Modifier.width(8.dp))
                        TvButton("Tela cheia e letras") { fullPlayer = true }
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
    val scroll = rememberLazyListState()
    LaunchedEffect(song.videoId, current) {
        if (current > 1) scroll.animateScrollToItem(current - 1)
    }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(
        palette.wash, palette.background, canvas)))) {
        AsyncImage(song.artworkAt(900), null, Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, alpha = 0.15f)
        Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Koda Music", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                TvButton("Voltar") { onBack() }
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(34.dp)) {
                Column(Modifier.width(290.dp)) {
                    AsyncImage(song.artworkAt(600), null,
                        Modifier.size(275.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.height(13.dp))
                    Text(song.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = Color(0xFFE2D9E9), fontSize = 17.sp)
                }
                Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                    .background(Color(0xD90D0B14)).padding(24.dp)) {
                    Text("Letras", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    if (visible.isEmpty()) {
                        Text(if (checked) "Letras indisponíveis para esta música." else "Buscando letras…",
                            color = Color.LightGray, fontSize = 18.sp)
                    } else {
                        LazyColumn(state = scroll, verticalArrangement = Arrangement.spacedBy(15.dp)) {
                            items(visible.size) { index ->
                                Text(visible[index].text,
                                    color = if (index == current) Color.White else Color(0xFFAAA2B4),
                                    fontSize = if (index == current) 24.sp else 20.sp,
                                    fontWeight = if (index == current) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(positionMs), color = Color.White)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxWidth((positionMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f))
                        .fillMaxHeight().background(violet))
                }
                Spacer(Modifier.width(12.dp))
                Text(formatTime(durationMs), color = Color.White)
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TvButton("Anterior", onPrevious)
                Spacer(Modifier.width(16.dp))
                TvButton(if (playing) "Pausar" else "Reproduzir", onToggle)
                Spacer(Modifier.width(16.dp))
                TvButton("Próxima", onNext)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
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
    Column(Modifier.width(154.dp).onFocusChanged { focused = it.isFocused }
        .clip(shape).border(if (focused) 3.dp else 0.dp, violet, shape).clickable(onClick = onClick).padding(3.dp)) {
        Box(Modifier.size(148.dp).clip(RoundedCornerShape(11.dp)).background(panel)) {
            AsyncImage(item.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(item.subtitle, color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF613799))) { Text(label) }
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
