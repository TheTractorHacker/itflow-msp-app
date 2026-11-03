import XCTest
import Foundation
@testable import RivetCore

private func decode<T: Decodable>(_ type: T.Type, _ json: String) throws -> T {
    try APIClient.decoder.decode(T.self, from: Data(json.utf8))
}

/// Trimmed copies of real RivetMSP demo-server report responses.
private enum ReportFixtures {
    static let volume = #"{"year":2026,"months":[{"month":9,"count":19},{"month":10,"count":28},{"month":1,"count":0}]}"#
    static let byClient = #"{"year":2026,"month":null,"clients":[{"client_id":5,"client":"Bluebird Bakery & Cafe","raised":7,"resolved":2,"priority_low":5,"priority_medium":0,"priority_high":2,"seconds_worked":8580,"avg_response_seconds":78,"avg_resolve_seconds":107},{"client_id":1,"client":"Harbor Dental Group","raised":5,"resolved":2,"priority_low":3,"priority_medium":0,"priority_high":2,"seconds_worked":8400,"avg_response_seconds":null,"avg_resolve_seconds":120600}]}"#
    static let timeByTech = #"{"year":2026,"technicians":[{"user_id":1,"name":"Alex Morgan","tickets_assigned":6,"tickets_touched":30,"seconds_worked":17040},{"user_id":2,"name":"Priya Nair","tickets_assigned":7,"tickets_touched":16,"seconds_worked":33600}]}"#
    static let unbilled = #"{"year":2026,"clients":[{"client_id":1,"client":"Harbor Dental Group","raised":5,"billable_closed":1,"unbilled":1},{"client_id":2,"client":"Lakeview Law Partners","raised":4,"billable_closed":1,"unbilled":1}]}"#
    static let balance = #"{"clients":[{"client_id":2,"client_name":"B","balance":40},{"client_id":1,"client_name":"Harbor Dental Group","balance":100}]}"#
    static let income = #"{"year":2026,"categories":[{"category_id":4,"category_name":"Service","months":[0,0,0,0,0,0,0,0,0,50,0,0],"total":50}],"grand_total":50}"#
    static let pl = #"{"year":2026,"months":[{"month":10,"income":50,"expense":10,"profit":40},{"month":9,"income":0,"expense":0,"profit":0}]}"#
    static let csat = #"{"date_from":"2026-10-01","date_to":"2026-11-02","summary":{"total_closed":4,"rated":0,"response_rate_pct":0,"avg_rating":null,"satisfied_pct":null,"needs_followup_count":0},"distribution":{"1":0,"2":0,"3":0,"4":0,"5":2},"trend":[{"label":"Oct 2026","count":0,"avg_rating":null}],"by_technician":[],"by_client":[],"feedback":[]}"#
    static let rmmEmpty = #"{"date_from":"2026-10-01","date_to":"2026-11-02","have_data":false,"totals":{"total":0,"resolved":0,"acknowledged":0,"open":0,"with_ticket":0,"conversion_pct":null,"mtta_seconds":null,"mttr_seconds":null},"by_severity":[],"by_status":[],"trend":{"labels":["Oct 2026","Nov 2026"],"counts":[0,0]},"noisiest_assets":[],"noisiest_clients":[]}"#
    static let rmm = #"{"have_data":true,"totals":{"total":9,"resolved":4,"acknowledged":1,"open":4,"with_ticket":3,"conversion_pct":33.3,"mtta_seconds":125,"mttr_seconds":7380},"by_severity":[{"severity":"critical","count":2}],"by_status":[{"status":"open","count":4}],"trend":{"labels":["Oct 2026"],"counts":[9]},"noisiest_assets":[{"asset_id":3,"asset_name":"SRV-01","alerts":5,"mttr_seconds":3600}],"noisiest_clients":[]}"#
    static let desk = #"{"date_from":"2026-10-01","date_to":"2026-11-02","maintenance":"include","totals":{"opened":28,"resolved":4,"open_now":40},"volume":{"labels":["Oct 2026","Nov 2026"],"opened":[28,0],"resolved":[4,0],"backlog":[24,24]},"aging":[{"label":"0-1d","count":0},{"label":"30d+","count":20}],"sla":{"response_total":0,"response_met":0,"response_pct":null,"resolution_total":0,"resolution_met":0,"resolution_pct":null},"csat":{"rated":0,"avg_rating":null,"satisfied":0,"satisfied_pct":null},"by_priority":[{"priority":"High","total":3,"avg_response_seconds":null,"avg_resolve_seconds":null}],"by_technician":[{"user_id":1,"name":"Alex","open":5,"resolved":1,"avg_resolve_seconds":90}]}"#
    static let util = #"{"date_from":"2026-10-01","date_to":"2026-11-02","capacity":{"hours_per_day":8,"days_per_week":5,"business_days":23,"capacity_seconds":662400,"capacity_hours_per_tech":184},"totals":{"billable_seconds":0,"nonbillable_seconds":12840,"total_seconds":12840,"billable_value":0,"tickets_closed":0,"csat_rated_count":0,"team_utilization_pct":0.6,"csat_avg_rating":null,"csat_satisfied_pct":null},"technicians":[{"user_id":2,"name":"Priya Nair","billable_seconds":0,"nonbillable_seconds":8400,"total_seconds":8400,"billable_value":0,"tickets_closed":0,"avg_handle_seconds":null,"csat_rated_count":0,"csat_avg_rating":null,"csat_satisfied_pct":null,"csat_comment_count":0,"utilization_pct":1.3}]}"#
}

