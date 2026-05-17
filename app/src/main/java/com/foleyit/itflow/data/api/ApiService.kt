package com.foleyit.itflow.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("auth")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @DELETE("auth")
    suspend fun logout()

    @POST("auth/fcm")
    suspend fun updateFcmToken(@Body body: Map<String, String>)

    // Dashboard
    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse

    // Tickets
    @GET("tickets")
    suspend fun getTickets(
        @Query("status") status: String = "open",
        @Query("mine") mine: Int = 0,
        @Query("search") search: String = "",
        @Query("page") page: Int = 1
    ): TicketsResponse

    @GET("tickets/{id}")
    suspend fun getTicket(@Path("id") id: Int): TicketDetail

    @POST("tickets/{id}/reply")
    suspend fun addReply(@Path("id") id: Int, @Body req: AddReplyRequest)

    @POST("tickets/{id}/time")
    suspend fun logTime(@Path("id") id: Int, @Body req: LogTimeRequest)

    // Clients
    @GET("clients")
    suspend fun getClients(
        @Query("search") search: String = "",
        @Query("page") page: Int = 1
    ): ClientsResponse

    @GET("clients/{id}")
    suspend fun getClient(@Path("id") id: Int): ClientDetail

    // Assets
    @GET("assets")
    suspend fun getAssets(
        @Query("search") search: String = "",
        @Query("page") page: Int = 1
    ): AssetsResponse

    @GET("assets/{id}")
    suspend fun getAsset(@Path("id") id: Int): AssetDetail

    // Credentials
    @GET("credentials")
    suspend fun getCredentials(
        @Query("search") search: String = "",
        @Query("page") page: Int = 1
    ): CredentialsResponse

    @GET("credentials/{id}")
    suspend fun getCredential(
        @Path("id") id: Int,
        @Header("X-Biometric") biometric: String = "1"
    ): CredentialDetail

    // Quotes
    @GET("quotes")
    suspend fun getQuotes(@Query("page") page: Int = 1): QuotesResponse

    @GET("quotes/{id}")
    suspend fun getQuote(@Path("id") id: Int): QuoteDetail

    // Invoices
    @GET("invoices")
    suspend fun getInvoices(@Query("page") page: Int = 1): InvoicesResponse

    @GET("invoices/{id}")
    suspend fun getInvoice(@Path("id") id: Int): InvoiceDetail

    // Expenses
    @GET("expenses")
    suspend fun getExpenses(@Query("page") page: Int = 1): ExpensesResponse

    @Multipart
    @POST("expenses")
    suspend fun createExpense(
        @Part("description") description: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("date") date: RequestBody,
        @Part("currency") currency: RequestBody,
        @Part("reference") reference: RequestBody,
        @Part("payment_method") paymentMethod: RequestBody,
        @Part receipt: MultipartBody.Part?
    )

    // Notifications
    @GET("notifications")
    suspend fun getNotifications(@Query("page") page: Int = 1): NotificationsResponse

    @POST("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Int)

    @POST("notifications/read-all")
    suspend fun markAllRead()
}

// Extension helpers
suspend fun ApiService.addReply(id: Int, reply: String, type: String = "reply", timeWorked: String? = null) =
    addReply(id, AddReplyRequest(reply, type, timeWorked))
