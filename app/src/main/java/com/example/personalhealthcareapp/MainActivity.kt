package com.example.personalhealthcareapp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalhealthcareapp.ViewModel.ViewModel
import com.example.personalhealthcareapp.chat_managment.Chat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatScreen()
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ViewModel = viewModel()) {
    // This watches our new LIST of messages
    val chatHistory by viewModel.chathistory.collectAsState()
    var userProjectInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Personal Health AI",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(chatHistory) { message ->
                ChatBubble(message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // The Input Area
        Row {
            TextField(
                value = userProjectInput,
                onValueChange = { userProjectInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type symptoms...") }
            )
            Button(
                onClick = {
                    if (userProjectInput.isNotBlank()) {
                        viewModel.sendMessage(userProjectInput)
                        userProjectInput = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: Chat) {
    val backgroundColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textAlignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = textAlignment) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}