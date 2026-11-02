import Foundation

// RivetMSP report payloads (port of the report models in the Android ApiModels.kt) and the neutral "document" each one
// is turned into, so one SwiftUI renderer can draw all of them and the number-crunching is testable on Linux.
// Every field is optional: a missing key shows as a dash, it never fails the whole report.

// MARK: Responses

public struct TimeReportResponse: Decodable {
    public let period: String?
    public let totalHours: Double?
    public let entries: [Entry]?
    public struct Entry: Decodable { public let client: String?; public let clientId: Int?; public let hours: Double?; public let ticketCount: Int? }
}
public struct TicketVolumeResponse: Decodable {
    public let year: Int?
    public let months: [Month]?
    public struct Month: Decodable { public let month: Int; public let count: Int }
}
public struct TicketsByClientResponse: Decodable {
    public let year: Int?
    public let month: Int?
    public let clients: [Row]?
    public struct Row: Decodable {
        public let clientId: Int?; public let client: String?; public let raised: Int?; public let resolved: Int?
        public let priorityLow: Int?; public let priorityMedium: Int?; public let priorityHigh: Int?
        public let secondsWorked: Int?; public let avgResponseSeconds: Int?; public let avgResolveSeconds: Int?
    }
}
public struct TimeByTechResponse: Decodable {
    public let year: Int?
    public let technicians: [Row]?
    public struct Row: Decodable { public let userId: Int?; public let name: String?; public let ticketsAssigned: Int?; public let ticketsTouched: Int?; public let secondsWorked: Int? }
}
public struct TechPerformanceResponse: Decodable {
    public let year: Int?
    public let technicians: [Row]?
    public struct Row: Decodable { public let name: String?; public let openTickets: Int?; public let resolvedThisYear: Int? }
}
public struct UnbilledTicketsResponse: Decodable {
    public let year: Int?
    public let clients: [Row]?
    public struct Row: Decodable { public let clientId: Int?; public let client: String?; public let raised: Int?; public let billableClosed: Int?; public let unbilled: Int? }
}
public struct ClientsWithBalanceResponse: Decodable {
    public let clients: [Row]?
    public struct Row: Decodable { public let clientId: Int?; public let clientName: String?; public let balance: Double? }
}
public struct FinancialSummaryResponse: Decodable {
    public let year: Int?
    public let categories: [Category]?
    public let grandTotal: Double?
    public struct Category: Decodable { public let categoryId: Int?; public let categoryName: String?; public let months: [Double]?; public let total: Double? }
}
public struct ProfitLossResponse: Decodable {
    public let year: Int?
    public let months: [Month]?
    public struct Month: Decodable { public let month: Int; public let income: Double?; public let expense: Double?; public let profit: Double? }
}
public struct ExpiringResponse: Decodable {
    public let type: String?
    public let days: Int?
    public let items: [Item]?
    public struct Item: Decodable { public let id: Int?; public let name: String?; public let expireDate: String?; public let clientName: String? }
}
public struct CsatReportResponse: Decodable {
    public let dateFrom: String?; public let dateTo: String?
    public let summary: Summary?
    public let distribution: [String: Int]?
    public let trend: [Point]?
    public let byTechnician: [Stat]?
    public let byClient: [Stat]?
    public let feedback: [Feedback]?
    public struct Summary: Decodable { public let totalClosed: Int?; public let rated: Int?; public let responseRatePct: Double?; public let avgRating: Double?; public let satisfiedPct: Double?; public let needsFollowupCount: Int? }
    public struct Point: Decodable { public let label: String?; public let count: Int?; public let avgRating: Double? }
    public struct Stat: Decodable { public let name: String?; public let ratedCount: Int?; public let avgRating: Double?; public let satisfiedPct: Double? }
    public struct Feedback: Decodable { public let ticketNumber: Int?; public let clientName: String?; public let techName: String?; public let rating: Int?; public let comment: String?; public let ratedAt: String? }
}
public struct RmmHealthReportResponse: Decodable {
    public let dateFrom: String?; public let dateTo: String?
    public let haveData: Bool?
    public let totals: Totals?
    public let bySeverity: [KeyCount]?
    public let byStatus: [KeyCount]?
    public let trend: Trend?
    public let noisiestAssets: [Asset]?
    public let noisiestClients: [Client]?
    public struct Totals: Decodable { public let total: Int?; public let resolved: Int?; public let acknowledged: Int?; public let open: Int?; public let withTicket: Int?; public let conversionPct: Double?; public let mttaSeconds: Int?; public let mttrSeconds: Int? }
    /// `by_severity` items carry `severity`, `by_status` items carry `status`; both are read as the key.
    public struct KeyCount: Decodable {
        public let key: String; public let count: Int
        enum CodingKeys: String, CodingKey { case severity, status, count }
        public init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            key = try c.decodeIfPresent(String.self, forKey: .severity) ?? c.decodeIfPresent(String.self, forKey: .status) ?? ""
            count = try c.decodeIfPresent(Int.self, forKey: .count) ?? 0
        }
    }
    public struct Trend: Decodable { public let labels: [String]?; public let counts: [Int]? }
    public struct Asset: Decodable { public let assetName: String?; public let alerts: Int?; public let mttrSeconds: Int? }
    public struct Client: Decodable { public let clientName: String?; public let alerts: Int?; public let withTicket: Int? }
}
public struct ServiceDeskReportResponse: Decodable {
    public let dateFrom: String?; public let dateTo: String?
    public let totals: Totals?
    public let volume: Volume?
    public let aging: [Bucket]?
    public let sla: Sla?
    public let csat: Csat?
    public let byPriority: [Priority]?
    public let byTechnician: [Tech]?
    public struct Totals: Decodable { public let opened: Int?; public let resolved: Int?; public let openNow: Int? }
    public struct Volume: Decodable { public let labels: [String]?; public let opened: [Int]?; public let resolved: [Int]?; public let backlog: [Int]? }
    public struct Bucket: Decodable { public let label: String?; public let count: Int? }
    public struct Sla: Decodable { public let responseTotal: Int?; public let responseMet: Int?; public let responsePct: Double?; public let resolutionTotal: Int?; public let resolutionMet: Int?; public let resolutionPct: Double? }
    public struct Csat: Decodable { public let rated: Int?; public let avgRating: Double?; public let satisfied: Int?; public let satisfiedPct: Double? }
    public struct Priority: Decodable { public let priority: String?; public let total: Int?; public let avgResponseSeconds: Int?; public let avgResolveSeconds: Int? }
    public struct Tech: Decodable { public let name: String?; public let open: Int?; public let resolved: Int?; public let avgResolveSeconds: Int? }
}
public struct TechUtilizationReportResponse: Decodable {
    public let dateFrom: String?; public let dateTo: String?
    public let capacity: Capacity?
    public let totals: Totals?
    public let technicians: [Tech]?
    public struct Capacity: Decodable { public let hoursPerDay: Int?; public let daysPerWeek: Int?; public let businessDays: Int?; public let capacitySeconds: Int?; public let capacityHoursPerTech: Int? }
    public struct Totals: Decodable { public let billableSeconds: Int?; public let nonbillableSeconds: Int?; public let totalSeconds: Int?; public let billableValue: Double?; public let ticketsClosed: Int?; public let csatRatedCount: Int?; public let teamUtilizationPct: Double?; public let csatAvgRating: Double?; public let csatSatisfiedPct: Double? }
    public struct Tech: Decodable { public let name: String?; public let billableSeconds: Int?; public let nonbillableSeconds: Int?; public let totalSeconds: Int?; public let billableValue: Double?; public let ticketsClosed: Int?; public let avgHandleSeconds: Int?; public let csatAvgRating: Double?; public let utilizationPct: Double? }
}

