package com.example.personalhealthcareapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.personalhealthcareapp.ViewModel.ViewModel
import com.example.personalhealthcareapp.chat_managment.Chat
import com.example.personalhealthcareapp.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalHealthCareAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: ViewModel = viewModel()) {
    val chatHistory by viewModel.chathistory.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    var userProjectInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Content Layer (Base Layer 0e0e0e)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp), // spacing-4
            contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp) // spacing-6 (2rem) between list items
        ) {
            item {
                HeaderGreeting()
            }
            items(chatHistory) { message ->
                ChatBubble(message)
            }
            if (isGenerating) {
                item {
                    PulseIndicator()
                }
            }
        }

        // Top Glassmorphism Header
        TopHeader(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        // Bottom Persistent Input Anchor
        InputAnchor(
            inputState = userProjectInput,
            onInputChange = { userProjectInput = it },
            onSend = {
                if (userProjectInput.isNotBlank()) {
                    viewModel.sendMessage(userProjectInput)
                    userProjectInput = ""
                }
            },
            enabled = !isGenerating,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun HeaderGreeting() {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            buildAnnotatedString {
                append("Good\nmorning.\n")
                withStyle(style = SpanStyle(color = Primary)) {
                    append("NeuroPocket")
                }
                append("\nis active.")
            },
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "How can I assist your cognitive workflow today?\nOur neural models are optimized for precision analysis.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TopHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(SurfaceContainerHighest.copy(alpha = 0.6f)) // Glassmorphism base
            // In a real device we'd apply BlurRenderEffect here if supported
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Asymmetry logo far left
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "NeuroPocket",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(SurfaceContainerHigh, shape = CircleShape)
        )
    }
}

@Composable
fun ChatBubble(message: Chat) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = when {
        message.isError -> SurfaceContainerHigh
        isUser -> SurfaceContainerHighest
        else -> SurfaceContainerLow
    }

    val shape = if (isUser) RoundedCornerShape(6.dp) else RoundedCornerShape(12.dp)
    val textStyle = if (isUser) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    val textColor = when {
        message.isError -> Primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(20.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.isLoading && message.text.isEmpty()) {
                PulseIndicator()
            } else {
                Text(text = message.text, style = textStyle, color = textColor)
            }
        }
    }
}

@Composable
fun PulseIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(Primary.copy(alpha = alpha), shape = CircleShape)
    )
}

@Composable
fun InputAnchor(
    inputState: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineAlpha by animateFloatAsState(targetValue = if (isFocused) 0.2f else 0f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)) // xl radius (0.75rem ~ 12dp)
            .background(SurfaceContainerHighest.copy(alpha = 0.8f)) 
            .border(1.dp, Primary.copy(alpha = outlineAlpha), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /* Session Settings bottom-right visual flow approximation */ }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings / AI Session", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        BasicTextField(
            value = inputState,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(Primary),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                if (inputState.isEmpty()) {
                    Text("Inquire about a topic...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                innerTextField()
            }
        )
        
        val primaryGradient = Brush.linearGradient(
            colors = listOf(Primary, PrimaryContainer)
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (enabled) primaryGradient else Brush.linearGradient(listOf(SurfaceContainerHigh, SurfaceContainerHigh)))
                .clickable(enabled = enabled) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            // "Send" or arrow. Using generic Send icon which could simulate right arrow.
            Icon(
                Icons.Default.Send, 
                contentDescription = "Send", 
                tint = OnPrimary
            )
        }
    }
}