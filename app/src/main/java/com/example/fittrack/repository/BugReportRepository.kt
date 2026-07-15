package com.example.fittrack.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repositório usado para enviar feedback/erros para o Firebase, permitindo recolher problemas encontrados pelos utilizadores.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class BugReportRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun submit(title: String, description: String, screenshot: Uri?): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            var screenshotPath = ""
            if (screenshot != null) {
                val ref = storage.reference.child("bug_reports/$uid/${UUID.randomUUID()}.jpg")
                ref.putFile(screenshot).await()
                screenshotPath = ref.path
            }
            db.collection("bug_reports").add(
                mapOf(
                    "reporter_uid" to uid,
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "screenshot_path" to screenshotPath,
                    "status" to "open",
                    "created_at" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
