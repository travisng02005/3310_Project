package com.example.a3310_project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Alignment

@PreviewScreenSizes
@Composable
fun ListingsScreen(modifier: Modifier = Modifier) {
    val numItems = 20
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.verticalScroll(
            rememberScrollState())) {
            Text("Home Screen Content")
            ScrollableContent(numItems)
        }
        // Button at bottom right
        Button(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Icon")
        }
    }
}
@Composable
fun showMyListings() {

}
@Composable
fun ScrollableContent(numItems: Int) {
    repeat(numItems) {
        Text(modifier = Modifier.padding(20.dp), text = "Test Text $it")
    }
}