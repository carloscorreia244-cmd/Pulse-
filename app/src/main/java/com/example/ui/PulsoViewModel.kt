package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatEntity
import com.example.data.local.LiveCommentEntity
import com.example.data.local.LiveStreamEntity
import com.example.data.local.UserEntity
import com.example.data.local.VerificationTier
import com.example.data.local.WalletTransactionEntity
import com.example.data.repository.PulsoRepository
import com.example.model.DecryptedMessage
import com.example.model.LiveGiftOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PulsoViewModel(private val repository: PulsoRepository) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repository.observeCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val contacts: StateFlow<List<UserEntity>> = repository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chats: StateFlow<List<ChatEntity>> = repository.observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val liveStreams: StateFlow<List<LiveStreamEntity>> = repository.observeLiveStreams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val transactions: StateFlow<List<WalletTransactionEntity>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages = _toastMessages.asSharedFlow()

    fun observeChat(chatId: String): Flow<ChatEntity?> = repository.observeChat(chatId)

    fun observeMessages(chatId: String): Flow<List<DecryptedMessage>> =
        repository.observeDecryptedMessages(chatId)

    fun observeLiveStream(liveId: String): Flow<LiveStreamEntity?> =
        repository.observeLiveStream(liveId)

    fun observeLiveComments(liveId: String): Flow<List<LiveCommentEntity>> =
        repository.observeLiveComments(liveId)

    fun markChatRead(chatId: String) {
        viewModelScope.launch { repository.markChatRead(chatId) }
    }

    fun togglePinChat(chatId: String, pinned: Boolean) {
        viewModelScope.launch { repository.togglePinChat(chatId, pinned) }
    }

    fun toggleMuteChat(chatId: String, muted: Boolean) {
        viewModelScope.launch { repository.toggleMuteChat(chatId, muted) }
    }

    fun setChatKeyVerified(chatId: String, verified: Boolean) {
        viewModelScope.launch {
            repository.setChatKeyVerified(chatId, verified)
            _toastMessages.tryEmit(
                if (verified) "Chave de segurança verificada!" else "Verificação removida."
            )
        }
    }

    fun sendTextMessage(chatId: String, text: String, replyToPreview: String? = null) {
        if (text.isBlank()) {
            _toastMessages.tryEmit("Digite uma mensagem antes de enviar.")
            return
        }
        viewModelScope.launch {
            repository.sendTextMessage(chatId, text, replyToPreview)
        }
    }

    fun sendImageMessage(chatId: String, uriString: String, caption: String = "") {
        viewModelScope.launch {
            repository.sendImageMessage(chatId, uriString, caption)
        }
    }

    fun sendAudioMessage(chatId: String, audioUri: String?, durationSec: Int) {
        viewModelScope.launch {
            repository.sendAudioMessage(chatId, audioUri, durationSec)
        }
    }

    fun toggleMessageReaction(messageId: String, currentReactions: List<String>, emoji: String) {
        viewModelScope.launch {
            repository.toggleMessageReaction(messageId, currentReactions, emoji)
        }
    }

    fun markMessageDownloaded(messageId: String) {
        viewModelScope.launch {
            repository.markMessageDownloaded(messageId)
            _toastMessages.tryEmit("Mídia salva localmente.")
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            _toastMessages.tryEmit("Mensagem removida.")
        }
    }

    fun openOrCreateChat(user: UserEntity, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository.startOrGetChatWithUser(user)
            onChatReady(id)
        }
    }

    fun createNewContactAndChat(name: String, handle: String, onChatReady: (String) -> Unit) {
        if (name.isBlank() || handle.isBlank()) {
            _toastMessages.tryEmit("Preencha o nome e o @usuário.")
            return
        }
        viewModelScope.launch {
            val id = repository.createContactAndChat(name, handle)
            onChatReady(id)
        }
    }

    fun sendPeerDonation(peerUserId: String, amountCents: Long, note: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.sendPeerDonation(peerUserId, amountCents, note)
            res.onSuccess {
                _toastMessages.tryEmit("Doação enviada com sucesso!")
                onSuccess()
            }.onFailure { err ->
                _toastMessages.tryEmit(err.message ?: "Não foi possível enviar a doação.")
            }
        }
    }

    fun addTopUp(amountCents: Long) {
        viewModelScope.launch {
            repository.addWalletTopUp(amountCents)
                .onSuccess { _toastMessages.tryEmit("Saldo adicionado via PIX!") }
                .onFailure { _toastMessages.tryEmit(it.message ?: "Erro na recarga.") }
        }
    }

    fun withdrawToPix(amountCents: Long, pixKey: String) {
        viewModelScope.launch {
            repository.withdrawToPix(amountCents, pixKey)
                .onSuccess { _toastMessages.tryEmit("Saque PIX solicitado!") }
                .onFailure { _toastMessages.tryEmit(it.message ?: "Erro ao sacar.") }
        }
    }

    fun sendLiveComment(liveId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.sendLiveComment(liveId, text) }
    }

    fun sendLiveGift(liveId: String, gift: LiveGiftOption, message: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.sendLiveGift(liveId, gift, message)
                .onSuccess {
                    _toastMessages.tryEmit("${gift.emoji} ${gift.label} enviado ao criador!")
                    onSuccess()
                }
                .onFailure { err ->
                    _toastMessages.tryEmit(err.message ?: "Erro ao enviar presente.")
                }
        }
    }

    fun subscribeToLiveCreator(liveId: String) {
        viewModelScope.launch {
            repository.subscribeToLiveCreator(liveId)
                .onSuccess { _toastMessages.tryEmit("Bem-vindo ao Clube de Apoiadores!") }
                .onFailure { _toastMessages.tryEmit(it.message ?: "Erro ao assinar.") }
        }
    }

    fun startMyLive(
        title: String,
        category: String,
        goalTitle: String,
        goalTargetCents: Long,
        supportersOnly: Boolean,
        onStarted: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.startMyLive(title, category, goalTitle, goalTargetCents, supportersOnly)
            onStarted(id)
        }
    }

    fun endMyLive(liveId: String) {
        viewModelScope.launch {
            repository.endMyLive(liveId)
            _toastMessages.tryEmit("Live encerrada e receita creditada na carteira!")
        }
    }

    fun updateProfileAndBadge(name: String, bio: String, tier: VerificationTier) {
        viewModelScope.launch {
            repository.updateProfileAndBadge(name, bio, tier)
            _toastMessages.tryEmit("Perfil e selo de verificação atualizados!")
        }
    }

    fun rotateMasterKeys() {
        viewModelScope.launch {
            repository.rotateMasterIdentityKeys()
            _toastMessages.tryEmit("Novo par de chaves ECDH P-256 gerado!")
        }
    }

    suspend fun buildExportTranscript(chatId: String?, includeCiphertext: Boolean): String {
        return repository.buildChatExportText(chatId, includeCiphertext)
    }

    fun notifyMessage(msg: String) {
        _toastMessages.tryEmit(msg)
    }

    class Factory(private val repository: PulsoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PulsoViewModel(repository) as T
        }
    }
}
