package com.example.testdkdn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testdkdn.ui.theme.TestDKDNTheme
import com.google.firebase.firestore.FirebaseFirestore

class QuanLyNguoiDung : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestDKDNTheme {
                QuanLyNguoiDungScreen {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuanLyNguoiDungScreen(onBack: () -> Unit) {
    var danhSachNguoiDung by remember { mutableStateOf<List<NguoiDung>>(emptyList()) }
    var userToDelete by remember { mutableStateOf<NguoiDung?>(null) }
    val db = FirebaseFirestore.getInstance()

    fun loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val nguoiDungList = result.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val email = doc.getString("email") ?: return@mapNotNull null
                    val role = doc.getLong("role")?.toInt() ?: return@mapNotNull null
                    NguoiDung(
                        id = doc.id,
                        name = name,
                        email = email,
                        role = role
                    )
                }.sortedBy { it.role } // Sắp xếp: User (1) lên trước Admin (2)
                danhSachNguoiDung = nguoiDungList
            }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quản lý Người Dùng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0077B6),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column( modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp)) {
            LazyColumn {
                items(danhSachNguoiDung) { nguoiDung ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = nguoiDung.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(text = nguoiDung.email, fontSize = 14.sp, color = Color.Gray)
                                }
                                Text(
                                    text = if (nguoiDung.role == 2) "Admin" else "User",
                                    color = if (nguoiDung.role == 2) Color(0xFF1565C0) else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Chỉ xóa nếu không phải admin
                                if (nguoiDung.role != 2) {
                                    Button(
                                        onClick = { userToDelete = nguoiDung },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Text("Xóa", color = Color.White)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(100.dp))
                                }

                                // Cấp quyền admin nếu chưa là admin
                                if (nguoiDung.role != 2) {
                                    Button(
                                        onClick = {
                                            db.collection("users")
                                                .document(nguoiDung.id)
                                                .update("role", 2)
                                                .addOnSuccessListener { loadUsers() }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                                    ) {
                                        Text("Cấp quyền admin", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog xác nhận xóa
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa người dùng này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        db.collection("users")
                            .document(userToDelete!!.id)
                            .delete()
                            .addOnSuccessListener {
                                userToDelete = null
                                loadUsers()
                            }
                    }
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                Button(onClick = { userToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

data class NguoiDung(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: Int = 0
)
