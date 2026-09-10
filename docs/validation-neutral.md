# Neutral redesign validation — candidate 5

Implementation: [decisions and material review](neutral-redesign.md).
All runtime work used disposable API 36 emulators and synthetic records. The owner’s
physical phone was not connected or installed onto. No publication was performed.

## Rendered composition

The agent inspected actual PNGs from the running app, including captured loading,
normal and constrained states. White/charcoal dominate; blue concentrates on
state/action/selection. One page header replaces brand-plus-heading repetition.
Compact daily rows make accumulated wear subordinate to the current-state block,
while retaining complete duration units. History controls occupy one group; report
ranges fit one row when possible. Supporting records and dialogs share neutral
containment. This is the implementer's aesthetic assessment, not owner approval.

Normal captures: 1080×2400, density 420 (411dp), font 100%. Constrained:
840×1600, density 420 (320dp), font 200%. Pseudolocales en-XA/ar-XB test expanded
and mirrored layout only; no translation is claimed. Established before/after
captures use the same 13-event synthetic database with two historical targets,
two schedule revisions, two tray records, a gap, note and appointment. Timers
naturally advance; these are equivalent-data comparisons, not pixel-diff fixtures.

| Journey | Published 4 before | Candidate 5 after |
| --- | --- | --- |
| Setup, deliberate IN selected | [Before](screenshots/neutral/before/onboarding-light.png) | [After](screenshots/neutral/after/onboarding-light.png) |
| Established Today, light | [Before](screenshots/neutral/before/today-light.png) | [After](screenshots/neutral/after/today-light.png) |
| Established Today, dark | [Before](screenshots/neutral/before/today-dark.png) | [After](screenshots/neutral/after/today-dark.png) |
| 320dp/200% dark | [Before](screenshots/neutral/before/today-small-dark.png) | [After](screenshots/neutral/after/today-small-dark.png) |
| History | [Before](screenshots/neutral/before/history-light.png) | [After](screenshots/neutral/after/history-light.png) |
| Schedule | [Before](screenshots/neutral/before/schedule-light.png) | [After](screenshots/neutral/after/schedule-light.png) |
| Reports | [Before](screenshots/neutral/before/reports-light.png) | [After](screenshots/neutral/after/reports-light.png) |
| Settings | [Before](screenshots/neutral/before/settings-light.png) | [After](screenshots/neutral/after/settings-light.png) |

Additional inspected states: [RTL](screenshots/neutral/after/today-small-dark-rtl.png),
[expanded text](screenshots/neutral/after/today-small-dark-expanded.png),
[date entry](screenshots/neutral/after/history-date-small-dark.png),
[keyboard](screenshots/neutral/after/details-keyboard-small-dark.png),
[actionable error](screenshots/neutral/after/details-error-small-dark.png),
[Journal](screenshots/neutral/after/journal-light.png),
[photos](screenshots/neutral/after/photos-light.png),
[loading](screenshots/neutral/after/loading-light.png),
[restore preview](screenshots/neutral/after/restore-preview.png).
The state and action remain legible in constrained views; secondary content scrolls.
At 200% plus pseudolocale expansion, a viewport cannot show every session/daily fact
at once. No text scale is capped or information removed to make it fit.

## Automated evidence

- [Repository check](evidence/neutral/repository-check.txt): Spotless, 61 phone and 9
  watch JVM tests, debug/release lint, phone/watch debug/test and minified release
  builds passed. One SDK download retried after a TLS transport error and passed
  checksum verification; no integrity check was bypassed.
- [Material recheck](evidence/neutral/material-recheck.txt) and
  [final header check](evidence/neutral/final-header-check.txt): focused app build,
  formatting and both lint variants passed after supporting-screen corrections.
  Final lint: 0 errors, 56 phone/3 watch warnings per variant; no warning-free claim.
- [General phone run](evidence/neutral/phone-instrumentation.txt): 40/44 initially.
  Three assertions referenced the superseded text navigation/64dp button; the
  notification test also required the harness to grant POST_NOTIFICATIONS.
  [Focused 17-case recheck](evidence/neutral/phone-focused-recheck.txt) passed after
  updating selectors to accessible descriptions, asserting the new 56dp target,
  and granting the required permission. All 44 unique cases therefore pass across
  the recorded runs; this is not one green full-suite run.