final class ReportBuilderTests: XCTestCase {
    func testTicketVolumeIsOrderedByMonthAndTotalled() throws {
        let doc = ReportBuilder.ticketVolume(try decode(TicketVolumeResponse.self, ReportFixtures.volume))
        XCTAssertEqual(doc.sections[0], .kpis([ReportKPI("Tickets raised", "47")]))
        guard case .bars(_, let bars) = doc.sections[1] else { return XCTFail() }
        XCTAssertEqual(bars.map(\.label), ["Jan", "Sep", "Oct"])
    }
    func testTicketsByClientAndTimeByTech() throws {
        let c = ReportBuilder.ticketsByClient(try decode(TicketsByClientResponse.self, ReportFixtures.byClient))
        XCTAssertEqual(c.sections[0], .kpis([ReportKPI("Raised", "12"), ReportKPI("Resolved", "4")]))
        guard case .rows(_, let rows, _) = c.sections[1] else { return XCTFail() }
        XCTAssertEqual(rows[0].trailing, "2/7"); XCTAssertTrue(rows[1].detail.contains("response —"), "a null average shows a dash")
        let t = ReportBuilder.timeByTech(try decode(TimeByTechResponse.self, ReportFixtures.timeByTech))
        XCTAssertEqual(t.sections[0], .kpis([ReportKPI("Total time", "14.1 h")]))
    }
    func testFinancialReports() throws {
        let u = ReportBuilder.unbilled(try decode(UnbilledTicketsResponse.self, ReportFixtures.unbilled))
        XCTAssertEqual(u.sections[0], .kpis([ReportKPI("Unbilled tickets", "2")]))
        let b = ReportBuilder.clientsWithBalance(try decode(ClientsWithBalanceResponse.self, ReportFixtures.balance))
        XCTAssertEqual(b.sections[0], .kpis([ReportKPI("Outstanding", "$140.00"), ReportKPI("Clients", "2")]))
        guard case .rows(_, let rows, _) = b.sections[1] else { return XCTFail() }
        XCTAssertEqual(rows[0].title, "Harbor Dental Group", "largest balance first")
        let i = ReportBuilder.financial(try decode(FinancialSummaryResponse.self, ReportFixtures.income), noun: "income")
        XCTAssertEqual(i.sections[0], .kpis([ReportKPI("Total income", "$50.00")]))
        guard case .bars(_, let bars) = i.sections[1] else { return XCTFail() }
        XCTAssertEqual(bars.count, 12); XCTAssertEqual(bars[9].display, "$50.00"); XCTAssertEqual(bars[9].label, "Oct")
        let pl = ReportBuilder.profitLoss(try decode(ProfitLossResponse.self, ReportFixtures.pl))
        XCTAssertEqual(pl.sections[0], .kpis([ReportKPI("Income", "$50.00"), ReportKPI("Expenses", "$10.00"), ReportKPI("Profit", "$40.00")]))
    }
    func testCsatWithNoRatingsShowsDashesNotZero() throws {
        let doc = ReportBuilder.csat(try decode(CsatReportResponse.self, ReportFixtures.csat))
        guard case .kpis(let k) = doc.sections[0] else { return XCTFail() }
        XCTAssertEqual(k[0], ReportKPI("Average rating", "—")); XCTAssertEqual(k[2], ReportKPI("Response rate", "0%"))
        guard case .bars(_, let bars) = doc.sections[1] else { return XCTFail() }
        XCTAssertEqual(bars.map(\.label), ["1★", "2★", "3★", "4★", "5★"]); XCTAssertEqual(bars[4].value, 2)
    }
    func testRmmHealth() throws {
        let empty = ReportBuilder.rmmHealth(try decode(RmmHealthReportResponse.self, ReportFixtures.rmmEmpty))
        XCTAssertEqual(empty.sections, [.note("No alert data in this range.")])
        let r = try decode(RmmHealthReportResponse.self, ReportFixtures.rmm)
        XCTAssertEqual(r.bySeverity?.first?.key, "critical", "by_severity reads `severity`"); XCTAssertEqual(r.byStatus?.first?.key, "open", "by_status reads `status`")
        guard case .kpis(let k) = ReportBuilder.rmmHealth(r).sections[0] else { return XCTFail() }
        XCTAssertEqual(k[3], ReportKPI("MTTA", "2m")); XCTAssertEqual(k[4], ReportKPI("MTTR", "2h 03m")); XCTAssertEqual(k[5], ReportKPI("Ticketed", "33.3%"))
    }
    func testServiceDeskAndUtilization() throws {
        let d = ReportBuilder.serviceDesk(try decode(ServiceDeskReportResponse.self, ReportFixtures.desk))
        guard case .kpis(let k) = d.sections[0] else { return XCTFail() }
        XCTAssertEqual(k[2], ReportKPI("Open now", "40")); XCTAssertEqual(k[3], ReportKPI("Response SLA", "—"))
        let u = ReportBuilder.techUtilization(try decode(TechUtilizationReportResponse.self, ReportFixtures.util))
        guard case .kpis(let uk) = u.sections[0] else { return XCTFail() }
        XCTAssertEqual(uk[0], ReportKPI("Team utilization", "0.6%"), "sub-percent utilisation is not rounded to 1% or 0%")
        XCTAssertEqual(uk[2], ReportKPI("Non-billable", "3.6 h"))
    }
    func testFormatting() {
        XCTAssertEqual(Fmt.duration(nil), "—"); XCTAssertEqual(Fmt.duration(30), "30s"); XCTAssertEqual(Fmt.duration(2700), "45m"); XCTAssertEqual(Fmt.duration(7500), "2h 05m")
        XCTAssertEqual(Fmt.month(13), "13"); XCTAssertEqual(Fmt.number(3), "3"); XCTAssertEqual(Fmt.number(2.55), "2.5")
    }
}

