package com.sekolah.aplikasismpn3pacet.ui.screens.student.features

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sekolah.aplikasismpn3pacet.data.entity.PetAchievement
import com.sekolah.aplikasismpn3pacet.data.entity.PetQuest
import com.sekolah.aplikasismpn3pacet.data.entity.VirtualPet
import com.sekolah.aplikasismpn3pacet.ui.viewmodel.VirtualPetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualPetScreen(
    viewModel: VirtualPetViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sahabat Belajar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.pet != null) {
                        CoinDisplay(coins = uiState.pet!!.coins)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.pet == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null && uiState.pet == null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.pet != null) {
                PetContent(
                    pet = uiState.pet!!,
                    quests = uiState.quests,
                    achievements = uiState.achievements,
                    onFeed = { viewModel.feedPet(uiState.pet!!) },
                    onPlay = { viewModel.playWithPet(uiState.pet!!) },
                    onSleep = { viewModel.sleepPet(uiState.pet!!) },
                    onStudy = { viewModel.studyWithPet(uiState.pet!!) },
                    onClaimQuest = { viewModel.claimQuest(it, uiState.pet!!) },
                    onDebugProgress = { viewModel.progressQuest(it) } // Hidden feature for testing
                )
            }
        }
    }
}

@Composable
fun CoinDisplay(coins: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFD700), // Gold
        modifier = Modifier.padding(end = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star, // Coin icon placeholder
                contentDescription = "Coins",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$coins",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PetContent(
    pet: VirtualPet,
    quests: List<PetQuest>,
    achievements: List<PetAchievement>,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onSleep: () -> Unit,
    onStudy: () -> Unit,
    onClaimQuest: (PetQuest) -> Unit,
    onDebugProgress: (PetQuest) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Status", "Misi Harian", "Pencapaian")

    Column(modifier = Modifier.fillMaxSize()) {
        // Pet Avatar & Main Stats Area
        PetHeader(pet)

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> StatusTab(pet, onFeed, onPlay, onSleep, onStudy)
                1 -> QuestsTab(quests, onClaimQuest, onDebugProgress)
                2 -> AchievementsTab(achievements)
            }
        }
    }
}

@Composable
fun PetHeader(pet: VirtualPet) {
    val infiniteTransition = rememberInfiniteTransition(label = "petAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Level Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Level ${pet.level}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                val progress = pet.experiencePoints.toFloat() / (pet.level * 100f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    text = "${pet.experiencePoints}/${pet.level * 100} XP",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when(pet.petType) {
                    "CAT" -> Icons.Default.Face
                    else -> Icons.Default.Face
                },
                contentDescription = "Pet",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = pet.petName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusTab(
    pet: VirtualPet,
    onFeed: () -> Unit,
    onPlay: () -> Unit,
    onSleep: () -> Unit,
    onStudy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Stats Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                StatItem("Kesehatan", pet.health / 100f, Color.Red, Icons.Default.Favorite)
                Spacer(modifier = Modifier.height(8.dp))
                StatItem("Kebahagiaan", pet.happiness / 100f, Color.Yellow, Icons.Default.Face)
                Spacer(modifier = Modifier.height(8.dp))
                StatItem("Energi", pet.energy / 100f, Color(0xFFFFA500), Icons.Default.ThumbUp) // Orange
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                StatItem("Kecerdasan", pet.intelligence / 100f, Color.Blue, Icons.Default.Info)
                Spacer(modifier = Modifier.height(8.dp))
                StatItem("Sosial", pet.social / 100f, Color.Green, Icons.Default.Person)
                Spacer(modifier = Modifier.height(8.dp))
                StatItem("Lapar", pet.hunger / 100f, Color.Gray, Icons.Default.ShoppingCart) // Hunger inverted visually?
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Aktivitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ActionButton("Makan", Icons.Default.Home, Color(0xFFE91E63), "10 Koin", onFeed)
            ActionButton("Main", Icons.Default.Face, Color(0xFF2196F3), "-10 Energi", onPlay)
            ActionButton("Belajar", Icons.Default.Info, Color(0xFF9C27B0), "-20 Energi", onStudy)
            ActionButton("Tidur", Icons.Default.ThumbUp, Color(0xFF4CAF50), "+Energi", onSleep)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TipsCard()
    }
}

