package com.company.employer.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect {
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mon Profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (!state.isEditing) {
                        IconButton(onClick = { viewModel.onEvent(ProfileEvent.StartEditing) }) {
                            Icon(Icons.Default.Edit, "Modifier")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.user == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.user != null -> {
                    ProfileContent(
                        state = state,
                        onEvent = viewModel::onEvent
                    )
                }
            }

            // Change password dialog
            if (state.showChangePasswordDialog) {
                ChangePasswordDialog(
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
fun ProfileContent(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${state.user?.firstName} ${state.user?.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "@${state.user?.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                AssistChip(
                    onClick = { },
                    label = { Text(state.user?.role ?: "EMPLOYER") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }
        }

        // Profile fields
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isEditing) {
                    // Editing mode
                    OutlinedTextField(
                        value = state.editEmail,
                        onValueChange = { onEvent(ProfileEvent.EmailChanged(it)) },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = state.editFirstName,
                        onValueChange = { onEvent(ProfileEvent.FirstNameChanged(it)) },
                        label = { Text("Prénom") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = state.editLastName,
                        onValueChange = { onEvent(ProfileEvent.LastNameChanged(it)) },
                        label = { Text("Nom") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = state.editPhoneNumber,
                        onValueChange = { onEvent(ProfileEvent.PhoneNumberChanged(it)) },
                        label = { Text("Téléphone") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onEvent(ProfileEvent.CancelEditing) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annuler")
                        }

                        Button(
                            onClick = { onEvent(ProfileEvent.SaveProfile) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Enregistrer")
                            }
                        }
                    }
                } else {
                    // View mode
                    ProfileField("Email", state.user?.email ?: "", Icons.Default.Email)
                    ProfileField("Prénom", state.user?.firstName ?: "", Icons.Default.Person)
                    ProfileField("Nom", state.user?.lastName ?: "", Icons.Default.Person)
                    ProfileField("Téléphone", state.user?.phoneNumber ?: "Non renseigné", Icons.Default.Phone)
                    state.user?.wilaya?.let {
                        ProfileField("Wilaya", it, Icons.Default.LocationOn)
                    }
                    state.user?.group?.let {
                        ProfileField("Groupe", it, Icons.Default.Group)
                    }
                }
            }
        }

        // Actions
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Changer le mot de passe") },
                    leadingContent = { Icon(Icons.Default.Lock, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        onEvent(ProfileEvent.ShowChangePasswordDialog)
                    }
                )

                Divider()

                ListItem(
                    headlineContent = {
                        Text(
                            "Se déconnecter",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable { onEvent(ProfileEvent.Logout) }
                )
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ChangePasswordDialog(
    state: ProfileState,
    onEvent: (ProfileEvent) -> Unit
) {
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onEvent(ProfileEvent.DismissChangePasswordDialog) },
        title = { Text("Changer le mot de passe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.currentPassword,
                    onValueChange = { onEvent(ProfileEvent.CurrentPasswordChanged(it)) },
                    label = { Text("Mot de passe actuel") },
                    visualTransformation = if (currentPasswordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = state.newPassword,
                    onValueChange = { onEvent(ProfileEvent.NewPasswordChanged(it)) },
                    label = { Text("Nouveau mot de passe") },
                    visualTransformation = if (newPasswordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = state.confirmPassword,
                    onValueChange = { onEvent(ProfileEvent.ConfirmPasswordChanged(it)) },
                    label = { Text("Confirmer le mot de passe") },
                    visualTransformation = if (confirmPasswordVisible)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                state.passwordChangeError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (state.passwordChangeSuccess) {
                    Text(
                        text = "✓ Mot de passe changé avec succès",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onEvent(ProfileEvent.ChangePassword) },
                enabled = !state.isLoading &&
                        state.currentPassword.isNotBlank() &&
                        state.newPassword.isNotBlank() &&
                        state.confirmPassword.isNotBlank()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Changer")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(ProfileEvent.DismissChangePasswordDialog) }) {
                Text("Annuler")
            }
        }
    )
}
