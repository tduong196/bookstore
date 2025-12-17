package com.bookstore.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookstore.R
import com.bookstore.ui.home.HomeActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/* ===================== COLOR PALETTE ===================== */

private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val CardColor = Color(0xFFFDFCFB)
private val TitleWhite = Color(0xFFFDFCFB)

/* ===================== ACTIVITY ===================== */

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser != null) {
            checkUserRoleAndNavigate()
            return
        }

        setContent {
            LoginScreen(
                onLoginSuccess = { checkUserRoleAndNavigate() },
                onRegisterClick = { navigateToRegister() }
            )
        }
    }

    private fun checkUserRoleAndNavigate() {
        val currentUser = auth.currentUser
        currentUser?.email?.let { email ->
            db.collection("users").document(email)
                .get()
                .addOnSuccessListener {
                    val role = it.getLong("role")?.toInt() ?: 1
                    navigateToHome(role)
                }
                .addOnFailureListener {
                    navigateToHome(1)
                }
        }
    }

    private fun navigateToHome(role: Int) {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                putExtra("ROLE", role)
            }
        )
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}

/* ===================== UI SCREEN ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        /* ===== BACKGROUND IMAGE ===== */
        Image(
            painter = painterResource(id = R.drawable.nenlogin),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        /* ===== GREEN OVERLAY – HÒA NỀN ===== */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF2F4F3F).copy(alpha = 0.45f),
                            Color(0xFF1E352B).copy(alpha = 0.75f)
                        )
                    )
                )
        )

        /* ===== CONTENT ===== */
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /* ===== TITLE ===== */
            Text(
                text = "BookStore",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TitleWhite,
                letterSpacing = 1.sp
            )

            Text(
                text = "Đăng nhập để tiếp tục",
                fontSize = 15.sp,
                color = TitleWhite.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )

            /* ===== LOGIN CARD ===== */
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    /* EMAIL */
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GreenPrimary.copy(alpha = 0.4f),
                            focusedLabelColor = GreenPrimary,
                            cursorColor = GreenPrimary
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    /* PASSWORD */
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = GreenPrimary.copy(alpha = 0.4f),
                            focusedLabelColor = GreenPrimary,
                            cursorColor = GreenPrimary
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { showForgotDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Quên mật khẩu?",
                            color = GreenPrimary,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    /* LOGIN BUTTON */
                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                auth.signInWithEmailAndPassword(
                                    email.trim(),
                                    password
                                ).addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Đăng nhập thất bại",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "ĐĂNG NHẬP",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Chưa có tài khoản?")
                        TextButton(onClick = onRegisterClick) {
                            Text(
                                "Đăng ký",
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    /* ===== FORGOT PASSWORD ===== */
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Đặt lại mật khẩu") },
            text = {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    Firebase.auth.sendPasswordResetEmail(email.trim())
                    Toast.makeText(
                        context,
                        "Đã gửi email đặt lại mật khẩu",
                        Toast.LENGTH_LONG
                    ).show()
                    showForgotDialog = false
                }) {
                    Text("Gửi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
