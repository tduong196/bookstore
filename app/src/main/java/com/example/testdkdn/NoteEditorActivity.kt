package com.example.testdkdn

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NoteEditorActivity : ComponentActivity() {
    private val database = FirebaseDatabase.getInstance().getReference("notes")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Biến lưu URI của ảnh
    private var selectedImageUri by mutableStateOf<Uri?>(null)

    // Launcher chọn ảnh từ thư viện
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it // Lưu URI ảnh thay vì cố lấy đường dẫn file
                Toast.makeText(this, "Ảnh đã chọn", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTitle = intent.getStringExtra("title") ?: ""
        val initialDescription = intent.getStringExtra("description") ?: ""

        setContent {
            NoteEditorScreen(initialTitle, initialDescription)
        }
    }

    @Composable
    fun NoteEditorScreen(titleInitial: String, descriptionInitial: String) {
        var title by remember { mutableStateOf(titleInitial) }
        var description by remember { mutableStateOf(descriptionInitial) }
        var imageUrl by remember { mutableStateOf<String?>(null) } // Đường dẫn ảnh lưu trong Firebase

        // Lấy dữ liệu từ Firebase khi mở ghi chú
        val context = LocalContext.current
        LaunchedEffect(titleInitial) {
            if (titleInitial.isNotEmpty()) {
                database.child(userId!!).child(titleInitial).child("file")
                    .get().addOnSuccessListener { snapshot ->
                        imageUrl = snapshot.getValue(String::class.java) ?: ""
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Lỗi tải ảnh: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (titleInitial.isEmpty()) "Thêm Ghi Chú" else "Chỉnh Sửa Ghi Chú",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 32.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tiêu đề") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Nội dung") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        saveOrUpdateNote(titleInitial, title, description, selectedImageUri?.toString() ?: imageUrl)
                        finish()
                    }
                ) {
                    Text(if (titleInitial.isEmpty()) "Thêm" else "Lưu thay đổi")
                }

                Button(
                    onClick = { imagePickerLauncher.launch("image/*") }
                ) {
                    Text("Chọn ảnh")
                }
            }

            // Hiển thị ảnh đã chọn hoặc ảnh từ Firebase
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Ảnh đã chọn",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else if (!imageUrl.isNullOrEmpty() && imageUrl != "a") {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Ảnh đã lưu",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            if (titleInitial.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        deleteNote(title)
                        finish()
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            }
        }
    }


    fun saveOrUpdateNote(oldTitle: String, newTitle: String, description: String, imageUri: String?) {
        if (newTitle.isNotBlank()) {
            val note = mapOf(
                "title" to newTitle,
                "description" to description,
                "file" to (imageUri ?: "a") // Nếu không có ảnh thì giữ nguyên
            )

            database.child(userId!!).child(newTitle).setValue(note)
                .addOnSuccessListener {
                    if (oldTitle.isNotEmpty() && oldTitle != newTitle) {
                        deleteNote(oldTitle)
                    }
                    Toast.makeText(this, "Lưu thành công", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    fun deleteNote(title: String) {
        database.child(userId!!).child(title).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
