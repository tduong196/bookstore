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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/* ===================== COLOR PALETTE ===================== */

private val GreenPrimary = Color(0xFF5B7F6A)
private val GreenDark = Color(0xFF3F5D4A)
private val CardColor = Color(0xFFFDFCFB)
private val TitleWhite = Color(0xFFFDFCFB)

/* ===================== ACTIVITY ===================== */

class RegisterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RegisterScreen(
                onRegisterSuccess = {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                },
                onBackToLogin = {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            )
        }
    }
}

/* ===================== UI SCREEN ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    fun validatePassword(pwd: String): String =
        if (pwd.length < 6) "Mật khẩu phải có ít nhất 6 ký tự" else ""

    Box(modifier = Modifier.fillMaxSize()) {

        /* ===== BACKGROUND IMAGE ===== */
        Image(
            painter = painterResource(id = R.drawable.nenlogin),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        /* ===== GREEN OVERLAY – ĐỒNG BỘ LOGIN ===== */
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
                text = "Tạo tài khoản",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TitleWhite,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Đăng ký để bắt đầu",
                fontSize = 15.sp,
                color = TitleWhite.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 26.dp)
            )

            /* ===== REGISTER CARD ===== */
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

                    /* NAME */
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(14.dp))

                    /* EMAIL */
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(14.dp))

                    /* PASSWORD */
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = validatePassword(it)
                        },
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Ẩn" else "Hiện", color = GreenPrimary)
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = passwordError.isNotEmpty(),
                        supportingText = {
                            if (passwordError.isNotEmpty()) {
                                Text(passwordError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(14.dp))

                    /* CONFIRM PASSWORD */
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Nhập lại mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            TextButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Text(if (confirmPasswordVisible) "Ẩn" else "Hiện", color = GreenPrimary)
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            androidx.compose.ui.text.input.VisualTransformation.None
                        else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = textFieldColors()
                    )

                    Spacer(Modifier.height(24.dp))

                    /* REGISTER BUTTON */
                    Button(
                        onClick = {
                            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (passwordError.isNotEmpty()) {
                                Toast.makeText(context, passwordError, Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userData = hashMapOf(
                                            "email" to email,
                                            "name" to name,
                                            "role" to 1
                                        )
                                        db.collection("users").document(email)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                showSuccessDialog = true
                                            }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Đăng ký thất bại", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
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
                                "ĐĂNG KÝ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Đã có tài khoản?")
                        TextButton(onClick = onBackToLogin) {
                            Text("Đăng nhập", fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                }
            }
        }
    }

    /* ===== SUCCESS DIALOG ===== */
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = onRegisterSuccess) {
                    Text("Đăng nhập ngay")
                }
            },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Đăng ký thành công") },
            text = { Text("Tài khoản của bạn đã được tạo.") }
        )
    }
}

/* ===================== COMMON TEXTFIELD COLORS ===================== */

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,

    focusedIndicatorColor = GreenPrimary,
    unfocusedIndicatorColor = GreenPrimary.copy(alpha = 0.4f),

    focusedLabelColor = GreenPrimary,
    cursorColor = GreenPrimary
)

