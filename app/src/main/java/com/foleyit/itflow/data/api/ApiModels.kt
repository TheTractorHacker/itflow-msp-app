package com.foleyit.itflow.data.api

import com.google.gson.annotations.SerializedName

// ── Auth ────────────────────────────────────────────────────────────────────
data class LoginRequest(val username: String, val password: String, val device_name: String, val totp_code: String? = null)
data class LoginResponse(
    val token: String? = null,
    val user: UserInfo? = null,
    @SerializedName("requires_2fa") val requires2fa: Boolean? = null
)
data class UserInfo(val id: Int, val name: String, val email: String, val type: Int)

// ── Dashboard ────────────────────────────────────────────────────────────────
data class DashboardResponse(
    @SerializedName("my_open")  val myOpen: Int,
    @SerializedName("all_open") val allOpen: Int,
    val overdue: Int,
    val unread: Int,
    val queue: List<TicketSummary>
)

// ── Tickets ──────────────────────────────────────────────────────────────────
data class TicketsResponse(val data: List<TicketSummary>, val total: Int)

data class TicketSummary(
    val id: Int,
    val number: Int,
    val subject: String,
    val priority: String?,
    val status: String?,
    @SerializedName("status_color") val statusColor: String?,
    val client: String?,
    @SerializedName("assigned_to") val assignedTo: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("due_at") val dueAt: String?,
    @SerializedName("resolved_at") val resolvedAt: String?
)

data class TicketDetail(
    val id: Int, val number: Int, val subject: String, val details: String?,
    val priority: String?, val status: String?,
    @SerializedName("status_color") val statusColor: String?,
    val client: String?,
    @SerializedName("assigned_to") val assignedTo: String?,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("contact_email") val contactEmail: String?,
    @SerializedName("contact_phone") val contactPhone: String?,
    val billable: Boolean,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("due_at") val dueAt: String?,
    @SerializedName("resolved_at") val resolvedAt: String?,
    val replies: List<TicketReply>
)

