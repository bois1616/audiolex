package de.hexenwoche.audiolex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("AudioLex", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Hörtraining: Klang → Wort → Bedeutung",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = {}, enabled = false, modifier = Modifier.width(240.dp)) {
                    Text("Lernmodus (ab M2)")
                }
                Button(onClick = {}, enabled = false, modifier = Modifier.width(240.dp)) {
                    Text("Prüfmodus (ab M3)")
                }
            }
        }
    }
}
