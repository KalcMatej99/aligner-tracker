# Product requirements

Working name: Aligner Tracker. Independent GPL-3.0-or-later Android app. Every shipped feature is free; no ads, subscriptions, paid export or watch unlock. Research: [competitor evidence](research.md), gathered September 2026. The original setup brief is archived in [initial-brief.md](initial-brief.md).

## Problem and audience

People using removable orthodontic aligners repeatedly remove them for meals and cleaning, forget to restart tracking or reinsert them, and need a trustworthy account of their routine. Primary users manage one active prescribed treatment on one Android phone. Secondary users enter retention or refinement later. The app records and reminds; it does not prescribe hours, approve tray changes, diagnose progress, or replace a clinician.

## Outcomes and acceptance

A first-time user sets up prescribed tray count, current tray, interval and daily wear target without an account. From Today, one tap records IN or OUT, visible immediately and recoverable after process death. An opted-in reminder helps end a break. A user can correct a forgotten switch, inspect covered wear history, explicitly advance/complete treatment and export/restore their data. This is the integrated MVP finish condition, alongside passing local automated checks and recorded emulator evidence. Physical notification reliability and release signing remain explicit Beta/v1 gates.

## Journeys

1. Read privacy/local-storage introduction → enter prescribed plan and current state → Today.
2. Before a meal tap Take out → see break duration → receive optional reminder → tap Put in.
3. Missed a tap → History → edit transition time with adjacent events visible → validated correction recalculates totals.
4. Tray due → Schedule → review date estimate → explicitly Change tray. No automatic changes. Last tray → explicit completion → retained read-only history.
5. Progress → inspect 7/30-day covered totals and target indicators, distinguish untracked/partial days from missed target.
6. Settings → choose reminder permissions → export JSON or CSV using system file picker → import preview → explicitly replace, or cancel without mutation.

## MVP

Onboarding/setup, current-state timer, persisted events, configurable prescribed goal, partial-day totals, correction timestamps, current tray/projection/manual change, completion, break/tray reminders with permission status, history/date browsing, basic statistics, dark/system theme, accessible touch targets and text scaling, local JSON backup/validated restore, CSV report, delete data, automated test/build/lint and emulator smoke. One plan and fixed per-tray interval; treatment-zone daily accounting. No predefined medical target presented as advice: input is labeled prescribed target, default merely editable example.

## Later releases

Beta: variable per-tray schedules, refinement/retainer phases, historical targets, notes/appointments, home widget + notification controls, encrypted portable backup, full calendar and charts, locale/RTL audit, physical/OEM reminder tests, usability sessions and battery profiling. v1: optional local photos/comparison/time-lapse, polished report/share, Wear OS companion prototype then production gate, translations, signed distribution/F-Droid feasibility. Optional user-controlled sync only after conflict/encryption design proves value; no mandatory backend. Do not promise all later candidates for v1.

## Differentiation

All delivered functionality free, including statistics and portable data that competitors may gate. Transparent tracking coverage, easy corrections, legible reminder status, complete local ownership, no account, readable backup schema, and tests for time correctness. Watch support alone is not a unique differentiator: newer TrayMinder listing advertises Wear OS. Specific competitor paid boundaries are documented with source/platform caveats in research, not guessed.

## Privacy, accessibility and ethical design

Treatment timestamps and future photos can reveal health routines. Collect the minimum, use no network permission, log no history, explain plaintext exports, disable automatic backup, allow complete deletion. No shame, leaderboard, pushy streak or guilt messaging; positive neutral feedback. Screen reader labels include action and state; minimum48dp controls, never color alone, scalable scroll layouts, adequate contrast, focus order and accessible error text. Initial language English; string resources allow translation. Distinguish implementation from device-audited accessibility.

## Release gates

MVP internal testing: setup→track→correct→history→reminder→backup/restore functional, deterministic time/import tests pass, APK builds, emulator runs without crash. Beta: API/device matrix, TalkBack/large font, no unreconciled data loss or reminder blockers, battery measurements. v1: signing ownership, license/dependency audit, privacy statement/store declarations, tested upgrade migration/restore and distribution documentation. No medical effectiveness claim.
