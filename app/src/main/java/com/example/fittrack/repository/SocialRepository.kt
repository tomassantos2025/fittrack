package com.example.fittrack.repository

import com.example.fittrack.model.ChatMessage
import com.example.fittrack.model.FriendRequest
import com.example.fittrack.model.Friendship
import com.example.fittrack.model.TrainingGroup
import com.example.fittrack.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Repositório da área social. Gere amigos, códigos de amizade, grupos e dados partilhados entre utilizadores.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class SocialRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val users = db.collection("users")
    private val requests = db.collection("friend_requests")
    private val friendships = db.collection("friendships")
    private val groups = db.collection("training_groups")

    private fun uid(): String = auth.currentUser?.uid.orEmpty()

    fun makeFriendCode(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray())
        return "FT-" + digest.joinToString("") { "%02X".format(it) }.take(10)
    }

    suspend fun ensureFriendCode(user: User): Result<User> {
        return try {
            if (user.friendCode.isNotBlank()) return Result.success(user)
            val code = makeFriendCode(user.uid)
            users.document(user.uid).set(mapOf("friend_code" to code), SetOptions.merge()).await()
            Result.success(user.copy(friendCode = code))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun findByFriendCode(code: String): Result<User> {
        return try {
            val normalized = code.trim().uppercase()
            val doc = users.whereEqualTo("friend_code", normalized).limit(1).get().await().documents.firstOrNull()
                ?: return Result.failure(Exception("Friend code not found"))
            val user = doc.toObject(User::class.java) ?: return Result.failure(Exception("Profile not found"))
            if (user.uid == uid()) return Result.failure(Exception("This is your own friend code"))
            Result.success(user)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sendFriendRequest(target: User): Result<Unit> {
        return try {
            val me = uid()
            if (me.isBlank()) return Result.failure(Exception("Not logged in"))
            if (areFriends(me, target.uid)) return Result.failure(Exception("Already friends"))
            val existing = requests.whereEqualTo("from_uid", me).whereEqualTo("to_uid", target.uid)
                .whereEqualTo("status", "pending").limit(1).get().await()
            if (!existing.isEmpty) return Result.failure(Exception("Request already sent"))
            val myName = users.document(me).get().await().getString("display_name").orEmpty()
            requests.add(FriendRequest(fromUid = me, fromName = myName, toUid = target.uid)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun observePendingIncomingRequestCount(onCount: (Int) -> Unit): ListenerRegistration? {
        val me = uid()
        if (me.isBlank()) return null
        return requests.whereEqualTo("to_uid", me).whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ -> onCount(snapshot?.size() ?: 0) }
    }

    suspend fun incomingRequests(): Result<List<FriendRequest>> {
        return try {
            val me = uid()
            if (me.isBlank()) return Result.failure(Exception("Not logged in"))
            Result.success(requests.whereEqualTo("to_uid", me).whereEqualTo("status", "pending").get().await()
                .toObjects(FriendRequest::class.java).sortedByDescending { it.createdAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun acceptRequest(request: FriendRequest): Result<Unit> {
        return try {
            val me = uid()
            if (me != request.toUid) return Result.failure(Exception("Not allowed"))
            val members = listOf(request.fromUid, request.toUid).sorted()
            db.runBatch { batch ->
                batch.set(friendships.document(members.joinToString("_")), Friendship(memberUids = members))
                batch.update(requests.document(request.id), "status", "accepted")
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun friends(): Result<List<User>> {
        return try {
            val me = uid()
            if (me.isBlank()) return Result.failure(Exception("Not logged in"))
            val links = friendships.whereArrayContains("member_uids", me).get().await().toObjects(Friendship::class.java)
            val ids = links.flatMap { it.memberUids }.filter { it != me }.distinct()
            Result.success(ids.mapNotNull { users.document(it).get().await().toObject(User::class.java) }
                .sortedBy { it.displayName.lowercase() })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun areFriends(a: String, b: String): Boolean = friendships.document(listOf(a, b).sorted().joinToString("_")).get().await().exists()

    suspend fun createGroup(name: String, friendUids: List<String>): Result<String> {
        return try {
            val me = uid()
            if (me.isBlank()) return Result.failure(Exception("Not logged in"))
            if (name.isBlank()) return Result.failure(Exception("Group name required"))
            val selected = friendUids.filter { it.isNotBlank() && it != me }.distinct()
            if (selected.isEmpty()) return Result.failure(Exception("Add at least one friend"))
            for (friendUid in selected) if (!areFriends(me, friendUid)) return Result.failure(Exception("Only current friends can be invited"))
            val ref = groups.document()
            ref.set(TrainingGroup(
                id = ref.id,
                name = name.trim(),
                ownerUid = me,
                memberUids = listOf(me) + selected,
                acceptedMemberUids = listOf(me),
                pendingMemberUids = selected
            )).await()
            Result.success(ref.id)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun myGroups(): Result<List<TrainingGroup>> {
        return try {
            val me = uid()
            if (me.isBlank()) return Result.failure(Exception("Not logged in"))
            Result.success(groups.whereArrayContains("member_uids", me).get().await().toObjects(TrainingGroup::class.java)
                .sortedByDescending { it.createdAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun group(groupId: String): Result<TrainingGroup> {
        return try {
            val item = groups.document(groupId).get().await().toObject(TrainingGroup::class.java)
                ?: return Result.failure(Exception("Group not found"))
            if (!item.memberUids.contains(uid())) return Result.failure(Exception("You are not a member of this group"))
            Result.success(item)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun acceptGroupInvitation(groupId: String): Result<Unit> {
        return try {
            val me = uid()
            groups.document(groupId).update(
                mapOf(
                    "pending_member_uids" to FieldValue.arrayRemove(me),
                    "accepted_member_uids" to FieldValue.arrayUnion(me)
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val me = uid()
            val current = group(groupId).getOrThrow()
            val remaining = current.memberUids.filter { it != me }
            if (remaining.isEmpty()) {
                groups.document(groupId).delete().await()
            } else {
                val fields = mutableMapOf<String, Any>(
                    "member_uids" to FieldValue.arrayRemove(me),
                    "accepted_member_uids" to FieldValue.arrayRemove(me),
                    "pending_member_uids" to FieldValue.arrayRemove(me)
                )
                if (current.ownerUid == me) fields["owner_uid"] = remaining.first()
                groups.document(groupId).update(fields).await()
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun groupMembers(groupId: String): Result<List<User>> {
        return try {
            val current = group(groupId).getOrThrow()
            Result.success(current.memberUids.mapNotNull { users.document(it).get().await().toObject(User::class.java) }
                .sortedBy { it.displayName.lowercase() })
        } catch (e: Exception) { Result.failure(e) }
    }

    fun observeGroupMessages(groupId: String, onResult: (Result<List<ChatMessage>>) -> Unit): ListenerRegistration =
        groups.document(groupId).collection("messages").orderBy("created_at", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) onResult(Result.failure(error))
                else onResult(Result.success(snap?.toObjects(ChatMessage::class.java).orEmpty().sortedBy { it.createdAt }))
            }

    suspend fun sendGroupMessage(groupId: String, text: String): Result<Unit> {
        return try {
            val me = uid()
            val current = group(groupId).getOrThrow()
            if (!current.acceptedMemberUids.ifEmpty { current.memberUids }.contains(me)) return Result.failure(Exception("Accept the group invitation first"))
            val user = users.document(me).get().await().toObject(User::class.java) ?: return Result.failure(Exception("Profile not found"))
            groups.document(groupId).collection("messages").add(ChatMessage(
                senderUid = me,
                senderName = user.displayName.ifBlank { user.username },
                text = text.trim(),
                readBy = listOf(me)
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markGroupMessagesRead(groupId: String, messageIds: List<String>, readerUid: String): Result<Unit> {
        return try {
            if (messageIds.isEmpty()) return Result.success(Unit)
            val current = group(groupId).getOrThrow()
            if (!current.memberUids.contains(readerUid)) return Result.failure(Exception("Not allowed"))
            val batch = db.batch()
            messageIds.forEach { id -> batch.update(groups.document(groupId).collection("messages").document(id), "read_by", FieldValue.arrayUnion(readerUid)) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
