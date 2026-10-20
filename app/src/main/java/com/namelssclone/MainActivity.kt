package com.namelssclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.namelssclone.core.CloneManager
import com.namelssclone.core.RootExecutor
import com.namelssclone.model.DeviceProfile
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var cloneManager: CloneManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloneManager = CloneManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CloneDashboard(cloneManager)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloneDashboard(cloneManager: CloneManager) {
    val coroutineScope = rememberCoroutineScope()
    var isRooted by remember { mutableStateOf<Boolean?>(null) }
    var selectedPackage by remember { mutableStateOf("com.instagram.android") }
    val clones = remember { mutableStateListOf<DeviceProfile>() }

    LaunchedEffect(Unit) {
        isRooted = RootExecutor.isRootAvailable()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NamelessClone Manager") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRooted == true) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = when (isRooted) {
                        true -> "Root Access: Granted (UID 0)"
                        false -> "Root Access: Denied (Root required)"
                        null -> "Checking Root Access..."
                    },
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = selectedPackage,
                onValueChange = { selectedPackage = it },
                label = { Text("Target Package Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val nextId = (clones.maxOfOrNull { it.cloneId } ?: 0) + 1
                    val newProfile = cloneManager.generateRandomProfile(selectedPackage, nextId)
                    clones.add(newProfile)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate New Clone Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Active Clones", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(clones) { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.profileName, style = MaterialTheme.typography.bodyLarge)
                                Text("Android ID: ${profile.androidId}", style = MaterialTheme.typography.bodySmall)
                                Text("Model: ${profile.build.model}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        cloneManager.launchClone(profile, ephemeral = false)
                                    }
                                }
                            ) {
                                Text("Launch")
                            }
                        }
                    }
                }
            }
        }
    }
}
