import UIKit
import BackgroundTasks
import UserNotifications
import RivetCore

/// Free alerts without Apple's push service: while the app is closed iOS wakes it now and then (BGAppRefreshTask),
/// we ask the server for notifications we have not shown, and raise each as a local notification. iOS chooses when
/// and how often it wakes the app (typically every 15 minutes or much less, and not at all after a force-quit),
/// so this is "soon", not instant. Real instant push needs an Apple Developer membership (APNs).
enum BackgroundAlerts {
    static let taskID = "com.foleyit.rivetmsp.ios.alerts"
    private static let markKey = "alerts.highWater"
    private static let linkKey = "link"

    /// Call once before the app finishes launching (BGTaskScheduler requires registration this early).
    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskID, using: nil) { task in
            guard let task = task as? BGAppRefreshTask else { task.setTaskCompleted(success: false); return }
            schedule()                                            // always line up the next wake first
            let work = Task {
                await check(notify: true)
                task.setTaskCompleted(success: !Task.isCancelled)
            }
            task.expirationHandler = { work.cancel() }
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: taskID)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)               // fails harmlessly in the simulator / with Background App Refresh off
    }

    static func cancel() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: taskID)
        UserDefaults.standard.removeObject(forKey: markKey)
        let center = UNUserNotificationCenter.current()
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
        Task { try? await center.setBadgeCount(0) }
    }

    /// Asks for permission the first time (no-op afterwards) and lines up the first background wake.
    static func enable() async {
        _ = try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
        schedule()
    }

    /// Fetches the user's notifications. With `notify` false (app in the foreground) it only moves the baseline
    /// and the badge, so nothing pops up for things the user can already see.
    static func check(notify: Bool) async {
        guard let api = makeClient() else { return }
        guard let response = try? await api.notifications(page: 1) else { return }
        let d = UserDefaults.standard
        let last = d.object(forKey: markKey) as? Int
        let digest = AlertDigest(items: response.data, lastSeen: last)
        d.set(digest.highWater, forKey: markKey)
        let center = UNUserNotificationCenter.current()
        try? await center.setBadgeCount(response.total)
        guard notify else { return }
        let settings = await center.notificationSettings()
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else { return }
        for item in digest.fresh {
            let content = UNMutableNotificationContent()
            content.title = item.type
            content.body = item.message
            content.sound = .default
            content.userInfo = [linkKey: "notifications"]
            content.threadIdentifier = "rivetmsp.notifications"
            try? await center.add(UNNotificationRequest(identifier: "n\(item.id)", content: content, trigger: nil))
        }
        if digest.overflow > 0 {
            let content = UNMutableNotificationContent()
            content.title = "\(digest.overflow) more notifications"
            content.body = "Open the app to see everything new."
            content.userInfo = [linkKey: "notifications"]
            content.threadIdentifier = "rivetmsp.notifications"
            try? await center.add(UNNotificationRequest(identifier: "more\(digest.highWater)", content: content, trigger: nil))
        }
    }

    /// A client built from what the app stored at sign-in; nil when signed out. The token is in the Keychain with
    /// "after first unlock" access, so this also works while the phone is locked.
    private static func makeClient() -> APIClient? {
        let d = UserDefaults.standard
        guard let s = d.string(forKey: Session.Keys.server), let url = URL(string: s),
              let token = Keychain.get(Session.Keys.token) else { return nil }
        return APIClient(serverURL: url, token: token,
                         transport: PinnedTransport(pinnedFingerprint: d.string(forKey: Session.Keys.fingerprint)))
    }

    /// Tapping an alert: hand the allow-listed route to the app (handled like any deep link).
    static func link(from response: UNNotificationResponse) -> String? {
        response.notification.request.content.userInfo[linkKey] as? String
    }
}

/// Holds a tapped alert's route until the SwiftUI app is ready to follow it (cold launch from a tap).
final class AlertLinkInbox {
    static let shared = AlertLinkInbox()
    static let received = Notification.Name("RivetMSPAlertLinkReceived")
    private var pending: String?
    func set(_ link: String) { pending = link; NotificationCenter.default.post(name: Self.received, object: nil) }
    func take() -> String? { defer { pending = nil }; return pending }
}

extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        if let link = BackgroundAlerts.link(from: response) { AlertLinkInbox.shared.set(link) }
    }
}
