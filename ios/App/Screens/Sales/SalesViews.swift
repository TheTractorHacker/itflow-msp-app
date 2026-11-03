import SwiftUI
import PhotosUI
import UIKit
import RivetCore

// RivetMSP-only modules: quotes, invoices and expenses (ports of QuotesScreen, InvoicesScreen, ExpensesScreen and their details).

extension FinancialTone {
    var pillTone: PillTone {
        switch self {
        case .success: return .cyan
        case .critical: return .danger
        case .warning: return .custom(Palette.priorityMedium)
        case .neutral: return .gray
        }
    }
}

// MARK: Quotes

@MainActor struct QuotesView: View {
    @EnvironmentObject var session: Session
    @StateObject private var loader = PagedLoader<QuoteSummary>()

    var body: some View {
        let state = loader.state
        Group {
            if let error = state.error, state.items.isEmpty { ErrorStateView(message: error, retry: reload) }
            else if state.items.isEmpty && state.isRefreshing { LoadingStateView() }
            else if state.items.isEmpty { EmptyStateView(title: "No quotes found", systemImage: "doc.text.magnifyingglass") }
            else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(state.items) { q in
                            NavigationLink(value: AppRoute.quote(q.id)) {
                                ListRowCard {
                                    IconTile(systemName: "doc.plaintext")
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(q.subject?.isEmpty == false ? q.subject! : (q.number.map { "Quote #\($0)" } ?? "Quote"))
                                            .font(.subheadline.weight(.semibold)).multilineTextAlignment(.leading)
                                        Text([q.client, q.date.map(RivetDate.dateOnly)].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " • "))
                                            .font(.caption).foregroundColor(Palette.label2).multilineTextAlignment(.leading)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 4) {
                                        Text(Money.format(q.total)).font(.subheadline.weight(.bold))
                                        if let s = q.status { Pill(text: s, tone: FinancialTone(s).pillTone, small: true) }
                                    }
                                }
                            }.buttonStyle(.plain)
                            .task { await loader.loadMoreIfNeeded(current: q, fetchPage) }
                        }
                        if state.isLoadingMore { ProgressView().padding() }
                    }
                }.refreshable { await reload() }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Quotes").navigationBarTitleDisplayMode(.inline)
        .task { await reload() }
    }

    private func fetchPage(_ page: Int) async throws -> Paged<QuoteSummary> { try await session.requireAPI().quotes(page: page) }
    private func reload() async { await loader.reload(fetchPage) }
}

/// A line-item table with the subtotal / tax / total footer, shared by quotes and invoices.
@MainActor struct LineItemsCard: View {
    let items: [LineItem]
    let subtotal: Double?
    let tax: Double?
    let total: Double?
    var balance: Double?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Line Items").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
            if items.isEmpty { Text("No line items.").font(.footnote).foregroundColor(Palette.label2) }
            ForEach(items) { item in
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.description ?? "").font(.subheadline.weight(.medium))
                        Text("\(Self.qty(item.quantity)) × \(Money.format(item.unitPrice))").font(.caption).foregroundColor(Palette.label2)
                    }
                    Spacer()
                    Text(Money.format(item.total)).font(.subheadline.weight(.bold))
                }
            }
            Divider()
            totalRow("Subtotal", subtotal, bold: false)
            totalRow("Tax", tax, bold: false)
            totalRow("Total", total, bold: true)
            if let balance { totalRow("Balance due", balance, bold: true) }
        }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
    }

    private func totalRow(_ label: String, _ value: Double?, bold: Bool) -> some View {
        HStack { Text(label); Spacer(); Text(Money.format(value)) }
            .font(.subheadline.weight(bold ? .bold : .regular))
    }

    static func qty(_ q: Double?) -> String {
        guard let q else { return "0" }
        return q == q.rounded() ? String(Int(q)) : String(q)
    }
}

