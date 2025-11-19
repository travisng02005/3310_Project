package com.example.a3310_project

import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {

    // --- Gallery State ---
    var galleryImages by remember { mutableStateOf(listOf<Uri>()) }
    var currentIndex by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            galleryImages = galleryImages + it
            currentIndex = galleryImages.size - 1
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
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier.align(Alignment.TopStart),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_placeholder),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Jessica Nguyen",
                    style = MaterialTheme.typography.headlineMedium
                )
                // --- Smaller text for username ---
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@jessicanguyen",
                    fontSize = 16.sp,
                    color = Color.Gray
                )


            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Divider(color = Color.Gray, thickness = 1.dp)

        Spacer(modifier = Modifier.height(32.dp)) // pushed gallery further down

        // --- Gallery Carousel ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Arrow
            Text(
                text = "<",
                fontSize = 30.sp,
                modifier = Modifier
                    .clickable {
                        if (currentIndex > 0) currentIndex--
                    }
                    .padding(8.dp)
            )

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
                    Text("+", fontSize = 48.sp, color = Color.White)
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(galleryImages[currentIndex]),
                        contentDescription = "User Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Arrow
            Text(
                text = ">",
                fontSize = 30.sp,
                modifier = Modifier
                    .clickable {
                        if (currentIndex < galleryImages.size - 1) currentIndex++
                        else currentIndex = galleryImages.size // show "+" box
                    }
                    .padding(8.dp)
            )
        }
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