- [Initial focused 16 cases](evidence/neutral/focused-instrumentation.txt) passed.
  The added real-activity date test checks previous/next bounds and unchanged records.
- [Wear instrumentation](evidence/neutral/wear-instrumentation.txt): 4/4 passed.
- [Connection lifecycle](evidence/neutral/connection-lifecycle.txt): 96 probes and 12
  cancellation attempts passed against the installed service. #34 fix retained.
- [Contrast](evidence/neutral/contrast.json): every audited text pair exceeds 4.5: 1
  in both themes. Native touch/selected/error/state semantics and constrained
  landscape are covered by the accessibility tests. Contrast is not an aesthetic test.

## Journeys and remaining acceptance

Real Room/ViewModel tests exercise deliberate IN/OUT starts without fields,
switching, correction without moving initial coverage, later details, established
editing, activity recreation, original records/history retention, partial and
covered reports, migrations, archives, widget and reminder stale actions.
Manual captures cover native navigation, supporting screens, text expansion,
keyboard, errors and preview cancellation. Signed candidate results follow below.

Owner aesthetic and comprehension feedback (#18/#32), physical TalkBack
speech/touch traversal (#12), OEM timing/battery and actual paired Wear behavior
remain unverified. Full translations remain #31. Tests, screenshots and an APK do
not close those gates. No physical phone changes or private-store publication
are authorized by this candidate preparation.

## Final signed candidate and restore

Source tag `neutral-phone-candidate-5-final` identifies
`5e5ba6d3c5109513a2eb0a5670ada5b376186aac`. Clean tagged phone/watch release
builds passed. Both signed APKs are versionCode 5, exceeding the live store's
versionCode 4 verified on 2026-09-10. The existing owner signer is unchanged:
`9cac6b2722e6e074bd607586df06335c3ae6300803616b3f38e88706d1ed53a9`.

[Native accessibility recheck](evidence/neutral/platform-accessibility-recheck.txt)
passed 2/2 with bundled TalkBack enabled. The initial test expected the removed
standalone “Your day” heading; it now checks the state heading and retains native
focus, event, action, state and error assertions. TalkBack was disabled afterward,
restoring the emulator's prior configuration. This is not a human listening test.

Manual notification denial and Android-settings recovery were exercised:
[denied](screenshots/neutral/after/reminder-denied.png),
[recovered](screenshots/neutral/after/reminder-recovered.png).

The actual downloaded, signed published 4 was installed on a disposable emulator
and seeded with the same synthetic fixture. In-place installation of the final
signed 5 succeeded. [Exact comparison](evidence/neutral/signed-upgrade.json) found
all 16 database tables and reminder preference bytes unchanged after opening
History/Today. [Installed artifact](evidence/neutral/installed-artifact.json) matches
the candidate hash. Compare [signed 4](screenshots/neutral/before/signed-upgrade-today.png)
and [signed 5](screenshots/neutral/after/signed-upgrade-today.png).
An initial fixture copy had stale SELinux categories from a different disposable
app UID; correcting the fixture's context and restarting from version 4 resolved
that harness failure. No production security setting was weakened.

On signed 5, a schema-3 archive was selected through Android's document picker.
Preview cancellation preserved all tables and preferences exactly. Explicit
replacement then restored every treatment/history table and preserved reminder
preferences; the tracker generation changed as designed to invalidate stale
commands. Cold restart and History/Today navigation succeeded:
[restore comparison](evidence/neutral/signed-restore.json),
[restored screen](screenshots/neutral/after/signed-restored-today.png).
The synthetic fixture contains no photos, so this signed upgrade does not prove
physical-camera or real-photo acceptance; existing archive/photo behavioral tests
remain the evidence for those contracts.

The constrained date-dialog screenshot predates the final neutral dialog-surface
adjustment; date interaction and compact layout were tested, and the final signed
restore preview shows the shared final dialog treatment. Other early intermediate
concept images are not included as final alternatives.

Installable bundle:
`/home/matejkalc/.local/share/aligner-tracker/releases/neutral-candidate-5/`.
Phone SHA-256: `d8c4574c89d648f829ee12af69c744fdabde235c5c55f70248dc9307478844ac`.
Watch SHA-256: `cae44425ef5a9d48740e6374da1b3cefc24ae804dde5cc57919474d91b2113a5`.
The bundle includes corresponding tagged source, license/notices, build instructions,
validation evidence and a checksum manifest. It has not been uploaded or published.
