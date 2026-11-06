import SwiftUI
import LocalAuthentication
import RivetCore

// MARK: Reports

@MainActor struct ReportsView: View {
    @EnvironmentObject var session: Session
    private struct Item: Identifiable { let kind: ReportKind; let symbol: String; let subtitle: String; var id: String { kind.rawValue } }
    private let tickets = [
        Item(kind: .time, symbol: "timer", subtitle: "Hours logged by client"),
        Item(kind: .volume, symbol: "chart.line.uptrend.xyaxis", subtitle: "Tickets raised per month"),
        Item(kind: .byClient, symbol: "building.2", subtitle: "Raised, resolved, priority breakdown"),
        Item(kind: .byTech, symbol: "person", subtitle: "Tickets assigned/touched, time worked"),
        Item(kind: .performance, symbol: "trophy", subtitle: "Open workload vs. resolved this year"),
        Item(kind: .overview, symbol: "chart.pie", subtitle: "Open tickets by priority, status, category"),
    ]
    private let financial = [
        Item(kind: .income, symbol: "dollarsign.circle", subtitle: "Monthly income by category"),
        Item(kind: .expense, symbol: "receipt", subtitle: "Monthly expenses by category"),
        Item(kind: .profitLoss, symbol: "scalemass", subtitle: "Income minus expenses by month"),
        Item(kind: .unbilled, symbol: "doc.badge.clock", subtitle: "Billable tickets not yet invoiced"),
        Item(kind: .balances, symbol: "wallet.pass", subtitle: "Outstanding invoice balances"),
    ]
    private let operations = [Item(kind: .expiring, symbol: "calendar.badge.exclamationmark", subtitle: "Renewals due in the next 30 days")]
    private let support = [
        Item(kind: .csat, symbol: "face.smiling", subtitle: "CSAT ratings, trend, feedback"),
        Item(kind: .rmm, symbol: "exclamationmark.triangle", subtitle: "Alert volume, MTTA/MTTR, noisiest assets"),
        Item(kind: .desk, symbol: "headphones", subtitle: "Volume, backlog aging, SLA compliance"),
        Item(kind: .utilization, symbol: "gauge", subtitle: "Billable vs. non-billable, capacity"),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                section("Tickets", tickets)
                // Money reports are hidden for roles without sales/finance access (the server refuses them anyway).
                if session.capabilities.showsInvoices || session.capabilities.showsExpenses { section("Financial", financial) }
                section("Operations", operations)
                section("Support & Operations", support)
            }.padding(.bottom, 20)
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Reports").navigationBarTitleDisplayMode(.inline)
    }

    private func section(_ title: String, _ items: [Item]) -> some View {
        VStack(spacing: 0) {
            SectionLabel(text: title, tint: true)
            ForEach(items) { i in
                NavigationLink {
                    if i.kind == .overview { OverviewReportView() } else { ReportScreen(kind: i.kind) }
                } label: {
                    ListRowCard {
                        IconTile(systemName: i.symbol, size: 38)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(i.kind.title).font(.subheadline.weight(.semibold))
                            Text(i.subtitle).font(.caption).foregroundColor(Palette.label2)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundColor(Palette.label3)
                    }
                }.buttonStyle(.plain)
            }
        }
    }
}

/// One report drawn from its neutral document, with the year stepper / date presets / period chips it needs.
@MainActor struct ReportScreen: View {
    let kind: ReportKind
    @EnvironmentObject var session: Session
    @StateObject private var loader = Loader<ReportDocument>()
    @State private var year = Calendar.current.component(.year, from: Date())
    @State private var preset = DateRangePreset.thisMonth
    @State private var periodIndex = 0
    private let periods = [("week", "This week"), ("month", "This month"), ("year", "This year")]

    private struct Key: Equatable { var year: Int; var preset: DateRangePreset; var period: Int }