@MainActor struct QuoteDetailView: View {
    let id: Int
    @EnvironmentObject var session: Session
    @Environment(\.openURL) private var openURL
    @StateObject private var loader = Loader<QuoteDetail>()

    var body: some View {
        LoadView(loader: loader, retry: load) { q in
            ScrollView {
                VStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(q.heading).font(.title2.weight(.bold))
                            Spacer()
                            if let url = session.serverURL.flatMap({ guestDocumentURL(server: $0, path: q.guestUrl) }) {
                                Button { openURL(url) } label: { Image(systemName: "safari") }.accessibilityLabel("Open in browser")
                            }
                        }
                        if let c = q.client, !c.isEmpty { Text(c).foregroundColor(Palette.brand) }
                        if let n = q.contactName, !n.isEmpty { Text(n).font(.subheadline).foregroundColor(Palette.label2) }
                        HStack(spacing: 8) {
                            if let d = q.date { Pill(text: RivetDate.dateOnly(d)) }
                            if let s = q.status { Pill(text: s, tone: FinancialTone(s).pillTone) }
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
                    LineItemsCard(items: q.items, subtotal: q.subtotal, tax: q.tax, total: q.total)
                    if let n = q.notes, !n.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Notes").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                            Text(HTMLText.plain(n))
                        }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
                    }
                }.padding(.vertical, 8)
            }.refreshable { await load() }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle(loader.value?.heading ?? "Quote").navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        guard let api = session.api else { return }
        await loader.load { try await api.quote(id) }
    }
}

// MARK: Invoices

@MainActor struct InvoicesView: View {
    @EnvironmentObject var session: Session
    @StateObject private var loader = PagedLoader<InvoiceSummary>()

    var body: some View {
        let state = loader.state
        Group {
            if let error = state.error, state.items.isEmpty { ErrorStateView(message: error, retry: reload) }
            else if state.items.isEmpty && state.isRefreshing { LoadingStateView() }
            else if state.items.isEmpty { EmptyStateView(title: "No invoices found", systemImage: "doc.text.magnifyingglass") }
            else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(state.items) { i in
                            NavigationLink(value: AppRoute.invoice(i.id)) {
                                ListRowCard {
                                    IconTile(systemName: "doc.richtext")
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(i.number.map { "Invoice #\($0)" } ?? "Invoice").font(.subheadline.weight(.semibold))
                                        Text([i.client, i.dueDate.map { "Due \(RivetDate.dateOnly($0))" }].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " • "))
                                            .font(.caption).foregroundColor(Palette.label2).multilineTextAlignment(.leading)
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 4) {
                                        Text(Money.format(i.total)).font(.subheadline.weight(.bold))
                                        if let s = i.status { Pill(text: s, tone: FinancialTone(s).pillTone, small: true) }
                                    }
                                }
                            }.buttonStyle(.plain)
                            .task { await loader.loadMoreIfNeeded(current: i, fetchPage) }
                        }
                        if state.isLoadingMore { ProgressView().padding() }
                    }
                }.refreshable { await reload() }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Invoices").navigationBarTitleDisplayMode(.inline)
        .task { await reload() }
    }

    private func fetchPage(_ page: Int) async throws -> Paged<InvoiceSummary> { try await session.requireAPI().invoices(page: page) }
    private func reload() async { await loader.reload(fetchPage) }
}

@MainActor struct InvoiceDetailView: View {
    let id: Int
    @EnvironmentObject var session: Session
    @Environment(\.openURL) private var openURL
    @StateObject private var loader = Loader<InvoiceDetail>()

