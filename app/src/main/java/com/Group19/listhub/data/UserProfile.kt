package com.Group19.listhub.data

import com.google.firebase.firestore.Exclude

/**
 * Extra profile data that Firebase Auth doesn't store for us (karma points,
 * level, and the Settings screen preferences). Firebase Auth only really
 * gives us uid/email/displayName, so we keep a matching document in the
 * "users" collection in Firestore, keyed by the Firebase Auth uid.
 */
data class UserProfile(
    @get:Exclude @set:Exclude var uid: String = "",

    var fullName: String = "",
    var email: String = "",

    var karmaPoints: Int = 0,
    var level: Int = 1,

    var themeDark: Boolean = false,
    var notificationsEnabled: Boolean = true,
    var languageCode: String = "en", // en / af / zu
    var fcmToken: String? = null
)
