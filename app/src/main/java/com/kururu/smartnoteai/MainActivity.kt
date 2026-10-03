package com.kururu.smartnoteai

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

private val Bg = Color(0xFFF7F7FB)
private val Ink = Color(0xFF181820)
private val Muted = Color(0xFF747480)
private val Accent = Color(0xFF5B5CE2)

data class Note(val title: String, val type: String, val duration: String, val date: String, val summary: String)

class MainActivity : ComponentActivity() {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SmartNoteApp(::startRecording, ::stopRecording) } }
    private fun startRecording(): Boolean {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { permission.launch(Manifest.permission.RECORD_AUDIO); return false }
        val file = File(filesDir, "recording_${System.currentTimeMillis()}.m4a"); currentFile = file
        recorder = MediaRecorder(this).apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(file.absolutePath); prepare(); start() }
        return true
    }
    private fun stopRecording(): String? { recorder?.runCatching { stop(); release() }; recorder = null; return currentFile?.absolutePath }
}

@Composable
fun SmartNoteApp(start: () -> Boolean, stop: () -> String?) {
    var screen by remember { mutableStateOf("home") }; var recording by remember { mutableStateOf(false) }; var seconds by remember { mutableLongStateOf(0L) }
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Meeting") }
    var selected by remember { mutableStateOf<Note?>(null) }
    var notes by remember { mutableStateOf(listOf(Note("Q4 Product Planning","Meeting","42 min","Today","The team discussed the Q4 roadmap, AI assistant priorities, Arabic support, and API performance."), Note("Software Architecture","Lecture","58 min","Yesterday","Clean architecture, boundaries, dependency inversion, and scalable service design."), Note("AI Strategy","Meeting","31 min","Sep 30","Discussed RAG, vector search, evaluation, and a phased AI rollout."))) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); if (recording) seconds++ } }
    MaterialTheme(colorScheme = lightColorScheme(background = Bg, surface = Color.White, primary = Accent)) {
        when(screen) {
            "home" -> Home(notes, { screen = "record" }) { selected = it; screen = "detail" }
            "record" -> RecordScreen(recording, seconds, { if (!recording) recording = start() }, { if (recording) { stop(); recording = false; screen = "save" } })
            "save" -> SaveScreen(title, description, type, { title=it }, { description=it }, { type=it }) { if(title.isNotBlank()) notes = listOf(Note(title,type,formatTime(seconds),"Just now","AI notes will be generated from the transcript."))+notes; screen="home" }
            "detail" -> selected?.let { DetailScreen(it) { screen="home" } } ?: run { screen="home" }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Home(notes: List<Note>, onRecord: () -> Unit, onDetail: (Note) -> Unit) {
    Scaffold(containerColor=Bg,bottomBar={ NavigationBar(containerColor=Color.White) { NavigationBarItem(true,{}, {},icon={Icon(Icons.Rounded.Home,null)},label={Text("Home")}); NavigationBarItem(false,{}, {},icon={Icon(Icons.Rounded.Search,null)},label={Text("Search")}); NavigationBarItem(false,{}, {},icon={Icon(Icons.Rounded.Settings,null)},label={Text("Settings")}) }}) { p ->
        LazyColumn(Modifier.padding(p).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { Spacer(Modifier.height(22.dp)); Text("Good evening 👋",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Ink); Text("Capture. Understand. Remember.",color=Muted,modifier=Modifier.padding(top=4.dp)) }
            item { Button(onClick=onRecord,modifier=Modifier.fillMaxWidth().height(64.dp),shape=RoundedCornerShape(20.dp)){Icon(Icons.Rounded.Mic,null);Spacer(Modifier.width(10.dp));Text("Start Recording",fontSize=17.sp)} }
            item { Text("Recent Notes",fontSize=21.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=10.dp)) }
            items(notes){ NoteCard(it){onDetail(it)} }
        }
    }
}

@Composable fun NoteCard(n: Note, click: () -> Unit) { Card(onClick=click,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(18.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(n.type=="Lecture")Icons.Rounded.School else Icons.Rounded.Groups,null,tint=Accent);Spacer(Modifier.width(10.dp));Text(n.type.uppercase(),fontSize=12.sp,fontWeight=FontWeight.Bold,color=Accent);Spacer(Modifier.weight(1f));Text(n.date,color=Muted,fontSize=12.sp)};Text(n.title,fontSize=19.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=10.dp));Text(n.summary,color=Muted,maxLines=2,modifier=Modifier.padding(top=6.dp));Text(n.duration,color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=10.dp))}} }

