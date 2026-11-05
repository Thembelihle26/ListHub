package com.Group19.listhub.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Single place that talks to Firebase Auth + Firestore, so our Activities
 * don't have Firebase calls scattered everywhere. This replaces the Room
 * AppDatabase/DAO/Repository setup from the RoomDB version of this prototype.
 */
class FirestoreRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")
    private val tasksCollection = firestore.collection("tasks")
    private val focusCollection = firestore.collection("focusSessions")

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun logout() = auth.signOut()

    // ---------- Auth ----------

    /**
     * Registers a new user with Firebase Authentication (Firebase handles
     * hashing/encrypting the password for us - that's the whole point of using
     * Firebase Auth instead of rolling our own hashing like we did in the
     * RoomDB version) and creates a matching profile document in Firestore.
     */
    suspend fun register(fullName: String, email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw IllegalStateException("Registration succeeded but no user was returned")

        // give the FirebaseUser object a display name too, not just our Firestore copy
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()
        firebaseUser.updateProfile(profileUpdates).await()

        val profile = UserProfile(uid = firebaseUser.uid, fullName = fullName, email = email)
        usersCollection.document(firebaseUser.uid).set(profile).await()

        return firebaseUser
    }

    suspend fun login(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw IllegalStateException("Login succeeded but no user was returned")
    }

    /**
     * Used after a successful Google Sign-In - exchanges the Google ID token
     * for a Firebase credential, then signs in/creates the Firebase Auth user.
     */
    suspend fun signInWithGoogleCredential(idToken: String): FirebaseUser {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: throw IllegalStateException("Google sign-in succeeded but no user was returned")

        // if this is the first time this Google account has signed in, create their profile doc
        val existingProfile = usersCollection.document(firebaseUser.uid).get().await()
        if (!existingProfile.exists()) {
            val profile = UserProfile(
                uid = firebaseUser.uid,
                fullName = firebaseUser.displayName ?: "List Hub User",
                email = firebaseUser.email ?: ""
            )
            usersCollection.document(firebaseUser.uid).set(profile).await()
        }

        return firebaseUser
    }

    // ---------- User profile / preferences / karma ----------

    suspend fun getUserProfile(uid: String): UserProfile? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.toObject(UserProfile::class.java)?.apply { this.uid = snapshot.id }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        usersCollection.document(profile.uid).set(profile).await()
    }

    // ---------- Tasks ----------

    suspend fun getAllTasks(ownerId: String): List<Task> {
        val snapshot = tasksCollection.whereEqualTo("ownerId", ownerId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Task::class.java)?.apply { taskId = doc.id }
        }.sortedBy { it.dueDate ?: Long.MAX_VALUE }
    }

    suspend fun getTodayTasks(ownerId: String, startOfDay: Long, endOfDay: Long): List<Task> {
        return getAllTasks(ownerId).filter { it.dueDate != null && it.dueDate!! in startOfDay..endOfDay }
    }

    suspend fun getUpcomingTasks(ownerId: String, endOfDay: Long): List<Task> {
        return getAllTasks(ownerId).filter { it.dueDate != null && it.dueDate!! > endOfDay }
    }

    suspend fun getTaskById(taskId: String): Task? {
        val doc = tasksCollection.document(taskId).get().await()
        return doc.toObject(Task::class.java)?.apply { this.taskId = doc.id }
    }

    suspend fun insertTask(task: Task): String {
        // .add() lets Firestore auto-generate the document id for us
        val ref = tasksCollection.add(task).await()
        task.taskId = ref.id
        return ref.id
    }

    suspend fun updateTask(task: Task) {
        tasksCollection.document(task.taskId).set(task).await()
    }

    suspend fun deleteTask(task: Task) {
        tasksCollection.document(task.taskId).delete().await()
    }

    suspend fun recordFocusSession(ownerId: String, taskTitle: String, elapsedSeconds: Long, plannedMinutes: Int) {
        focusCollection.add(mapOf("ownerId" to ownerId, "taskTitle" to taskTitle, "elapsedSeconds" to elapsedSeconds, "plannedMinutes" to plannedMinutes, "completedAt" to System.currentTimeMillis())).await()
    }
}
