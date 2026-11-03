import XCTest
import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif
@testable import RivetCore

private func decode<T: Decodable>(_ type: T.Type, _ json: String) throws -> T {
    try APIClient.decoder.decode(T.self, from: Data(json.utf8))
}

/// Responses copied from a RivetMSP demo server (quotes, invoices, expenses) and the server's handler source (charges, worksheets).
private enum MSPFixtures {
    static let quotes = #"{"data":[{"id":1,"number":"9001","subject":null,"status":"Sent","date":"2026-10-06","total":300,"client":"Harbor Dental Group","guest_url":"\/quote.php?key=qaseedquo9001"}],"total":1}"#
    static let quote = #"{"id":1,"number":"9001","subject":null,"status":"Sent","date":"2026-10-06","subtotal":300,"tax":0,"total":300,"client":"Harbor Dental Group","notes":"QA seed quote","guest_url":"\/quote.php?key=qaseedquo9001","items":[{"description":"Seed quote line","quantity":3,"unit_price":100,"total":300,"taxable":false}]}"#
    static let invoice = #"{"id":1,"number":"9001","date":"2026-10-06","due_date":"2026-11-05","status":"Sent","subtotal":150,"tax":0,"total":150,"client":"Harbor Dental Group","notes":"QA seed invoice","guest_url":"\/invoice.php?key=qaseedinv9001","items":[{"description":"Seed line","quantity":2,"unit_price":75,"total":150,"taxable":false}]}"#
    static let invoices = #"{"data":[{"id":1,"number":"9001","date":"2026-10-06","due_date":"2026-11-05","status":"Sent","total":150,"client":"Harbor Dental Group","guest_url":"\/invoice.php?key=qaseedinv9001"}],"total":1}"#
    static let expenses = #"{"data":[{"id":1,"description":"QA-maestro-expense","amount":7.25,"currency":"USD","date":"2026-10-06","reference":"","payment_method":"","has_receipt":false,"client":null}],"total":1}"#
    static let charges = #"{"charges":[{"id":4,"name":"Cable","description":"Cat6 1m","quantity":2,"unit_price":3.5,"total":7,"invoiced":false,"created_by":"Alex Morgan","created_at":"2026-10-07 09:00:00"}],"total":7}"#
    static let worksheet = #"{"id":9,"template_name":"Onsite checklist","signed_name":null,"signed_at":null,"completed_at":null,"signed":false,"fields":[{"id":1,"name":"Router rebooted","type":"checkbox","options":null,"required":true,"value":"1"},{"id":2,"name":"Result","type":"select","options":"Pass, Fail , Retest","required":false,"value":null}]}"#
    static let outtake = #"{"id":3,"sign_token":"abc","notes":"Laptop returned","created_by":"Alex Morgan","created_at":"2026-10-07 09:00:00","signed_name":null,"signed_at":null,"signed":false,"ticket_subject":"Screen","client":"Harbor Dental Group","contact_name":"Dana Lee"}"#
}

