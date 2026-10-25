package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CatalogData
import com.example.model.CatalogItem
import com.example.model.FamilyConstants
import com.example.model.PackItem
import com.example.model.ThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PackingViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: PackingViewModel = viewModel()
      val uiState by viewModel.uiState.collectAsState()
      val isDark = when (uiState.themeMode) {
        ThemeMode.NORMAL -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
        PacklisteSarnApp(viewModel = viewModel)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacklisteSarnApp(viewModel: PackingViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  var showSettingsDialog by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<PackItem?>(null) }
  var searchQuery by remember { mutableStateOf("") }

  // Handle toast/snackbar notifications
  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  val activeTripList = if (uiState.tripDirection == "nach_sarn") uiState.sarnList else uiState.homeList
  val activeUnpacked = activeTripList.filter { !it.isPacked }

  val filteredUnpacked = if (uiState.activeFilterPerson == "alle") {
    activeUnpacked
  } else {
    activeUnpacked.filter {
      FamilyConstants.normalizePersonId(it.personId) == FamilyConstants.normalizePersonId(uiState.activeFilterPerson)
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBarSection(
        uiState = uiState,
        onTripDirectionChange = { viewModel.setTripDirection(it) },
        onSyncClick = { viewModel.triggerSync() },
        onSettingsClick = { showSettingsDialog = true }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 14.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. INPUT & LIVE AUTOCOMPLETE SECTION
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            // Text Input with Live Autocomplete
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Artikel suchen oder hinzufügen...", fontSize = 14.sp) },
              leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Suchen", tint = MaterialTheme.colorScheme.primary)
              },
              trailingIcon = {
                if (searchQuery.isNotBlank()) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                  ) {
                    IconButton(
                      onClick = { searchQuery = "" },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        Icons.Default.Clear,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                    IconButton(
                      onClick = {
                        viewModel.addItem(searchQuery)
                        searchQuery = ""
                      },
                      modifier = Modifier
                        .size(32.dp)
                        .testTag("add_item_button")
                    ) {
                      Icon(
                        Icons.Default.Add,
                        contentDescription = "Hinzufügen",
                        tint = MaterialTheme.colorScheme.primary
                      )
                    }
                  }
                }
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
              keyboardActions = KeyboardActions(onDone = {
                if (searchQuery.isNotBlank()) {
                  viewModel.addItem(searchQuery)
                  searchQuery = ""
                }
              }),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("item_search_input")
            )

            // Inline Live-Vorschläge (Tastatur bleibt offen und Vorschläge aktualisieren sich fortlaufend)
            androidx.compose.animation.AnimatedVisibility(
              visible = searchQuery.isNotBlank(),
              enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
              exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
              val trimmed = searchQuery.trim()
              val matches = remember(trimmed) {
                if (trimmed.isEmpty()) emptyList()
                else CatalogData.CATEGORIES.flatMap { it.items }
                  .filter { it.name.contains(trimmed, ignoreCase = true) }
                  .distinctBy { it.name.lowercase() }
                  .take(5)
              }

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp)
              ) {
                Column(
                  modifier = Modifier.padding(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "VORSCHLÄGE (Tippen zum Hinzufügen):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )

                  matches.forEach { item ->
                    Surface(
                      onClick = {
                        viewModel.addItem(item.name, item.icon)
                        searchQuery = ""
                      },
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.surface,
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(item.icon, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                          Text(
                            item.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                        }
                        Surface(
                          shape = RoundedCornerShape(6.dp),
                          color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                          Text(
                            "+ Hinzufügen",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                      }
                    }
                  }

                  // Option: Eingegebenen Text als neuen Artikel anlegen
                  Surface(
                    onClick = {
                      viewModel.addItem(trimmed)
                      searchQuery = ""
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(Modifier.width(6.dp))
                      Text(
                        text = "\"$trimmed\" als neuen Artikel anlegen",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }
                  }
                }
              }
            }

            Spacer(Modifier.height(10.dp))

            // Person selector chips for assignment
            Text(
              text = "Zuordnen für:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FamilyConstants.MEMBERS.forEach { member ->
                val isSelected = uiState.activeAssignPerson == member.id
                Surface(
                  onClick = { viewModel.setAssignPerson(member.id) },
                  shape = RoundedCornerShape(20.dp),
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                  border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.height(32.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(member.colorHex))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                      text = member.name,
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. VIEW FILTER BAR
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "Ansicht filtern:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // "Alle" filter chip
            val isAll = uiState.activeFilterPerson == "alle"
            FilterChip(
              selected = isAll,
              onClick = { viewModel.setFilterPerson("alle") },
              label = {
                Text(
                  "Alle (${activeUnpacked.size})",
                  fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              )
            )

            // Per-person filter chips
            FamilyConstants.MEMBERS.forEach { member ->
              val isSelected = uiState.activeFilterPerson == member.id
              val count = activeUnpacked.count {
                FamilyConstants.normalizePersonId(it.personId) == member.id
              }
              FilterChip(
                selected = isSelected,
                onClick = { viewModel.setFilterPerson(member.id) },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(member.colorHex))
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("${member.name} ($count)")
                  }
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
              )
            }
          }
        }
      }

      // 3. HAUPTBEREICH: "PACKLISTE" – HERVORRAGEND GERAHMT & INS ZENTRUM GERÜCKT
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
          ),
          border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Header der hervorgehobenen Packliste
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🧳", fontSize = 15.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                  Text(
                    text = "AKTUELLE PACKLISTE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = if (filteredUnpacked.isNotEmpty()) "${filteredUnpacked.size} Artikel noch einzupacken" else "Alles erledigt",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              if (filteredUnpacked.isNotEmpty()) {
                TextButton(
                  onClick = { viewModel.packAllFiltered() },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Alle abpacken", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            HorizontalDivider(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              thickness = 1.dp
            )

            // 3-Spaltiges Kachel-Raster (Kompakte Kacheln, 3 nebeneinander)
            if (filteredUnpacked.isEmpty()) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 20.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text("🎒", fontSize = 38.sp)
                Spacer(Modifier.height(8.dp))
                Text("Alles eingepackt!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                  "Wähle unten aus dem Katalog, was für die Reise nach Sarn mit soll.",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            } else {
              val chunked = filteredUnpacked.chunked(3)
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                chunked.forEach { trio ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    trio.forEach { item ->
                      BringTileCompact(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onTileClick = { viewModel.toggleItemPacked(item.id) },
                        onEditClick = { editingItem = item }
                      )
                    }
                    repeat(3 - trio.size) {
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 4. KATALOG & ZULETZT VERWENDET SECTION
      item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "📦 ARTIKEL-KATALOG",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black
            )
            val currentPerson = FamilyConstants.getMember(uiState.activeAssignPerson)
            Text(
              text = "1-Klick auf Kachel fügt Artikel für ${currentPerson.name} hinzu",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 11.sp
            )
          }
        }
      }

      // "Zuletzt verwendet" Chips
      if (uiState.recents.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "⚡ Zuletzt verwendet",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              uiState.recents.take(8).forEach { name ->
                val icon = CatalogData.findIcon(name)
                Surface(
                  onClick = { viewModel.addItem(name, icon) },
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.height(34.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                  ) {
                    Text(icon, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  }
                }
              }
            }
          }
        }
      }

      // Categories Sections
      CatalogData.CATEGORIES.forEach { category ->
        item {
          CatalogCategoryBlock(
            category = category,
            activeTripList = activeTripList,
            onItemClick = { catalogItem ->
              viewModel.addItem(catalogItem.name, catalogItem.icon)
            }
          )
        }
      }
    }
  }

  // Edit Item Dialog (Quantity +/- and Person Change)
  editingItem?.let { item ->
    EditItemDialog(
      item = item,
      onDismiss = { editingItem = null },
      onQtyChange = { delta -> viewModel.updateItemQty(item.id, delta) },
      onPersonChange = { newPersonId -> viewModel.updateItemPerson(item.id, newPersonId) },
      onDelete = {
        viewModel.deleteItem(item.id)
        editingItem = null
      }
    )
  }

  // Settings & Cloud Dialog
  if (showSettingsDialog) {
    SettingsDialog(
      uiState = uiState,
      onDismiss = { showSettingsDialog = false },
      onSaveUrl = { url -> viewModel.saveScriptUrl(url) },
      onTriggerSync = { viewModel.triggerSync() },
      onSetThemeMode = { mode -> viewModel.setThemeMode(mode) },
      onResetTrip = { viewModel.resetCurrentTrip() },
      onResetAll = { viewModel.resetAllToDefault() }
    )
  }
}

