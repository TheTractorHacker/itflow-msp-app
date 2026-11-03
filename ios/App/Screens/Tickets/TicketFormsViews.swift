import SwiftUI
import RivetCore

// Ticket charges, worksheets and outtake forms (ports of ChargesCard, WorksheetsCard, FillWorksheetScreen, OuttakeSignScreen).

/// Charges on a ticket with an inline "add charge" panel. A failed load says so (it never shows "no charges").
@MainActor struct ChargesCard: View {
    let ticketId: Int
    @EnvironmentObject var session: Session
    @State private var response: ChargesResponse?
    @State private var loadError: String?
    @State private var adding = false
    @State private var name = ""
    @State private var details = ""
    @State private var qty = "1"
    @State private var price = ""
    @State private var products: [Product] = []
    @State private var saving = false
    @State private var formError: String?

    private var caps: Capabilities { session.capabilities }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Charges").font(.subheadline.weight(.semibold))
                Spacer()
                if let r = response, !r.charges.isEmpty { Text(Money.format(r.total)).font(.subheadline.weight(.bold)).foregroundColor(Palette.brand) }
                if caps.canAddCharge {
                    Button { adding.toggle(); if adding { Task { await loadProducts() } } } label: {
                        Image(systemName: adding ? "xmark" : "plus").frame(width: 32, height: 32)
                            .background(Palette.container, in: Circle()).foregroundColor(Palette.onContainer)
                    }.accessibilityLabel(adding ? "Close add charge" : "Add charge")
                }
            }
            if let loadError {
                InlineError(message: "Charges couldn't be loaded: \(loadError)")
                Button("Retry") { Task { await load() } }.font(.footnote.weight(.semibold))
            } else if let r = response {
                if r.charges.isEmpty { Text("No charges on this ticket.").font(.caption).foregroundColor(Palette.label2) }
                ForEach(r.charges) { c in
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(c.name).font(.subheadline.weight(.medium))
                            Text("\(LineItemsCard.qty(c.quantity)) × \(Money.format(c.unitPrice))" + (c.invoiced ? " • Invoiced" : ""))
                                .font(.caption).foregroundColor(Palette.label2)
                            if let d = c.description, !d.isEmpty { Text(d).font(.caption).foregroundColor(Palette.label2) }
                        }
                        Spacer()
                        Text(Money.format(c.total)).font(.subheadline.weight(.bold))
                    }
                }
            } else { ProgressView() }

            if adding { addPanel }
        }
        .frame(maxWidth: .infinity, alignment: .leading).card()
        .onAppear { Task { await load() } }     // also refreshes when returning from a pushed screen
    }

    private var addPanel: some View {
        VStack(alignment: .leading, spacing: 8) {
            Divider()
            TextField("Name", text: $name).textFieldStyle(.roundedBorder)
            if !products.isEmpty && name.count < 40 {
                let matches = products.filter { name.isEmpty || $0.name.localizedCaseInsensitiveContains(name) }.prefix(4)
                ForEach(Array(matches)) { p in
                    Button { name = p.name; details = p.description.map(HTMLText.plain) ?? details; price = String(p.price) } label: {
                        HStack { Text(p.name).lineLimit(1); Spacer(); Text(Money.format(p.price)).foregroundColor(Palette.label2) }.font(.footnote)
                    }.buttonStyle(.plain)
                }
            }
            TextField("Description (optional)", text: $details).textFieldStyle(.roundedBorder)
            HStack {
                TextField("Qty", text: $qty).keyboardType(.decimalPad).textFieldStyle(.roundedBorder)
                TextField("Unit price", text: $price).keyboardType(.decimalPad).textFieldStyle(.roundedBorder)
            }
            if let formError { InlineError(message: formError) }
            Button { Task { await save() } } label: {
                HStack { if saving { ProgressView().tint(Palette.onBrand) }; Text("Add charge") }.frame(maxWidth: .infinity)
            }.buttonStyle(PrimaryButtonStyle()).disabled(saving)
        }
    }

    private func load() async {
        guard let api = session.api else { return }
        do { response = try await api.charges(ticketId: ticketId); loadError = nil }
        catch is CancellationError { return }
        catch { loadError = userMessage(for: error) }
    }

    private func loadProducts() async {
        guard products.isEmpty, let api = session.api else { return }
        products = (try? await api.products()) ?? []      // suggestions only: a failure just means none are offered
    }

    private func save() async {
        guard !saving else { return }
        guard !name.trimmingCharacters(in: .whitespaces).isEmpty else { formError = "Enter a name"; return }
        guard let q = ChargeDraft.quantity(qty) else { formError = "Quantity must be more than zero"; return }
        guard let p = ChargeDraft.price(price) else { formError = "Enter the unit price"; return }
        saving = true; formError = nil
        defer { saving = false }
        do {
            try await session.requireAPI().addCharge(ticketId: ticketId, AddChargeRequest(
                name: name.trimmingCharacters(in: .whitespaces), description: details, quantity: q, unitPrice: p))
            name = ""; details = ""; qty = "1"; price = ""; adding = false
            await load()
        } catch { formError = userMessage(for: error) }
    }
}