    var body: some View {
        VStack(spacing: 0) {
            if kind.usesYear {
                HStack {
                    Button { year -= 1 } label: { Image(systemName: "chevron.left") }.accessibilityLabel("Previous year")
                    Text(String(year)).font(.headline).frame(minWidth: 70)
                    Button { year += 1 } label: { Image(systemName: "chevron.right") }.accessibilityLabel("Next year")
                }.padding(.vertical, 8)
            }
            if kind.usesRange {
                FilterChips(items: DateRangePreset.allCases.map(\.rawValue), selection: Binding(
                    get: { DateRangePreset.allCases.firstIndex(of: preset) ?? 0 },
                    set: { preset = DateRangePreset.allCases[$0] })).padding(.vertical, 8)
            }
            if kind == .time { FilterChips(items: periods.map(\.1), selection: $periodIndex).padding(.vertical, 8) }
            LoadView(loader: loader, retry: load) { doc in
                ScrollView {
                    VStack(spacing: 12) { ForEach(Array(doc.sections.enumerated()), id: \.offset) { _, s in sectionView(s) } }.padding(.vertical, 8)
                }.refreshable { await load() }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle(kind.title).navigationBarTitleDisplayMode(.inline)
        .task(id: Key(year: year, preset: preset, period: periodIndex)) { await load() }
    }

    @ViewBuilder private func sectionView(_ s: ReportSection) -> some View {
        switch s {
        case .kpis(let items):
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(items, id: \.label) { k in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(k.value).font(.title3.weight(.bold)); Text(k.label).font(.caption).foregroundColor(Palette.label2)
                    }.frame(maxWidth: .infinity, alignment: .leading).card(padding: 12)
                }
            }.padding(.horizontal, 16)
        case .bars(let title, let bars):
            if !bars.isEmpty {
                let peak = max(bars.map(\.value).max() ?? 0, 1)
                VStack(alignment: .leading, spacing: 8) {
                    Text(title).font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                    ForEach(bars, id: \.label) { b in
                        HStack(spacing: 8) {
                            Text(b.label).font(.caption).frame(width: 64, alignment: .leading).lineLimit(1)
                            GeometryReader { g in
                                RoundedRectangle(cornerRadius: 4).fill(Palette.brand).frame(width: max(2, g.size.width * CGFloat(b.value / peak)))
                            }.frame(height: 14)
                            Text(b.display).font(.caption.weight(.semibold)).frame(minWidth: 40, alignment: .trailing)
                        }
                    }
                }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
            }
        case .rows(let title, let rows, let empty):
            VStack(alignment: .leading, spacing: 10) {
                Text(title).font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                if rows.isEmpty { Text(empty).font(.footnote).foregroundColor(Palette.label2) }
                ForEach(Array(rows.enumerated()), id: \.offset) { _, r in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(r.title).font(.subheadline.weight(.medium))
                            if !r.detail.isEmpty { Text(r.detail).font(.caption).foregroundColor(Palette.label2) }
                        }
                        Spacer(minLength: 8)
                        Text(r.trailing).font(.subheadline.weight(.bold)).multilineTextAlignment(.trailing)
                    }
                }
            }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
        case .note(let text):
            Text(text).font(.footnote).foregroundColor(Palette.label2).frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 20)
        }
    }

    private func load() async {
        guard let api = session.api else { return }
        let range = preset.range()
        let period = periods[periodIndex].0
        let y = year
        await loader.load { try await api.reportDocument(kind, year: y, range: range, period: period) }
    }
}

/// Shown for a report the app does not know.
@MainActor struct ReportUnavailableView: View {
    let title: String
    var body: some View {
        EmptyStateView(title: "\(title) isn't available in the iOS app yet. Open it in the web app or the Android app.", systemImage: "chart.bar")
            .navigationTitle(title).navigationBarTitleDisplayMode(.inline)
    }
}

@MainActor struct OverviewReportView: View {
    @EnvironmentObject var session: Session
    @StateObject private var loader = Loader<OverviewReport>()