/**
 * Top App Bar with Trip Direction Switcher [🚗 Nach Sarn] | [🏠 Nach Hause]
 * Inklusive großzügigem Abstand zum oberen Displayrand (statusBarsPadding + 14dp)
 */
@Composable
fun TopAppBarSection(
  uiState: com.example.viewmodel.PackingUiState,
  onTripDirectionChange: (String) -> Unit,
  onSyncClick: () -> Unit,
  onSettingsClick: () -> Unit
) {
  val sarnCount = uiState.sarnList.count { !it.isPacked }
  val homeCount = uiState.homeList.count { !it.isPacked }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    shadowElevation = 3.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(top = 14.dp, start = 14.dp, end = 14.dp, bottom = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Text("🏔️", fontSize = 20.sp)
          }
          Spacer(Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                "Packliste Sarn",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(Modifier.width(6.dp))
              val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (uiState.isCloudConfigured) (if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else MaterialTheme.colorScheme.surfaceVariant
              ) {
                Text(
                  text = if (uiState.isCloudConfigured) "Cloud ✓" else "Lokal",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (uiState.isCloudConfigured) (if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)) else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
              }
            }
            Text(
              "Familien-Packliste",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onSyncClick) {
            if (uiState.isSyncing) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
          IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Einstellungen", tint = MaterialTheme.colorScheme.onSurface)
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      // FAHRTRICHTUNG UMSCHALTER: [🚗 Nach Sarn] | [🏠 Nach Hause]
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .padding(4.dp)
      ) {
        val isSarn = uiState.tripDirection == "nach_sarn"

        Surface(
          onClick = { onTripDirectionChange("nach_sarn") },
          shape = RoundedCornerShape(10.dp),
          color = if (isSarn) MaterialTheme.colorScheme.surface else Color.Transparent,
          shadowElevation = if (isSarn) 2.dp else 0.dp,
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text("🚗 Nach Sarn", fontWeight = if (isSarn) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Surface(
              shape = CircleShape,
              color = if (isSarn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                "$sarnCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSarn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
              )
            }
          }
        }

        Surface(
          onClick = { onTripDirectionChange("nach_hause") },
          shape = RoundedCornerShape(10.dp),
          color = if (!isSarn) MaterialTheme.colorScheme.surface else Color.Transparent,
          shadowElevation = if (!isSarn) 2.dp else 0.dp,
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text("🏠 Nach Hause", fontWeight = if (!isSarn) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Surface(
              shape = CircleShape,
              color = if (!isSarn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                "$homeCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isSarn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Kompakte Bring!-Kachel für 3-Spalten-Layout (3 Kacheln nebeneinander)
 * 1-Klick hakt den Artikel sofort ab
 */
@Composable
fun BringTileCompact(
  item: PackItem,
  modifier: Modifier = Modifier,
  onTileClick: () -> Unit,
  onEditClick: () -> Unit
) {
  val member = FamilyConstants.getMember(item.personId)
  val memberColor = Color(member.colorHex)
  val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

  // Farblich vom Hintergrund abheben
  val tileBg = if (isDark) {
    Color(0xFF1E293B)
  } else {
    Color(member.badgeBgHex).copy(alpha = 0.55f)
  }

  // Deutlicher, farblich abgestimmter Rahmen (hebt die Kachel spürbar ab)
  val tileBorder = BorderStroke(
    1.75.dp,
    if (isDark) memberColor.copy(alpha = 0.7f) else memberColor.copy(alpha = 0.55f)
  )

  Card(
    onClick = onTileClick,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = tileBg
    ),
    border = tileBorder,
    elevation = CardDefaults.cardElevation(
      defaultElevation = 3.dp,
      pressedElevation = 6.dp
    ),
    modifier = modifier
      .height(98.dp)
      .testTag("item_tile_${item.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp, vertical = 5.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Oberste Zeile: Person-Badge + Menge / Edit-Icon
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (isDark) memberColor.copy(alpha = 0.35f) else Color.White,
          border = BorderStroke(1.dp, memberColor.copy(alpha = 0.35f))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
          ) {
            Box(
              modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(memberColor)
            )
            Spacer(Modifier.width(3.dp))
            Text(
              text = member.name,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = memberColor,
              maxLines = 1
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (item.qty > 1) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.primary
            ) {
              Text(
                "${item.qty}x",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
              )
            }
          }
          IconButton(
            onClick = onEditClick,
            modifier = Modifier.size(18.dp)
          ) {
            Icon(
              Icons.Default.MoreVert,
              contentDescription = "Bearbeiten",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }

      // Zentrum: Icon
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(item.icon, fontSize = 24.sp)
      }

      // Unten: Name (vergrösserte, kontrastreiche Schrift)
      Text(
        text = item.name,
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        maxLines = 2,
        lineHeight = 14.5.sp,
        textAlign = TextAlign.Center,
        overflow = TextOverflow.Ellipsis,
        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

/**
 * Catalog Category Block with 1-Click addition tiles
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogCategoryBlock(
  category: com.example.model.CatalogCategory,
  activeTripList: List<PackItem>,
  onItemClick: (CatalogItem) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(category.icon, fontSize = 16.sp)
      Spacer(Modifier.width(6.dp))
      Text(
        category.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      maxItemsInEachRow = 3
    ) {
      category.items.forEach { catalogItem ->
        val activeItem = activeTripList.find {
          !it.isPacked && it.name.equals(catalogItem.name, ignoreCase = true)
        }
        val isAlreadyActive = activeItem != null

        Surface(
          onClick = { onItemClick(catalogItem) },
          shape = RoundedCornerShape(12.dp),
          color = if (isAlreadyActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAlreadyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
          ),
          modifier = Modifier
            .weight(1f)
            .height(84.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
              if (isAlreadyActive && activeItem != null) {
                val member = FamilyConstants.getMember(activeItem.personId)
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(member.colorHex))
                )
              }
            }

            Text(catalogItem.icon, fontSize = 24.sp)

            Text(
              text = catalogItem.name,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              textAlign = TextAlign.Center,
              maxLines = 2,
              lineHeight = 13.sp,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

/**
 * Edit Item Dialog
 */
@Composable
fun EditItemDialog(
  item: PackItem,
  onDismiss: () -> Unit,
  onQtyChange: (Int) -> Unit,
  onPersonChange: (String) -> Unit,
  onDelete: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(item.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
        Text(item.name, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Person assignment
        Text("Person zuweisen:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FamilyConstants.MEMBERS.forEach { member ->
            val isSelected = FamilyConstants.normalizePersonId(item.personId) == member.id
            Surface(
              onClick = { onPersonChange(member.id) },
              shape = RoundedCornerShape(16.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.height(32.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(member.colorHex))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                  member.name,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        // Quantity selector
        Text("Menge / Anzahl:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp)
        ) {
          Button(
            onClick = { onQtyChange(-1) },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(36.dp)
          ) {
            Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }

          Text(
            text = "${item.qty}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 24.dp)
          )

          Button(
            onClick = { onQtyChange(1) },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(36.dp)
          ) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Fertig")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDelete,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
      ) {
        Text("Löschen")
      }
    }
  )
}

/**
 * Settings and Cloud Sync Dialog
 */
@Composable
fun SettingsDialog(
  uiState: com.example.viewmodel.PackingUiState,
  onDismiss: () -> Unit,
  onSaveUrl: (String) -> Unit,
  onTriggerSync: () -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onResetTrip: () -> Unit,
  onResetAll: () -> Unit
) {
  var urlInput by remember { mutableStateOf(uiState.scriptUrl) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Einstellungen", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Theme-Auswahl (Dark Mode / Normal Mode)
        Text("Erscheinungsbild:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val modes = listOf(
            Triple(ThemeMode.NORMAL, "☀️ Normal", "Hell"),
            Triple(ThemeMode.DARK, "🌙 Dark", "Dunkel"),
            Triple(ThemeMode.SYSTEM, "⚙️ Auto", "System")
          )
          modes.forEach { (mode, title, desc) ->
            val isSelected = uiState.themeMode == mode
            Surface(
              onClick = { onSetThemeMode(mode) },
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              border = BorderStroke(
                1.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
              ),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
            ) {
              Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = title,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = desc,
                  fontSize = 10.sp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        HorizontalDivider()

        Text("Google Apps Script Web App URL:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
          value = urlInput,
          onValueChange = { urlInput = it },
          placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 12.sp) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { onSaveUrl(urlInput) },
            modifier = Modifier.weight(1f)
          ) {
            Text("URL Speichern", fontSize = 12.sp)
          }
          OutlinedButton(
            onClick = {
              onSaveUrl(urlInput)
              onTriggerSync()
            },
            modifier = Modifier.weight(1f)
          ) {
            Text("Sync", fontSize = 12.sp)
          }
        }

        HorizontalDivider()

        Text("Listen-Aktionen:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(
          onClick = {
            onResetTrip()
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Aktuelle Fahrt leeren", color = Color(0xFFD97706), fontSize = 12.sp)
        }

        OutlinedButton(
          onClick = {
            onResetAll()
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Alles zurücksetzen", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Schliessen")
      }
    }
  )
}
