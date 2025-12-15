package com.bookstore.ui.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bookstore.ui.theme.BookstoreTheme
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import com.bookstore.BuildConfig
import com.bookstore.data.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookstoreTheme {
                ChatScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf("AI: Xin chào! Tôi có thể giúp bạn tìm sách hoặc tư vấn về các đầu sách trong cửa hàng.")) }
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var books by remember { mutableStateOf(listOf<Book>()) }
    var booksLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Load books from Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("books")
            .get()
            .addOnSuccessListener { result ->
                books = result.documents.mapNotNull { doc ->
                    doc.toObject(Book::class.java)?.apply { id = doc.id }
                }
                booksLoaded = true
            }
            .addOnFailureListener { e ->
                messages = messages + "AI: Lỗi khi tải dữ liệu sách: ${e.message}"
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "AI Chat Assistant",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 8.dp),
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color
        )

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { message ->
                val isAI = message.startsWith("AI:")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 280.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAI) Color(0xFFE3F2FD) else Color(0xFF0077B6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(12.dp),
                            color = if (isAI) Color.Black else Color.White
                        )
                    }
                }
            }

            // Loading indicator
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang suy nghĩ...")
                            }
                        }
                    }
                }
            }
        }

        // Auto scroll to bottom
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input area
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
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (userInput.isNotBlank() && !isLoading && booksLoaded) {
                        val question = userInput.trim()
                        val newMessage = "Bạn: $question"
                        messages = messages + newMessage
                        userInput = ""
                        isLoading = true

                        scope.launch {
                            try {
                                val aiResponse = callGroqAPIWithBooks(question, books)
                                messages = messages + "AI: $aiResponse"
                            } catch (e: Exception) {
                                messages = messages + "AI: Xin lỗi, đã có lỗi: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = userInput.isNotBlank() && !isLoading && booksLoaded
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi",
                    tint = if (userInput.isNotBlank() && !isLoading && booksLoaded) Color(0xFF0077B6) else Color.Gray
                )
            }
        }
    }
}

suspend fun callGroqAPIWithBooks(prompt: String, books: List<Book>): String {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // LẤY API KEY MIỄN PHÍ TỪ: https://console.groq.com/keys
            // Groq cho phép 30 requests/phút miễn phí!
            val apiKey = BuildConfig.GROQ_API_KEY

            if (apiKey.isBlank()) {
                return@withContext """ 
                    HƯỚNG DẪN LẤY GROQ API KEY (MIỄN PHÍ):
                    1. Truy cập: https://console.groq.com/keys
                    2. Đăng ký tài khoản (Google/GitHub)
                    3. Click "Create API Key"
                    4. Copy key và thêm vào file `local.properties` như: GROQ_API_KEY=your_key
                """.trimIndent()
            }

            // Tạo context từ danh sách sách
            val booksContext = buildString {
                appendLine("Dưới đây là danh sách sách hiện có trong cửa hàng:")
                books.forEachIndexed { index, book ->
                    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    appendLine("\n${index + 1}. Tên: ${book.title}")
                    appendLine("   Tác giả: ${book.author}")
                    appendLine("   Thể loại: ${book.category}")
                    appendLine("   Mô tả: ${book.description}")
                    appendLine("   Giá: ${formatter.format(book.price)}")
                    appendLine("   Đánh giá: ${book.rating}/5.0")
                    appendLine("   Số lượng còn: ${book.quantity}")
                }
            }

            val systemPrompt = """
                Bạn là trợ lý AI của cửa hàng sách Bookstore.
                QUAN TRỌNG: Bạn chỉ được trả lời các câu hỏi liên quan đến:
                - Gợi ý sách trong cửa hàng
                - Thông tin chi tiết về sách (giá, tác giả, mô tả, đánh giá)
                - So sánh các cuốn sách trong danh sách
                - Tìm sách theo thể loại, tác giả, giá
                - Khuyến nghị sách phù hợp với sở thích người dùng
                
                Nếu người dùng hỏi về chủ đề KHÔNG liên quan đến sách trong cửa hàng, 
                hãy lịch sự từ chối và hướng dẫn họ hỏi về sách.
                
                $booksContext
                
                Hãy trả lời ngắn gọn, thân thiện và chính xác bằng tiếng Việt.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 800)
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            when {
                response.isSuccessful -> {
                    response.close()
                    val jsonResponse = JSONObject(responseBody)
                    val content = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    return@withContext content.trim()
                }
                response.code == 401 -> {
                    response.close()
                    return@withContext "API key không hợp lệ. Tạo key mới tại: https://console.groq.com/keys"
                }
                response.code == 429 -> {
                    response.close()
                    return@withContext "Vượt giới hạn 30 requests/phút. Vui lòng chờ 1 phút."
                }
                else -> {
                    response.close()
                    return@withContext "Lỗi ${response.code}: ${responseBody.take(100)}"
                }
            }

        } catch (e: Exception) {
            return@withContext "Lỗi kết nối: ${e.message}"
        }
    }
}