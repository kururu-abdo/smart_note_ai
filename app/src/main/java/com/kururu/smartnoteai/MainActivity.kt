package com.kururu.smartnoteai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kururu.smartnoteai.di.AppContainer
import com.kururu.smartnoteai.domain.model.Note
import com.kururu.smartnoteai.domain.speech.SpeechTranscriber
import com.kururu.smartnoteai.domain.audio.AudioRecorder
import com.kururu.smartnoteai.presentation.MainContract
import com.kururu.smartnoteai.presentation.MainPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.Locale

private val Bg = Color(0xFFF7F7FB)
private val Ink = Color(0xFF181820)
private val Muted = Color(0xFF747480)
private val Accent = Color(0xFF5B5CE2)

class MainActivity : ComponentActivity(), MainContract.View {
    private lateinit var container: AppContainer
    private lateinit var presenter: MainPresenter
    private lateinit var transcriber: SpeechTranscriber
    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var audioRecorder: AudioRecorder
    private var currentAudioPath: String? = null
    private var state by mutableStateOf(MainContract.State())
    private val requestMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startCapture() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        presenter = MainPresenter(container.noteRepository, container.generateSmartNotesUseCase, presenterScope)
        transcriber = container.speechTranscriber
        audioRecorder = container.audioRecorder
        transcriber.setListener(object : SpeechTranscriber.Listener {
            override fun onPartial(text: String) { transcript = text }
            override fun onFinal(text: String) { transcript = if (transcript.isBlank()) text else "$transcript $text" }
            override fun onError(message: String) { aiError = message }
        })
        presenter.attach(this)
        setContent { SmartNoteApp() }
    }

    private var transcript by mutableStateOf("")
    private var aiError by mutableStateOf<String?>(null)
    private var isRecording by mutableStateOf(false)
    private var elapsed by mutableLongStateOf(0L)

    private fun startCapture() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestMic.launch(Manifest.permission.RECORD_AUDIO); return }
        currentAudio = File(filesDir, "recording_${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder(this).apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(currentAudio!!.absolutePath); prepare(); start() }
        transcript = ""; elapsed = 0; isRecording = true; transcriber.start(Locale.getDefault().toLanguageTag())
    }

    private fun stopCapture() { recorder?.runCatching { stop(); release() }; recorder = null; transcriber.stop(); isRecording = false }
    override fun render(state: MainContract.State) { this.state = state }
    override fun showError(message: String) { aiError = message }
    override fun onDestroy() { transcriber.release(); audioRecorder.release(); presenter.detach(); presenterScope.cancel(); super.onDestroy() }

    @Composable private fun SmartNoteApp() {
        var screen by remember { mutableStateOf("home") }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Meeting") }
        var selected by remember { mutableStateOf<Note?>(null) }
        LaunchedEffect(isRecording) { while (isRecording) { kotlinx.coroutines.delay(1000); elapsed++ } }
        MaterialTheme(colorScheme = lightColorScheme(background = Bg, surface = Color.White, primary = Accent)) {
            if (aiError != null) AlertDialog(onDismissRequest = { aiError = null }, confirmButton = { TextButton({ aiError = null }) { Text("OK") } }, title = { Text("SmartNote AI") }, text = { Text(aiError!!) })
            when (screen) {
                "home" -> HomeScreen(state.notes, { screen = "record" }) { selected = it; screen = "detail" }
                "record" -> RecordScreen(isRecording, elapsed, transcript, ::startCapture) { stopCapture(); screen = "save" }
                "save" -> SaveScreen(title, description, type, transcript, { title = it }, { description = it }, { type = it }) {
                    presenter.generate(transcript) { markdown -> presenter.saveNote(Note(title = title, description = description, type = type, durationSeconds = elapsed, date = System.currentTimeMillis(), summary = markdown, transcript = transcript, audioPath = currentAudio?.absolutePath)); screen = "home" }
                }
                "detail" -> selected?.let { DetailScreen(it) { screen = "home" } }
            }
        }
    }
}

