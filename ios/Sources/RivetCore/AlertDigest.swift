import Foundation

/// Decides which server notifications are new since the last check, so the app can raise a local alert for
/// each one exactly once. Pure so it can be tested without UIKit or the network.
public struct AlertDigest: Equatable {
    /// Notifications not seen before, oldest first, capped at `limit`.
    public let fresh: [AppNotification]
    /// How many new ones were left out by the cap (shown as "and N more").
    public let overflow: Int
    /// Highest notification id seen so far; store it and pass it to the next check.
    public let highWater: Int

    /// - Parameter lastSeen: the stored high-water mark, or nil on the first check. The first check only
    ///   records a baseline: a freshly signed-in user is not buried under their whole backlog.
    public init(items: [AppNotification], lastSeen: Int?, limit: Int = 3) {
        let top = max(items.map(\.id).max() ?? 0, lastSeen ?? 0)
        highWater = top
        guard let lastSeen else { fresh = []; overflow = 0; return }
        let new = items.filter { $0.id > lastSeen }.sorted { $0.id < $1.id }
        fresh = Array(new.suffix(limit))
        overflow = max(0, new.count - limit)
    }
}