    var body: some View {
        LoadView(loader: loader, retry: load) { r in
            ScrollView {
                VStack(spacing: 12) {
                    if let h = r.avgResolutionHours {
                        HStack { Text("Average resolution"); Spacer(); Text(String(format: "%.1f h", h)).fontWeight(.semibold) }.card().padding(.horizontal, 16)
                    }
                    group("By priority", r.byPriority.map { ($0.priority ?? "None", $0.count) })
                    group("By status", r.byStatus.map { ($0.status ?? "None", $0.count) })
                    group("By category", r.byCategory.map { ($0.category ?? "None", $0.count) })
                }.padding(.vertical, 8)
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Overview").navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func group(_ title: String, _ rows: [(String, Int)]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(title).font(.caption.weight(.semibold)).foregroundColor(Palette.label2).padding(.bottom, 4)
            ForEach(Array(rows.enumerated()), id: \.offset) { _, r in KeyValueRow(key: r.0, value: "\(r.1)") }
        }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
    }

    private func load() async {
        guard let api = session.api else { return }
        await loader.load { try await api.overviewReport() }
    }
}

// MARK: Profile

@MainActor struct ProfileView: View {
    @EnvironmentObject var session: Session
    @State private var name = ""
    @State private var email = ""
    @State private var saving = false
    @State private var message: String?
    @State private var failed = false
    @State private var showPassword = false
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView {
            VStack(spacing: 12) {
                VStack(spacing: 4) {
                    Text(Initials.letter(name)).font(.largeTitle.weight(.semibold)).frame(width: 64, height: 64)
                        .background(Palette.brand, in: Circle()).foregroundColor(Palette.onBrand)
                    Text(name).font(.title3.weight(.semibold)).padding(.top, 6)
                    Text(email).font(.footnote).opacity(0.75)
                }.frame(maxWidth: .infinity).padding(.vertical, 20).background(Palette.container).foregroundColor(Palette.onContainer)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Account").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                    TextField("Full Name", text: $name).padding(12).background(Palette.fill, in: RoundedRectangle(cornerRadius: 10))
                    TextField("Email", text: $email).keyboardType(.emailAddress).textInputAutocapitalization(.never)
                        .padding(12).background(Palette.fill, in: RoundedRectangle(cornerRadius: 10))
                    if let message { if failed { InlineError(message: message) } else { Text(message).font(.footnote).foregroundColor(Palette.brand) } }
                    Button { save() } label: { if saving { ProgressView().tint(Palette.onBrand) } else { Text("Save Profile") } }
                        .buttonStyle(PrimaryButtonStyle()).disabled(saving || name.isEmpty || email.isEmpty)
                }.card().padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 0) {
                    Text("Security & Server").font(.caption.weight(.semibold)).foregroundColor(Palette.label2).padding(16)
                    settingRow("touchid", "Biometric Lock", "Lock after 5 min in background") {
                        Toggle("", isOn: Binding(get: { session.biometricLock }, set: { setLock($0) })).labelsHidden().tint(Palette.brand)
                    }
                    settingRow("lock", "Change Password", "Update your account password") {
                        Button { showPassword = true } label: { Image(systemName: "chevron.right").foregroundColor(Palette.label3) }
                    }
                    settingRow("server.rack", "Server", session.serverDisplay) {
                        Button("Change") { Task { await session.changeServer() } }.font(.subheadline)
                    }
                    settingRow("bell.badge", "Notifications", "New-notification alerts and badge; manage in Settings") {
                        Button { if let u = URL(string: UIApplication.openSettingsURLString) { openURL(u) } } label: {
                            Image(systemName: "arrow.up.forward.square").foregroundColor(Palette.label3)
                        }
                    }
                }.background(Palette.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous)).padding(.horizontal, 16)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Appearance").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                    Picker("Theme", selection: $session.themeMode) { ForEach(ThemeMode.allCases) { Text($0.title).tag($0) } }
                        .pickerStyle(.segmented)
                }.card().padding(.horizontal, 16)

                Button(role: .destructive) { Task { await session.signOut() } } label: {
                    Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right").frame(maxWidth: .infinity)
                }.buttonStyle(.bordered).padding(.horizontal, 16)

                Text(appVersion).font(.footnote).foregroundColor(Palette.label3).padding(.top, 8)
            }.padding(.bottom, 20)
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Profile").navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showPassword) { ChangePasswordSheet(name: name, email: email).presentationDetents([.medium]) }
        .onAppear { if let p = session.profile { name = p.name; email = p.email } }
    }

    private var appVersion: String {
        let v = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        let b = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
        return "RivetMSP \(v) (\(b))"
    }

    private func settingRow<Trailing: View>(_ symbol: String, _ title: String, _ subtitle: String, @ViewBuilder trailing: () -> Trailing) -> some View {
        HStack(spacing: 12) {
            IconTile(systemName: symbol, size: 36)
            VStack(alignment: .leading, spacing: 1) {
                Text(title).font(.subheadline.weight(.semibold))
                Text(subtitle).font(.caption).foregroundColor(Palette.label2).lineLimit(1)
            }
            Spacer()
            trailing()
        }.padding(.horizontal, 16).padding(.vertical, 8)
    }

    /// Turning the lock on proves the device can authenticate first, so the user cannot lock themselves out.
    private func setLock(_ on: Bool) {
        guard on else { session.biometricLock = false; return }
        let ctx = LAContext(); var err: NSError?
        guard ctx.canEvaluatePolicy(.deviceOwnerAuthentication, error: &err) else {
            failed = true; message = "Set up a passcode, Face ID or Touch ID on this device first."; return
        }
        ctx.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Turn on the RivetMSP app lock") { ok, _ in
            DispatchQueue.main.async { if ok { session.biometricLock = true } }
        }
    }

    private func save() {
        guard let api = session.api, !saving else { return }
        saving = true; message = nil
        Task {
            defer { saving = false }
            do {
                try await api.updateProfile(UpdateProfileRequest(name: name.trimmingCharacters(in: .whitespaces), email: email.trimmingCharacters(in: .whitespaces)))
                await session.refreshProfile(); failed = false; message = "Profile saved."
            } catch { failed = true; message = userMessage(for: error) }
        }
    }
}

