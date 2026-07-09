package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.database.CharacterEntity
import com.example.data.database.GeneratedImageEntity
import com.example.data.NanobanaGenerator.PortraitStyle
import com.example.ui.ScanStep
import com.example.ui.AppViewModel
import com.example.ui.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    viewModel: AppViewModel,
    state: UiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Bottom Sheet states
    var showCharacterSheet by remember { mutableStateOf(false) }
    var showModelSettingsSheet by remember { mutableStateOf(false) }
    var activeManagingCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    
    // Media Picker for image references
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachReferenceUri(uri.toString())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (state.isDarkTheme) {
                        listOf(Color(0xFF0F0E17), Color(0xFF1E1B29))
                    } else {
                        listOf(Color(0xFFF9F9FB), Color(0xFFEFEFF4))
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp) // Leave space for bottom bar and dock
        ) {
            // Elegant Cosmic Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "NANOBANA STABILIZATION ENGINE",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Consistent Face Synthesis",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan. Lock Features. Generate Portraits.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Results and Previews Card area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If generating, show a futuristic scanning loader
                if (state.isGenerating) {
                    GenerationLoader(progressText = state.generationProgressText)
                } else if (state.latestGeneratedImage != null) {
                    // Latest result display card
                    LatestResultCard(
                        image = state.latestGeneratedImage,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDelete = { viewModel.deleteImage(it) }
                    )
                } else {
                    // Empty showcase state
                    EmptyShowcaseState(onStartScan = { viewModel.startFaceScanning() })
                }

                // Selected character indicator banner if active
                if (state.selectedCharacter != null) {
                    SelectedCharacterBanner(
                        character = state.selectedCharacter,
                        onClear = { viewModel.selectCharacter(null) }
                    )
                }

                // Reference image indicator if active
                if (state.attachedReferenceUri != null) {
                    AttachedReferenceBanner(
                        uri = state.attachedReferenceUri,
                        onClear = { viewModel.attachReferenceUri(null) }
                    )
                }

                // 1. CHARACTER PROFILE ENGINE (Inline horizontal scrolling list of characters)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MY CHARACTER PRESETS",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Create New Character Card
                        item {
                            Card(
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(125.dp)
                                    .clickable { viewModel.startFaceScanning() },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Scan face",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "New Scan",
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Save Preset",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Saved Character Cards
                        items(state.characters.size) { index ->
                            val character = state.characters[index]
                            val isSelected = state.selectedCharacter?.id == character.id
                            Card(
                                modifier = Modifier
                                    .width(105.dp)
                                    .height(125.dp)
                                    .clickable {
                                        if (isSelected) {
                                            viewModel.selectCharacter(null)
                                        } else {
                                            viewModel.selectCharacter(character)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Gray.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Upper Settings trigger for manager Dialog
                                    IconButton(
                                        onClick = { activeManagingCharacter = character },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Manage Preset",
                                            tint = Color.Gray.copy(alpha = 0.8f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(Color.Gray.copy(alpha = 0.2f))
                                        ) {
                                            if (character.straightFaceUri != null) {
                                                AsyncImage(
                                                    model = character.straightFaceUri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Face,
                                                    contentDescription = null,
                                                    tint = Color.Gray,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = character.name,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "${(character.stabilityWeight * 100).toInt()}% Lock",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. NANOBANA STYLE PRESETS
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "NANOBANA HIGH-FIDELITY STYLES",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allStyles = PortraitStyle.values()
                        items(allStyles.size) { idx ->
                            val style = allStyles[idx]
                            val isSelectedStyle = state.selectedStyle == style
                            
                            FilterChip(
                                selected = isSelectedStyle,
                                onClick = { viewModel.setSelectedStyle(style) },
                                label = { 
                                    Text(
                                        text = style.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ) 
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when(style) {
                                            PortraitStyle.CYBERPUNK -> Icons.Default.Bolt
                                            PortraitStyle.RENAISSANCE -> Icons.Default.Palette
                                            PortraitStyle.COSMIC -> Icons.Default.Public
                                            PortraitStyle.NEON_NOIR -> Icons.Default.VisibilityOff
                                            PortraitStyle.FANTASY_ELF -> Icons.Default.Park
                                            PortraitStyle.ANIME -> Icons.Default.AutoAwesome
                                            else -> Icons.Default.Portrait
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                    containerColor = Color.Gray.copy(alpha = 0.08f),
                                    labelColor = Color.White,
                                    iconColor = Color.Gray
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelectedStyle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 3. PORTRAIT ASPECT RATIO SELECTOR
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "IMAGE ASPECT RATIO",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ratios = listOf("1:1", "16:9", "9:16", "4:3")
                        ratios.forEach { ratio ->
                            val isSelectedRatio = state.selectedAspectRatio == ratio
                            val label = when(ratio) {
                                "1:1" -> "1:1 Square"
                                "16:9" -> "16:9 Land"
                                "9:16" -> "9:16 Port"
                                else -> "4:3 Classic"
                            }
                            
                            FilterChip(
                                selected = isSelectedRatio,
                                onClick = { viewModel.setSelectedAspectRatio(ratio) },
                                label = { 
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    containerColor = Color.Gray.copy(alpha = 0.08f),
                                    labelColor = Color.White
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelectedRatio) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // --- Bottom Combined Dock (Prompt input and expander options) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (state.isDarkTheme) Color(0xFF161521).copy(alpha = 0.95f)
                        else Color.White.copy(alpha = 0.98f)
                    )
                    .border(
                        1.dp,
                        if (state.isDarkTheme) Color.White.copy(alpha = 0.12f)
                        else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(12.dp)
            ) {
                // Horizontal quick status widgets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nanobana Stable Model v3.2 Pro",
                            color = if (state.isDarkTheme) Color.Gray else Color.DarkGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Model parameters quick link
                    IconButton(
                        onClick = { showModelSettingsSheet = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Parameters",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Real Prompt Entry Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Input Icons Row (Upload Image, Select Character, Create Character)
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Image upload reference button
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("upload_reference_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddPhotoAlternate,
                                contentDescription = "Add Reference Image",
                                tint = if (state.attachedReferenceUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Character Select bottom sheet trigger
                        IconButton(
                            onClick = { showCharacterSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("select_character_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Select Face Profile",
                                tint = if (state.selectedCharacter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Face Scanner Trigger Button
                        IconButton(
                            onClick = { viewModel.startFaceScanning() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("build_character_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Scan New Face",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // TextField Input
                    TextField(
                        value = state.prompt,
                        onValueChange = { viewModel.updatePrompt(it) },
                        placeholder = {
                            Text(
                                "Describe your portrait scene...",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("prompt_input_field"),
                        maxLines = 2,
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Generate Floating Action Button
                    IconButton(
                        onClick = { viewModel.generatePortrait() },
                        enabled = !state.isGenerating,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (state.isGenerating) Color.Gray
                                else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .testTag("submit_generation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Generate Portrait",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (activeManagingCharacter != null) {
            CharacterManagerDialog(
                character = activeManagingCharacter!!,
                onDismiss = { activeManagingCharacter = null },
                viewModel = viewModel
            )
        }
    }

    // --- Bottom Sheet: Character Selection ---
    if (showCharacterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCharacterSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = if (state.isDarkTheme) Color(0xFF161521) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT FACE PROFILE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            showCharacterSheet = false
                            viewModel.startFaceScanning()
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Face")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (state.characters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Scanned Faces Found",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Use the 3D Scan to lock your features.",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.heightIn(max = 300.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            state.characters.forEach { character ->
                                CharacterListItem(
                                    character = character,
                                    isSelected = state.selectedCharacter?.id == character.id,
                                    onSelect = {
                                        viewModel.selectCharacter(character)
                                        showCharacterSheet = false
                                    },
                                    onDelete = { viewModel.deleteCharacter(character) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- Bottom Sheet: Model Settings & Seeds ---
    if (showModelSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = if (state.isDarkTheme) Color(0xFF161521) else Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "NANOBANA MODEL SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Version selection
                Text(
                    text = "Model Consistency Core",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (state.isDarkTheme) Color.LightGray else Color.Black
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Nanobana v3.2-Consistent", "Nanobana v3.0-Classic").forEach { ver ->
                        val isSel = state.modelVersion == ver
                        Button(
                            onClick = { viewModel.setModelVersion(ver) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f),
                                contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(ver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weight Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Face Lock Guidance Weight",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (state.isDarkTheme) Color.LightGray else Color.Black
                    )
                    Text(
                        text = "${(state.guidanceWeight * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = state.guidanceWeight,
                    onValueChange = { viewModel.setGuidanceWeight(it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Higher weight enforces rigid feature stabilization. Lower weight blends context fluidly.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Seed Locks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Seed Lock Stabilizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (state.isDarkTheme) Color.LightGray else Color.Black
                        )
                        Text(
                            text = "Lock image seed for continuous iterations",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.seedLock,
                        onCheckedChange = {
                            viewModel.toggleSeedLock(it)
                            if (it) viewModel.setCustomSeed(382947L)
                            else viewModel.setCustomSeed(null)
                        }
                    )
                }

                if (state.seedLock) {
                    OutlinedTextField(
                        value = state.customSeed?.toString() ?: "",
                        onValueChange = { viewModel.setCustomSeed(it.toLongOrNull()) },
                        label = { Text("Custom Seed Value") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun GenerationLoader(progressText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loader_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SYNTHESIZING PORTRAIT",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progressText,
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
fun LatestResultCard(
    image: GeneratedImageEntity,
    onToggleFavorite: (GeneratedImageEntity) -> Unit,
    onDelete: (GeneratedImageEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("latest_result_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                AsyncImage(
                    model = image.imageUrl,
                    contentDescription = "Generated Character",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Sparkle visual tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Cyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HIGH-FIDELITY",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Text info & actions
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = image.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Model: ${image.modelUsed}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Seed: #${image.seed}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Row {
                        IconButton(onClick = { onToggleFavorite(image) }) {
                            Icon(
                                imageVector = if (image.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (image.isFavorite) Color.Red else Color.Gray
                            )
                        }
                        IconButton(onClick = { onDelete(image) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyShowcaseState(onStartScan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Portrait,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "CREATE YOUR MODEL",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Stable Character Portrait Studio",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a creative scenario in the dock below, or scan your facial features using our interactive video scanner to build a persistent consistent character.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch 3D Scanner")
            }
        }
    }
}

@Composable
fun SelectedCharacterBanner(
    character: CharacterEntity,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    if (character.straightFaceUri != null) {
                        AsyncImage(
                            model = character.straightFaceUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Face,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = character.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Consistent Face locked (Seed: #${character.seed})",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear Lock", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AttachedReferenceBanner(
    uri: String,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Ref Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Reference Image Attached",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Applying style structures from image",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear Photo", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun CharacterListItem(
    character: CharacterEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Gray.copy(alpha = 0.08f)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                if (character.straightFaceUri != null) {
                    AsyncImage(
                        model = character.straightFaceUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = character.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Stabilization Lock: ${(character.stabilityWeight * 100).toInt()}%",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Character", tint = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterManagerDialog(
    character: CharacterEntity,
    onDismiss: () -> Unit,
    viewModel: AppViewModel
) {
    var editName by remember { mutableStateOf(character.name) }
    var stabilityWeight by remember { mutableStateOf(character.stabilityWeight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "CHARACTER PRESET MANAGER",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name rename section
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Character Identity") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    viewModel.renameCharacter(character, editName)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Feature Stabilization Weight slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Feature Stabilization Lock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${(stabilityWeight * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Controls how rigidly the 3D geometry enforces specific facial features.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Slider(
                        value = stabilityWeight,
                        onValueChange = {
                            stabilityWeight = it
                            viewModel.updateCharacterStabilityWeight(character, it)
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5-alignment view thumbnail grids
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Saved 3D Alignment Angles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val scannedSteps = listOf(
                            Pair("Front", character.straightFaceUri),
                            Pair("Right", character.rightFaceUri),
                            Pair("Left", character.leftFaceUri),
                            Pair("Down", character.downFaceUri),
                            Pair("Up", character.upFaceUri)
                        )
                        
                        scannedSteps.forEach { (label, uri) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    if (uri != null) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp).align(Alignment.Center)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Character Model details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Facial Geometry Seed:", fontSize = 11.sp, color = Color.Gray)
                            Text("#${character.seed}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Offline Synchronization:", fontSize = 11.sp, color = Color.Gray)
                            Text("Active Local Cache", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Green)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.deleteCharacter(character)
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Preset", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF161521),
        shape = RoundedCornerShape(20.dp)
    )
}
