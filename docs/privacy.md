# Privacy design and internal-build notice

2026-09-08. Aligner Tracker stores user-entered treatment dates, goals, tray state and timestamped IN/OUT events on the device. Those records may reveal health routines. The app requires no account and contains no Internet permission, telemetry, advertising or billing integration. No developer receives records automatically.

Room files are in app-private storage protected by Android's sandbox/device encryption. This is not separate application-level database encryption. Android automatic backup and device transfer are excluded explicitly; users must create their own portable backup before uninstalling or changing phones. Device compromise and exports are outside the app sandbox's protection.

JSON backups and CSV exports are **plaintext**. Android's system document picker lets the user select a local folder or an installed document provider; choosing a cloud provider can upload that export under that provider's rules. The app does not choose a destination, retain broad storage access, or send a report to a clinician. Shared exported files remain outside the app and are not removed by in-app deletion.

MVP backups contain the treatment and wear events, not notification permission grants or reminder preferences. Import preserves this device's reminder choices, cancels stale alarms and resets delivery records. Replacement needs a preview and confirmation; malformed, inconsistent and oversized records must leave existing records unchanged. The caller must trust the destination. Optional encrypted backups are tracked in #16.

Notifications are optional. They use private lock-screen visibility and a generic public version; users and Android lock-screen settings control final visibility. Channel, notification and exact-alarm grants are user-controlled. No foreground timer service or blanket power exemption is requested. No guarantee is made for timely delivery during force-stop, OEM restrictions or without permission.

Delete data clears treatment/event records and pending reminder work/alarms/visible notifications. Reminder preference toggles are device settings rather than health history; no record content is logged. Exported files must be deleted separately. Photos, accounts and synchronization are not in the MVP and require their own data lifecycle before implementation.

This document describes the implemented design, not legal certification. Release #21 covers distribution-specific privacy declarations and dependency audit. Test evidence and open checks are in validation.md.
