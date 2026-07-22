package com.corporacionronceros.fieldsync.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.corporacionronceros.fieldsync.presentation.navigation.FieldSyncNavHost
import com.corporacionronceros.fieldsync.presentation.theme.FieldSyncTheme
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity (single-activity + Compose Navigation). Hilt inyecta los ViewModels. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FieldSyncTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FieldSyncNavHost()
                }
            }
        }
    }
}
