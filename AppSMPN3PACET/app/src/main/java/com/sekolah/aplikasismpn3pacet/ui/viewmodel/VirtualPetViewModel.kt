package com.sekolah.aplikasismpn3pacet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sekolah.aplikasismpn3pacet.data.SchoolRepository
import com.sekolah.aplikasismpn3pacet.data.entity.VirtualPet
import com.sekolah.aplikasismpn3pacet.data.entity.PetQuest
import com.sekolah.aplikasismpn3pacet.data.entity.PetAchievement
import com.sekolah.aplikasismpn3pacet.data.PetStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class VirtualPetUiState(
    val pet: VirtualPet? = null,
    val quests: List<PetQuest> = emptyList(),
    val achievements: List<PetAchievement> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class VirtualPetViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(VirtualPetUiState(isLoading = true))
    val uiState: StateFlow<VirtualPetUiState> = _uiState.asStateFlow()

    private var petJob: Job? = null

    fun loadPet(studentId: Long) {
        petJob?.cancel()
        petJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getVirtualPetByStudentId(studentId).collect { pet ->
                    if (pet == null) {
                        createPet(studentId)
                    } else {
                        // Combine quests and achievements
                        combine(
                            repository.getPetQuests(pet.id),
                            repository.getPetAchievements(pet.id)
                        ) { quests, achievements ->
                            // Check for daily reset
                            checkDailyReset(pet, quests)
                            Triple(pet, quests, achievements)
                        }.collect { (currentPet, quests, achievements) ->
                            _uiState.update { 
                                it.copy(
                                    pet = currentPet,
                                    quests = quests,
                                    achievements = achievements,
                                    isLoading = false
                                ) 
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load pet", isLoading = false) }
            }
        }
    }

    private suspend fun createPet(studentId: Long) {
        val newPet = VirtualPet(
            studentId = studentId,
            petName = "Buddy",
            petType = "CAT",
            status = PetStatus.HAPPY,
            lastFed = System.currentTimeMillis(),
            lastPlayed = System.currentTimeMillis(),
            accessories = null,
            intelligence = 50,
            energy = 80,
            social = 60,
            coins = 100,
            lastQuestReset = System.currentTimeMillis()
        )
        val petId = repository.insertVirtualPet(newPet)

        // Initial Quests
        createDailyQuests(petId)

        // Initial Achievements
        val achievements = listOf(
            PetAchievement(petId = petId, title = "Pemula", description = "Mulai perjalananmu", icon = "star", unlocked = true, unlockedAt = System.currentTimeMillis()),
            PetAchievement(petId = petId, title = "Rajin", description = "Hadir 7 hari berturut-turut", icon = "calendar"),
            PetAchievement(petId = petId, title = "Cerdas", description = "Capai Intelligence 80", icon = "brain"),
            PetAchievement(petId = petId, title = "Sultan", description = "Kumpulkan 1000 Koin", icon = "coin")
        )
        achievements.forEach { repository.insertPetAchievement(it) }
    }
    
    private suspend fun createDailyQuests(petId: Long) {
        val quests = listOf(
            PetQuest(petId = petId, title = "Hadir Hari Ini", description = "Masuk sekolah dan tercatat hadir", target = 1, reward = 30),
            PetQuest(petId = petId, title = "Praktik 3 Kebiasaan", description = "Lakukan 3 dari 7 kebiasaan", target = 3, reward = 50),
            PetQuest(petId = petId, title = "Belajar Fokus", description = "Selesaikan tugas tanpa gangguan", target = 1, reward = 40)
        )
        quests.forEach { repository.insertPetQuest(it) }
    }

    private fun checkDailyReset(pet: VirtualPet, quests: List<PetQuest>) {
        val lastReset = pet.lastQuestReset ?: 0L
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        calendar.timeInMillis = lastReset
        val lastResetDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        if (currentDay != lastResetDay) {
            viewModelScope.launch {
                // Reset quests
                repository.deletePetQuests(pet.id)
                createDailyQuests(pet.id)
                
                // Update pet last reset
                repository.updateVirtualPet(pet.copy(lastQuestReset = System.currentTimeMillis()))
            }
        }
    }

    fun feedPet(pet: VirtualPet) {
        viewModelScope.launch {
            if (pet.coins >= 10) {
                val updatedPet = pet.copy(
                    hunger = (pet.hunger - 20).coerceAtLeast(0),
                    health = (pet.health + 5).coerceAtMost(100),
                    happiness = (pet.happiness + 5).coerceAtMost(100),
                    energy = (pet.energy + 5).coerceAtMost(100),
                    coins = pet.coins - 10,
                    lastFed = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateVirtualPet(updatedPet)
            }
        }
    }

    fun playWithPet(pet: VirtualPet) {
        viewModelScope.launch {
            if (pet.energy >= 10) {
                val updatedPet = pet.copy(
                    happiness = (pet.happiness + 15).coerceAtMost(100),
                    hunger = (pet.hunger + 10).coerceAtMost(100),
                    energy = (pet.energy - 10).coerceAtLeast(0),
                    social = (pet.social + 5).coerceAtMost(100),
                    experiencePoints = pet.experiencePoints + 15,
                    lastPlayed = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val finalPet = checkLevelUp(updatedPet)
                repository.updateVirtualPet(finalPet)
            }
        }
    }

    fun studyWithPet(pet: VirtualPet) {
        viewModelScope.launch {
             if (pet.energy >= 20) {
                val updatedPet = pet.copy(
                    intelligence = (pet.intelligence + 5).coerceAtMost(100),
                    energy = (pet.energy - 20).coerceAtLeast(0),
                    hunger = (pet.hunger + 15).coerceAtMost(100),
                    experiencePoints = pet.experiencePoints + 20,
                    updatedAt = System.currentTimeMillis()
                )
                val finalPet = checkLevelUp(updatedPet)
                repository.updateVirtualPet(finalPet)
             }
        }
    }

    fun sleepPet(pet: VirtualPet) {
        viewModelScope.launch {
            val updatedPet = pet.copy(
                health = (pet.health + 10).coerceAtMost(100),
                energy = 100, // Full restore
                updatedAt = System.currentTimeMillis()
            )
            repository.updateVirtualPet(updatedPet)
        }
    }

    fun claimQuest(quest: PetQuest, pet: VirtualPet) {
        viewModelScope.launch {
            if (!quest.isCompleted && quest.progress >= quest.target) {
                val updatedQuest = quest.copy(isCompleted = true)
                repository.updatePetQuest(updatedQuest)
                
                val updatedPet = pet.copy(
                    coins = pet.coins + quest.reward,
                    experiencePoints = pet.experiencePoints + (quest.reward / 2)
                )
                repository.updateVirtualPet(checkLevelUp(updatedPet))
            }
        }
    }
    
    // Debug function to simulate quest progress
    fun progressQuest(quest: PetQuest) {
        viewModelScope.launch {
            if (!quest.isCompleted && quest.progress < quest.target) {
                repository.updatePetQuest(quest.copy(progress = quest.progress + 1))
            }
        }
    }

    private fun checkLevelUp(pet: VirtualPet): VirtualPet {
        val xpThreshold = pet.level * 100
        return if (pet.experiencePoints >= xpThreshold) {
            pet.copy(
                level = pet.level + 1,
                experiencePoints = pet.experiencePoints - xpThreshold,
                happiness = 100,
                intelligence = (pet.intelligence + 2).coerceAtMost(100),
                energy = 100
            )
        } else {
            pet
        }
    }
}