// MARK: Neutral document

public struct ReportKPI: Equatable { public let label: String; public let value: String
    public init(_ label: String, _ value: String) { self.label = label; self.value = value } }
public struct ReportBar: Equatable { public let label: String; public let value: Double; public let display: String
    public init(_ label: String, _ value: Double, display: String? = nil) { self.label = label; self.value = value; self.display = display ?? Fmt.number(value) } }
public struct ReportRow: Equatable { public let title: String; public let detail: String; public let trailing: String
    public init(_ title: String, detail: String = "", trailing: String = "") { self.title = title; self.detail = detail; self.trailing = trailing } }

public enum ReportSection: Equatable {
    case kpis([ReportKPI])
    case bars(title: String, [ReportBar])
    case rows(title: String, [ReportRow], empty: String)
    case note(String)
}

public struct ReportDocument: Equatable {
    public var sections: [ReportSection]
    public init(_ sections: [ReportSection]) { self.sections = sections }
}

/// Number formatting shared by the report builders (a dash for a missing value, never "nil" or "0.0").
public enum Fmt {
    public static let dash = "—"
    public static func number(_ v: Double) -> String { v == v.rounded() && abs(v) < 1e12 ? String(Int(v)) : String(format: "%.1f", v) }
    public static func int(_ v: Int?) -> String { v.map(String.init) ?? dash }
    public static func pct(_ v: Double?) -> String { v.map { $0 == $0.rounded() ? String(format: "%.0f%%", $0) : String(format: "%.1f%%", $0) } ?? dash }
    public static func rating(_ v: Double?) -> String { v.map { String(format: "%.1f / 5", $0) } ?? dash }
    /// "2h 05m", "45m", "30s"; a dash for nil.
    public static func duration(_ seconds: Int?) -> String {
        guard let s = seconds, s >= 0 else { return dash }
        let h = s / 3600, m = (s % 3600) / 60
        if h > 0 { return String(format: "%dh %02dm", h, m) }
        if m > 0 { return "\(m)m" }
        return "\(s)s"
    }
    public static func hours(seconds: Int?) -> String { seconds.map { String(format: "%.1f h", Double($0) / 3600) } ?? dash }
    public static let monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
    public static func month(_ n: Int) -> String { (1...12).contains(n) ? monthNames[n - 1] : String(n) }
}

