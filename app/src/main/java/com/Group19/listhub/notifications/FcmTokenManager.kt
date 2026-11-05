package com.Group19.listhub.notifications

import com.Group19.listhub.data.FirestoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmTokenManager {
    fun saveToken(token: String) {
        val repository = FirestoreRepository()
        val uid = repository.getCurrentUser()?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = repository.getUserProfile(uid) ?: return@launch
                profile.fcmToken = token
                repository.updateUserProfile(profile)
            } catch (_: Exception) {
            }
        }
    }
}