@Composable
fun TipsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9C27B0), // Purple
                            Color(0xFFE91E63)  // Pink/Red
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info, // Lightbulb replacement
                        contentDescription = "Tips",
                        tint = Color.Yellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tips Hari Ini",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Praktikkan 7 Kebiasaan Anak Indonesia Hebat setiap hari untuk pet yang sehat dan kuat! ⭐ Mulai dari bangun pagi, beribadah, berolahraga, makan sehat, gemar belajar, bermasyarakat, hingga tidur cepat. Semua kebiasaan ini akan boost stats pet kamu secara maksimal! 🌟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pro Tip Box
                InfoBox(
                    icon = Icons.Default.Star,
                    title = "Pro Tip:",
                    content = "Lakukan semua 7 kebiasaan dalam sehari untuk unlock achievement \"7 Habits Master\" (Gold)! Bangun pagi dan tidur cepat sangat penting untuk Energy dan Health pet!",
                    backgroundColor = Color(0xFFFFC107).copy(alpha = 0.2f),
                    borderColor = Color(0xFFFFC107),
                    iconColor = Color(0xFFFFC107)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Literasi Box
                InfoBox(
                    icon = Icons.Default.Menu, // Book replacement
                    title = "Literasi:",
                    content = "Gabungkan kebiasaan \"Gemar Belajar\" dengan membaca buku perpustakaan untuk combo EXP dan Intelligence boost maksimal!",
                    backgroundColor = Color(0xFF00BCD4).copy(alpha = 0.2f),
                    borderColor = Color(0xFF00BCD4),
                    iconColor = Color(0xFF00BCD4)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Perhatian Box
                InfoBox(
                    icon = Icons.Default.Warning,
                    title = "Perhatian:",
                    content = "Setiap pelanggaran akan menurunkan stats pet kamu! Poin pelanggaran tinggi bisa membuat pet sakit bahkan mati. Jaga kedisiplinan untuk pet yang sehat dan bahagia!",
                    backgroundColor = Color(0xFFF44336).copy(alpha = 0.2f),
                    borderColor = Color(0xFFF44336),
                    iconColor = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun InfoBox(
    icon: ImageVector,
    title: String,
    content: String,
    backgroundColor: Color,
    borderColor: Color,
    iconColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, progress: Float, color: Color, icon: ImageVector) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = color,
            )
        }
    }
}

@Composable
fun ActionButton(
    label: String, 
    icon: ImageVector, 
    color: Color, 
    cost: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color),
            modifier = Modifier.size(60.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(icon, null, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(cost, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun QuestsTab(
    quests: List<PetQuest>, 
    onClaim: (PetQuest) -> Unit,
    onDebugProgress: (PetQuest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(quests) { quest ->
            QuestItem(quest, onClaim, onDebugProgress)
        }
    }
}

@Composable
fun QuestItem(
    quest: PetQuest, 
    onClaim: (PetQuest) -> Unit,
    onDebugProgress: (PetQuest) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (quest.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp).clickable { onDebugProgress(quest) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(quest.title, fontWeight = FontWeight.Bold)
                    Text(quest.description, style = MaterialTheme.typography.bodySmall)
                }
                if (quest.isCompleted) {
                    Icon(Icons.Default.Check, "Selesai", tint = Color.Green)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "${quest.reward} XP", 
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { quest.progress.toFloat() / quest.target.toFloat() },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("${quest.progress}/${quest.target}")
            }
            if (!quest.isCompleted && quest.progress >= quest.target) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onClaim(quest) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Klaim Hadiah")
                }
            }
        }
    }
}

@Composable
fun AchievementsTab(achievements: List<PetAchievement>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(achievements) { achievement ->
            AchievementItem(achievement)
        }
    }
}

@Composable
fun AchievementItem(achievement: PetAchievement) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) Color(0xFFFFD700).copy(alpha=0.2f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (achievement.unlocked) Icons.Default.Star else Icons.Default.Person, // Placeholder icons
                contentDescription = null,
                tint = if (achievement.unlocked) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                achievement.title, 
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                achievement.description, 
                style = MaterialTheme.typography.bodySmall, 
                textAlign = TextAlign.Center,
                minLines = 2
            )
        }
    }
}