data class TicketReply(
    val id: Int, val body: String, val type: String,
    @SerializedName("time_worked") val timeWorked: String?,
    val onsite: Boolean?,
    val by: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class AddReplyRequest(val reply: String, val type: String, val time_worked: String?, val onsite: Int = 0)
data class TicketStatus(val id: Int, val name: String, val color: String)

data class LogTimeRequest(val time_worked: String, val note: String)

// ── Clients ──────────────────────────────────────────────────────────────────
data class ClientsResponse(val data: List<ClientSummary>, val total: Int)

data class ClientSummary(
    val id: Int, val name: String, val phone: String?,
    val city: String?, val state: String?, val website: String?
)

data class ClientDetail(
    val id: Int, val name: String, val phone: String?, val address: String?,
    val city: String?, val state: String?, val zip: String?, val website: String?,
    val notes: String?,
    @SerializedName("open_tickets") val openTickets: Int,
    val contacts: List<Contact>
)

data class Contact(
    val id: Int, val name: String, val title: String?,
    val email: String?, val phone: String?, val extension: String?,
    val client: String?
)

// ── Assets ───────────────────────────────────────────────────────────────────
data class AssetsResponse(val data: List<AssetSummary>, val total: Int)

data class AssetSummary(
    val id: Int, val name: String, val type: String?,
    val make: String?, val model: String?, val serial: String?,
    val os: String?, val status: String?, val client: String?
)

data class AssetDetail(
    val id: Int, val name: String, val type: String?,
    val make: String?, val model: String?, val serial: String?,
    val os: String?, val status: String?, val description: String?,
    @SerializedName("physical_location") val physicalLocation: String?,
    @SerializedName("location_name") val locationName: String?,
    @SerializedName("location_city") val locationCity: String?,
    @SerializedName("location_state") val locationState: String?,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("contact_phone") val contactPhone: String?,
    val client: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("purchase_date") val purchaseDate: String?,
    @SerializedName("warranty_expire") val warrantyExpire: String?,
    val notes: String?
)

// ── Credentials ──────────────────────────────────────────────────────────────
data class CredentialsResponse(val data: List<CredentialSummary>, val total: Int)

data class CredentialSummary(
    val id: Int, val name: String,
    val uri: String?, val client: String?
)

data class CredentialDetail(
    val id: Int, val name: String, val username: String?, val password: String?,
    val uri: String?, val uri2: String?, val otpSecret: String?,
    val note: String?, val client: String?
)

// ── Quotes ───────────────────────────────────────────────────────────────────
data class QuotesResponse(val data: List<QuoteSummary>, val total: Int)

data class QuoteSummary(
    val id: Int, val number: String?, val subject: String?,
    val status: String?, val date: String?, val total: Double?, val client: String?,
    @SerializedName("guest_url") val guestUrl: String?
)

data class QuoteDetail(
    val id: Int, val number: String?, val subject: String?,
    val status: String?, val date: String?,
    val subtotal: Double?, val tax: Double?, val total: Double?,
    val client: String?,
    @SerializedName("contact_name") val contactName: String?,
    val notes: String?,
    @SerializedName("guest_url") val guestUrl: String?,
    val items: List<LineItem>
)

// ── Invoices ─────────────────────────────────────────────────────────────────
data class InvoicesResponse(val data: List<InvoiceSummary>, val total: Int)

data class InvoiceSummary(
    val id: Int, val number: String?, val date: String?,
    @SerializedName("due_date") val dueDate: String?,
    val status: String?, val total: Double?, val client: String?,
    @SerializedName("guest_url") val guestUrl: String?
)

data class InvoiceDetail(
    val id: Int, val number: String?, val date: String?,
    @SerializedName("due_date") val dueDate: String?,
    val status: String?,
    val subtotal: Double?, val tax: Double?, val total: Double?, val balance: Double?,
    val client: String?,
    @SerializedName("contact_name") val contactName: String?,
    val notes: String?,
    @SerializedName("guest_url") val guestUrl: String?,
    val items: List<LineItem>
)

data class LineItem(
    val description: String?, val quantity: Double?,
    @SerializedName("unit_price") val unitPrice: Double?,
    val total: Double?, val taxable: Boolean
)

// ── Expenses ─────────────────────────────────────────────────────────────────
data class ExpensesResponse(val data: List<ExpenseSummary>, val total: Int)

data class ExpenseSummary(
    val id: Int, val description: String?, val amount: Double?,
    val currency: String?, val date: String?,
    val reference: String?,
    @SerializedName("payment_method") val paymentMethod: String?,
    @SerializedName("has_receipt") val hasReceipt: Boolean,
    val client: String?
)

// ── Notifications ─────────────────────────────────────────────────────────────
data class NotificationsResponse(val data: List<Notification>, val total: Int)

data class Notification(
    val id: Int, val type: String, val message: String,
    val action: String?, val timestamp: String?
)

// ── Client Tabs ──────────────────────────────────────────────────────────────
data class ClientLocation(
    val id: Int, val name: String?, val address: String?,
    val city: String?, val state: String?, val zip: String?,
    val phone: String?, val primary: Boolean
)

data class ClientContract(
    val id: Int, val name: String?, val status: String?, val type: String?
)

// ── Charges & Worksheets ─────────────────────────────────────────────────────
data class TicketCharge(
    val id: Int, val name: String, val description: String?,
    val quantity: Double, @SerializedName("unit_price") val unitPrice: Double,
    val total: Double, val invoiced: Boolean,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class ChargesResponse(val charges: List<TicketCharge>, val total: Double)

data class WorksheetSummary(
    val id: Int, @SerializedName("template_name") val templateName: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("signed_name") val signedName: String?,
    @SerializedName("signed_at") val signedAt: String?,
    @SerializedName("is_outtake") val isOuttake: Boolean,
    val signed: Boolean
)

data class WorksheetField(
    val id: Int, val name: String, val type: String,
    val options: String?, val required: Boolean, val value: String?
)

data class WorksheetDetail(
    val id: Int, @SerializedName("template_name") val templateName: String?,
    @SerializedName("signed_name") val signedName: String?,
    @SerializedName("signed_at") val signedAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    val signed: Boolean, val fields: List<WorksheetField>
)

data class WorksheetTemplate(val id: Int, val name: String, val description: String?)

data class SignRequest(
    @SerializedName("signed_name") val signedName: String,
    val signature: String
)

// ── Profile ──────────────────────────────────────────────────────────────────
data class UserProfile(
    val id: Int, val name: String, val email: String,
    val type: Int, val color: String?, val avatar: String?
)

data class AddChargeRequest(
    val name: String, val description: String,
    val quantity: Double, @com.google.gson.annotations.SerializedName("unit_price") val unitPrice: Double
)

data class CreateWorksheetRequest(
    @com.google.gson.annotations.SerializedName("template_id") val templateId: Int = 0,
    @com.google.gson.annotations.SerializedName("is_outtake") val isOuttake: Int = 0
)

data class SaveResponsesRequest(val responses: List<FieldResponse>)
data class FieldResponse(
    @com.google.gson.annotations.SerializedName("field_id") val fieldId: Int,
    val value: String
)

data class UpdateProfileRequest(
    val name: String, val email: String,
    @com.google.gson.annotations.SerializedName("current_password") val currentPassword: String = "",
    @com.google.gson.annotations.SerializedName("new_password") val newPassword: String = ""
)

// ── Outtake Forms ────────────────────────────────────────────────────────────
data class OuttakeSummary(
    val id: Int,
    @SerializedName("sign_token") val signToken: String?,
    val notes: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("signed_name") val signedName: String?,
    @SerializedName("signed_at") val signedAt: String?,
    val signed: Boolean
)

data class OuttakeDetail(
    val id: Int,
    @SerializedName("sign_token") val signToken: String?,
    val notes: String?,
    @SerializedName("created_by") val createdBy: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("signed_name") val signedName: String?,
    @SerializedName("signed_at") val signedAt: String?,
    val signed: Boolean,
    @SerializedName("ticket_subject") val ticketSubject: String?,
    val client: String?
)

// ── Products ─────────────────────────────────────────────────────────────────
data class Product(
    val id: Int, val name: String, val type: String?,
    val description: String?, val price: Double,
    val currency: String?, val code: String?
)
