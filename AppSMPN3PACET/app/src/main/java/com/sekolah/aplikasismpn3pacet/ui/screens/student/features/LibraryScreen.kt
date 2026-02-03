package com.sekolah.aplikasismpn3pacet.ui.screens.student.features

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sekolah.aplikasismpn3pacet.data.SubmissionStatus
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyLog
import com.sekolah.aplikasismpn3pacet.data.entity.LiteracyTask
import com.sekolah.aplikasismpn3pacet.data.entity.User
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.LiteracyUiState
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.StudentLiteracyViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    user: User, 
    onBack: () -> Unit,
    viewModel: StudentLiteracyViewModel? = null
) {
    var webView: WebView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lentera Digital") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                    IconButton(onClick = { webView?.evaluateJavascript("typeof window.__captureAndSend==='function'&&window.__captureAndSend()", null) }) {
                        Icon(Icons.Default.Send, "Kirim ke Wali Kelas")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ELibraryWebView(user, viewModel) { view -> webView = view }
        }
    }
}

@Composable
fun LiteracySubmissionTab(user: User, viewModel: StudentLiteracyViewModel) {
    var activeSubTab by remember { mutableStateOf("Input") } // "Input" or "Riwayat"
    val logs by viewModel.logs.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeTask by viewModel.activeTask.collectAsState(initial = null)

    LaunchedEffect(user.id) {
        viewModel.loadLogsByUserId(user.id)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Banner Area
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2962FF), // Blue 700
                                Color(0xFF448AFF)  // Blue Accent
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Program Literasi Sabtu",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"Buku adalah jendela dunia. Mari membaca!\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Points Card
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Poin",
                                tint = Color(0xFFFFD700), // Gold
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Total Poin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "1,250", // Placeholder value
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Books Card
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu, // Placeholder for Book icon
                                contentDescription = "Buku",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Buku Dibaca",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${logs.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Toggle Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(
                    text = "Tantangan Minggu Ini",
                    isSelected = activeSubTab == "Input",
                    onClick = { activeSubTab = "Input" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Riwayat Bacaan",
                    isSelected = activeSubTab == "Riwayat",
                    onClick = { activeSubTab = "Riwayat" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (activeSubTab == "Input") {
            item {
                if (activeTask != null) {
                    WeeklyChallengeCard(activeTask)
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada tugas aktif saat ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                LiteracyForm(user, viewModel, uiState)
            }
        } else {
            items(logs) { log ->
                LiteracyLogItem(log)
            }
            if (logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat literasi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

class LiteracyJsBridge(private val ctx: Context, private val user: User, private val viewModel: StudentLiteracyViewModel?) {
    @JavascriptInterface
    fun submit(payloadJson: String) {
        try {
            val obj = org.json.JSONObject(payloadJson)
            val title = obj.optString("title")
            val author = obj.optString("author")
            val duration = obj.optString("duration")
            val summary = obj.optString("summary")
            viewModel?.submitLogByUserId(user.id, title, author, duration, summary)
            Toast.makeText(ctx, "Mengirim: $title • $author • $duration", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(ctx, "Gagal mengambil data dari portal", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun WeeklyChallengeCard(task: LiteracyTask?) {
    if (task == null) return

    val dateFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID"))
    val dateString = try {
        dateFormat.format(java.util.Date(task.createdAt))
    } catch (e: Exception) {
        "Hari ini"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Badge
                Surface(
                    color = Color(0xFFE3F2FD), // Light Blue
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Date",
                            tint = Color(0xFF1565C0), // Blue
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = Color(0xFFFFF3E0), // Light Orange
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF6C00), // Orange
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info, // Should be Clock/Schedule but Info works as placeholder
                    contentDescription = "Duration",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.durationMinutes} Menit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Points",
                    tint = Color(0xFFFFD700), // Gold
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.points} Poin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun StatsChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = null
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteracyForm(user: User, viewModel: StudentLiteracyViewModel, uiState: LiteracyUiState) {
    var bookTitle by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var readingDuration by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var durationExpanded by remember { mutableStateOf(false) }
    
    val durations = listOf("15 Menit", "30 Menit", "45 Menit", "1 Jam", "> 1 Jam")
    // Removed scrollState as parent LazyColumn handles scrolling now

    Column(modifier = Modifier.fillMaxWidth()) {
        // Form Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu, // Using Menu as placeholder for Book/Pen icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Laporan Literasi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OutlinedTextField(
            value = bookTitle,
            onValueChange = { bookTitle = it },
            label = { Text("Judul Buku / Cerpen") },
            placeholder = { Text("Contoh: Laskar Pelangi") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Penulis") },
                placeholder = { Text("Nama Penulis") },
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = readingDuration,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Durasi Baca") },
                    placeholder = { Text("Pilih...") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            "Dropdown",
                            Modifier.clickable { durationExpanded = true }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = durationExpanded,
                    onDismissRequest = { durationExpanded = false }
                ) {
                    durations.forEach { duration ->
                        DropdownMenuItem(
                            text = { Text(duration) },
                            onClick = {
                                readingDuration = duration
                                durationExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("Ringkasan / Hikmah") },
            placeholder = { Text("Ceritakan sedikit apa yang kamu baca...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(bottom = 16.dp),
            maxLines = 10
        )

        Button(
            onClick = {
                viewModel.submitLogByUserId(user.id, bookTitle, author, readingDuration, summary)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2962FF) // Blue 700 to match design
            ),
            enabled = uiState !is LiteracyUiState.Loading
        ) {
            if (uiState is LiteracyUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mengirim...")
            } else {
                Text("Kirim Laporan")
            }
        }
        
        if (uiState is LiteracyUiState.Success) {
            Text(
                text = (uiState as LiteracyUiState.Success).message,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            LaunchedEffect(uiState) {
                bookTitle = ""
                author = ""
                readingDuration = ""
                summary = ""
                // viewModel.resetState() // Optional: reset state after delay if needed
            }
        }
        
        if (uiState is LiteracyUiState.Error) {
            Text(
                text = (uiState as LiteracyUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun LiteracyHistoryList(logs: List<LiteracyLog>) {
    LazyColumn {
        items(logs) { log ->
            LiteracyLogItem(log)
        }
        if (logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat literasi.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LiteracyLogItem(log: LiteracyLog) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(log.bookTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Penulis: ${log.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Durasi: ${log.readingDuration}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(log.submissionDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(log.status, log.grade)
            }
            if (!log.feedback.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Catatan Guru: ${log.feedback}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: SubmissionStatus, grade: String?) {
    val (color, text) = when (status) {
        SubmissionStatus.PENDING -> Color(0xFFEF6C00) to "Menunggu" // Orange
        SubmissionStatus.GRADED -> Color(0xFF2E7D32) to (grade ?: "Dinilai") // Green
        SubmissionStatus.REJECTED -> MaterialTheme.colorScheme.error to "Ditolak"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ELibraryWebView(user: User, viewModel: StudentLiteracyViewModel?, onWebViewCreated: (WebView) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT // Allow caching for session persistence
                    allowFileAccess = true
                    allowContentAccess = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile; rv:88.0) Gecko/88.0 Firefox/88.0"
                }
                
                addJavascriptInterface(LiteracyJsBridge(context, user, viewModel), "NativeLiteracy")
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        val js = """
                            (function() {
                                var attempts = 0;
                                var maxAttempts = 20;
                                
                                function tryLogin() {
                                    var emailInput = document.querySelector('input[type="email"]') || document.querySelector('input[name="email"]') || document.querySelector('input[name="username"]');
                                    var passwordInput = document.querySelector('input[type="password"]') || document.querySelector('input[name="password"]');
                                    var submitBtn = document.querySelector('button[type="submit"]') || document.querySelector('input[type="submit"]');

                                    if (emailInput && passwordInput && submitBtn) {
                                        if(emailInput.value === '${user.username}' && passwordInput.value === '${user.password}') {
                                            return; 
                                        }

                                        function setNativeValue(element, value) {
                                            const valueSetter = Object.getOwnPropertyDescriptor(element, 'value').set;
                                            const prototype = Object.getPrototypeOf(element);
                                            const prototypeValueSetter = Object.getOwnPropertyDescriptor(prototype, 'value').set;
                                            
                                            if (valueSetter && valueSetter !== prototypeValueSetter) {
                                                prototypeValueSetter.call(element, value);
                                            } else {
                                                valueSetter.call(element, value);
                                            }
                                            element.dispatchEvent(new Event('input', { bubbles: true }));
                                        }

                                        setNativeValue(emailInput, '${user.username}'); 
                                        setNativeValue(passwordInput, '${user.password}');

                                        setTimeout(function() {
                                            submitBtn.click();
                                        }, 500);
                                    } else {
                                        attempts++;
                                        if (attempts < maxAttempts) {
                                            setTimeout(tryLogin, 500);
                                        }
                                    }
                                }
                                
                                tryLogin();
                            })();
                        """
                        view?.evaluateJavascript(js, null)
                        
                        val hook = """
                            (function(){
                              function getVal(sel){
                                var el = document.querySelector(sel);
                                return el ? (el.value || el.innerText || "") : "";
                              }
                              function pickDuration(){
                                var sel = document.querySelector('select');
                                if (sel) return sel.value || (sel.options[sel.selectedIndex] ? sel.options[sel.selectedIndex].text : "");
                                var dd = Array.from(document.querySelectorAll('button,div')).find(function(x){ return /Menit|Jam/i.test((x.textContent||"").trim()); });
                                return dd ? dd.textContent.trim() : "";
                              }
                              function send(){
                                var payload = {
                                  title: getVal('input[placeholder*="Judul"]') || getVal('input[name*="judul"]'),
                                  author: getVal('input[placeholder*="Penulis"]') || getVal('input[name*="penulis"]'),
                                  duration: pickDuration(),
                                  summary: getVal('textarea') || getVal('div[contenteditable="true"]')
                                };
                                if (window.NativeLiteracy && typeof window.NativeLiteracy.submit === 'function') {
                                  window.NativeLiteracy.submit(JSON.stringify(payload));
                                }
                              }
                              window.__captureAndSend = send;
                              function attach(){
                                var buttons = Array.from(document.querySelectorAll('button,input[type="submit"]'));
                                buttons.filter(function(b){ return /Kirim|Submit|Simpan/i.test((b.textContent||b.value||"").trim()); })
                                       .forEach(function(b){
                                          if (!b.__nativeHooked) {
                                            b.__nativeHooked = true;
                                            b.addEventListener('click', function(){
                                              setTimeout(send, 200);
                                            }, true);
                                          }
                                       });
                              }
                              var tries=0; var max=60;
                              (function loop(){
                                attach();
                                tries++;
                                if (tries < max) setTimeout(loop, 500);
                              })();
                            })();
                        """
                        view?.evaluateJavascript(hook, null)
                        CookieManager.getInstance().flush()
                    }
                }
                
                loadUrl("https://e-perpustakaan-sekolah.vercel.app/login")
                
                onWebViewCreated(this)
            }
        }
    )
}