@MainActor struct ChangePasswordSheet: View {
    let name: String
    let email: String
    @EnvironmentObject var session: Session
    @Environment(\.dismiss) private var dismiss
    @State private var current = ""
    @State private var new = ""
    @State private var confirm = ""
    @State private var saving = false
    @State private var error: String?

    private var valid: Bool { !current.isEmpty && new.count >= 8 && new == confirm }

    var body: some View {
        NavigationStack {
            Form {
                SecureField("Current password", text: $current).textContentType(.password)
                SecureField("New password (8+ characters)", text: $new).textContentType(.newPassword)
                SecureField("Confirm new password", text: $confirm).textContentType(.newPassword)
                if !confirm.isEmpty && new != confirm { InlineError(message: "The passwords do not match.") }
                if let error { InlineError(message: error) }
            }
            .navigationTitle("Change Password").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() }.disabled(saving) }
                ToolbarItem(placement: .confirmationAction) {
                    Button { save() } label: { if saving { ProgressView() } else { Text("Save") } }.disabled(!valid || saving)
                }
            }
        }
    }

    private func save() {
        guard let api = session.api else { return }
        saving = true; error = nil
        Task {
            defer { saving = false }
            do { try await api.updateProfile(UpdateProfileRequest(name: name, email: email, currentPassword: current, newPassword: new)); dismiss() }
            catch { self.error = userMessage(for: error) }
        }
    }
}

// MARK: Knowledge base

@MainActor struct KnowledgeBaseView: View {
    @EnvironmentObject var session: Session
    @StateObject private var loader = PagedLoader<KbArticleSummary>()
    @State private var search = ""
    @State private var categories: [KbCategory] = []
    @State private var categoryIndex = 0
    private struct Key: Equatable { var search: String; var category: Int }

    var body: some View {
        VStack(spacing: 0) {
            SearchField(placeholder: "Search articles…", text: $search).padding(.horizontal, 16).padding(.vertical, 8)
            FilterChips(items: ["All"] + categories.map(\.name), selection: $categoryIndex).padding(.bottom, 8)
            let state = loader.state
            if let error = state.error, state.items.isEmpty { ErrorStateView(message: error, retry: reload) }
            else if state.items.isEmpty && state.isRefreshing { LoadingStateView() }
            else if state.items.isEmpty { EmptyStateView(title: "No articles found", systemImage: "book") }
            else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(state.items) { a in
                            NavigationLink(value: AppRoute.kbArticle(a.id)) {
                                ListRowCard {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(a.title).font(.subheadline.weight(.semibold)).multilineTextAlignment(.leading)
                                        HStack(spacing: 8) {
                                            if let c = a.categoryName { Pill(text: c, small: true) }
                                            Text(RivetDate.display(a.updatedAt)).font(.caption).foregroundColor(Palette.label2)
                                        }
                                    }
                                    Spacer()
                                }
                            }.buttonStyle(.plain)
                            .task { await loader.loadMoreIfNeeded(current: a, fetchPage) }
                        }
                        if state.isLoadingMore { ProgressView().padding() }
                    }
                }.refreshable { await reload() }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Knowledge Base").navigationBarTitleDisplayMode(.inline)
        .task { categories = (try? await session.api?.kbCategories()) ?? [] }
        .task(id: Key(search: search, category: categoryIndex)) {
            if !search.isEmpty { try? await Task.sleep(nanoseconds: 350_000_000) }
            if Task.isCancelled { return }
            await reload()
        }
    }

    private func fetchPage(_ page: Int) async throws -> Paged<KbArticleSummary> {
        let cat = categoryIndex > 0 && categoryIndex <= categories.count ? categories[categoryIndex - 1].id : nil
        return try await session.requireAPI().kbArticles(categoryId: cat, search: search, page: page)
    }
    private func reload() async { await loader.reload(fetchPage) }
}

