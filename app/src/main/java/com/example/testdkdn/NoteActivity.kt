package com.example.testdkdn

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NoteActivity : ComponentActivity() {
    private val database = FirebaseDatabase.getInstance().getReference("notes")
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NoteScreen()
        }
    }

    @Composable
    fun NoteScreen() {
        var notes by remember { mutableStateOf(listOf<Note>()) }
        val auth = FirebaseAuth.getInstance()

        LaunchedEffect(Unit) {
            loadNotes { notes = it }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        startActivity(Intent(this@NoteActivity, NoteEditorActivity::class.java))
                    },
                    modifier = Modifier.padding(bottom = 64.dp)
                ) {
                    Text("+")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Danh sách Ghi Chú",
                    style = MaterialTheme.typography.headlineMedium
                )

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(notes) { note ->
                        NoteItem(note, onSelect = {
                            val intent = Intent(this@NoteActivity, NoteEditorActivity::class.java)
                            intent.putExtra("title", it.title)
                            intent.putExtra("description", it.description)
                            startActivity(intent)
                        })
                    }
                }

                Button(
                    onClick = {
                        auth.signOut()
                        startActivity(Intent(this@NoteActivity, MainActivity::class.java))
                        finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text("Đăng xuất", color = Color.White)
                }
            }
        }
    }

    fun loadNotes(callback: (List<Note>) -> Unit) {
        database.child(userId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notesList = mutableListOf<Note>()
                for (noteSnapshot in snapshot.children) {
                    val title = noteSnapshot.key ?: continue
                    val description = noteSnapshot.child("description").getValue(String::class.java) ?: ""
                    notesList.add(Note(title, description))
                }
                callback(notesList)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NoteActivity, "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @Composable
    fun NoteItem(note: Note, onSelect: (Note) -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onSelect(note) },
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "📌 ${note.title}", style = MaterialTheme.typography.titleLarge)
                Text(text = note.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