/// Worksheets and outtake forms for a ticket: open, create, complete and delete.
@MainActor struct WorksheetsCard: View {
    let ticketId: Int
    @EnvironmentObject var session: Session
    @EnvironmentObject var router: Router
    @State private var worksheets: [WorksheetSummary] = []
    @State private var outtakes: [OuttakeSummary] = []
    @State private var loadError: String?
    @State private var templates: [WorksheetTemplate] = []
    @State private var showTemplates = false
    @State private var busy = false
    @State private var actionError: String?
    @State private var deleteWorksheet: WorksheetSummary?
    @State private var deleteOuttake: OuttakeSummary?

    private var canWrite: Bool { session.capabilities.canWrite(Capabilities.support) }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if let loadError {
                InlineError(message: "Worksheets and forms couldn't be loaded: \(loadError)")
                Button("Retry") { Task { await load() } }.font(.footnote.weight(.semibold))
            }
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Worksheets").font(.subheadline.weight(.semibold)); Spacer()
                    if canWrite { Button { Task { await openTemplates() } } label: { Image(systemName: "plus") }.accessibilityLabel("New worksheet") }
                }
                if worksheets.isEmpty && loadError == nil { Text("No worksheets.").font(.caption).foregroundColor(Palette.label2) }
                ForEach(worksheets) { w in
                    HStack {
                        Button { router.push(.worksheet(w.id)) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(w.templateName ?? "Worksheet #\(w.id)").font(.subheadline.weight(.medium))
                                Text(w.signed ? "Signed by \(w.signedName ?? "")" : (w.completedAt != nil ? "Completed" : "Not started") + (w.createdBy.map { " · by \($0)" } ?? ""))
                                    .font(.caption).foregroundColor(Palette.label2)
                            }.frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
                        }.buttonStyle(.plain)
                        Pill(text: w.completedAt != nil ? "Completed" : "In progress", tone: w.completedAt != nil ? .cyan : .gray, small: true)
                        if canWrite {
                            Button { deleteWorksheet = w } label: { Image(systemName: "trash").foregroundColor(Palette.danger) }.accessibilityLabel("Delete worksheet")
                        }
                    }
                }
            }
            Divider()
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Outtake Forms").font(.subheadline.weight(.semibold)); Spacer()
                    if canWrite { Button { Task { await createOuttake() } } label: { Image(systemName: "plus") }.accessibilityLabel("New outtake form").disabled(busy) }
                }
                Text("The contact signs on pickup").font(.caption).foregroundColor(Palette.label2)
                if outtakes.isEmpty && loadError == nil { Text("No outtake forms.").font(.caption).foregroundColor(Palette.label2) }
                ForEach(outtakes) { o in
                    HStack {
                        Button { router.push(.outtake(o.id)) } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Outtake Form").font(.subheadline.weight(.medium))
                                Text(o.signed ? "Signed by \(o.signedName ?? "")" : "Not signed" + (o.createdBy.map { " · by \($0)" } ?? ""))
                                    .font(.caption).foregroundColor(Palette.label2)
                            }.frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
                        }.buttonStyle(.plain)
                        Pill(text: o.signed ? "Signed" : "Awaiting signature", tone: o.signed ? .cyan : .gray, small: true)
                        if canWrite {
                            Button { deleteOuttake = o } label: { Image(systemName: "trash").foregroundColor(Palette.danger) }.accessibilityLabel("Delete outtake form")
                        }
                    }
                }
            }
            if let actionError { InlineError(message: actionError) }
        }
        .frame(maxWidth: .infinity, alignment: .leading).card()
        .onAppear { Task { await load() } }     // also refreshes after a worksheet or form is filled in
        .confirmationDialog("New worksheet", isPresented: $showTemplates, titleVisibility: .visible) {
            ForEach(templates) { t in Button(t.name) { Task { await createWorksheet(t) } } }
        } message: { Text(templates.isEmpty ? "No worksheet templates are set up." : "Choose a template") }
        .alert("Delete worksheet?", isPresented: Binding(get: { deleteWorksheet != nil }, set: { if !$0 { deleteWorksheet = nil } })) {
            Button("Cancel", role: .cancel) { deleteWorksheet = nil }
            Button("Delete", role: .destructive) { if let w = deleteWorksheet { Task { await remove { try await $0.deleteWorksheet(id: w.id) } } }; deleteWorksheet = nil }
        } message: { Text("\"\(deleteWorksheet?.templateName ?? "Worksheet")\" will be permanently deleted.") }
        .alert("Delete outtake form?", isPresented: Binding(get: { deleteOuttake != nil }, set: { if !$0 { deleteOuttake = nil } })) {
            Button("Cancel", role: .cancel) { deleteOuttake = nil }
            Button("Delete", role: .destructive) { if let o = deleteOuttake { Task { await remove { try await $0.deleteOuttake(id: o.id) } } }; deleteOuttake = nil }
        } message: { Text("This outtake form will be permanently deleted.") }
    }

    private func load() async {
        guard let api = session.api else { return }
        do {
            async let w = api.worksheets(ticketId: ticketId)
            async let o = api.outtakes(ticketId: ticketId)
            (worksheets, outtakes) = try await (w, o)
            loadError = nil
        } catch is CancellationError { return }
        catch { loadError = userMessage(for: error) }
    }

    private func openTemplates() async {
        guard let api = session.api else { return }
        do { templates = try await api.worksheetTemplates(); actionError = nil; showTemplates = true }
        catch { actionError = "Couldn't load worksheet templates: \(userMessage(for: error))" }
    }

    private func createWorksheet(_ t: WorksheetTemplate) async {
        guard !busy else { return }
        busy = true; defer { busy = false }
        do {
            let created = try await session.requireAPI().createWorksheet(ticketId: ticketId, CreateWorksheetRequest(templateId: t.id))
            await load()
            if let id = created.id { router.push(.worksheet(id)) }
        } catch { actionError = "Failed to create worksheet: \(userMessage(for: error))" }
    }

    private func createOuttake() async {
        guard !busy else { return }
        busy = true; defer { busy = false }
        do {
            let created = try await session.requireAPI().createOuttake(ticketId: ticketId)
            await load()
            if let id = created.id { router.push(.outtake(id)) }
        } catch { actionError = "Failed to create outtake form: \(userMessage(for: error))" }
    }

    private func remove(_ work: (APIClient) async throws -> Void) async {
        do { try await work(try session.requireAPI()); actionError = nil; await load() }
        catch { actionError = "Delete failed: \(userMessage(for: error))" }
    }
}