final class ReportRequestTests: XCTestCase {
    func testQueriesMatchWhatTheServerReads() async throws {
        let t = MockTransport(); t.body = Data(ReportFixtures.volume.utf8)
        let api = APIClient(serverURL: URL(string: "https://msp.example.com")!, token: "t", transport: t)
        _ = try await api.reportDocument(.volume, year: 2026)
        XCTAssertEqual(t.requests[0].url?.path, "/api/v1/reports/tickets"); XCTAssertEqual(t.requests[0].url?.query, "year=2026")
        t.body = Data(ReportFixtures.csat.utf8)
        _ = try await api.reportDocument(.csat, year: 2026, range: ("2026-10-01", "2026-11-02"))
        XCTAssertEqual(t.requests[1].url?.path, "/api/v1/reports/csat"); XCTAssertEqual(t.requests[1].url?.query, "from=2026-10-01&to=2026-11-02")
        t.body = Data(ReportFixtures.util.utf8)
        _ = try await api.reportDocument(.utilization, range: ("2026-10-01", "2026-11-02"))
        XCTAssertEqual(t.requests[2].url?.path, "/api/v1/reports/technician-performance")
        t.body = Data(#"{"type":"domains","days":30,"items":[]}"#.utf8)
        _ = try await api.reportDocument(.expiring)
        XCTAssertEqual(t.requests[3].url?.query, "days=30&type=domains")
    }
    func testEveryKindHasATitleAndExactlyOneParameterStyle() {
        for k in ReportKind.allCases {
            XCTAssertFalse(k.title.isEmpty); XCTAssertFalse(k.usesYear && k.usesRange, "\(k) must not be both")
        }
        XCTAssertEqual(Set(ReportKind.allCases.map(\.endpoint)).count, ReportKind.allCases.count, "endpoints are unique")
    }
    func testDateRangePresets() {
        var cal = Calendar(identifier: .gregorian); cal.timeZone = TimeZone(identifier: "UTC")!
        let today = cal.date(from: DateComponents(year: 2026, month: 11, day: 2))!
        XCTAssertEqual(DateRangePreset.thisMonth.range(today: today, calendar: cal).from, "2026-11-01")
        XCTAssertEqual(DateRangePreset.last30.range(today: today, calendar: cal).from, "2026-10-03")
        XCTAssertEqual(DateRangePreset.last90.range(today: today, calendar: cal).from, "2026-08-04")
        XCTAssertEqual(DateRangePreset.thisYear.range(today: today, calendar: cal).from, "2026-01-01")
        XCTAssertEqual(DateRangePreset.thisYear.range(today: today, calendar: cal).to, "2026-11-02")
    }
}