@MainActor struct KbArticleView: View {
    let id: Int
    @EnvironmentObject var session: Session
    @Environment(\.openURL) private var openURL
    @StateObject private var loader = Loader<KbArticleDetail>()

    var body: some View {
        LoadView(loader: loader, retry: load) { a in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    if let c = a.categoryName { Pill(text: c, small: true) }
                    Text(a.title).font(.title2.weight(.bold))
                    // Article HTML is shown as text; embedded images and rich formatting are not rendered yet.
                    Text(HTMLText.plain(a.content ?? "")).font(.body)
                    if let files = a.attachments, !files.isEmpty {
                        Divider()
                        Text("Attachments").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                        ForEach(files) { f in Button { open(f.url) } label: { Label(f.name, systemImage: "paperclip") } }
                    }
                }.padding(20).frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle(loader.value?.title ?? "Article").navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    /// Only http(s) links are opened, the same rule the Android app applies to KB attachments.
    private func open(_ raw: String) {
        guard let base = session.serverURL, let url = URL(string: raw, relativeTo: base)?.absoluteURL,
              ["http", "https"].contains(url.scheme?.lowercased() ?? "") else { return }
        openURL(url)
    }

    private func load() async {
        guard let api = session.api else { return }
        await loader.load { try await api.kbArticle(id) }
    }
}

// MARK: Credentials

/// Credentials sit behind Face ID / Touch ID / passcode. Names and URLs are listed; opening one proves a fresh
/// biometric unlock to the server (see `CredentialDetailView`) before any secret is sent.
@MainActor struct CredentialsView: View {
    @EnvironmentObject var session: Session
    @EnvironmentObject var router: Router
    @StateObject private var loader = PagedLoader<CredentialSummary>()
    @State private var unlocked = false
    @State private var gateMessage: String?
    @State private var search = ""

    var body: some View {
        Group {
            if unlocked { list } else { gate }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Credentials").navigationBarTitleDisplayMode(.inline)
    }

    private var gate: some View {
        VStack(spacing: 14) {
            Image(systemName: "faceid").font(.system(size: 80, weight: .light)).foregroundColor(Palette.brand)
            Text("Authentication Required").font(.headline)
            Text("Verify your identity to view credentials").foregroundColor(Palette.label2)
            if let gateMessage { InlineError(message: gateMessage).padding(.horizontal, 32) }
            Button { authenticate() } label: { Label("Authenticate", systemImage: "faceid") }
                .buttonStyle(PrimaryButtonStyle()).frame(maxWidth: 240).padding(.top, 8)
        }.frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var list: some View {
        VStack(spacing: 0) {
            SearchField(placeholder: "Search credentials…", text: $search).padding(.horizontal, 16).padding(.vertical, 8)
            let state = loader.state
            if let error = state.error, state.items.isEmpty { ErrorStateView(message: error, retry: reload) }
            else if state.items.isEmpty && state.isRefreshing { LoadingStateView() }
            else if state.items.isEmpty { EmptyStateView(title: "No credentials found", systemImage: "key") }
            else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(state.items) { c in
                            Button { router.push(.credential(c.id)) } label: {
                                ListRowCard {
                                    IconTile(systemName: "key")
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(c.name).font(.subheadline.weight(.semibold))
                                        Text([c.client, c.uri].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " • "))
                                            .font(.caption).foregroundColor(Palette.label2).lineLimit(1)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundColor(Palette.label3)
                                }
                            }
                            .buttonStyle(.plain)
                            .accessibilityHint("Opens this credential after you verify your identity")
                            .task { await loader.loadMoreIfNeeded(current: c, fetchPage) }
                        }
                    }
                }.refreshable { await reload() }
            }
        }
        .task(id: search) {
            if !search.isEmpty { try? await Task.sleep(nanoseconds: 350_000_000) }
            if Task.isCancelled { return }
            await reload()
        }
    }

    private func fetchPage(_ page: Int) async throws -> Paged<CredentialSummary> {
        try await session.requireAPI().credentials(search: search, page: page)
    }
    private func reload() async { await loader.reload(fetchPage) }

    private func authenticate() {
        let ctx = LAContext(); var err: NSError?
        guard ctx.canEvaluatePolicy(.deviceOwnerAuthentication, error: &err) else {
            gateMessage = "No Face ID, Touch ID or passcode is set up on this device. Set one up in Settings to view credentials."; return
        }
        gateMessage = nil
        ctx.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Access credentials") { ok, error in
            DispatchQueue.main.async {
                if ok { unlocked = true }
                else if let e = error as? LAError, e.code != .userCancel, e.code != .appCancel { gateMessage = e.localizedDescription }
            }
        }
    }
}

