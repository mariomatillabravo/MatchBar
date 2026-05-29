package com.matchbar.app.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matchbar.app.ui.common.Appear
import com.matchbar.app.ui.common.BrandBadge
import com.matchbar.app.ui.common.GradientButton

@Composable
fun LoginScreen(
    vmFactory: androidx.lifecycle.ViewModelProvider.Factory,
    onLoginSuccess: () -> Unit,
    onRegister: () -> Unit
) {
    val vm: AuthViewModel = viewModel(factory = vmFactory)
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success) onLoginSuccess() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            Appear {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BrandBadge(size = 96.dp, pulse = true)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "MatchBar",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Encuentra dónde ver el fútbol",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Appear(indexForStagger = 1) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Iniciar sesión",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

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
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Filled.Lock, null) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedVisibility(
                            visible = state.error != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                state.error ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(20.dp))
                        GradientButton(
                            text = "Entrar",
                            onClick = vm::login,
                            enabled = !state.loading,
                            loading = state.loading,
                            leadingIcon = Icons.AutoMirrored.Filled.Login
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Appear(indexForStagger = 2) {
                TextButton(onClick = onRegister) {
                    Text("¿No tienes cuenta? ")
                    Text("Regístrate", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
