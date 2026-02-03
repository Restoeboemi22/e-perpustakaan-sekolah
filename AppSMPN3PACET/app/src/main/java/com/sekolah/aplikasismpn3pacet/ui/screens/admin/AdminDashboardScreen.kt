package com.sekolah.aplikasismpn3pacet.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DashboardMenuItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val menuItems = listOf(
        DashboardMenuItem("Siswa", Icons.Default.Person, "manage_students", MaterialTheme.colorScheme.primary),
        DashboardMenuItem("Guru", Icons.Default.Face, "manage_teachers", MaterialTheme.colorScheme.secondary),
        DashboardMenuItem("Kelas", Icons.Default.Home, "manage_classes", MaterialTheme.colorScheme.tertiary),
        DashboardMenuItem("Perpustakaan", Icons.Default.Menu, "library", MaterialTheme.colorScheme.error), // Book icon not always available in default set, use Menu or similar
        DashboardMenuItem("Kedisiplinan", Icons.Default.Warning, "discipline", Color(0xFFE91E63)),
        DashboardMenuItem("Laporan", Icons.Default.List, "reports", Color(0xFF009688)),
        DashboardMenuItem("Pengaturan", Icons.Default.Settings, "settings", Color.Gray)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Admin Dashboard", fontWeight = FontWeight.Bold)
                        Text("SMPN 3 Pacet", style = MaterialTheme.typography.bodySmall)
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
            Text(
                text = "Selamat Datang, Admin",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menuItems) { item ->
                    DashboardMenuCard(item, onNavigate)
                }
            }
        }
    }
}

@Composable
fun DashboardMenuCard(
    item: DashboardMenuItem,
    onClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick(item.route) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(40.dp),
                tint = item.color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
