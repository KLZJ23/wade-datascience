package com.example.quarterproject

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SpatialUnderstandingScreen()
            }
        }
    }
}

@Composable
fun SpatialUnderstandingScreen(viewModel: SpatialViewModel = SpatialViewModel()) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("Find the cars") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val uiState by viewModel.uiState.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        bitmap = uri?.let { viewModel.uriToBitmap(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Image Display Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { btm ->
                // Check the UI state to decide what to display
                when (val state = uiState) {
                    is UiState.Success -> {
                        // If successful, show the image with bounding boxes
                        ImageWithBoundingBoxes(
                            bitmap = btm,
                            detectedObjects = state.detectedObjects
                        )
                    }
                    else -> {
                        // Otherwise, just show the original image
                        Image(
                            bitmap = btm.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Show a loading indicator
            if (uiState is UiState.Loading) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show error messages
        if (uiState is UiState.Error) {
            Text(
                text = (uiState as UiState.Error).errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        // --- Controls Area ---
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Enter a prompt") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Image")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                bitmap?.let { viewModel.analyzeImage(prompt, it) }
            },
            enabled = bitmap != null && uiState !is UiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Analyze Image")
        }
    }
}


@Composable
fun ImageWithBoundingBoxes(
    bitmap: Bitmap,
    detectedObjects: List<DetectedObject>
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Base Image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Analyzed Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Canvas for drawing bounding boxes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // The image might not fill the canvas. We need to calculate the scale and offset.
            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val canvasAspectRatio = canvasWidth / canvasHeight

            val drawnImageWidth: Float
            val drawnImageHeight: Float
            val xOffset: Float
            val yOffset: Float

            if (imageAspectRatio > canvasAspectRatio) {
                // Image is wider than the canvas area
                drawnImageWidth = canvasWidth
                drawnImageHeight = canvasWidth / imageAspectRatio
                xOffset = 0f
                yOffset = (canvasHeight - drawnImageHeight) / 2
            } else {
                // Image is taller than or equal to the canvas area
                drawnImageHeight = canvasHeight
                drawnImageWidth = canvasHeight * imageAspectRatio
                xOffset = (canvasWidth - drawnImageWidth) / 2
                yOffset = 0f
            }

            // Draw each bounding box
            detectedObjects.forEach { obj ->
                val box = obj.boundingBox
                drawRect(
                    color = Color.Red,
                    // Scale the normalized coordinates to the actual drawn image size
                    topLeft = androidx.compose.ui.geometry.Offset(
                        x = xOffset + box.x_min * drawnImageWidth,
                        y = yOffset + box.y_min * drawnImageHeight
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = (box.x_max - box.x_min) * drawnImageWidth,
                        height = (box.y_max - box.y_min) * drawnImageHeight
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}