@Composable fun RecordScreen(recording:Boolean,seconds:Long,start:()->Unit,stop:()->Unit){Column(Modifier.fillMaxSize().background(Bg).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth()){Text("Recording",fontSize=22.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Icon(Icons.Rounded.Close,null)};Spacer(Modifier.height(90.dp));Text(formatTime(seconds),fontSize=52.sp,fontWeight=FontWeight.Bold,color=Ink);Text(if(recording)"Recording in progress" else "Ready to capture",color=Muted,modifier=Modifier.padding(top=8.dp));Spacer(Modifier.height(40.dp));Waveform(recording);Spacer(Modifier.weight(1f));Row(horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={},enabled=recording,modifier=Modifier.height(58.dp),shape=RoundedCornerShape(18.dp)){Icon(Icons.Rounded.Pause,null);Spacer(Modifier.width(6.dp));Text("Pause")};Button(onClick=if(recording)stop else start,modifier=Modifier.size(82.dp),shape=RoundedCornerShape(28.dp),colors=ButtonDefaults.buttonColors(containerColor=if(recording)Color(0xFFD9535F)else Accent)){Icon(if(recording)Icons.Rounded.Stop else Icons.Rounded.Mic,null)}};Spacer(Modifier.height(35.dp))}}

@Composable fun Waveform(active:Boolean){Row(Modifier.height(70.dp).fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){repeat(34){i->Box(Modifier.width(4.dp).height(((18+(i*17)%46)*if(active)1 else .45).dp).clip(RoundedCornerShape(4.dp)).background(if(active)Accent else Color.LightGray));Spacer(Modifier.width(3.dp))}}}

@Composable fun SaveScreen(title:String,description:String,type:String,setTitle:(String)->Unit,setDescription:(String)->Unit,setType:(String)->Unit,save:()->Unit){Column(Modifier.fillMaxSize().background(Bg).padding(22.dp)){Text("Save your recording",fontSize=27.sp,fontWeight=FontWeight.Bold);Text("Add context so your future self can find it.",color=Muted,modifier=Modifier.padding(top=6.dp,bottom=24.dp));OutlinedTextField(title,setTitle,modifier=Modifier.fillMaxWidth(),label={Text("Title")},singleLine=true,shape=RoundedCornerShape(14.dp));Spacer(Modifier.height(14.dp));OutlinedTextField(description,setDescription,modifier=Modifier.fillMaxWidth().height(120.dp),label={Text("Description")},shape=RoundedCornerShape(14.dp));Spacer(Modifier.height(18.dp));Text("Type",fontWeight=FontWeight.SemiBold);Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Meeting","Lecture","Interview").forEach{AssistChip(onClick={setType(it)},label={Text(it)},leadingIcon=if(type==it)({Icon(Icons.Rounded.Check,null)})else null)}};Spacer(Modifier.weight(1f));Button(onClick=save,enabled=title.isNotBlank(),modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(18.dp)){Icon(Icons.Rounded.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text("Generate Smart Notes")}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DetailScreen(note:Note,back:()->Unit){Scaffold(containerColor=Bg,topBar={TopAppBar(title={Text(note.title,fontWeight=FontWeight.Bold)},navigationIcon={IconButton(back){Icon(Icons.Rounded.ArrowBack,null)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=Bg))}){p->LazyColumn(Modifier.padding(p).padding(horizontal=22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{Text("SMART NOTES",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Accent);Text("Executive Summary",fontSize=25.sp,fontWeight=FontWeight.Bold);Text(note.summary,fontSize=16.sp,lineHeight=25.sp,color=Ink)};item{Section("🎯 Key Points",listOf("Priorities and next steps were discussed","Important requirements were identified","The team aligned on the next milestone"))};item{Section("🚀 Action Items",listOf("Review the meeting outcomes","Prepare the next milestone","Share follow-up notes"))};item{Section("💡 Decisions",listOf("The key priorities should be handled first","Follow-up is required before the next meeting"))};item{Section("❓ Open Questions",listOf("What are the remaining dependencies?","What should be finalized next?"))};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){AssistChip(onClick={},label={Text("Transcript")});AssistChip(onClick={},label={Text("Export Markdown")})};Spacer(Modifier.height(30.dp))}}}}

@Composable fun Section(title:String,items:List<String>){Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(18.dp)){Text(title,fontSize=19.sp,fontWeight=FontWeight.Bold);items.forEach{Row(Modifier.padding(top=12.dp),verticalAlignment=Alignment.Top){Text("•",color=Accent,fontSize=20.sp);Spacer(Modifier.width(8.dp));Text(it,color=Ink,lineHeight=22.sp)}}}}}
fun formatTime(seconds:Long):String=String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60)
