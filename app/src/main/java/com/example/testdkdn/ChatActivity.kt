package com.example.testdkdn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.testdkdn.ui.theme.TestDKDNTheme
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestDKDNTheme {
                ChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf("AI: Xin chào! Tôi có thể giúp gì cho bạn?")) }
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Lưu lịch sử hội thoại để gửi nguyên cho API
    val messagesHistory = remember {
        mutableStateListOf(
            mapOf("role" to "assistant", "content" to "Xin chào! Tôi có thể giúp gì cho bạn?")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { message ->
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                        .wrapContentWidth(
                            if (message.startsWith("AI:")) Alignment.Start else Alignment.End
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.startsWith("AI:")) Color(0xFFE3F2FD) else Color(0xFF0077B6)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(8.dp),
                        color = if (message.startsWith("AI:")) Color.Black else Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = userInput,
                onValueChange = { userInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = {
                    if (userInput.isNotBlank() && !isLoading) {
                        val newMessage = "Bạn: $userInput"
                        messages = messages + newMessage
                        messagesHistory.add(mapOf("role" to "user", "content" to userInput))
                        isLoading = true

                        val question = userInput // lưu lại trước khi reset

                        scope.launch {
                            val aiResponse = callGeminiAPI("""
                    Bạn là một trợ lý AI nói tiếng Việt.
                    Trả lời trực tiếp và ngắn gọn cho câu hỏi sau:
                    $question
                """.trimIndent())
                            messages = messages + "AI: $aiResponse"
                            messagesHistory.add(mapOf("role" to "assistant", "content" to aiResponse))
                            delay(1500)
                            isLoading = false
                        }
                        userInput = "" // reset sau
                    }
                }
            )
            {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gửi",
                    tint = if (userInput.isNotBlank()) Color(0xFF0077B6) else Color.Gray
                )
            }
        }
    }
}

// Hàm gọi OpenAI API với retry khi lỗi 429
// Thay callOpenAIAPI bằng callGeminiAPI
suspend fun callGeminiAPI(prompt: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val apiKey = "AIzaSyC2PSJYpfTvH56-usHelYt51l0zIjZrnwc" // Lấy từ Google AI Studio

            val jsonObject = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonObject.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) {
                    return@withContext "Lỗi ${response.code}: $body"
                }

                val jsonResp = JSONObject(body!!)
                val reply = jsonResp
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                return@withContext reply.trim()
            }

        } catch (e: Exception) {
            return@withContext "Lỗi kết nối: ${e.localizedMessage}"
        }
    }
}
