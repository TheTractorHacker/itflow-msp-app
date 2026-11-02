import SwiftUI
import UIKit
import RivetCore

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255, alpha: 1)
    }
}

extension Color {
    /// A colour that switches with light/dark mode.
    static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }

    /// "#dc3545" from the server (status colours).
    init(serverHex: String?) {
        let raw = (serverHex ?? "").trimmingCharacters(in: CharacterSet(charactersIn: "# "))
        if raw.count == 6, let v = UInt32(raw, radix: 16) { self = Color(UIColor(hex: v)) }
        else { self = Color(UIColor.systemGray) }
    }
}

/// Design tokens shared with the Android app (RivetMSP brand, same semantic status colours).
enum Palette {
    static let brand = Color.dynamic(light: 0x0066CC, dark: 0x00D9FF)
    static let onBrand = Color.dynamic(light: 0xFFFFFF, dark: 0x020E0E)
    static let container = Color.dynamic(light: 0xC4E1FF, dark: 0x00393A)
    static let onContainer = Color.dynamic(light: 0x05162C, dark: 0xB0EBEA)

    static let background = Color(UIColor.systemGroupedBackground)
    static let card = Color(UIColor.secondarySystemGroupedBackground)
    static let label = Color(UIColor.label)
    static let label2 = Color(UIColor.secondaryLabel)
    static let label3 = Color(UIColor.tertiaryLabel)
    static let separator = Color(UIColor.separator)
    static let fill = Color(UIColor.tertiarySystemFill)

    static let lavender = Color.dynamic(light: 0xDFD4F8, dark: 0x3A2659)
    static let onLavender = Color.dynamic(light: 0x170F23, dark: 0xE0D2FD)
    static let tealSoft = Color.dynamic(light: 0xBEE2C9, dark: 0x00391B)
    static let graySoft = Color.dynamic(light: 0xE6ECEC, dark: 0x2B2F30)
    static let dangerSoft = Color.dynamic(light: 0xFFDAD6, dark: 0x93000A)
    static let danger = Color.dynamic(light: 0xBA1A1A, dark: 0xFFB4AB)

    static let priorityLow = Color(UIColor(hex: 0x8A9396))
    static let priorityMedium = Color(UIColor(hex: 0xF9A825))
    static let priorityHigh = Color(UIColor(hex: 0xE65100))
    static let priorityCritical = Color(UIColor(hex: 0xDC3545))

    static func priority(_ raw: String?) -> Color {
        switch PriorityTone(raw) {
        case .low, .none: return priorityLow
        case .medium: return priorityMedium
        case .high: return priorityHigh
        case .critical: return priorityCritical
        }
    }

    static let brandGradient = LinearGradient(colors: [Color(UIColor(hex: 0x0066CC)), Color(UIColor(hex: 0x00D9FF))],
                                              startPoint: .topLeading, endPoint: .bottomTrailing)
}

extension View {
    /// White card with the app's corner radius, used for every grouped block.
    func card(padding: CGFloat = 16) -> some View {
        self.padding(padding)
            .background(Palette.card, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}
