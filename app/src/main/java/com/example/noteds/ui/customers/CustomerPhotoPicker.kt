package com.example.noteds.ui.customers

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CustomerPhotoPicker(
    title: String,
    subtitle: String,
    currentPhotoUri: String?,
    modifier: Modifier = Modifier,
    onPhotoSelected: (String) -> Unit,
    onViewPhoto: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val authority = remember(context.packageName) { "${context.packageName}.fileprovider" }
    var showSheet by remember { mutableStateOf(false) }
    var pendingCamera by remember { mutableStateOf(false) }
    var latestTempUri by remember { mutableStateOf<Uri?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            latestTempUri?.let { onPhotoSelected(it.toString()) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPhotoSelected(it.toString()) }
        showSheet = false
    }

    fun launchCamera() {
        val uri = createTempImageUri(context, authority)
        latestTempUri = uri
        if (uri != null) {
            takePictureLauncher.launch(uri)
        }
        showSheet = false
    }

    LaunchedEffect(cameraPermissionState.status) {
        if (pendingCamera) {
            if (cameraPermissionState.status.isGranted) {
                pendingCamera = false
                launchCamera()
            } else {
                pendingCamera = false
            }
        }
    }

    Column(
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentPhotoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentPhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { showSheet = true }
                )
            } else {
                BoxPlaceholder(onClick = { showSheet = true })
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { showSheet = true }) {
                    Text(text = if (currentPhotoUri == null) "Add photo" else "Change photo")
                }
                if (currentPhotoUri != null && onViewPhoto != null) {
                    TextButton(onClick = { onViewPhoto(currentPhotoUri) }) {
                        Text("View photo")
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Update photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        launchCamera()
                    } else {
                        pendingCamera = true
                        cameraPermissionState.launchPermissionRequest()
                    }
                }) {
                    Text("Take photo")
                }
                TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Choose from gallery")
                }
                TextButton(onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                    }
                }) {
                    Text("Cancel")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PhotoGrid(
    title: String,
    subtitle: String,
    photoUris: List<String?>,
    onPhotoChanged: (Int, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authority = remember(context.packageName) { "${context.packageName}.fileprovider" }
    val slots = List(3) { photoUris.getOrNull(it) }

    var selectedIndex by remember { mutableStateOf(0) }
    var showSheet by remember { mutableStateOf(false) }
    var pendingCamera by remember { mutableStateOf(false) }
    var latestTempUri by remember { mutableStateOf<Uri?>(null) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            latestTempUri?.let { uri ->
                onPhotoChanged(selectedIndex, uri.toString())
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPhotoChanged(selectedIndex, it.toString()) }
        showSheet = false
    }

    fun launchCamera() {
        val uri = createTempImageUri(context, authority)
        latestTempUri = uri
        if (uri != null) {
            takePictureLauncher.launch(uri)
        }
        showSheet = false
    }

    LaunchedEffect(cameraPermissionState.status) {
        if (pendingCamera) {
            if (cameraPermissionState.status.isGranted) {
                pendingCamera = false
                launchCamera()
            } else {
                pendingCamera = false
            }
        }
    }

    Column(modifier = modifier) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            slots.forEachIndexed { index, uri ->
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            selectedIndex = index
                            showSheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "點擊上傳",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "選擇照片來源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        launchCamera()
                    } else {
                        pendingCamera = true
                        cameraPermissionState.launchPermissionRequest()
                    }
                }) {
                    Text("拍照")
                }
                TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("從相冊選擇")
                }

                // --- 新增：移除照片按钮 ---
                if (slots[selectedIndex] != null) {
                    TextButton(onClick = {
                        onPhotoChanged(selectedIndex, null)
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                        }
                    }) {
                        Text("移除照片", color = MaterialTheme.colorScheme.error)
                    }
                }
                // -----------------------

                TextButton(onClick = {
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                    }
                }) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
private fun BoxPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tap to add",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun createTempImageUri(context: Context, authority: String): Uri? {
    return try {
        val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        val image = File.createTempFile("customer_photo_", ".jpg", imagesDir)
        FileProvider.getUriForFile(context, authority, image)
    } catch (ioe: IOException) {
        null
    }
}