import Foundation

// RivetMSP-only payloads: quotes, invoices, expenses, ticket charges, products, worksheets, outtake forms and time.
// Shapes come from the Android app's ApiModels.kt and the server's api/v1 handlers; the quote, invoice and expense
// fixtures in the tests are real responses from a RivetMSP demo server.

// MARK: Sales documents

public struct LineItem: Decodable, Hashable, Identifiable {
    public var id: String { "\(description ?? "")|\(quantity ?? 0)|\(unitPrice ?? 0)" }
    public let description: String?
    public let quantity: Double?
    public let unitPrice: Double?
    public let total: Double?
    public let taxable: Bool
    enum CodingKeys: String, CodingKey { case description, quantity, unitPrice, total, taxable }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        description = try c.decodeIfPresent(String.self, forKey: .description)
        quantity = try c.decodeIfPresent(Double.self, forKey: .quantity)
        unitPrice = try c.decodeIfPresent(Double.self, forKey: .unitPrice)
        total = try c.decodeIfPresent(Double.self, forKey: .total)
        taxable = LossyBool.decode(c, .taxable) ?? false
    }
}

public struct QuoteSummary: Decodable, Identifiable, Hashable {
    public let id: Int
    public let number: String?
    public let subject: String?
    public let status: String?
    public let date: String?
    public let total: Double?
    public let client: String?
    public let guestUrl: String?
}

public struct QuoteDetail: Decodable, Hashable {
    public let id: Int
    public let number: String?
    public let subject: String?
    public let status: String?
    public let date: String?
    public let subtotal: Double?
    public let tax: Double?
    public let total: Double?
    public let client: String?
    public let contactName: String?
    public let notes: String?
    public let guestUrl: String?
    public let items: [LineItem]
    enum CodingKeys: String, CodingKey { case id, number, subject, status, date, subtotal, tax, total, client, contactName, notes, guestUrl, items }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        number = try c.decodeIfPresent(String.self, forKey: .number)
        subject = try c.decodeIfPresent(String.self, forKey: .subject)
        status = try c.decodeIfPresent(String.self, forKey: .status)
        date = try c.decodeIfPresent(String.self, forKey: .date)
        subtotal = try c.decodeIfPresent(Double.self, forKey: .subtotal)
        tax = try c.decodeIfPresent(Double.self, forKey: .tax)
        total = try c.decodeIfPresent(Double.self, forKey: .total)
        client = try c.decodeIfPresent(String.self, forKey: .client)
        contactName = try c.decodeIfPresent(String.self, forKey: .contactName)
        notes = try c.decodeIfPresent(String.self, forKey: .notes)
        guestUrl = try c.decodeIfPresent(String.self, forKey: .guestUrl)
        items = try c.decodeIfPresent([LineItem].self, forKey: .items) ?? []
    }
    /// Quotes often have no subject; the number keeps the screen identifiable.
    public var heading: String {
        if let s = subject, !s.trimmingCharacters(in: .whitespaces).isEmpty { return s }
        return number.map { "Quote #\($0)" } ?? "Quote"
    }
}

public struct InvoiceSummary: Decodable, Identifiable, Hashable {
    public let id: Int
    public let number: String?
    public let date: String?
    public let dueDate: String?
    public let status: String?
    public let total: Double?
    public let client: String?
    public let guestUrl: String?
}

public struct InvoiceDetail: Decodable, Hashable {
    public let id: Int
    public let number: String?
    public let date: String?
    public let dueDate: String?
    public let status: String?
    public let subtotal: Double?
    public let tax: Double?
    public let total: Double?
    public let balance: Double?
    public let client: String?
    public let contactName: String?
    public let notes: String?
    public let guestUrl: String?
    public let items: [LineItem]
    enum CodingKeys: String, CodingKey { case id, number, date, dueDate, status, subtotal, tax, total, balance, client, contactName, notes, guestUrl, items }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        number = try c.decodeIfPresent(String.self, forKey: .number)
        date = try c.decodeIfPresent(String.self, forKey: .date)
        dueDate = try c.decodeIfPresent(String.self, forKey: .dueDate)
        status = try c.decodeIfPresent(String.self, forKey: .status)
        subtotal = try c.decodeIfPresent(Double.self, forKey: .subtotal)
        tax = try c.decodeIfPresent(Double.self, forKey: .tax)
        total = try c.decodeIfPresent(Double.self, forKey: .total)
        balance = try c.decodeIfPresent(Double.self, forKey: .balance)
        client = try c.decodeIfPresent(String.self, forKey: .client)
        contactName = try c.decodeIfPresent(String.self, forKey: .contactName)
        notes = try c.decodeIfPresent(String.self, forKey: .notes)
        guestUrl = try c.decodeIfPresent(String.self, forKey: .guestUrl)
        items = try c.decodeIfPresent([LineItem].self, forKey: .items) ?? []
    }
    public var heading: String { number.map { "Invoice #\($0)" } ?? "Invoice" }
}