final class MSPDecodingTests: XCTestCase {
    func testQuotesAndInvoicesDecodeFromRealResponses() throws {
        let q = try decode(Paged<QuoteSummary>.self, MSPFixtures.quotes)
        XCTAssertEqual(q.total, 1); XCTAssertEqual(q.data[0].number, "9001"); XCTAssertNil(q.data[0].subject)
        XCTAssertEqual(q.data[0].guestUrl, "/quote.php?key=qaseedquo9001")
        let d = try decode(QuoteDetail.self, MSPFixtures.quote)
        XCTAssertEqual(d.items.count, 1); XCTAssertEqual(d.items[0].unitPrice, 100); XCTAssertFalse(d.items[0].taxable)
        XCTAssertEqual(d.heading, "Quote #9001", "a quote with no subject is named by its number")
        let i = try decode(InvoiceDetail.self, MSPFixtures.invoice)
        XCTAssertEqual(i.dueDate, "2026-11-05"); XCTAssertEqual(i.heading, "Invoice #9001"); XCTAssertNil(i.balance)
        XCTAssertEqual(try decode(Paged<InvoiceSummary>.self, MSPFixtures.invoices).data[0].dueDate, "2026-11-05")
    }
    func testQuoteWithoutItemsKeyStillDecodes() throws {
        let d = try decode(QuoteDetail.self, #"{"id":2,"number":"1","status":"Draft"}"#)
        XCTAssertTrue(d.items.isEmpty)
    }
    func testExpensesChargesWorksheetsOuttakes() throws {
        let e = try decode(Paged<ExpenseSummary>.self, MSPFixtures.expenses)
        XCTAssertEqual(e.data[0].amount, 7.25); XCTAssertFalse(e.data[0].hasReceipt); XCTAssertNil(e.data[0].client)
        let c = try decode(ChargesResponse.self, MSPFixtures.charges)
        XCTAssertEqual(c.total, 7); XCTAssertEqual(c.charges[0].unitPrice, 3.5); XCTAssertFalse(c.charges[0].invoiced)
        XCTAssertEqual(try decode(ChargesResponse.self, #"{"charges":[],"total":0}"#).charges.count, 0)
        let w = try decode(WorksheetDetail.self, MSPFixtures.worksheet)
        XCTAssertEqual(w.fields.count, 2); XCTAssertTrue(w.fields[0].required)
        XCTAssertEqual(w.fields[1].choices, ["Pass", "Fail", "Retest"], "select options are comma separated and trimmed")
        let o = try decode(OuttakeDetail.self, MSPFixtures.outtake)
        XCTAssertEqual(o.contactName, "Dana Lee"); XCTAssertFalse(o.signed)
    }
    func testProductsAndClientFiles() throws {
        XCTAssertEqual(try decode([Product].self, "[]").count, 0)
        let p = try decode([Product].self, #"[{"id":1,"name":"Hour","type":"service","description":null,"price":120,"currency":"USD","code":"H1"}]"#)
        XCTAssertEqual(p[0].price, 120)
        let f = try decode([ClientFile].self, #"[{"id":1,"ticket_id":5,"ticket_number":1005,"ticket_subject":"Pickup","signed_name":"Dana","signed_at":"2026-10-07 10:00:00"}]"#)
        XCTAssertEqual(f[0].ticketNumber, 1005)
    }
}

final class MSPRuleTests: XCTestCase {
    func testFinancialTone() {
        XCTAssertEqual(FinancialTone("Paid"), .success); XCTAssertEqual(FinancialTone("Accepted"), .success)
        XCTAssertEqual(FinancialTone("Overdue"), .critical); XCTAssertEqual(FinancialTone("Declined"), .critical)
        XCTAssertEqual(FinancialTone("Partial"), .warning)
        XCTAssertEqual(FinancialTone("Sent"), .neutral); XCTAssertEqual(FinancialTone(nil), .neutral)
    }
    func testGuestUrlOnlyOnTheConnectedServer() {
        let s = URL(string: "https://msp.example.com")!
        XCTAssertEqual(guestDocumentURL(server: s, path: "/quote.php?key=abc")?.absoluteString, "https://msp.example.com/quote.php?key=abc")
        XCTAssertNotNil(guestDocumentURL(server: s, path: "/invoice.php?key=abc"))
        XCTAssertNil(guestDocumentURL(server: s, path: "https://evil.example.net/quote.php?key=abc"))
        XCTAssertNil(guestDocumentURL(server: s, path: "http://msp.example.com/quote.php?key=abc"), "scheme must match")
        XCTAssertNil(guestDocumentURL(server: s, path: "javascript:alert(1)"))
        XCTAssertNil(guestDocumentURL(server: s, path: "/admin/login.php"))
        XCTAssertNil(guestDocumentURL(server: s, path: "//evil.example.net/quote.php"))
        XCTAssertNil(guestDocumentURL(server: s, path: nil))
    }
    func testExpenseDraftValidation() {
        var d = ExpenseDraft(date: "2026-10-06")
        XCTAssertEqual(d.problem, .missing)
        d.description = "Parking"; d.amount = "abc"
        XCTAssertEqual(d.problem, .invalidAmount)
        d.amount = "-3"; XCTAssertEqual(d.problem, .invalidAmount)
        d.amount = "nan"; XCTAssertEqual(d.problem, .invalidAmount)
        d.amount = "12.50"; XCTAssertNil(d.problem)
        d.date = "10/06/2026"; XCTAssertEqual(d.problem, .invalidDate)
        d.date = "2026-13-01"; XCTAssertEqual(d.problem, .invalidDate)
        d.date = "2026-10-06"; XCTAssertNil(d.problem)
        let f = Dictionary(uniqueKeysWithValues: d.formFields())
        XCTAssertEqual(f["amount"], "12.5"); XCTAssertEqual(f["currency"], "USD"); XCTAssertEqual(f["payment_method"], "")
    }
    func testTimeEntryAndChargeInput() {
        XCTAssertEqual(TimeEntry.hms(hours: 1, minutes: 30), "01:30:00")
        XCTAssertEqual(TimeEntry.hms(hours: 0, minutes: 15), "00:15:00")
        XCTAssertNil(TimeEntry.hms(hours: 0, minutes: 0)); XCTAssertNil(TimeEntry.hms(hours: 0, minutes: 60))
        XCTAssertNil(TimeEntry.hms(hours: -1, minutes: 5)); XCTAssertNil(TimeEntry.hms(hours: 25, minutes: 0))
        XCTAssertEqual(ChargeDraft.quantity("2.5"), 2.5); XCTAssertNil(ChargeDraft.quantity("0")); XCTAssertNil(ChargeDraft.quantity("x"))
        XCTAssertEqual(ChargeDraft.price("0"), 0); XCTAssertNil(ChargeDraft.price("-1")); XCTAssertNil(ChargeDraft.price("inf"))
    }
    func testMoneyFormat() {
        XCTAssertEqual(Money.format(300), "$300.00"); XCTAssertEqual(Money.format(7.25), "$7.25"); XCTAssertEqual(Money.format(nil), "$0.00")
        XCTAssertEqual(Money.format(1234.5), "$1,234.50")
    }
    func testSignaturePayloadMatchesServerRule() {
        let png = Data([0x89, 0x50, 0x4E, 0x47])
        let s = Signature.dataURL(png: png)
        XCTAssertEqual(s, "data:image/png;base64,iVBORw==")
        XCTAssertTrue(Signature.isAcceptable(s!))
        XCTAssertNil(Signature.dataURL(png: Data()))
        XCTAssertNil(Signature.dataURL(png: Data(repeating: 1, count: 400_000)), "over 512 KiB encoded is refused before sending")
        XCTAssertFalse(Signature.isAcceptable("data:image/gif;base64,AAAA"))
        XCTAssertFalse(Signature.isAcceptable("data:image/png;base64,AA AA"))
        XCTAssertFalse(Signature.isAcceptable("data:image/png;base64,"))
    }
    func testWorksheetAndOuttakeDeepLinks() {
        XCTAssertEqual(DeepLinks.resolve("quotes/4"), .quote(4)); XCTAssertEqual(DeepLinks.resolve("quotes"), .quotes)
        XCTAssertEqual(DeepLinks.resolve("invoices/9"), .invoice(9)); XCTAssertEqual(DeepLinks.resolve("expenses"), .expenses)
        XCTAssertEqual(DeepLinks.resolve("worksheets/7/fill"), .worksheet(7)); XCTAssertEqual(DeepLinks.resolve("outtakes/7/sign"), .outtake(7))
        XCTAssertNil(DeepLinks.resolve("worksheets")); XCTAssertNil(DeepLinks.resolve("worksheets/7/delete")); XCTAssertNil(DeepLinks.resolve("outtakes/7/fill"))
        XCTAssertNil(DeepLinks.resolve("quotes/4/../../admin"))
    }
    func testModuleGating() {
        // A server that reports nothing keeps everything visible (the Android app shows it all and lets the server answer 403).
        XCTAssertTrue(Capabilities().showsQuotes); XCTAssertTrue(Capabilities().showsExpenses); XCTAssertTrue(Capabilities().showsTicketCharges)
        let off = Capabilities(isAdmin: true, modules: ModuleFlags(accountingEnabled: false, ticketChargesEnabled: false))
        XCTAssertFalse(off.showsQuotes); XCTAssertFalse(off.showsInvoices); XCTAssertFalse(off.showsExpenses)
        XCTAssertFalse(off.showsTicketCharges); XCTAssertFalse(off.canAddCharge)
        let tech = Capabilities(permissions: ["module_support": 2, "module_sales": 0, "module_financial": 1])
        XCTAssertFalse(tech.showsQuotes); XCTAssertTrue(tech.showsExpenses); XCTAssertTrue(tech.canAddCharge)
        let reader = Capabilities(permissions: ["module_support": 1])
        XCTAssertTrue(reader.showsTicketCharges); XCTAssertFalse(reader.canAddCharge)
    }
}

final class MSPRequestTests: XCTestCase {
    private func client(_ t: MockTransport) -> APIClient { APIClient(serverURL: URL(string: "https://msp.example.com")!, token: "t", transport: t) }

    func testExpenseIsOneMultipartRequestWithReceipt() async throws {
        let t = MockTransport(); t.body = Data("{}".utf8)
        var d = ExpenseDraft(date: "2026-10-06"); d.description = "Parking"; d.amount = "12.5"
        try await client(t).createExpense(d, receipt: ReplyFile(name: "r.jpg", mimeType: "image/jpeg", data: Data([1, 2, 3])))
        let r = t.requests[0]
        XCTAssertEqual(r.httpMethod, "POST"); XCTAssertEqual(r.url?.path, "/api/v1/expenses")
        XCTAssertTrue(r.value(forHTTPHeaderField: "Content-Type")!.hasPrefix("multipart/form-data; boundary="))
        let body = String(decoding: r.httpBody!, as: UTF8.self)
        XCTAssertTrue(body.contains("name=\"receipt\"; filename=\"r.jpg\""), "the receipt goes under `receipt`, not `files[]`")
        XCTAssertTrue(body.contains("name=\"amount\"\r\n\r\n12.5")); XCTAssertFalse(body.contains("files[]"))
    }
    func testChargeAndTimeBodiesAreSnakeCase() async throws {
        let t = MockTransport(); t.body = Data("{}".utf8)
        try await client(t).addCharge(ticketId: 5, AddChargeRequest(name: "Cable", description: "", quantity: 2, unitPrice: 3.5))
        try await client(t).logTime(ticketId: 5, LogTimeRequest(timeWorked: "01:00:00", note: "n"))
        let c = String(decoding: t.requests[0].httpBody!, as: UTF8.self), l = String(decoding: t.requests[1].httpBody!, as: UTF8.self)
        XCTAssertTrue(c.contains("\"unit_price\":3.5")); XCTAssertEqual(t.requests[0].url?.path, "/api/v1/tickets/5/charges")
        XCTAssertTrue(l.contains("\"time_worked\":\"01:00:00\"")); XCTAssertEqual(t.requests[1].url?.path, "/api/v1/tickets/5/time")
    }
    func testWorksheetAndOuttakeRequests() async throws {
        let t = MockTransport(); t.body = Data(#"{"id":12}"#.utf8)
        let id = try await client(t).createWorksheet(ticketId: 5, CreateWorksheetRequest(templateId: 2))
        XCTAssertEqual(id.id, 12)
        XCTAssertTrue(String(decoding: t.requests[0].httpBody!, as: UTF8.self).contains("\"template_id\":2"))
        try await client(t).saveWorksheetResponses(id: 12, SaveResponsesRequest(responses: [FieldResponse(fieldId: 1, value: "1")]))
        XCTAssertTrue(String(decoding: t.requests[1].httpBody!, as: UTF8.self).contains("\"field_id\":1"))
        try await client(t).completeWorksheet(id: 12, completed: true)
        XCTAssertEqual(t.requests[2].url?.path, "/api/v1/worksheets/12/complete")
        _ = try await client(t).createOuttake(ticketId: 5)
        XCTAssertEqual(t.requests[3].url?.path, "/api/v1/tickets/5/outtake")
        XCTAssertTrue(String(decoding: t.requests[3].httpBody!, as: UTF8.self).contains("\"is_outtake\":0"))
        try await client(t).signOuttake(id: 3, SignRequest(signedName: "Dana", signature: "data:image/png;base64,AA=="))
        XCTAssertEqual(t.requests[4].url?.path, "/api/v1/outtakes/3/sign")
        XCTAssertTrue(String(decoding: t.requests[4].httpBody!, as: UTF8.self).contains("\"signed_name\":\"Dana\""))
    }
}