// MARK: Fill a worksheet

@MainActor struct FillWorksheetView: View {
    let id: Int
    @EnvironmentObject var session: Session
    @EnvironmentObject var router: Router
    @StateObject private var loader = Loader<WorksheetDetail>()
    @State private var values: [Int: String] = [:]
    @State private var seeded = false
    @State private var saving = false
    @State private var error: String?

    private var canWrite: Bool { session.capabilities.canWrite(Capabilities.support) }

    var body: some View {
        LoadView(loader: loader, retry: load) { ws in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(ws.templateName ?? "Worksheet").font(.title3.weight(.bold))
                        if ws.signed { Text("Signed by \(ws.signedName ?? "")" + (ws.signedAt.map { " on \(RivetDate.display($0))" } ?? "")).font(.footnote).foregroundColor(Palette.label2) }
                    }.frame(maxWidth: .infinity, alignment: .leading).card()
                    ForEach(ws.fields) { field in fieldView(field) }
                    if ws.fields.isEmpty { Text("This worksheet has no fields.").font(.footnote).foregroundColor(Palette.label2) }
                    if let error { InlineError(message: error) }
                    if canWrite && !ws.signed {
                        Button { Task { await save(ws) } } label: {
                            HStack { if saving { ProgressView().tint(Palette.onBrand) }; Text("Save") }.frame(maxWidth: .infinity)
                        }.buttonStyle(PrimaryButtonStyle()).disabled(saving)
                    }
                }.padding(16)
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Worksheet").navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    @ViewBuilder private func fieldView(_ f: WorksheetField) -> some View {
        let binding = Binding<String>(get: { values[f.id] ?? "" }, set: { values[f.id] = $0 })
        let locked = !canWrite || (loader.value?.signed ?? false)
        VStack(alignment: .leading, spacing: 6) {
            Text(f.name + (f.required ? " *" : "")).font(.footnote.weight(.semibold)).foregroundColor(Palette.label2)
            switch f.type {
            case "checkbox":
                Toggle(f.name, isOn: Binding(get: { values[f.id] == "1" }, set: { values[f.id] = $0 ? "1" : "0" }))
                    .labelsHidden().disabled(locked).tint(Palette.brand)
            case "select":
                Picker(f.name, selection: binding) {
                    Text("Choose…").tag("")
                    ForEach(f.choices, id: \.self) { Text($0).tag($0) }
                }.pickerStyle(.menu).disabled(locked)
            case "textarea":
                TextEditor(text: binding).frame(minHeight: 90).disabled(locked)
                    .padding(6).background(Palette.card, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            case "text", "number", "date", "email", "phone", "url":
                TextField(f.name, text: binding).textFieldStyle(.roundedBorder).disabled(locked)
                    .keyboardType(f.type == "number" ? .decimalPad : .default)
            default:
                Text("\(f.type) fields aren't collected on mobile").font(.footnote).foregroundColor(Palette.label3)
            }
        }.frame(maxWidth: .infinity, alignment: .leading).card()
    }

