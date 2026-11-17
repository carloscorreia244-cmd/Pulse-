package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 0 ORDER BY isOnline DESC, displayName ASC")
    fun observeContacts(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(users: List<UserEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTimeMillis DESC")
    fun observeAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun observeChat(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE peerUserId = :peerUserId LIMIT 1")
    suspend fun getChatByPeerId(peerUserId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: ChatEntity)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatRead(chatId: String)

    @Query("UPDATE chats SET isPinned = :pinned WHERE id = :chatId")
    suspend fun setPinned(chatId: String, pinned: Boolean)

    @Query("UPDATE chats SET isMuted = :muted WHERE id = :chatId")
    suspend fun setMuted(chatId: String, muted: Boolean)

    @Query("UPDATE chats SET keyVerifiedByUser = :verified WHERE id = :chatId")
    suspend fun setKeyVerified(chatId: String, verified: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMillis ASC")
    fun observeMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestampMillis ASC")
    suspend fun getMessagesForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestampMillis ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactions: String)

    @Query("UPDATE messages SET isDownloaded = 1 WHERE id = :messageId")
    suspend fun markDownloaded(messageId: String)

    @Query("UPDATE messages SET isDownloaded = 1 WHERE chatId = :chatId")
    suspend fun markAllDownloadedInChat(chatId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)
}

@Dao
interface LiveStreamDao {
    @Query("SELECT * FROM live_streams ORDER BY isLive DESC, viewerCount DESC")
    fun observeLiveStreams(): Flow<List<LiveStreamEntity>>

    @Query("SELECT * FROM live_streams WHERE id = :liveId LIMIT 1")
    fun observeLiveStream(liveId: String): Flow<LiveStreamEntity?>

    @Query("SELECT * FROM live_streams WHERE id = :liveId LIMIT 1")
    suspend fun getLiveStream(liveId: String): LiveStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(streams: List<LiveStreamEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stream: LiveStreamEntity)

    @Query("UPDATE live_streams SET isLive = 0 WHERE id = :liveId")
    suspend fun endLive(liveId: String)
}

@Dao
interface LiveCommentDao {
    @Query("SELECT * FROM live_comments WHERE liveId = :liveId ORDER BY timestampMillis ASC")
    fun observeComments(liveId: String): Flow<List<LiveCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: LiveCommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<LiveCommentEntity>)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_transactions ORDER BY timestampMillis DESC")
    fun observeTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: WalletTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<WalletTransactionEntity>)
}