public enum ReportBuilder {
    public static func time(_ r: TimeReportResponse) -> ReportDocument {
        let e = r.entries ?? []
        return ReportDocument([
            .kpis([ReportKPI("Total hours", String(format: "%.1f", r.totalHours ?? 0))]),
            .rows(title: "By client", e.map { ReportRow($0.client ?? "—", detail: "\($0.ticketCount ?? 0) tickets", trailing: String(format: "%.1f h", $0.hours ?? 0)) }, empty: "No time logged in this period"),
        ])
    }
    public static func ticketVolume(_ r: TicketVolumeResponse) -> ReportDocument {
        let m = r.months ?? []
        return ReportDocument([
            .kpis([ReportKPI("Tickets raised", String(m.reduce(0) { $0 + $1.count }))]),
            .bars(title: "Per month", m.sorted { $0.month < $1.month }.map { ReportBar(Fmt.month($0.month), Double($0.count)) }),
        ])
    }
    public static func ticketsByClient(_ r: TicketsByClientResponse) -> ReportDocument {
        let c = r.clients ?? []
        return ReportDocument([
            .kpis([ReportKPI("Raised", String(c.reduce(0) { $0 + ($1.raised ?? 0) })), ReportKPI("Resolved", String(c.reduce(0) { $0 + ($1.resolved ?? 0) }))]),
            .rows(title: "By client", c.map { ReportRow($0.client ?? "—",
                detail: "Low \($0.priorityLow ?? 0) · Med \($0.priorityMedium ?? 0) · High \($0.priorityHigh ?? 0) · \(Fmt.hours(seconds: $0.secondsWorked)) worked · response \(Fmt.duration($0.avgResponseSeconds))",
                trailing: "\($0.resolved ?? 0)/\($0.raised ?? 0)") }, empty: "No tickets in this period"),
        ])
    }
    public static func timeByTech(_ r: TimeByTechResponse) -> ReportDocument {
        let t = r.technicians ?? []
        return ReportDocument([
            .kpis([ReportKPI("Total time", Fmt.hours(seconds: t.reduce(0) { $0 + ($1.secondsWorked ?? 0) }))]),
            .rows(title: "By technician", t.map { ReportRow($0.name ?? "—", detail: "\($0.ticketsAssigned ?? 0) assigned · \($0.ticketsTouched ?? 0) touched", trailing: Fmt.hours(seconds: $0.secondsWorked)) }, empty: "No technicians"),
        ])
    }
    public static func techPerformance(_ r: TechPerformanceResponse) -> ReportDocument {
        let t = r.technicians ?? []
        return ReportDocument([
            .bars(title: "Resolved this year", t.map { ReportBar($0.name ?? "—", Double($0.resolvedThisYear ?? 0)) }),
            .rows(title: "Workload", t.map { ReportRow($0.name ?? "—", detail: "\($0.openTickets ?? 0) open", trailing: "\($0.resolvedThisYear ?? 0) resolved") }, empty: "No technicians"),
        ])
    }
    public static func unbilled(_ r: UnbilledTicketsResponse) -> ReportDocument {
        let c = r.clients ?? []
        return ReportDocument([
            .kpis([ReportKPI("Unbilled tickets", String(c.reduce(0) { $0 + ($1.unbilled ?? 0) }))]),
            .rows(title: "By client", c.map { ReportRow($0.client ?? "—", detail: "\($0.raised ?? 0) raised · \($0.billableClosed ?? 0) billable closed", trailing: "\($0.unbilled ?? 0) unbilled") }, empty: "Nothing unbilled"),
        ])
    }
    public static func clientsWithBalance(_ r: ClientsWithBalanceResponse) -> ReportDocument {
        let c = (r.clients ?? []).sorted { ($0.balance ?? 0) > ($1.balance ?? 0) }
        return ReportDocument([
            .kpis([ReportKPI("Outstanding", Money.format(c.reduce(0) { $0 + ($1.balance ?? 0) })), ReportKPI("Clients", String(c.count))]),
            .rows(title: "Balances", c.map { ReportRow($0.clientName ?? "—", trailing: Money.format($0.balance)) }, empty: "No client has an outstanding balance"),
        ])
    }
    /// Income and expense summaries share a shape: categories with 12 monthly figures.
    public static func financial(_ r: FinancialSummaryResponse, noun: String) -> ReportDocument {
        let cats = r.categories ?? []
        var monthTotals = [Double](repeating: 0, count: 12)
        for c in cats { for (i, v) in (c.months ?? []).prefix(12).enumerated() { monthTotals[i] += v } }
        return ReportDocument([
            .kpis([ReportKPI("Total \(noun)", Money.format(r.grandTotal ?? cats.reduce(0) { $0 + ($1.total ?? 0) }))]),
            .bars(title: "Per month", monthTotals.enumerated().map { ReportBar(Fmt.month($0.offset + 1), $0.element, display: Money.format($0.element)) }),
            .rows(title: "By category", cats.map { ReportRow($0.categoryName ?? "—", trailing: Money.format($0.total)) }, empty: "No \(noun) recorded"),
        ])
    }
    public static func profitLoss(_ r: ProfitLossResponse) -> ReportDocument {
        let m = (r.months ?? []).sorted { $0.month < $1.month }
        let income = m.reduce(0) { $0 + ($1.income ?? 0) }, expense = m.reduce(0) { $0 + ($1.expense ?? 0) }
        return ReportDocument([
            .kpis([ReportKPI("Income", Money.format(income)), ReportKPI("Expenses", Money.format(expense)), ReportKPI("Profit", Money.format(income - expense))]),
            .rows(title: "By month", m.map { ReportRow(Fmt.month($0.month), detail: "Income \(Money.format($0.income)) · Expenses \(Money.format($0.expense))", trailing: Money.format($0.profit)) }, empty: "No data"),
        ])
    }
    public static func expiring(_ r: ExpiringResponse) -> ReportDocument {
        let i = r.items ?? []
        return ReportDocument([
            .rows(title: "Next \(r.days ?? 30) days", i.map { ReportRow($0.name ?? "—", detail: $0.clientName ?? "", trailing: RivetDate.dateOnly($0.expireDate)) },
                  empty: "Nothing expiring in the next \(r.days ?? 30) days"),
        ])
    }
    public static func csat(_ r: CsatReportResponse) -> ReportDocument {
        let s = r.summary
        let dist = (r.distribution ?? [:]).sorted { $0.key < $1.key }
        return ReportDocument([
            .kpis([ReportKPI("Average rating", Fmt.rating(s?.avgRating)), ReportKPI("Satisfied", Fmt.pct(s?.satisfiedPct)),
                   ReportKPI("Response rate", Fmt.pct(s?.responseRatePct)), ReportKPI("Needs follow-up", Fmt.int(s?.needsFollowupCount))]),
            .bars(title: "Ratings", dist.map { ReportBar("\($0.key)★", Double($0.value)) }),
            .bars(title: "Trend (responses)", (r.trend ?? []).map { ReportBar($0.label ?? "", Double($0.count ?? 0)) }),
            .rows(title: "By technician", (r.byTechnician ?? []).map { ReportRow($0.name ?? "—", detail: "\($0.ratedCount ?? 0) rated · \(Fmt.pct($0.satisfiedPct)) satisfied", trailing: Fmt.rating($0.avgRating)) }, empty: "No ratings yet"),
            .rows(title: "By client", (r.byClient ?? []).map { ReportRow($0.name ?? "—", detail: "\($0.ratedCount ?? 0) rated", trailing: Fmt.rating($0.avgRating)) }, empty: "No ratings yet"),
            .rows(title: "Recent feedback", (r.feedback ?? []).map { ReportRow("#\($0.ticketNumber ?? 0) · \($0.clientName ?? "")", detail: [$0.comment, $0.techName.map { "Tech: \($0)" }].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: "\n"), trailing: "\($0.rating ?? 0)★") }, empty: "No feedback in this range"),
        ])
    }
    public static func rmmHealth(_ r: RmmHealthReportResponse) -> ReportDocument {
        guard r.haveData != false else { return ReportDocument([.note("No alert data in this range.")]) }
        let t = r.totals
        return ReportDocument([
            .kpis([ReportKPI("Alerts", Fmt.int(t?.total)), ReportKPI("Open", Fmt.int(t?.open)), ReportKPI("Resolved", Fmt.int(t?.resolved)),
                   ReportKPI("MTTA", Fmt.duration(t?.mttaSeconds)), ReportKPI("MTTR", Fmt.duration(t?.mttrSeconds)), ReportKPI("Ticketed", Fmt.pct(t?.conversionPct))]),
            .bars(title: "By severity", (r.bySeverity ?? []).map { ReportBar($0.key.capitalized, Double($0.count)) }),
            .bars(title: "By status", (r.byStatus ?? []).map { ReportBar($0.key.capitalized, Double($0.count)) }),
            .bars(title: "Trend", zip(r.trend?.labels ?? [], r.trend?.counts ?? []).map { ReportBar($0, Double($1)) }),
            .rows(title: "Noisiest assets", (r.noisiestAssets ?? []).map { ReportRow($0.assetName ?? "—", detail: "MTTR \(Fmt.duration($0.mttrSeconds))", trailing: "\($0.alerts ?? 0) alerts") }, empty: "None"),
            .rows(title: "Noisiest clients", (r.noisiestClients ?? []).map { ReportRow($0.clientName ?? "—", detail: "\($0.withTicket ?? 0) became tickets", trailing: "\($0.alerts ?? 0) alerts") }, empty: "None"),
        ])
    }
    public static func serviceDesk(_ r: ServiceDeskReportResponse) -> ReportDocument {
        let t = r.totals, s = r.sla, c = r.csat
        return ReportDocument([
            .kpis([ReportKPI("Opened", Fmt.int(t?.opened)), ReportKPI("Resolved", Fmt.int(t?.resolved)), ReportKPI("Open now", Fmt.int(t?.openNow)),
                   ReportKPI("Response SLA", Fmt.pct(s?.responsePct)), ReportKPI("Resolution SLA", Fmt.pct(s?.resolutionPct)), ReportKPI("CSAT", Fmt.rating(c?.avgRating))]),
            .bars(title: "Opened", zip(r.volume?.labels ?? [], r.volume?.opened ?? []).map { ReportBar($0, Double($1)) }),
            .bars(title: "Backlog", zip(r.volume?.labels ?? [], r.volume?.backlog ?? []).map { ReportBar($0, Double($1)) }),
            .bars(title: "Open ticket age", (r.aging ?? []).map { ReportBar($0.label ?? "", Double($0.count ?? 0)) }),
            .rows(title: "By priority", (r.byPriority ?? []).map { ReportRow($0.priority ?? "—", detail: "Response \(Fmt.duration($0.avgResponseSeconds)) · Resolve \(Fmt.duration($0.avgResolveSeconds))", trailing: "\($0.total ?? 0)") }, empty: "None"),
            .rows(title: "By technician", (r.byTechnician ?? []).map { ReportRow($0.name ?? "—", detail: "\($0.open ?? 0) open · avg resolve \(Fmt.duration($0.avgResolveSeconds))", trailing: "\($0.resolved ?? 0) resolved") }, empty: "None"),
        ])
    }
    public static func techUtilization(_ r: TechUtilizationReportResponse) -> ReportDocument {
        let t = r.totals
        return ReportDocument([
            .kpis([ReportKPI("Team utilization", Fmt.pct(t?.teamUtilizationPct)), ReportKPI("Billable", Fmt.hours(seconds: t?.billableSeconds)),
                   ReportKPI("Non-billable", Fmt.hours(seconds: t?.nonbillableSeconds)), ReportKPI("Billable value", Money.format(t?.billableValue)),
                   ReportKPI("Tickets closed", Fmt.int(t?.ticketsClosed)), ReportKPI("CSAT", Fmt.rating(t?.csatAvgRating))]),
            .rows(title: "By technician", (r.technicians ?? []).map { ReportRow($0.name ?? "—",
                detail: "\(Fmt.hours(seconds: $0.billableSeconds)) billable · \(Fmt.hours(seconds: $0.nonbillableSeconds)) non-billable · \($0.ticketsClosed ?? 0) closed · \(Money.format($0.billableValue))",
                trailing: Fmt.pct($0.utilizationPct)) }, empty: "No technicians"),
            .note("Capacity: \(Fmt.int(r.capacity?.capacityHoursPerTech)) h per technician over \(Fmt.int(r.capacity?.businessDays)) business days."),
        ])
    }
}