    private func load() async {
        guard let api = session.api else { return }
        await loader.load { try await api.worksheet(id) }
        if let ws = loader.value, !seeded {
            values = Dictionary(uniqueKeysWithValues: ws.fields.map { ($0.id, $0.value ?? "") })
            seeded = true
        }
    }

    private func save(_ ws: WorksheetDetail) async {
        guard !saving else { return }
        if let missing = ws.fields.first(where: { $0.required && ($0.type != "checkbox") && (values[$0.id] ?? "").trimmingCharacters(in: .whitespaces).isEmpty }) {
            error = "\(missing.name) is required"; return
        }
        saving = true; error = nil
        defer { saving = false }
        do {
            let api = try session.requireAPI()
            let responses = ws.fields.filter { !["file", "signature"].contains($0.type) }
                .map { FieldResponse(fieldId: $0.id, value: values[$0.id] ?? "") }
            try await api.saveWorksheetResponses(id: id, SaveResponsesRequest(responses: responses))
            try await api.completeWorksheet(id: id, completed: true)
            router.pop()
        } catch { self.error = userMessage(for: error) }
    }
}

// MARK: Outtake form and signature

/// Finger-drawn signature. Strokes are normalised to the pad size so the exported PNG matches what was drawn.
struct SignaturePad: View {
    @Binding var strokes: [[CGPoint]]
    /// The size the pad is actually laid out at; the exported PNG uses it so it matches what was drawn.
    @Binding var size: CGSize
    var body: some View {
        Canvas { context, _ in
            for stroke in strokes where stroke.count > 0 {
                var path = Path()
                path.move(to: stroke[0])
                if stroke.count == 1 { path.addLine(to: CGPoint(x: stroke[0].x + 0.1, y: stroke[0].y)) }
                for p in stroke.dropFirst() { path.addLine(to: p) }
                context.stroke(path, with: .color(.black), style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
            }
        }
        .background(Color.white)
        .background(GeometryReader { g in
            Color.clear.onAppear { size = g.size }.onChange(of: g.size) { size = $0 }
        })
        .gesture(DragGesture(minimumDistance: 0)
            .onChanged { value in
                if value.translation == .zero || strokes.isEmpty { strokes.append([value.location]) }
                else { strokes[strokes.count - 1].append(value.location) }
            })
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(Palette.separator))
        .accessibilityLabel("Signature pad")
    }

