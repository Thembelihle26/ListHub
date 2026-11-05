package com.Group19.listhub.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The actual Google Tasks API, as specified in section 5.4 of our Part 1
 * planning document. Every call needs an "Authorization: Bearer <token>"
 * header, which we build fresh in GoogleTasksClient using the OAuth token
 * from GoogleAuthHelper (see that class for why a plain API key on its own
 * isn't enough to read a specific user's private task lists).
 */
interface GoogleTasksApiService {

    @GET("users/@me/lists")
    suspend fun getTaskLists(@HeaderMap headers: Map<String, String>): GoogleTaskListResponse

    @GET("lists/{taskListId}/tasks")
    suspend fun getTasks(
        @Path("taskListId") taskListId: String,
        @HeaderMap headers: Map<String, String>
    ): GoogleTaskListTasksResponse

    @POST("lists/{taskListId}/tasks")
    suspend fun createTask(
        @Path("taskListId") taskListId: String,
        @Body task: GoogleTask,
        @HeaderMap headers: Map<String, String>
    ): GoogleTask

    @PUT("lists/{taskListId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String,
        @Body task: GoogleTask,
        @HeaderMap headers: Map<String, String>
    ): GoogleTask

    @DELETE("lists/{taskListId}/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("taskListId") taskListId: String,
        @Path("taskId") taskId: String,
        @HeaderMap headers: Map<String, String>
    )
}
