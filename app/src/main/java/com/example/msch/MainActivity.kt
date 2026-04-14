package com.example.msch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.msch.ui.theme.MSCHTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "period-db"
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MSCHTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        dao = db.periodDao(),
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(dao: PeriodDao, modifier: Modifier = Modifier) {
    val records by dao.getAllRecords().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val nextDateMillis = remember(records) {
        CyclePredictor.predictNextCycle(records)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Approximate next cycle date:")
                val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                Text(
                    text = sdf.format(Date(nextDateMillis)),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val newRecord = PeriodRecord(startDate = System.currentTimeMillis())
                    dao.insert(newRecord)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Period started today")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "History:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(records) { record ->
                PeriodItem(record)
            }
        }
    }
}

@Composable
fun PeriodItem(record: PeriodRecord) {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
    val dateString = sdf.format(Date(record.startDate))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = dateString,
            modifier = Modifier.padding(16.dp)
        )
    }
}