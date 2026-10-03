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
            override fun onPartial(text: String) {
                transcript = joinTranscript(committedTranscript, text)
            }

            override fun onFinal(text: String) {
                committedTranscript = joinTranscript(committedTranscript, text)
                transcript = committedTranscript
            }

            override fun onError(message: String) { aiError = message }
        })
        presenter.attach(this)
        setContent { SmartNoteApp() }
    }

    private var transcript by mutableStateOf("")
    private var committedTranscript = ""
    private var selectedLanguage by mutableStateOf(SpeechLanguage.Auto)
    private var aiError by mutableStateOf<String?>(null)
    private var isRecording by mutableStateOf(false)
    private var elapsed by mutableLongStateOf(0L)

    private fun startCapture() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        audioRecorder.start().onSuccess {
            transcript = ""
            committedTranscript = ""
            elapsed = 0
            isRecording = true
            transcriber.start(selectedLanguage.tag(Locale.getDefault().toLanguageTag()))
        }.onFailure { aiError = it.message ?: "Unable to start recording." }
    }

    private fun pauseCapture() {
        audioRecorder.pause()
            .onSuccess {
                transcriber.pause()
                isRecording = false
            }
            .onFailure { aiError = it.message ?: "Unable to pause recording." }
    }

    private fun resumeCapture() {
        audioRecorder.resume()
            .onSuccess {
                transcriber.resume()
                isRecording = true
            }
            .onFailure { aiError = it.message ?: "Unable to resume recording." }
    }

    private fun stopCapture(): Boolean {
        transcriber.stop()
        val result = audioRecorder.stop().getOrElse {
            aiError = it.message ?: "Unable to stop recording."
            return false
        }
        currentAudioPath = result.file.absolutePath
        elapsed = result.durationMs / 1000
        isRecording = false
        return true
    }

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
                "record" -> RecordScreen(
                    isRecording,
                    elapsed,
                    transcript,
                    selectedLanguage,
                    { selectedLanguage = it },
                    ::startCapture,
                    ::pauseCapture,
                    ::resumeCapture,
                ) { if (stopCapture()) screen = "save" }
                "save" -> SaveScreen(title, description, type, transcript, { title = it }, { description = it }, { type = it }) {
                    presenter.generate(transcript) { markdown -> presenter.saveNote(Note(title = title, description = description, type = type, durationSeconds = elapsed, date = System.currentTimeMillis(), summary = markdown, transcript = transcript, audioPath = currentAudioPath)); screen = "home" }
                }
                "detail" -> selected?.let { DetailScreen(it) { screen = "home" } }
            }
        }
    }
}

