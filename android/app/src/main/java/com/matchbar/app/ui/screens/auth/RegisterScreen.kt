package com.matchbar.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.data.model.Role
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onRegistered: () -> Unit,
    onBack: () -> Unit
) {
    val vm: AuthViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success) onRegistered() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Appear {
                Text(
                    "Únete a MatchBar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Crea tu cuenta en segundos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            Appear(indexForStagger = 1) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = vm::onName,
                            label = { Text("Nombre") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = vm::onEmail,
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Filled.Email, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = vm::onPassword,
                            label = { Text("Contraseña (mín. 8)") },
                            leadingIcon = { Icon(Icons.Filled.Lock, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Appear(indexForStagger = 2) {
                Text(
                    "¿QUÉ TIPO DE CUENTA?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoleCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Person,
                        title = "Usuario",
                        subtitle = "Busco bares",
                        selected = state.role == Role.USER,
                        onClick = { vm.onRole(Role.USER) }
                    )
                    RoleCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.SportsBar,
                        title = "Bar",
                        subtitle = "Tengo un local",
                        selected = state.role == Role.BAR,
                        onClick = { vm.onRole(Role.BAR) }
                    )
                }
            }

            AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(28.dp))
            Appear(indexForStagger = 3) {
                GradientButton(
                    text = "Crear cuenta",
                    onClick = vm::register,
                    enabled = !state.loading,
                    loading = state.loading
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "roleBorder"
    )
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "roleBg"
    )
    val scale by animateFloatAsState(if (selected) 1f else 0.97f, label = "roleScale")
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        colors = CardDefaults.outlinedCardColors(containerColor = container),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = contentColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
    }
}
