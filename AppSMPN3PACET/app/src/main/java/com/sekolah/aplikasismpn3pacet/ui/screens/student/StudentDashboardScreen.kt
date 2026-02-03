package com.sekolah.aplikasismpn3pacet.ui.screens.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.sekolah.aplikasismpn3pacet.data.entity.User

data class StudentFeatureItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    user: User,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val features = listOf(
        StudentFeatureItem("Lentera Digital", Icons.Default.Menu, "library", Color(0xFFE91E63)), // E-Library (Was Perpustakaan)
        StudentFeatureItem("Absensi", Icons.Default.LocationOn, "attendance", MaterialTheme.colorScheme.tertiary), // Absensi
        StudentFeatureItem("Kedisiplinan", Icons.Default.Info, "discipline", Color(0xFF673AB7)), // Kedisiplinan
        StudentFeatureItem("Virtual Pet", Icons.Default.Face, "virtual_pet", Color(0xFFFF9800)), // Virtual Pet
        StudentFeatureItem("7 KAIH", Icons.Default.Star, "seven_habits", Color(0xFF009688)), // 7 Kebiasaan
        StudentFeatureItem("Lapor Bullying", Icons.Default.Warning, "report_bullying", Color.Red), // Anti-Bullying
        StudentFeatureItem("Notification & Alert System", Icons.Default.Notifications, "notifications", MaterialTheme.colorScheme.primary) // Was Jadwal
    )

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hai, ${user.fullName}", fontWeight = FontWeight.Bold)
                        Text("Siswa SMPN 3 Pacet", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pengumuman Terbaru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ujian Tengah Semester dimulai minggu depan. Harap persiapkan diri.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = "Menu Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(features) { feature ->
                    StudentFeatureCard(feature, onNavigate)
                }
            }
        }
    }
}

@Composable
fun StudentFeatureCard(
    feature: StudentFeatureItem,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick(feature.route) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                modifier = Modifier.size(36.dp),
                tint = feature.color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