@Composable private fun HomeScreen(notes: List<Note>, onRecord: () -> Unit, onSelect: (Note) -> Unit) { Scaffold(containerColor = Bg, bottomBar = { NavigationBar { NavigationBarItem(true, {}, { Text("Home") }, icon = { Icon(Icons.Rounded.Home, null) }); NavigationBarItem(false, {}, { Text("Search") }, icon = { Icon(Icons.Rounded.Search, null) }) } }) { padding -> LazyColumn(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("Good evening 👋", 27.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Capture. Understand. Remember.", color = Muted) }; item { Button(onClick = onRecord, Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(20.dp)) { Icon(Icons.Rounded.Mic, null); Spacer(Modifier.width(10.dp)); Text("Start Recording", 17.sp) } }; item { Text("Recent Notes", 21.sp, fontWeight = FontWeight.Bold) }; items(notes) { NoteCard(it) { onSelect(it) } } } } }
@Composable private fun NoteCard(note: Note, onClick: () -> Unit) { Card(onClick = onClick, Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(note.type.uppercase(), 12.sp, fontWeight = FontWeight.Bold, color = Accent); Text(note.title, 19.sp, fontWeight = FontWeight.SemiBold, Modifier.padding(top = 8.dp)); Text(note.summary, color = Muted, maxLines = 4, Modifier.padding(top = 6.dp)); Text(formatTime(note.durationSeconds), 12.sp, color = Muted, Modifier.padding(top = 10.dp)) } } }
private enum class SpeechLanguage {
    Auto,
    Arabic,
    English;

    fun tag(defaultTag: String): String = when (this) {
        Auto -> defaultTag
        Arabic -> "ar-SA"
        English -> "en-US"
    }
}

@Composable private fun RecordScreen(
    recording: Boolean,
    elapsed: Long,
    transcript: String,
    language: SpeechLanguage,
    onLanguageChange: (SpeechLanguage) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) { Column(Modifier.fillMaxSize().background(Bg).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Recording", 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(60.dp)); Text(formatTime(elapsed), 52.sp, fontWeight = FontWeight.Bold); Text(if (recording) "Listening on device" else "Ready", color = Muted)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpeechLanguage.entries.forEach { option ->
            FilterChip(
                selected = option == language,
                onClick = { if (!recording) onLanguageChange(option) },
                label = { Text(option.name) },
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Waveform(recording); if (transcript.isNotBlank()) Text(transcript, color = Ink, maxLines = 5, Modifier.padding(16.dp)); Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (recording) {
            OutlinedButton(onClick = onPause, shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause")
            }
        } else {
            OutlinedButton(onClick = onResume, shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume")
            }
        }
        Button(onClick = onStop, Modifier.height(58.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9535F))) {
            Icon(Icons.Rounded.Stop, null); Spacer(Modifier.width(6.dp)); Text("Finish")
        }
    }
    Spacer(Modifier.height(30.dp)) } }
@Composable private fun Waveform(active: Boolean) { Row(Modifier.fillMaxWidth().height(70.dp), Arrangement.Center, Alignment.CenterVertically) { repeat(28) { i -> Box(Modifier.width(4.dp).height(((18 + (i * 17) % 46) * if (active) 1 else .45).dp).background(if (active) Accent else Color.LightGray, RoundedCornerShape(4.dp))); Spacer(Modifier.width(3.dp)) } } }
@Composable private fun SaveScreen(title: String, description: String, type: String, transcript: String, setTitle: (String) -> Unit, setDescription: (String) -> Unit, setType: (String) -> Unit, onSave: () -> Unit) { Column(Modifier.fillMaxSize().background(Bg).padding(22.dp)) { Text("Save your recording", 27.sp, fontWeight = FontWeight.Bold); Text("Your transcript stays on this device.", color = Muted); Spacer(Modifier.height(22.dp)); OutlinedTextField(title, setTitle, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true); Spacer(Modifier.height(12.dp)); OutlinedTextField(description, setDescription, Modifier.fillMaxWidth().height(120.dp), label = { Text("Description") }); Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Meeting", "Lecture", "Interview").forEach { AssistChip(onClick = { setType(it) }, label = { Text(it) }, leadingIcon = if (it == type) ({ Icon(Icons.Rounded.Check, null) }) else null) } }; Text("Transcript: ${transcript.length} characters", color = Muted, Modifier.padding(top = 16.dp)); Spacer(Modifier.weight(1f)); Button(onClick = onSave, enabled = title.isNotBlank() && transcript.isNotBlank(), Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Rounded.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Generate Smart Notes") } } }
@Composable private fun DetailScreen(note: Note, back: () -> Unit) { Scaffold(containerColor = Bg, topBar = { TopAppBar(title = { Text(note.title, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(back) { Icon(Icons.Rounded.ArrowBack, null) } }) }) { padding -> LazyColumn(Modifier.padding(padding).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Text("SMART NOTES", 12.sp, fontWeight = FontWeight.Bold, color = Accent); MarkdownLikeText(note.summary); if (note.transcript.isNotBlank()) { Text("Transcript", 20.sp, fontWeight = FontWeight.Bold, Modifier.padding(top = 18.dp)); Text(note.transcript, color = Muted) } } } } }
@Composable private fun MarkdownLikeText(markdown: String) { markdown.lines().forEach { line -> val clean = line.trim(); when { clean.startsWith("# ") -> Text(clean.removePrefix("# "), 27.sp, fontWeight = FontWeight.Bold); clean.startsWith("## ") -> Text(clean.removePrefix("## "), 20.sp, fontWeight = FontWeight.Bold, Modifier.padding(top = 12.dp)); clean.startsWith("- ") || clean.startsWith("• ") -> Text("• ${clean.drop(2)}", 16.sp, color = Ink, Modifier.padding(top = 5.dp)); clean.isNotBlank() -> Text(clean, 16.sp, color = Ink, Modifier.padding(top = 5.dp)) } } }
private fun joinTranscript(base: String, next: String): String =
    listOf(base.trim(), next.trim()).filter(String::isNotBlank).joinToString(" ")

private fun formatTime(seconds: Long): String = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
