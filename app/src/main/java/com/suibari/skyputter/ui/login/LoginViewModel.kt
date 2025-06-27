package com.suibari.skyputter.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suibari.skyputter.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import work.socialhub.kbsky.BlueskyFactory
import work.socialhub.kbsky.api.entity.com.atproto.server.ServerCreateSessionRequest

class LoginViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun login(
        serviceHost: String,
        identifier: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = BlueskyFactory
                    .instance(serviceHost)
                    .server()
                    .createSession(ServerCreateSessionRequest().also {
                        it.identifier = identifier
                        it.password = password
                    })

                val accessJwt = response.data.accessJwt
                val refreshJwt = response.data.refreshJwt
                val did = response.data.did

                SessionManager.saveSession(accessJwt, refreshJwt, did, serviceHost)

                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "ログイン失敗：${e.message ?: "不明なエラー"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