/// Colour family for a quote/invoice status (Android `forFinancialStatus`).
public enum FinancialTone: Equatable {
    case success, critical, warning, neutral
    public init(_ status: String?) {
        switch status {
        case "Paid", "Accepted": self = .success
        case "Overdue", "Declined": self = .critical
        case "Partial": self = .warning
        default: self = .neutral
        }
    }
}

/// A server-supplied `guest_url` (the customer-facing quote/invoice page) is opened only when it resolves to an
/// http(s) address on the connected server itself, under `/quote.php` or `/invoice.php`.
public func guestDocumentURL(server: URL, path: String?) -> URL? {
    guard let path, !path.isEmpty, let link = URL(string: path, relativeTo: server)?.absoluteURL else { return nil }
    guard link.scheme?.lowercased() == server.scheme?.lowercased(), link.host?.lowercased() == server.host?.lowercased(),
          link.port == server.port, link.user == nil, link.password == nil,
          link.path == "/quote.php" || link.path == "/invoice.php" else { return nil }
    return link
}

// MARK: Expenses & products

public struct ExpenseSummary: Decodable, Identifiable, Hashable {
    public let id: Int
    public let description: String?
    public let amount: Double?
    public let currency: String?
    public let date: String?
    public let reference: String?
    public let paymentMethod: String?
    public let hasReceipt: Bool
    public let client: String?
    enum CodingKeys: String, CodingKey { case id, description, amount, currency, date, reference, paymentMethod, hasReceipt, client }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        description = try c.decodeIfPresent(String.self, forKey: .description)
        amount = try c.decodeIfPresent(Double.self, forKey: .amount)
        currency = try c.decodeIfPresent(String.self, forKey: .currency)
        date = try c.decodeIfPresent(String.self, forKey: .date)
        reference = try c.decodeIfPresent(String.self, forKey: .reference)
        paymentMethod = try c.decodeIfPresent(String.self, forKey: .paymentMethod)
        hasReceipt = LossyBool.decode(c, .hasReceipt) ?? false
        client = try c.decodeIfPresent(String.self, forKey: .client)
    }
}

public struct Product: Decodable, Identifiable, Hashable {
    public let id: Int
    public let name: String
    public let type: String?
    public let description: String?
    public let price: Double
    public let currency: String?
    public let code: String?
    enum CodingKeys: String, CodingKey { case id, name, type, description, price, currency, code }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        name = try c.decode(String.self, forKey: .name)
        type = try c.decodeIfPresent(String.self, forKey: .type)
        description = try c.decodeIfPresent(String.self, forKey: .description)
        price = try c.decodeIfPresent(Double.self, forKey: .price) ?? 0
        currency = try c.decodeIfPresent(String.self, forKey: .currency)
        code = try c.decodeIfPresent(String.self, forKey: .code)
    }
}

/// The fields of the "add expense" form, validated the way the Android screen does before anything is sent.
public struct ExpenseDraft: Equatable {
    public var description = ""
    public var amount = ""
    public var date = ""
    public var paymentMethod = ""
    public var reference = ""
    public init(date: String = "") { self.date = date }

    public enum Problem: Equatable { case missing, invalidAmount, invalidDate }

    /// The amount as a number, or nil when it is not a positive, finite decimal.
    public var parsedAmount: Double? {
        guard let v = Double(amount.trimmingCharacters(in: .whitespaces)), v.isFinite, v > 0 else { return nil }
        return v
    }

    public var problem: Problem? {
        if description.trimmingCharacters(in: .whitespaces).isEmpty || amount.trimmingCharacters(in: .whitespaces).isEmpty { return .missing }
        if parsedAmount == nil { return .invalidAmount }
        if !ExpenseDraft.isISODate(date) { return .invalidDate }
        return nil
    }

    public static func isISODate(_ s: String) -> Bool {
        let parts = s.split(separator: "-", omittingEmptySubsequences: false)
        guard parts.count == 3, parts[0].count == 4, parts[1].count == 2, parts[2].count == 2,
              let y = Int(parts[0]), let m = Int(parts[1]), let d = Int(parts[2]) else { return false }
        return y >= 1970 && (1...12).contains(m) && (1...31).contains(d)
    }

