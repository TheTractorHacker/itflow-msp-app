# Daily-use audit: RivetMSP Android and RivetIT Android (2026-11-03)

Screenshots (phone 1080x2400, light and dark, real demo data) are in `docs/screenshots-audit/`:
`msp-*` and `it-*` are the main screens before the change; `clock-*` and `it-new-*`/`it-clock-*`
are after. iOS was not screenshotted (no macOS here).

## What the audit found
- Home was six stat tiles (four of them "0" on the MSP demo) with My Queue, the actual work, below the fold.
- The ticket timer was local state in `TicketDetailScreen`: leave the ticket, switch tickets or lose the
  process and the running time was gone.
- Floating buttons covered the last row of the Tickets, Assets and Appointments lists.

## What the PSA apps do better (sources: store listings, vendor docs; offline, widgets, lock-screen
actions and voice notes were not found for any of them; Autotask and Halo coverage was thin)
- ConnectWise Mobile: agenda-first home; pausable, persistent timers that convert to time entries;
  receipt photos; signature forms; barcode scan. Complaints: awkward time entry, hard ticket browsing.
- Syncro: schedule view, intake/outtake forms with signatures, notification centre with read/unread,
  timers synced with the web app. Complaints: 24 h sessions, chat spinners.
- HaloPSA: barcode asset lookup with multi-match, ticket timer that fills Time Taken. Complaint: limited, often
  falls back to the web app.
- Autotask LiveMobile: real-time tickets, time entries, calendar. Complaints: crashes/lag, 2FA after updates,
  users asking for offline entry.

## The one bold change: a persistent "On the clock" work session, with a work-first Home
- App-wide session (`WorkClock`), persisted, wall-clock based, several tickets can be paused, one runs.
- Bar above the bottom nav on every screen (hidden on the clocked ticket's own screen); one tap stops and
  opens the note sheet with the time rounded to the minute and prefilled.
- Home: queue first, start/pause on every card, one-line pulse of non-zero counts.
- Read-only roles (RivetIT) get no start button.

## Verified on the emulator (API 36, demo servers 8444 and 8445)
Start from Home, bar on Home/Clients/Tickets, ticking; force-stop and relaunch (time kept); Stop and log opens
the sheet with 1 min; saving posted a note with `00:01:00` to the demo ticket and cleared the session; a second
timer paused the first ("1 more paused"); dark mode; list end clear of the floating button (RivetIT Tickets).
Unit tests: 11 new (`WorkClockTest`), MSP 52 and RivetIT 83 pass, lint run on MSP.

## Not verified
Reboot persistence (only app kill), a day-long session, multi-day sessions, TalkBack, tablets, the MSP
Assets/Appointments bottom inset visually, the ticket screen's clock card on RivetIT, a failed save while
offline (session is kept; not exercised), iOS (not built; no equivalent change made).