// MARK: Search

@MainActor struct SearchView: View {
    @EnvironmentObject var session: Session
    @State private var query = ""
    @State private var result: SearchResult?
    @State private var loading = false
    @State private var error: String?
    @FocusState private var focused: Bool

    var body: some View {
        VStack(spacing: 0) {
            SearchField(placeholder: "Search…", text: $query).focused($focused).padding(.horizontal, 16).padding(.vertical, 8)
            if query.trimmingCharacters(in: .whitespaces).count < 2 {
                EmptyStateView(title: "Type at least 2 characters to search", systemImage: "magnifyingglass")
            } else if let error { ErrorStateView(message: error, retry: nil) }
            else if loading && result == nil { LoadingStateView() }
            else if let r = result {
                if isEmpty(r) { EmptyStateView(title: "No results", systemImage: "magnifyingglass") }
                else {
                    ScrollView { LazyVStack(alignment: .leading, spacing: 0) { results(r) }.padding(.bottom, 20) }
                }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Search").navigationBarTitleDisplayMode(.inline)
        .onAppear { focused = true }
        .task(id: query) {
            let q = query.trimmingCharacters(in: .whitespaces)
            guard q.count >= 2 else { result = nil; error = nil; return }
            try? await Task.sleep(nanoseconds: 400_000_000)
            if Task.isCancelled { return }
            loading = true; error = nil
            do { result = try await session.requireAPI().search(q) } catch { self.error = userMessage(for: error) }
            loading = false
        }
    }

    private func isEmpty(_ r: SearchResult) -> Bool {
        r.tickets.isEmpty && r.clients.isEmpty && r.assets.isEmpty && r.contacts.isEmpty && r.credentials.isEmpty && r.articles.isEmpty
    }

    @ViewBuilder private func results(_ r: SearchResult) -> some View {
        if !r.tickets.isEmpty {
            SectionLabel(text: "Tickets")
            ForEach(r.tickets) { t in NavigationLink(value: AppRoute.ticket(t.id)) { TicketRow(ticket: t) }.buttonStyle(.plain) }
        }
        if !r.clients.isEmpty {
            SectionLabel(text: "Clients")
            ForEach(r.clients) { c in
                NavigationLink(value: AppRoute.client(c.id)) { ListRowCard { AvatarCircle(letter: Initials.letter(c.name), size: 38); Text(c.name).font(.subheadline.weight(.semibold)); Spacer() } }.buttonStyle(.plain)
            }
        }
        if !r.assets.isEmpty {
            SectionLabel(text: "Assets")
            ForEach(r.assets) { a in NavigationLink(value: AppRoute.asset(a.id)) { AssetRow(asset: a) }.buttonStyle(.plain) }
        }
        if !r.contacts.isEmpty {
            SectionLabel(text: "Contacts")
            ForEach(r.contacts) { c in
                ListRowCard { AvatarCircle(letter: Initials.letter(c.name), size: 38)
                    VStack(alignment: .leading) { Text(c.name).font(.subheadline.weight(.semibold)); if let e = c.email { Text(e).font(.caption).foregroundColor(Palette.label2) } }; Spacer() }
            }
        }
        if !r.articles.isEmpty {
            SectionLabel(text: "Knowledge base")
            ForEach(r.articles) { a in NavigationLink(value: AppRoute.kbArticle(a.id)) { ListRowCard { Image(systemName: "book"); Text(a.title).font(.subheadline); Spacer() } }.buttonStyle(.plain) }
        }
    }
}