    /// Text fields in the order the server's `expenses.php` reads them. The amount is sent as a plain decimal.
    public func formFields(currency: String = "USD") -> [(String, String)] {
        [("description", description.trimmingCharacters(in: .whitespacesAndNewlines)),
         ("amount", String(parsedAmount ?? 0)),
         ("date", date), ("currency", currency),
         ("reference", reference.trimmingCharacters(in: .whitespacesAndNewlines)),
         ("payment_method", paymentMethod.trimmingCharacters(in: .whitespacesAndNewlines))]
    }
}

/// Money as the Android app prints it: a US-locale currency string.
public enum Money {
    public static func format(_ value: Double?, currency: String = "USD") -> String {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US")
        f.numberStyle = .currency
        f.currencyCode = currency.isEmpty ? "USD" : currency
        return f.string(from: NSNumber(value: value ?? 0)) ?? String(format: "%.2f", value ?? 0)
    }
}

// MARK: Ticket charges & time

public struct TicketCharge: Decodable, Identifiable, Hashable {
    public let id: Int
    public let name: String
    public let description: String?
    public let quantity: Double
    public let unitPrice: Double
    public let total: Double
    public let invoiced: Bool
    public let createdBy: String?
    public let createdAt: String?
    enum CodingKeys: String, CodingKey { case id, name, description, quantity, unitPrice, total, invoiced, createdBy, createdAt }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        description = try c.decodeIfPresent(String.self, forKey: .description)
        quantity = try c.decodeIfPresent(Double.self, forKey: .quantity) ?? 0
        unitPrice = try c.decodeIfPresent(Double.self, forKey: .unitPrice) ?? 0
        total = try c.decodeIfPresent(Double.self, forKey: .total) ?? 0
        invoiced = LossyBool.decode(c, .invoiced) ?? false
        createdBy = try c.decodeIfPresent(String.self, forKey: .createdBy)
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
    }
}

public struct ChargesResponse: Decodable {
    public let charges: [TicketCharge]
    public let total: Double
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        charges = try c.decodeIfPresent([TicketCharge].self, forKey: .charges) ?? []
        total = try c.decodeIfPresent(Double.self, forKey: .total) ?? 0
    }
    enum CodingKeys: String, CodingKey { case charges, total }
}

public struct AddChargeRequest: Encodable {
    public var name: String
    public var description: String
    public var quantity: Double
    public var unitPrice: Double
    public init(name: String, description: String, quantity: Double, unitPrice: Double) {
        self.name = name; self.description = description; self.quantity = quantity; self.unitPrice = unitPrice
    }
}

/// `POST /tickets/{id}/time` — standalone time entry as an internal note.
public struct LogTimeRequest: Encodable {
    public var timeWorked: String
    public var note: String
    public init(timeWorked: String, note: String) { self.timeWorked = timeWorked; self.note = note }
}

public enum TimeEntry {
    /// Hours and minutes to the `HH:MM:SS` text the server stores. Nil for zero, negative or absurd (over 24 h) input.
    public static func hms(hours: Int, minutes: Int) -> String? {
        guard hours >= 0, (0..<60).contains(minutes), hours * 60 + minutes > 0, hours * 60 + minutes <= 24 * 60 else { return nil }
        return String(format: "%02d:%02d:00", hours, minutes)
    }
}

public enum ChargeDraft {
    /// A quantity or price typed by the user: a finite decimal that is positive (quantity) or not negative (price).
    public static func quantity(_ s: String) -> Double? {
        guard let v = Double(s.trimmingCharacters(in: .whitespaces)), v.isFinite, v > 0 else { return nil }
        return v
    }
    public static func price(_ s: String) -> Double? {
        guard let v = Double(s.trimmingCharacters(in: .whitespaces)), v.isFinite, v >= 0 else { return nil }
        return v
    }
}

// MARK: Worksheets & outtake forms

public struct WorksheetField: Decodable, Identifiable, Hashable {
    public let id: Int
    public let name: String
    public let type: String
    public let options: String?
    public let required: Bool
    public let value: String?
    enum CodingKeys: String, CodingKey { case id, name, type, options, required, value }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        name = try c.decodeIfPresent(String.self, forKey: .name) ?? ""
        type = try c.decodeIfPresent(String.self, forKey: .type) ?? "text"
        options = try c.decodeIfPresent(String.self, forKey: .options)
        required = LossyBool.decode(c, .required) ?? false
        value = try c.decodeIfPresent(String.self, forKey: .value)
    }
    /// The choices of a `select` field (the server stores them comma-separated).
    public var choices: [String] {
        (options ?? "").split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }
}

