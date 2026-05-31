package com.matchbar.app.ui.screens.incidents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.matchbar.app.data.api.MatchBarApi
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.GradientButton
import com.matchbar.app.util.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class IncidentUiState(
    val subject: String = "",
    val message: String = "",
    val photoUris: List<Uri> = emptyList(),
    val submitting: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class IncidentViewModel(private val api: MatchBarApi) : ViewModel() {
    private val _state = MutableStateFlow(IncidentUiState())
    val state: StateFlow<IncidentUiState> = _state.asStateFlow()

    fun onSubject(v: String) = _state.update { it.copy(subject = v, error = null) }
    fun onMessage(v: String) = _state.update { it.copy(message = v, error = null) }
    fun addPhotos(uris: List<Uri>) = _state.update { it.copy(photoUris = it.photoUris + uris) }
    fun removePhoto(uri: Uri) = _state.update { it.copy(photoUris = it.photoUris - uri) }

    fun submit(context: Context) = viewModelScope.launch {
        val s = _state.value
        if (s.subject.isBlank() || s.message.isBlank()) {
            _state.update { it.copy(error = "El asunto y la descripción son obligatorios") }
            return@launch
        }
        _state.update { it.copy(submitting = true, error = null) }
        runCatching {
            val subjectPart = s.subject.trim().toRequestBody("text/plain".toMediaTypeOrNull())
            val messagePart = s.message.trim().toRequestBody("text/plain".toMediaTypeOrNull())
            val photoParts = s.photoUris.map { partFromUri(context, it) }
            api.submitIncident(subjectPart, messagePart, photoParts)
        }.onSuccess {
            _state.update { it.copy(submitting = false, success = true) }
        }.onFailure { e ->
            _state.update { it.copy(submitting = false, error = e.userMessage()) }
        }
    }

    private fun partFromUri(context: Context, uri: Uri): MultipartBody.Part {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val filename = queryDisplayName(context, uri) ?: "foto.jpg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer la imagen")
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData("photos", filename, body)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIncidentScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onBack: () -> Unit
) {
    val vm: IncidentViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.success) {
        if (state.success) {
            snackbarHost.showSnackbar("Incidencia enviada. Gracias por avisarnos.")
            onBack()
        }
    }

    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> if (uris.isNotEmpty()) vm.addPhotos(uris) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Crear incidencia") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Appear {
                Column {
                    Text(
                        "¿Algún problema o sugerencia?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Cuéntanoslo y nuestro equipo lo revisará.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Appear(indexForStagger = 1) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = state.subject,
                            onValueChange = vm::onSubject,
                            label = { Text("Asunto") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.message,
                            onValueChange = vm::onMessage,
                            label = { Text("Descripción") },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )

                        Spacer(Modifier.height(16.dp))
                        Text(
                            "FOTOS (OPCIONAL)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        if (state.photoUris.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.photoUris) { uri ->
                                    Box {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Foto de la incidencia",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 120.dp, height = 90.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.55f))
                                                .clickable { vm.removePhoto(uri) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Close, "Quitar foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        OutlinedButton(
                            onClick = { pickPhotos.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AddAPhoto, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Adjuntar fotos")
                        }
                    }
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))
            GradientButton(
                text = "Enviar incidencia",
                onClick = { vm.submit(context) },
                enabled = !state.submitting && state.subject.isNotBlank() && state.message.isNotBlank(),
                loading = state.submitting
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