// MARK: Which reports exist and how they are fetched

public enum ReportKind: String, CaseIterable {
    case time, volume, byClient = "byclient", byTech = "bytech", performance = "perf", overview
    case income, expense, profitLoss = "pl", unbilled, balances
    case expiring, csat, rmm, desk, utilization

    public var title: String {
        switch self {
        case .time: return "Time Summary"; case .volume: return "Ticket Volume"; case .byClient: return "Tickets by Client"
        case .byTech: return "Time by Technician"; case .performance: return "Technician Performance"; case .overview: return "Overview"
        case .income: return "Income Summary"; case .expense: return "Expense Summary"; case .profitLoss: return "Profit & Loss"
        case .unbilled: return "Unbilled Tickets"; case .balances: return "Clients with a Balance"
        case .expiring: return "Expiring Domains & Certs"; case .csat: return "Customer Satisfaction"; case .rmm: return "RMM Health"
        case .desk: return "Service Desk"; case .utilization: return "Technician Utilization"
        }
    }
    /// Reports that read the `?year=` parameter.
    public var usesYear: Bool { [.volume, .byClient, .byTech, .performance, .overview, .income, .expense, .profitLoss, .unbilled].contains(self) }
    /// Reports that read `from`/`to`.
    public var usesRange: Bool { [.csat, .rmm, .desk, .utilization].contains(self) }
    /// Financial reports are company money: they need the finance/sales role, not just reporting.
    public var isFinancial: Bool { [.income, .expense, .profitLoss, .unbilled, .balances].contains(self) }
    /// The route path under /reports, as the server names it (and the Android hub links it).
    public var endpoint: String {
        switch self {
        case .time: return "time"; case .volume: return "tickets"; case .byClient: return "tickets-by-client"; case .byTech: return "time-by-tech"
        case .performance: return "tech-performance"; case .overview: return "overview"; case .income: return "income-summary"
        case .expense: return "expense-summary"; case .profitLoss: return "profit-loss"; case .unbilled: return "unbilled-tickets"
        case .balances: return "clients-with-balance"; case .expiring: return "expiring"; case .csat: return "csat"
        case .rmm: return "rmm-health"; case .desk: return "service-desk"; case .utilization: return "technician-performance"
        }
    }
}

