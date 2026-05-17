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
        @Query("page") page: Int = 1,
        @Query("priority") priority: String = "",
        @Query("onsite") onsite: Int = -1
    ): TicketsResponse

    @GET("tickets/{id}")
    suspend fun getTicket(@Path("id") id: Int): TicketDetail

    @POST("tickets/{id}/reply")
    suspend fun addReply(@Path("id") id: Int, @Body req: AddReplyRequest)

    
    @POST("tickets/{id}/status")
    suspend fun updateTicketStatus(@Path("id") id: Int, @Body body: Map<String, Int>)

    @GET("statuses")
    suspend fun getTicketStatuses(): List<TicketStatus>

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

    @GET("tickets/{id}/charges")
    suspend fun getTicketCharges(@Path("id") id: Int): ChargesResponse

    @GET("tickets/{id}/worksheets")
    suspend fun getTicketWorksheets(@Path("id") id: Int): List<WorksheetSummary>

    @GET("tickets/{id}/outtakes")
    suspend fun getTicketOuttakes(@Path("id") id: Int): List<OuttakeSummary>

    @GET("outtakes/{id}")
    suspend fun getOuttake(@Path("id") id: Int): OuttakeDetail

    @POST("outtakes/{id}/sign")
    suspend fun signOuttake(@Path("id") id: Int, @Body body: SignRequest)

    @DELETE("outtakes/{id}")
    suspend fun deleteOuttake(@Path("id") id: Int)

    @DELETE("worksheets/{id}")
    suspend fun deleteWorksheet(@Path("id") id: Int)

    @GET("worksheets/{id}")
    suspend fun getWorksheet(@Path("id") id: Int): WorksheetDetail

    @POST("worksheets/{id}/sign")
    suspend fun signWorksheet(@Path("id") id: Int, @Body body: SignRequest)

    @GET("worksheet-templates")
    suspend fun getWorksheetTemplates(): List<WorksheetTemplate>


    @GET("products")
    suspend fun getProducts(@Query("search") search: String = ""): List<Product>

    @POST("tickets/{id}/outtake")
    suspend fun createOuttake(@Path("id") id: Int, @Body body: CreateWorksheetRequest): Map<String, Int>


    @GET("me")
    suspend fun getProfile(): UserProfile

    @PUT("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest)

    @POST("tickets/{id}/charges")
    suspend fun addCharge(@Path("id") id: Int, @Body body: AddChargeRequest)

    @POST("tickets/{id}/worksheets")
    suspend fun createWorksheet(@Path("id") id: Int, @Body body: CreateWorksheetRequest): Map<String, Int>

    @POST("worksheets/{id}/responses")
    suspend fun saveResponses(@Path("id") id: Int, @Body body: SaveResponsesRequest)


    @GET("appointments")
    suspend fun getAppointments(
        @Query("when") when_: String = "future",
        @Query("mine") mine: Int = 0
    ): List<Appointment>

    @GET("clients/{id}/tickets")
    suspend fun getClientTickets(@Path("id") id: Int): List<TicketSummary>

    @GET("clients/{id}/assets")
    suspend fun getClientAssets(@Path("id") id: Int): List<AssetSummary>

    @GET("clients/{id}/locations")
    suspend fun getClientLocations(@Path("id") id: Int): List<ClientLocation>

    @GET("clients/{id}/credentials")
    suspend fun getClientCredentials(@Path("id") id: Int): List<CredentialSummary>

    @GET("clients/{id}/contracts")
    suspend fun getClientContracts(@Path("id") id: Int): List<ClientContract>


    @POST("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Int)

    @POST("notifications/read-all")
    suspend fun markAllRead()
}

// Extension helpers
suspend fun ApiService.addReply(id: Int, reply: String, type: String = "reply", timeWorked: String? = null, onsite: Boolean = false) =
    addReply(id, AddReplyRequest(reply, type, timeWorked, if (onsite) 1 else 0))


// ── Appointments ─────────────────────────────────────────────────────────────
data class Appointment(
    val id: Int, val number: Int, val subject: String,
    val schedule: String?, @com.google.gson.annotations.SerializedName("schedule_end") val scheduleEnd: String?,
    val onsite: Boolean, val notes: String?,
    val priority: String?, val status: String?,
    @com.google.gson.annotations.SerializedName("status_color") val statusColor: String?,
    val client: String?, @com.google.gson.annotations.SerializedName("assigned_to") val assignedTo: String?
)
