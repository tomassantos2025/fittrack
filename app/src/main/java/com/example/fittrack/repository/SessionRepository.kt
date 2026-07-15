package com.example.fittrack.repository

import com.example.fittrack.model.Session
import com.example.fittrack.model.SetLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Repositório das sessões de treino. Guarda e lê o histórico privado de treinos de cada utilizador.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SessionRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private fun sessionsRef() =
        db.collection("users").document(auth.currentUser?.uid ?: "").collection("sessions")

    suspend fun saveSession(session: Session, setLogs: List<SetLog>): Result<String> {
        return try {
            val docRef = sessionsRef().document()
            val completedAt = session.completedAt ?: Date()
            val sessionWithId = session.copy(id = docRef.id, completedAt = completedAt)
            docRef.set(sessionWithId).await()

            // Save set logs as sub-collection
            for (log in setLogs) {
                docRef.collection("set_logs").add(log).await()
            }

            val uid = auth.currentUser?.uid.orEmpty()
            if (uid.isNotBlank()) {
                val allSessions = sessionsRef().get().await().toObjects(Session::class.java)
                db.collection("users").document(uid).set(
                    mapOf(
                        "total_sessions" to allSessions.size,
                        "last_workout_date" to completedAt
                    ), com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentSessions(limit: Int = 10): Result<List<Session>> {
        return try {
            val snapshot = sessionsRef()
                .orderBy("completed_at", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            val sessions = snapshot.toObjects(Session::class.java)
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSessionsSince(daysAgo: Int): Result<List<Session>> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val since = calendar.time

            val snapshot = sessionsRef()
                .whereGreaterThanOrEqualTo("completed_at", since)
                .orderBy("completed_at", Query.Direction.DESCENDING)
                .get()
                .await()
            val sessions = snapshot.toObjects(Session::class.java)
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSessions(): Result<List<Session>> {
        return try {
            val snapshot = sessionsRef()
                .orderBy("completed_at", Query.Direction.DESCENDING)
                .get()
                .await()
            val sessions = snapshot.toObjects(Session::class.java)
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSession(sessionId: String): Result<Session> {
        return try {
            val snapshot = sessionsRef().document(sessionId).get().await()
            val session = snapshot.toObject(Session::class.java)
                ?: return Result.failure(Exception("Session not found"))
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSetLogs(sessionId: String): Result<List<SetLog>> {
        return try {
            val snapshot = sessionsRef().document(sessionId)
                .collection("set_logs")
                .get()
                .await()
            val logs = snapshot.toObjects(SetLog::class.java)
                .sortedWith(compareBy<SetLog> { it.exerciseOrder }.thenBy { it.setNumber })
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeeklyStats(): Result<WeeklyStats> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.time

            val snapshot = sessionsRef().get().await()
            val allSessions = snapshot.toObjects(Session::class.java)
            val sessions = allSessions.filter { (it.completedAt ?: Date(0)).time >= weekStart.time }

            val totalSessions = sessions.size
            val totalVolume = sessions.sumOf { it.totalVolume }
            val activeDays = sessions.mapNotNull { it.completedAt }
                .map { date ->
                    val cal = Calendar.getInstance()
                    cal.time = date
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                }
                .distinct()
                .size
            val streak = calculateStreak(allSessions)

            Result.success(WeeklyStats(totalSessions, totalVolume, activeDays, streak))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateStreak(sessions: List<Session>): Int {
        val workoutDays = sessions.mapNotNull { it.completedAt }.map { date ->
            Calendar.getInstance().apply { time = date; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        }.toSet()
        if (workoutDays.isEmpty()) return 0

        val cursor = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        var streak = 0
        if (!workoutDays.contains(cursor.timeInMillis)) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (workoutDays.contains(cursor.timeInMillis)) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    data class WeeklyStats(
        val totalSessions: Int,
        val totalVolume: Double,
        val activeDays: Int,
        val currentStreak: Int
    )
}
