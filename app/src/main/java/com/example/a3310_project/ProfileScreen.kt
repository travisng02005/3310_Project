package com.example.a3310_project

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    // Login state
    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf<Profile?>(null) }

    // Check login status
    LaunchedEffect(Unit) {
        userPreferences.isLoggedInFlow.collect { loggedIn ->
            isLoggedIn = loggedIn
            if (loggedIn) {
                userPreferences.loggedInUserIdFlow.collect { id ->
                    currentUserId = id
                    // Load user profile from database
                    id?.let {
                        currentUser = dbHelper.getProfile(it)
                    }
                }
            } else {
                currentUserId = null
                currentUser = null
            }
        }
    }

    // Show login prompt if not logged in
    if (!isLoggedIn) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please log in to view your profile")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Go to the Home screen to log in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        return
    }

    // Also return if user data hasn't loaded yet
    val user = currentUser
    if (user == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // --- Gallery State ---
    var galleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedProfilePhotoUri by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableStateOf(0) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    // Load saved gallery and profile photo from preferences
    LaunchedEffect(currentUser?.userId) {
        userPreferences.galleryFlow.collect { savedUris ->
            galleryImages = savedUris.toList()
        }
    }

    LaunchedEffect(currentUser?.userId) {
        userPreferences.profilePhotoFlow.collect { savedPhotoUri ->
            selectedProfilePhotoUri = savedPhotoUri
        }
    }

    // Image picker launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val uriString = it.toString()
            // Add to gallery if not already present
            if (!galleryImages.contains(uriString)) {
                galleryImages = galleryImages + uriString
                currentIndex = galleryImages.size - 1
                // Save gallery to preferences
                coroutineScope.launch {
                    userPreferences.saveGallery(galleryImages)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Profile Section ---
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsButton(
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo - clickable to change
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .clickable { showPhotoOptions = true }
                ) {
                    if (selectedProfilePhotoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(Uri.parse(selectedProfilePhotoUri)),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.profile_placeholder),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Edit overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = currentUser?.name ?: "null",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "@${currentUser?.userId}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Divider(color = Color.Gray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(24.dp))

        // --- Photo Gallery Section ---
        Text(
            text = "My Gallery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gallery Carousel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Arrow
            IconButton(
                onClick = {
                    if (currentIndex > 0) currentIndex--
                },
                enabled = currentIndex > 0
            ) {
                Text(
                    text = "<",
                    fontSize = 30.sp,
                    color = if (currentIndex > 0) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Image Box
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.LightGray)
                    .clickable {
                        pickImageLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (galleryImages.isEmpty() || currentIndex >= galleryImages.size) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("+", fontSize = 48.sp, color = Color.White)
                        Text("Add Photo", fontSize = 14.sp, color = Color.White)
                    }
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(galleryImages[currentIndex])),
                        contentDescription = "User Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Arrow
            IconButton(
                onClick = {
                    if (currentIndex < galleryImages.size - 1) currentIndex++
                    else if (currentIndex == galleryImages.size - 1) currentIndex = galleryImages.size
                },
                enabled = currentIndex < galleryImages.size
            ) {
                Text(
                    text = ">",
                    fontSize = 30.sp,
                    color = if (currentIndex < galleryImages.size) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }

        // Gallery indicator
        if (galleryImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (currentIndex < galleryImages.size) 
                    "${currentIndex + 1} of ${galleryImages.size}" 
                else 
                    "Add new photo",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons for current gallery photo
        if (galleryImages.isNotEmpty() && currentIndex < galleryImages.size) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Set as profile photo button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            userPreferences.saveProfilePhoto(galleryImages[currentIndex])
                            selectedProfilePhotoUri = galleryImages[currentIndex]
                        }
                    }
                ) {
                    Text("Set as Profile Photo")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete photo button
                OutlinedButton(
                    onClick = {
                        val updatedGallery = galleryImages.toMutableList()
                        val removedUri = updatedGallery.removeAt(currentIndex)
                        galleryImages = updatedGallery
                        
                        // If deleted photo was profile photo, clear it
                        if (selectedProfilePhotoUri == removedUri) {
                            coroutineScope.launch {
                                userPreferences.clearProfilePhoto()
                            }
                            selectedProfilePhotoUri = null
                        }
                        
                        // Adjust current index
                        if (currentIndex >= galleryImages.size && currentIndex > 0) {
                            currentIndex = galleryImages.size - 1
                        }
                        
                        // Save updated gallery
                        coroutineScope.launch {
                            userPreferences.saveGallery(galleryImages)
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Thumbnail gallery strip
        if (galleryImages.isNotEmpty()) {
            Text(
                text = "All Photos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(galleryImages) { index, uriString ->
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (index == currentIndex) 3.dp else 0.dp,
                                color = if (index == currentIndex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { currentIndex = index }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(Uri.parse(uriString)),
                            contentDescription = "Gallery thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Show indicator if this is the profile photo
                        if (uriString == selectedProfilePhotoUri) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Add photo button at the end
                item {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 24.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // Photo options dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = { Text("Change Profile Photo", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (galleryImages.isNotEmpty()) {
                        Text("Select from your gallery:")
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(galleryImages) { index, uriString ->
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (uriString == selectedProfilePhotoUri) 3.dp else 0.dp,
                                            color = if (uriString == selectedProfilePhotoUri) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                userPreferences.saveProfilePhoto(uriString)
                                                selectedProfilePhotoUri = uriString
                                            }
                                            showPhotoOptions = false
                                        }
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(Uri.parse(uriString)),
                                        contentDescription = "Gallery photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Text("Or add a new photo from your device")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPhotoOptions = false
                        pickImageLauncher.launch("image/*")
                    }
                ) {
                    Text("Upload New Photo")
                }
            },
            dismissButton = {
                if (selectedProfilePhotoUri != null) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                userPreferences.clearProfilePhoto()
                            }
                            selectedProfilePhotoUri = null
                            showPhotoOptions = false
                        }
                    ) {
                        Text("Remove Photo")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showPhotoOptions = false }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Button(
        onClick = {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        },
        modifier = modifier
    ) {
        Text(text = "Settings")
    }
}