@Composable private fun HomeScreen(notes: List<Note>, onRecord: () -> Unit, onSelect: (Note) -> Unit) { Scaffold(containerColor = Bg, bottomBar = { NavigationBar { NavigationBarItem(true, {}, { Text("Home") }, icon = { Icon(Icons.Rounded.Home, null) }); NavigationBarItem(false, {}, { Text("Search") }, icon = { Icon(Icons.Rounded.Search, null) }) } }) { padding -> LazyColumn(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("Good evening 👋", 27.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Capture. Understand. Remember.", color = Muted) }; item { Button(onClick = onRecord, Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) { Icon(Icons.Rounded.Mic, null); Spacer(Modifier.width(10.dp)); Text("Start Recording", 17.sp) } }; item { Text("Recent Notes", 21.sp, fontWeight = FontWeight.Bold) }; items(notes) { NoteCard(it) { onSelect(it) } } } } }
@Composable private fun NoteCard(note: Note, onClick: () -> Unit) { Card(onClick = onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(note.type.uppercase(), 12.sp, fontWeight = FontWeight.Bold, color = Accent); Text(note.title, 19.sp, fontWeight = FontWeight.SemiBold, Modifier.padding(top = 8.dp)); Text(note.summary, color = Muted, maxLines = 4, Modifier.padding(top = 6.dp)); Text(formatTime(note.durationSeconds), 12.sp, color = Muted, Modifier.padding(top = 10.dp)) } } }
@Composable private fun RecordScreen(recording: Boolean, elapsed: Long, transcript: String, onStart: () -> Unit, onStop: () -> Unit) { Column(Modifier.fillMaxSize().background(Bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Recording", 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(60.dp)); Text(formatTime(elapsed), 52.sp, fontWeight = FontWeight.Bold); Text(if (recording) "Listening on device" else "Ready", color = Muted); Spacer(Modifier.height(30.dp)); Waveform(recording); if (transcript.isNotBlank()) Text(transcript, color = Ink, maxLines = 5, Modifier.padding(16.dp)); Spacer(Modifier.weight(1f)); Button(onClick = if (recording) onStop else onStart, Modifier.size(82.dp), shape = RoundedCornerShape(28.dp), colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color(0xFFD9535F) else Accent)) { Icon(if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic, null) }; Spacer(Modifier.height(30.dp)) } }
@Composable private fun Waveform(active: Boolean) { Row(Modifier.fillMaxWidth().height(70.dp), Arrangement.Center, Alignment.CenterVertically) { repeat(28) { i -> Box(Modifier.width(4.dp).height(((18 + (i * 17) % 46) * if (active) 1 else .45).dp).background(if (active) Accent else Color.LightGray, RoundedCornerShape(4.dp))); Spacer(Modifier.width(3.dp)) } } }
@Composable private fun SaveScreen(title: String, description: String, type: String, transcript: String, setTitle: (String) -> Unit, setDescription: (String) -> Unit, setType: (String) -> Unit, onSave: () -> Unit) { Column(Modifier.fillMaxSize().background(Bg).padding(22.dp)) { Text("Save your recording", 27.sp, fontWeight = FontWeight.Bold); Text("Your transcript stays on this device.", color = Muted); Spacer(Modifier.height(22.dp)); OutlinedTextField(title, setTitle, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true); Spacer(Modifier.height(12.dp)); OutlinedTextField(description, setDescription, Modifier.fillMaxWidth().height(120.dp), label = { Text("Description") }); Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Meeting", "Lecture", "Interview").forEach { AssistChip(onClick = { setType(it) }, label = { Text(it) }, leadingIcon = if (it == type) ({ Icon(Icons.Rounded.Check, null) }) else null) } }; Text("Transcript: ${transcript.length} characters", color = Muted, Modifier.padding(top = 16.dp)); Spacer(Modifier.weight(1f)); Button(onClick = onSave, enabled = title.isNotBlank() && transcript.isNotBlank(), Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Generate Smart Notes") } } }
@Composable private fun DetailScreen(note: Note, back: () -> Unit) { Scaffold(containerColor = Bg, topBar = { TopAppBar(title = { Text(note.title, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(back) { Icon(Icons.Rounded.ArrowBack, null) } }) }) { padding -> LazyColumn(Modifier.padding(padding).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Text("SMART NOTES", 12.sp, fontWeight = FontWeight.Bold, color = Accent); MarkdownLikeText(note.summary); if (note.transcript.isNotBlank()) { Text("Transcript", 20.sp, fontWeight = FontWeight.Bold, Modifier.padding(top = 18.dp)); Text(note.transcript, color = Muted) } } } } }
@Composable private fun MarkdownLikeText(markdown: String) { markdown.lines().forEach { line -> val clean = line.trim(); when { clean.startsWith("# ") -> Text(clean.removePrefix("# "), 27.sp, fontWeight = FontWeight.Bold); clean.startsWith("## ") -> Text(clean.removePrefix("## "), 20.sp, fontWeight = FontWeight.Bold, Modifier.padding(top = 12.dp)); clean.startsWith("- ") || clean.startsWith("• ") -> Text("• ${clean.drop(2)}", 16.sp, color = Ink, Modifier.padding(top = 5.dp)); clean.isNotBlank() -> Text(clean, 16.sp, color = Ink, Modifier.padding(top = 5.dp)) } } }
private fun formatTime(seconds: Long): String = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