    var body: some View {
        LoadView(loader: loader, retry: load) { i in
            ScrollView {
                VStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack(alignment: .top) {
                            Text(i.heading).font(.title2.weight(.bold))
                            Spacer()
                            if let url = session.serverURL.flatMap({ guestDocumentURL(server: $0, path: i.guestUrl) }) {
                                Button { openURL(url) } label: { Image(systemName: "safari") }.accessibilityLabel("Open in browser")
                            }
                        }
                        if let c = i.client, !c.isEmpty { Text(c).foregroundColor(Palette.brand) }
                        if let n = i.contactName, !n.isEmpty { Text(n).font(.subheadline).foregroundColor(Palette.label2) }
                        HStack(spacing: 8) {
                            if let d = i.date { Pill(text: RivetDate.dateOnly(d)) }
                            if let due = i.dueDate { Pill(text: "Due \(RivetDate.dateOnly(due))") }
                            if let s = i.status { Pill(text: s, tone: FinancialTone(s).pillTone) }
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
                    LineItemsCard(items: i.items, subtotal: i.subtotal, tax: i.tax, total: i.total, balance: i.balance)
                    if let n = i.notes, !n.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Notes").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                            Text(HTMLText.plain(n))
                        }.frame(maxWidth: .infinity, alignment: .leading).card().padding(.horizontal, 16)
                    }
                }.padding(.vertical, 8)
            }.refreshable { await load() }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle(loader.value?.heading ?? "Invoice").navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        guard let api = session.api else { return }
        await loader.load { try await api.invoice(id) }
    }
}

// MARK: Expenses

@MainActor struct ExpensesView: View {
    @EnvironmentObject var session: Session
    @EnvironmentObject var router: Router
    @StateObject private var loader = PagedLoader<ExpenseSummary>()

    var body: some View {
        let state = loader.state
        ZStack(alignment: .bottomTrailing) {
            Group {
                if let error = state.error, state.items.isEmpty { ErrorStateView(message: error, retry: reload) }
                else if state.items.isEmpty && state.isRefreshing { LoadingStateView() }
                else if state.items.isEmpty { EmptyStateView(title: "No expenses found", systemImage: "creditcard") }
                else {
                    ScrollView {
                        LazyVStack(spacing: 0) {
                            ForEach(state.items) { e in
                                ListRowCard {
                                    IconTile(systemName: e.hasReceipt ? "receipt" : "creditcard")
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(e.description ?? "Expense").font(.subheadline.weight(.semibold)).multilineTextAlignment(.leading)
                                        Text([e.date.map(RivetDate.dateOnly), e.client, e.paymentMethod].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " • "))
                                            .font(.caption).foregroundColor(Palette.label2).multilineTextAlignment(.leading)
                                    }
                                    Spacer()
                                    Text(Money.format(e.amount, currency: e.currency ?? "USD")).font(.subheadline.weight(.bold))
                                }
                                .task { await loader.loadMoreIfNeeded(current: e, fetchPage) }
                            }
                            if state.isLoadingMore { ProgressView().padding() }
                        }.padding(.bottom, 80)
                    }.refreshable { await reload() }
                }
            }
            if session.capabilities.canWrite(Capabilities.financial) {
                FloatingButton(systemName: "plus", label: "Add expense") { router.push(.addExpense) }
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Expenses").navigationBarTitleDisplayMode(.inline)
        .task { await reload() }
    }

    private func fetchPage(_ page: Int) async throws -> Paged<ExpenseSummary> { try await session.requireAPI().expenses(page: page) }
    private func reload() async { await loader.reload(fetchPage) }
}

@MainActor struct AddExpenseView: View {
    @EnvironmentObject var session: Session
    @EnvironmentObject var router: Router
    @State private var draft = ExpenseDraft(date: AddExpenseView.today())
    @State private var receipt: ReplyFile?
    @State private var receiptPreview: UIImage?
    @State private var pickerItem: PhotosPickerItem?
    @State private var showCamera = false
    @State private var saving = false
    @State private var error: String?

