package com.Group19.listhub.network

// matches the shape of Google Tasks API resources
// https://developers.google.com/tasks/reference/rest/v1/tasks

data class GoogleTaskList(
    val id: String,
    val title: String
)

data class GoogleTaskListResponse(
    val items: List<GoogleTaskList>?
)

data class GoogleTask(
    val id: String? = null,
    val title: String,
    val notes: String? = null,
    val due: String? = null,       // RFC3339 timestamp
    val status: String? = null     // "needsAction" or "completed"
)

data class GoogleTaskListTasksResponse(
    val items: List<GoogleTask>?
)
