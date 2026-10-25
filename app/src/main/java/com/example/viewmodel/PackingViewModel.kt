package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PackingRepository
import com.example.model.CatalogData
import com.example.model.FamilyConstants
import com.example.model.PackItem
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PackingUiState(
  val tripDirection: String = "nach_sarn", // "nach_sarn" | "nach_hause"
  val activeAssignPerson: String = "diverse",
  val activeFilterPerson: String = "alle",
  val sarnList: List<PackItem> = emptyList(),
  val homeList: List<PackItem> = emptyList(),
  val recents: List<String> = emptyList(),
  val isPackedSectionExpanded: Boolean = false,
  val scriptUrl: String = "",
  val isSyncing: Boolean = false,
  val isCloudConfigured: Boolean = false,
  val toastMessage: String? = null,
  val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class PackingViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = PackingRepository(application)

  private val _uiState = MutableStateFlow(PackingUiState())
  val uiState: StateFlow<PackingUiState> = _uiState.asStateFlow()

  init {
    loadInitialData()
  }

  private fun loadInitialData() {
    val (sarn, home) = repository.loadLists()
    val recents = repository.loadRecents()
    val url = repository.getScriptUrl()
    val themeMode = repository.getThemeMode()

    _uiState.update {
      it.copy(
        sarnList = sarn,
        homeList = home,
        recents = recents,
        scriptUrl = url,
        isCloudConfigured = url.isNotBlank(),
        themeMode = themeMode
      )
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    repository.setThemeMode(mode)
    _uiState.update { it.copy(themeMode = mode) }
  }

  fun setTripDirection(direction: String) {
    _uiState.update { it.copy(tripDirection = direction) }
  }

  fun setAssignPerson(personId: String) {
    _uiState.update { it.copy(activeAssignPerson = personId) }
  }

  fun setFilterPerson(personId: String) {
    _uiState.update { it.copy(activeFilterPerson = personId) }
  }

  fun togglePackedSectionExpanded() {
    _uiState.update { it.copy(isPackedSectionExpanded = !it.isPackedSectionExpanded) }
  }

  fun toggleItemPacked(itemId: String) {
    val currentDir = _uiState.value.tripDirection
    var packedName = ""
    var isNowPacked = false

    if (currentDir == "nach_sarn") {
      val updated = _uiState.value.sarnList.map {
        if (it.id == itemId) {
          packedName = it.name
          isNowPacked = !it.isPacked
          it.copy(isPacked = isNowPacked)
        } else it
      }
      _uiState.update { it.copy(sarnList = updated) }
    } else {
      val updated = _uiState.value.homeList.map {
        if (it.id == itemId) {
          packedName = it.name
          isNowPacked = !it.isPacked
          it.copy(isPacked = isNowPacked)
        } else it
      }
      _uiState.update { it.copy(homeList = updated) }
    }

    if (isNowPacked && packedName.isNotBlank()) {
      recordRecent(packedName)
      showToast("✓ \"$packedName\" eingepackt!")
    } else if (!isNowPacked && packedName.isNotBlank()) {
      showToast("↩️ \"$packedName\" wieder auf der Liste")
    }

    saveAndPushAsync()
  }

  fun addItem(rawName: String, rawIcon: String? = null, personId: String? = null) {
    val name = rawName.trim()
    if (name.isBlank()) return

    val targetPerson = FamilyConstants.normalizePersonId(personId ?: _uiState.value.activeAssignPerson)
    val icon = rawIcon ?: CatalogData.findIcon(name)
    val currentDir = _uiState.value.tripDirection
    val currentList = if (currentDir == "nach_sarn") _uiState.value.sarnList else _uiState.value.homeList

    val existing = currentList.find { 
      it.name.equals(name, ignoreCase = true) && 
      FamilyConstants.normalizePersonId(it.personId) == targetPerson 
    }

    val updatedList = if (existing != null) {
      if (existing.isPacked) {
        currentList.map { if (it.id == existing.id) it.copy(isPacked = false, qty = 1) else it }
      } else {
        currentList.map { if (it.id == existing.id) it.copy(qty = it.qty + 1) else it }
      }
    } else {
      val newItem = PackItem(
        id = "item_${System.currentTimeMillis()}_${(100..999).random()}",
        name = name,
        personId = targetPerson,
        isPacked = false,
        qty = 1,
        icon = icon
      )
      listOf(newItem) + currentList
    }

    val personName = FamilyConstants.getMember(targetPerson).name
    showToast("+ \"$name\" für $personName")
    recordRecent(name)

    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = updatedList) }
    } else {
      _uiState.update { it.copy(homeList = updatedList) }
    }

    saveAndPushAsync()
  }

  fun updateItemQty(itemId: String, delta: Int) {
    val currentDir = _uiState.value.tripDirection
    fun adjust(list: List<PackItem>) = list.map {
      if (it.id == itemId) {
        val newQty = (it.qty + delta).coerceAtLeast(1)
        it.copy(qty = newQty)
      } else it
    }

    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = adjust(it.sarnList)) }
    } else {
      _uiState.update { it.copy(homeList = adjust(it.homeList)) }
    }
    saveAndPushAsync()
  }

  fun updateItemPerson(itemId: String, newPersonId: String) {
    val currentDir = _uiState.value.tripDirection
    fun adjust(list: List<PackItem>) = list.map {
      if (it.id == itemId) it.copy(personId = newPersonId) else it
    }

    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = adjust(it.sarnList)) }
    } else {
      _uiState.update { it.copy(homeList = adjust(it.homeList)) }
    }
    saveAndPushAsync()
  }

  fun deleteItem(itemId: String) {
    val currentDir = _uiState.value.tripDirection
    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = it.sarnList.filter { i -> i.id != itemId }) }
    } else {
      _uiState.update { it.copy(homeList = it.homeList.filter { i -> i.id != itemId }) }
    }
    showToast("Artikel gelöscht")
    saveAndPushAsync()
  }

  fun reactivateAllPacked() {
    val currentDir = _uiState.value.tripDirection
    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = it.sarnList.map { i -> i.copy(isPacked = false) }) }
    } else {
      _uiState.update { it.copy(homeList = it.homeList.map { i -> i.copy(isPacked = false) }) }
    }
    showToast("Alle abgehakten Artikel wieder aktiviert")
    saveAndPushAsync()
  }

  fun packAllFiltered() {
    val currentDir = _uiState.value.tripDirection
    val filter = _uiState.value.activeFilterPerson

    fun pack(list: List<PackItem>) = list.map {
      val matches = filter == "alle" || FamilyConstants.normalizePersonId(it.personId) == FamilyConstants.normalizePersonId(filter)
      if (!it.isPacked && matches) {
        it.copy(isPacked = true)
      } else it
    }

    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = pack(it.sarnList)) }
    } else {
      _uiState.update { it.copy(homeList = pack(it.homeList)) }
    }
    showToast("Auswahl eingepackt ✓")
    saveAndPushAsync()
  }

  fun resetCurrentTrip() {
    val currentDir = _uiState.value.tripDirection
    if (currentDir == "nach_sarn") {
      _uiState.update { it.copy(sarnList = emptyList()) }
    } else {
      _uiState.update { it.copy(homeList = emptyList()) }
    }
    showToast("Fahrt-Liste geleert")
    saveAndPushAsync()
  }

  fun resetAllToDefault() {
    val sarn = repository.defaultSarnItems()
    val home = repository.defaultHomeItems()
    _uiState.update { it.copy(sarnList = sarn, homeList = home) }
    showToast("Standard-Packliste wiederhergestellt")
    saveAndPushAsync()
  }

  fun saveScriptUrl(url: String) {
    repository.setScriptUrl(url)
    _uiState.update { it.copy(scriptUrl = url, isCloudConfigured = url.isNotBlank()) }
    showToast("Cloud-URL gespeichert!")
    if (url.isNotBlank()) {
      triggerSync()
    }
  }

  fun triggerSync() {
    val url = _uiState.value.scriptUrl
    if (url.isBlank()) {
      showToast("Keine Cloud-URL hinterlegt (arbeitet lokal)")
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isSyncing = true) }
      try {
        val (sarn, home) = repository.syncFromCloud(url)
        _uiState.update {
          it.copy(
            sarnList = sarn,
            homeList = home,
            isSyncing = false
          )
        }
        repository.saveLists(sarn, home)
        showToast("☁️ Mit Google Sheet synchronisiert!")
      } catch (e: Exception) {
        _uiState.update { it.copy(isSyncing = false) }
        showToast("⚠️ Sync fehlgeschlagen (offline)")
      }
    }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  private fun showToast(msg: String) {
    _uiState.update { it.copy(toastMessage = msg) }
  }

  private fun recordRecent(name: String) {
    val current = _uiState.value.recents
    val updated = listOf(name) + current.filterNot { it.equals(name, ignoreCase = true) }
    _uiState.update { it.copy(recents = updated.take(15)) }
    repository.saveRecents(updated)
  }

  private fun saveAndPushAsync() {
    val sarn = _uiState.value.sarnList
    val home = _uiState.value.homeList
    repository.saveLists(sarn, home)

    val url = _uiState.value.scriptUrl
    if (url.isNotBlank()) {
      viewModelScope.launch {
        repository.pushToCloud(url, sarn, home)
      }
    }
  }
}
