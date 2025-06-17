package com.example.skyposter.data.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skyposter.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionViewModel : ViewModel() {
    var hasSession by mutableStateOf<Boolean?>(null)
        private set

    var myDid by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val session = SessionManager.getSession()
            withContext(Dispatchers.Main) {
                if (session != null) {
                    hasSession = true
                    myDid = session.did
                } else {
                    hasSession = false
                }
            }
        }
    }
}
