package com.example.fittrack.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Conjunto de modelos usados na parte social: amigos, pedidos, grupos e publicações relacionadas com treinos.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
data class FriendRequest(
    @DocumentId val id: String = "",
    @get:PropertyName("from_uid") @set:PropertyName("from_uid") var fromUid: String = "",
    @get:PropertyName("from_name") @set:PropertyName("from_name") var fromName: String = "",
    @get:PropertyName("to_uid") @set:PropertyName("to_uid") var toUid: String = "",
    var status: String = "pending",
    @ServerTimestamp @get:PropertyName("created_at") @set:PropertyName("created_at") var createdAt: Date? = null
)

data class Friendship(
    @DocumentId val id: String = "",
    @get:PropertyName("member_uids") @set:PropertyName("member_uids") var memberUids: List<String> = emptyList(),
    @ServerTimestamp @get:PropertyName("created_at") @set:PropertyName("created_at") var createdAt: Date? = null
)

data class TrainingGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    @get:PropertyName("owner_uid") @set:PropertyName("owner_uid") var ownerUid: String = "",
    @get:PropertyName("member_uids") @set:PropertyName("member_uids") var memberUids: List<String> = emptyList(),
    @get:PropertyName("accepted_member_uids") @set:PropertyName("accepted_member_uids") var acceptedMemberUids: List<String> = emptyList(),
    @get:PropertyName("pending_member_uids") @set:PropertyName("pending_member_uids") var pendingMemberUids: List<String> = emptyList(),
    @ServerTimestamp @get:PropertyName("created_at") @set:PropertyName("created_at") var createdAt: Date? = null
)