public enum DateRangePreset: String, CaseIterable {
    case thisMonth = "This month", last30 = "Last 30 days", last90 = "Last 90 days", thisYear = "This year"

    /// (from, to) as `yyyy-MM-dd`, anchored on `today` in the given calendar.
    public func range(today: Date = Date(), calendar: Calendar = .current) -> (from: String, to: String) {
        var cal = calendar; cal.locale = Locale(identifier: "en_US_POSIX")
        let start: Date
        switch self {
        case .thisMonth: start = cal.date(from: cal.dateComponents([.year, .month], from: today)) ?? today
        case .last30: start = cal.date(byAdding: .day, value: -30, to: today) ?? today
        case .last90: start = cal.date(byAdding: .day, value: -90, to: today) ?? today
        case .thisYear: start = cal.date(from: cal.dateComponents([.year], from: today)) ?? today
        }
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.calendar = cal; f.timeZone = cal.timeZone; f.dateFormat = "yyyy-MM-dd"
        return (f.string(from: start), f.string(from: today))
    }
}

public extension APIClient {
    /// Fetches one report and turns it into a document. `Overview` keeps its own screen in the app.
    func reportDocument(_ kind: ReportKind, year: Int? = nil, range: (from: String, to: String)? = nil, period: String = "week") async throws -> ReportDocument {
        var q: [String: String] = [:]
        if kind.usesYear, let year { q["year"] = String(year) }
        if kind.usesRange, let range { q["from"] = range.from; q["to"] = range.to }
        if kind == .time { q["period"] = period }
        if kind == .expiring { q["type"] = "domains"; q["days"] = "30" }
        let path = "reports/\(kind.endpoint)"
        switch kind {
        case .time: return ReportBuilder.time(try await get(path, query: q))
        case .volume: return ReportBuilder.ticketVolume(try await get(path, query: q))
        case .byClient: return ReportBuilder.ticketsByClient(try await get(path, query: q))
        case .byTech: return ReportBuilder.timeByTech(try await get(path, query: q))
        case .performance: return ReportBuilder.techPerformance(try await get(path, query: q))
        case .income: return ReportBuilder.financial(try await get(path, query: q), noun: "income")
        case .expense: return ReportBuilder.financial(try await get(path, query: q), noun: "expenses")
        case .profitLoss: return ReportBuilder.profitLoss(try await get(path, query: q))
        case .unbilled: return ReportBuilder.unbilled(try await get(path, query: q))
        case .balances: return ReportBuilder.clientsWithBalance(try await get(path, query: q))
        case .expiring: return ReportBuilder.expiring(try await get(path, query: q))
        case .csat: return ReportBuilder.csat(try await get(path, query: q))
        case .rmm: return ReportBuilder.rmmHealth(try await get(path, query: q))
        case .desk: return ReportBuilder.serviceDesk(try await get(path, query: q))
        case .utilization: return ReportBuilder.techUtilization(try await get(path, query: q))
        case .overview: throw RivetError.decoding("Overview has its own screen")
        }
    }
}