    /// Renders the strokes on white as a PNG, or nil when nothing was drawn.
    @MainActor static func png(strokes: [[CGPoint]], size: CGSize) -> Data? {
        guard strokes.contains(where: { !$0.isEmpty }), size.width > 0, size.height > 0 else { return nil }
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1          // points, not device pixels: keeps the PNG under the server's 512 KiB limit
        format.opaque = true
        let renderer = UIGraphicsImageRenderer(size: size, format: format)
        let image = renderer.image { ctx in
            UIColor.white.setFill(); ctx.fill(CGRect(origin: .zero, size: size))
            UIColor.black.setStroke()
            for stroke in strokes where !stroke.isEmpty {
                let path = UIBezierPath()
                path.lineWidth = 3; path.lineCapStyle = .round; path.lineJoinStyle = .round
                path.move(to: stroke[0])
                if stroke.count == 1 { path.addLine(to: CGPoint(x: stroke[0].x + 0.1, y: stroke[0].y)) }
                for p in stroke.dropFirst() { path.addLine(to: p) }
                path.stroke()
            }
        }
        return image.pngData()
    }
}

@MainActor struct OuttakeSignView: View {
    let id: Int
    @EnvironmentObject var session: Session
    @EnvironmentObject var router: Router
    @StateObject private var loader = Loader<OuttakeDetail>()
    @State private var name = ""
    @State private var strokes: [[CGPoint]] = []
    @State private var agreed = false
    @State private var saving = false
    @State private var error: String?
    @State private var padSize = CGSize.zero

    private var canWrite: Bool { session.capabilities.canWrite(Capabilities.support) }

    var body: some View {
        LoadView(loader: loader, retry: load) { ot in
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Outtake / Pickup Form").font(.title3.weight(.bold))
                        if let s = ot.ticketSubject, !s.isEmpty { Text(s).font(.subheadline) }
                        Text([ot.client, ot.contactName].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " • ")).font(.footnote).foregroundColor(Palette.label2)
                        if let n = ot.notes, !n.isEmpty { Text(n).font(.footnote) }
                        if ot.signed {
                            Pill(text: "Signed by \(ot.signedName ?? "") on \(RivetDate.display(ot.signedAt))", tone: .cyan)
                        }
                    }.frame(maxWidth: .infinity, alignment: .leading).card()

                    if !ot.signed && canWrite {
                        VStack(alignment: .leading, spacing: 10) {
                            TextField("Customer name", text: $name).textFieldStyle(.roundedBorder)
                            HStack {
                                Text("Customer signature").font(.footnote.weight(.semibold)).foregroundColor(Palette.label2)
                                Spacer()
                                Button("Clear") { strokes = [] }.font(.footnote)
                            }
                            SignaturePad(strokes: $strokes, size: $padSize).frame(height: 180)
                            Toggle("The customer confirms the items listed were collected", isOn: $agreed).font(.footnote).tint(Palette.brand)
                        }.card()
                        if let error { InlineError(message: error) }
                        Button { Task { await sign(ot) } } label: {
                            HStack { if saving { ProgressView().tint(Palette.onBrand) }; Text("Sign & Complete") }.frame(maxWidth: .infinity)
                        }.buttonStyle(PrimaryButtonStyle()).disabled(saving)
                    }
                }.padding(16)
            }
        }
        .background(Palette.background.ignoresSafeArea())
        .navigationTitle("Outtake Form").navigationBarTitleDisplayMode(.inline)
        .task { await load() }
    }

    private func load() async {
        guard let api = session.api else { return }
        await loader.load { try await api.outtake(id) }
        if name.isEmpty, let c = loader.value?.contactName, !c.isEmpty { name = c }
    }

    private func sign(_ ot: OuttakeDetail) async {
        guard !saving else { return }
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { error = "Name is required"; return }
        guard agreed else { error = "Confirm the collection first"; return }
        guard let data = SignaturePad.png(strokes: strokes, size: padSize),
              let dataURL = Signature.dataURL(png: data) else {
            error = strokes.isEmpty ? "Please sign in the box" : "The signature is too large; clear it and sign again"
            return
        }
        saving = true; error = nil
        defer { saving = false }
        do {
            try await session.requireAPI().signOuttake(id: id, SignRequest(signedName: trimmed, signature: dataURL))
            router.pop()
        } catch { self.error = userMessage(for: error) }
    }
}