public struct WorksheetDetail: Decodable, Hashable {
    public let id: Int
    public let templateName: String?
    public let signedName: String?
    public let signedAt: String?
    public let completedAt: String?
    public let signed: Bool
    public let fields: [WorksheetField]
    enum CodingKeys: String, CodingKey { case id, templateName, signedName, signedAt, completedAt, signed, fields }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        templateName = try c.decodeIfPresent(String.self, forKey: .templateName)
        signedName = try c.decodeIfPresent(String.self, forKey: .signedName)
        signedAt = try c.decodeIfPresent(String.self, forKey: .signedAt)
        completedAt = try c.decodeIfPresent(String.self, forKey: .completedAt)
        signed = LossyBool.decode(c, .signed) ?? false
        fields = try c.decodeIfPresent([WorksheetField].self, forKey: .fields) ?? []
    }
}

public struct WorksheetTemplate: Decodable, Identifiable, Hashable {
    public let id: Int
    public let name: String
    public let description: String?
}

public struct CreateWorksheetRequest: Encodable {
    public var templateId: Int
    public var isOuttake: Int
    public init(templateId: Int = 0, isOuttake: Int = 0) { self.templateId = templateId; self.isOuttake = isOuttake }
}

public struct FieldResponse: Encodable, Hashable {
    public var fieldId: Int
    public var value: String
    public init(fieldId: Int, value: String) { self.fieldId = fieldId; self.value = value }
}
public struct SaveResponsesRequest: Encodable {
    public var responses: [FieldResponse]
    public init(responses: [FieldResponse]) { self.responses = responses }
}
public struct CompleteWorksheetRequest: Encodable {
    public var completed: Bool
    public init(completed: Bool) { self.completed = completed }
}

public struct OuttakeDetail: Decodable, Hashable {
    public let id: Int
    public let notes: String?
    public let createdBy: String?
    public let createdAt: String?
    public let signedName: String?
    public let signedAt: String?
    public let signed: Bool
    public let ticketSubject: String?
    public let client: String?
    public let contactName: String?
    enum CodingKeys: String, CodingKey { case id, notes, createdBy, createdAt, signedName, signedAt, signed, ticketSubject, client, contactName }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        notes = try c.decodeIfPresent(String.self, forKey: .notes)
        createdBy = try c.decodeIfPresent(String.self, forKey: .createdBy)
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
        signedName = try c.decodeIfPresent(String.self, forKey: .signedName)
        signedAt = try c.decodeIfPresent(String.self, forKey: .signedAt)
        signed = LossyBool.decode(c, .signed) ?? false
        ticketSubject = try c.decodeIfPresent(String.self, forKey: .ticketSubject)
        client = try c.decodeIfPresent(String.self, forKey: .client)
        contactName = try c.decodeIfPresent(String.self, forKey: .contactName)
    }
}

/// `POST /outtakes/{id}/sign` body: the typed name and the drawn signature as a PNG data URL.
public struct SignRequest: Encodable {
    public var signedName: String
    public var signature: String
    public init(signedName: String, signature: String) { self.signedName = signedName; self.signature = signature }
}

public enum Signature {
    /// The server rejects anything over 512 KiB or not shaped `data:image/(png|jpeg);base64,…`.
    public static let maxPayloadBytes = 524_288

    /// `data:image/png;base64,…` for PNG bytes, or nil when the result would exceed the server's limit or the input is empty.
    public static func dataURL(png: Data) -> String? {
        guard !png.isEmpty else { return nil }
        let s = "data:image/png;base64," + png.base64EncodedString()
        return s.utf8.count <= maxPayloadBytes ? s : nil
    }

    /// True for a payload the server would accept (used before sending and in tests).
    public static func isAcceptable(_ s: String) -> Bool {
        guard s.utf8.count <= maxPayloadBytes else { return false }
        for prefix in ["data:image/png;base64,", "data:image/jpeg;base64,"] where s.hasPrefix(prefix) {
            let body = s.dropFirst(prefix.count)
            return !body.isEmpty && body.allSatisfy { $0.isASCII && ($0.isLetter || $0.isNumber || $0 == "+" || $0 == "/" || $0 == "=") }
        }
        return false
    }
}

// MARK: Client files & milestones

public struct ClientFile: Decodable, Identifiable, Hashable {
    public let id: Int
    public let ticketId: Int
    public let ticketNumber: Int
    public let ticketSubject: String?
    public let signedName: String?
    public let signedAt: String?
}
