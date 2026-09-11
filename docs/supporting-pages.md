# Supporting pages — issue #53

The owner requested an autonomous redesign of Settings, Treatment details,
Calendar and Notes, with private photos connected to Progress. This candidate
builds on merged Progress PR #52 (`ad0be2e`). It is not a published release.

## Information architecture

Settings is now a small set of labelled cards: Treatment details, optional
reminders, accounting zone, local data, and Wear information. Reminder permission
status is visible before opening its controls. The duplicate target form is
removed here; Treatment details owns target editing. Backup/restore/delete,
permissions and explicit reminder saving retain their existing callbacks and
confirmations. Nothing silently enables reminders or changes a saved setting.

Treatment details starts with the recorded tray position, target and interval.
The original Canvas dot diagram identifies the current position in the prescribed
sequence; it is not a tooth-movement or completion score. Unknown values stay
unknown. Editing target/tray details, variable schedules and beginning a new phase
are clearly separated. Advanced forms no longer show errors on the landing view.
Existing schedule corrections and phase confirmations remain. Expandable content
retains saveable drafts when closed and reopened. Completed plans remain read-only
where the original rules require it; starting a new prescribed phase remains a
separate explicit action.

Calendar opens with the month visible and marks dates containing appointments or
notes. The selected day is highlighted and described accessibly. A compact saved
wear total accompanies the date; appointment records and their actions follow.
Calendar cells remain at least 48dp wide and expand with font scaling. On narrow
screens the month can scroll horizontally, with an explicit visible cue. Locale
week starts and full date semantics remain; count descriptions explain the dots.

Notes is a separate tab with recent notes across dates, readable date labels and
compact edit/delete controls. Add/Edit opens the relevant form rather than leaving
empty note and appointment forms on the landing page. Save callbacks close an
editor only on success. Native date/time pickers, overlap choices, validation,
delete confirmation, reminder options and missing-interval confirmation remain.
The correction form is reached explicitly from Calendar, not shown under Notes.

Progress now contains a labelled Photo progress card. It opens the existing private
photo journal and returns to Progress via Back. Photos are removed from the generic
More menu. The original import/camera/comparison/export/storage contracts are
unchanged; no generated or competitor dental images are added.

## Research applied

[NN/g form grouping](https://www.nngroup.com/articles/form-design-white-space/)
supports grouping related labels and fields so their relationships are clear.
[Progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/)
supports keeping common information visible and labelling access to less frequent
controls. Here the summaries precede editing, and the user selects the editing task.
[Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
supports explicit state/action information for custom presentation. Cards expose
expanded/collapsed state; native buttons/switches keep their roles; calendar dates
include record counts. These sources do not prove comprehension or aesthetic
acceptance of these specific screens.

The KB search found no relevant saved design decisions for these pages. Product,
architecture, roadmap, prior neutral design/localization/accessibility guidance,
current main and issue #53 were inspected. This is a UI-only refinement: no schema,
network permission, account, reminder scheduling, backups, accounting, timezone
placement, widget or Wear data contract changes. English resource strings are
translation-ready; real translated-language delivery remains separate under #31.

## Authorship and review

`SupportingBlocks.kt` contains original Compose information cards and a tray-position
diagram authored by Codex for this project. Camera/document icons extend the existing
original Canvas icon set. No bitmap asset or chart/image dependency was introduced.

The material visual review corrected inconsistent calendar cell geometry, kept
large text readable with horizontal calendar scrolling, removed the duplicated
wear diagram and global gap explanation from the calendar landing page, and made
Notes and appointment actions compact. Exact correction details remain in their dedicated editor.
The initial matrix driver incorrectly selected both scroll containers and captured
one unpresented window; it was corrected to scroll vertically and capture the
composed root. Those incomplete captures are not acceptance evidence.

## Validation

Evidence is recorded after final checks. Isolated emulator matrix captures use
synthetic notes/appointments and plan data; full-app screenshots include navigation.
No owner-phone installation, publication, human comprehension or TalkBack speech
quality is claimed. Save failures continue to use the existing ViewModel error path.

The final matrix passed with 40 Compose-root captures covering Settings, Treatment,
Calendar and Notes in normal/dark/RTL, 320dp with 200% font, expanded pseudolocale,
unknown details, busy and completed states. The revised capture and draft-retention
tests passed; the six focused phase/journal/accessibility/Progress tests also passed
in the preceding run. Eight distinct focused tests are accepted across those runs.
Corrected normal, constrained and RTL captures were visually inspected. Large text
requires vertical scrolling and the calendar also permits horizontal scrolling.

67 phone and 9 Wear JVM tests passed. Formatting, debug/release lint, debug and
instrumentation APK builds passed. The initial combined check's release invocation
was interrupted (exit 143); a final phone formatting/lint/release check and separate
Wear release build both subsequently passed. No hosted-CI pass is inferred from
these local results.

Full-app evidence:
- [Settings](screenshots/supporting-pages/settings.png),
  [before](screenshots/supporting-pages/settings-before.png)
- [Treatment](screenshots/supporting-pages/treatment.png),
  [before](screenshots/supporting-pages/treatment-before.png)
- [Calendar](screenshots/supporting-pages/calendar.png),
  [appointment](screenshots/supporting-pages/appointment.png),
  [prior combined journal](screenshots/supporting-pages/journal-before.png)
- [Notes](screenshots/supporting-pages/notes.png)
- [Photo entry on Progress](screenshots/supporting-pages/progress-photos.png)

A synthetic note and appointment were created through the actual new forms and
remained after restarting the app. These samples were added after the before
screenshots; the records therefore differ. Photo progress opened the existing
private photo screen and Back returned to Progress. No personal photos were added.

After compacting the appointment action row, the journal semantics test and full
capture matrix passed again (two focused tests). The final debug lint and APK/test
build recheck also passed. The final screenshots include this correction.