    private static func today() -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Details").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                    field("Description", text: $draft.description)
                    HStack(spacing: 12) {
                        field("Amount", text: $draft.amount, keyboard: .decimalPad)
                        field("Date (YYYY-MM-DD)", text: $draft.date, keyboard: .numbersAndPunctuation)
                    }
                    field("Payment method", text: $draft.paymentMethod)
                    field("Reference", text: $draft.reference)
                }.card()

                VStack(alignment: .leading, spacing: 10) {
                    Text("Receipt").font(.caption.weight(.semibold)).foregroundColor(Palette.label2)
                    if let img = receiptPreview {
                        Image(uiImage: img).resizable().scaledToFill().frame(height: 160).frame(maxWidth: .infinity)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous)).accessibilityLabel("Receipt photo")
                        Button(role: .destructive) { receipt = nil; receiptPreview = nil; pickerItem = nil } label: {
                            Label("Remove", systemImage: "trash").frame(maxWidth: .infinity)
                        }.buttonStyle(OutlineButtonStyle())
                    } else {
                        HStack(spacing: 12) {
                            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                                Button { showCamera = true } label: { Label("Camera", systemImage: "camera").frame(maxWidth: .infinity) }
                                    .buttonStyle(OutlineButtonStyle())
                            }
                            PhotosPicker(selection: $pickerItem, matching: .images) {
                                Label("Photos", systemImage: "photo").frame(maxWidth: .infinity)
                            }.buttonStyle(OutlineButtonStyle())
                        }
                    }
                }.card()

                if let error { InlineError(message: error) }
                Button { Task { await save() } } label: {
                    HStack { if saving { ProgressView().tint(Palette.onBrand) }; Text("Save expense") }.frame(maxWidth: .infinity)
                }.buttonStyle(PrimaryButtonStyle()).disabled(saving)
            }.padding(16)
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Add Expense").navigationBarTitleDisplayMode(.inline)
        .onChange(of: pickerItem) { item in
            guard let item else { return }
            Task {
                guard let data = try? await item.loadTransferable(type: Data.self) else { error = "Couldn't read that photo."; return }
                setReceipt(data)
            }
        }
        .sheet(isPresented: $showCamera) {
            CameraPicker { image in
                if let data = image.jpegData(compressionQuality: 0.8) { setReceipt(data) }
            }.ignoresSafeArea()
        }
    }

    private func field(_ title: String, text: Binding<String>, keyboard: UIKeyboardType = .default) -> some View {
        TextField(title, text: text).keyboardType(keyboard).textFieldStyle(.roundedBorder)
    }

    /// Photos are re-encoded as JPEG (also strips location metadata) and must fit the server's upload rules.
    private func setReceipt(_ data: Data) {
        guard let image = UIImage(data: data), let jpeg = image.jpegData(compressionQuality: 0.8) else { error = "That file isn't a photo."; return }
        guard jpeg.count <= AttachmentRules.maxBytesPerReply else { error = "That photo is too large."; return }
        receipt = ReplyFile(name: "receipt.jpg", mimeType: "image/jpeg", data: jpeg)
        receiptPreview = image; error = nil
    }

    private func save() async {
        guard !saving else { return }
        switch draft.problem {
        case .missing: error = "Description and amount required"; return
        case .invalidAmount: error = "Enter the amount as a positive number"; return
        case .invalidDate: error = "Enter the date as YYYY-MM-DD"; return
        case nil: break
        }
        saving = true; error = nil
        defer { saving = false }
        do {
            try await session.requireAPI().createExpense(draft, receipt: receipt)
            router.pop()
        } catch {
            self.error = userMessage(for: error)
        }
    }
}

/// Minimal camera capture (the Android "Camera" button). Only offered when a camera exists.
struct CameraPicker: UIViewControllerRepresentable {
    var onImage: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let p = UIImagePickerController(); p.sourceType = .camera; p.delegate = context.coordinator; return p
    }
    func updateUIViewController(_ vc: UIImagePickerController, context: Context) {}
    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPicker
        init(_ parent: CameraPicker) { self.parent = parent }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let image = info[.originalImage] as? UIImage { parent.onImage(image) }
            parent.dismiss()
        }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) { parent.dismiss() }
    }
}
