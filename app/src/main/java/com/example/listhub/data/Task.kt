package com.example.listhub.data

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

const val PRIORITY_LOW = 1
const val PRIORITY_MEDIUM = 2
const val PRIORITY_HIGH = 3

/**
 * Firestore version of our task model. We swapped this over from a Room @Entity
 * to a plain POJO since Firestore maps documents to objects using reflection
 * (it needs a no-arg constructor, which we get for free here because every
 * property has a default value).
 *
 * taskId is marked @Exclude because it's not actually a field inside the
 * document - it's the Firestore document ID itself, which we set manually
 * after reading/writing (see FirestoreRepository).
 */
data class Task(
    @get:Exclude @set:Exclude var taskId: String = "",

    @get:PropertyName("ownerId") @set:PropertyName("ownerId")
    var ownerId: String = "",

    var title: String = "",
    var description: String? = null,

    var dueDate: Long? = null,
    var priority: Int = PRIORITY_MEDIUM,
    var categoryId: String? = null,

    var isCompleted: Boolean = false
)
