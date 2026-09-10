# Neutral phone redesign — candidate 5

Owner request, 2026-09-10. This supersedes the warm/pine aesthetic in
[redesign.md](redesign.md). It is one implemented native Compose direction.

## Baseline and evidence distinctions

- **Owner feedback:** published version 4's earthy green, cream/brown tint,
  oversized typography and rounded containers did not achieve the desired quality.
  This is a rejection of the complete composition, not merely a color preference.
- **Observed:** ran the actual version 4 on the disposable API36 emulator. Brand,
  page heading, session and four daily values competed at large sizes. The filled
  session block and repeated header consumed the first viewport. History's date
  controls took three rows; Reports forced period controls into two rows. Settings
  put a long watch explanation between prescribed-hours input and its save action.
- **Design judgments:** white/charcoal with restrained blue, 20dp gutters, smaller
  section headings, flatter groups and small button corners form a clearer and
  more consistent composition. These are visual judgments, not scientific proof
  of attractiveness or owner acceptance.
- **Hypotheses:** compact labelled facts make session versus daily totals easier
  to distinguish; putting History beside Today helps corrections; one screen
  header aids orientation. The owner must still assess comprehension and comfort.

Live inspection: checkout `design/calm-tracking` was clean at `11f68c2`.
PR #35 was open and unmerged, with main at `9c79442`; its head includes the #34
connection fix. Issues #12/#18/#31/#32/#33/#34 were open; #24 was closed.
All requested issue and PR comments were read. A KB search found no relevant
release/design record. Current documentation and live evidence were used.

The live Tailscale-only store catalogue listed version codes 4, 3 and 2; latest4
APK was downloaded and hashed to
`b9dd73216cb615f24a472f9ea3c1f138f7ece9d711f7617ca86b11b51a6447b8`.
LAN access correctly returned403; the existing Tailscale address succeeded.
Work is isolated on `design/neutral-tracking` in `../aligner-tracker-neutral`,
based on PR35's head so the published changes and Wear fix are retained.

## Primary guidance and concrete decisions

Consulted on 2026-09-10. Sources justify principles; they do not prescribe this palette.

| Primary source | Application |
| --- | --- |
| [Android color](https://developer.android.com/design/ui/mobile/guides/styles/color) | Explicit semantic roles in both themes. Blue identifies tracking/action emphasis; normal surfaces remain neutral. OUT is a routine state, not a red error. State is always written in words. |
| [Android layout basics](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-basics) | Retain reachable tracking action, safe insets, keyboard-aware navigation, scrollable forms and constrained layouts. Never cap user font scale. |
| [Android content structure](https://developer.android.com/design/ui/mobile/guides/layout-and-content/content-structure) | Align labels/values and use spacing/dividers. Remove the Today container. Retain subtle containment for report aggregates, individual notes, appointments and photo records. |
| [Android navigation](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns) | Keep four native primary destinations and labelled overflow/settings icons. Prioritize Today, History, Schedule, Progress. Preserve origin-aware Back and per-screen saved state. |
| [Android accessibility](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility) | Preserve explicit state, section headings, minimum48dp controls, selected semantics, localized input, error text and scaled/RTL layouts. |
| [NN/g visual hierarchy](https://www.nngroup.com/articles/visual-hierarchy-ux-definition/) | Give state the first emphasis, labelled session the second, then compact daily accumulated facts. Remove repeated headings and equally oversized metrics. |
| [NN/g usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/) | Keep transactional feedback with a visible dismiss affordance, correction adjacent to tracking, bounded date navigation and preview/cancel/confirm recovery. |
| [GOV.UK question pages](https://design-system.service.gov.uk/patterns/question-pages/) | Preserve deliberate IN/OUT plus Start with no typed fields. Mark optional data, retain restore and later editing; avoid a mandatory wizard. |
| [NN/g usability testing](https://www.nngroup.com/articles/usability-testing-101/) | Exercise realistic start/correct/edit/restore tasks with synthetic records. Automated and agent-operated checks are distinct from actual owner usability findings. |

## Implemented system

White foundation, charcoal text and slate secondary text; blue primary `#245BC3`.
Dark uses `#12151B` and light ink, with `#AFC6FF` primary. Surface containers,
outline, inverse and native picker roles are explicit. Dialogs use a neutral
container without a tint overlay. Launcher mark retains its original vector and
uses the new accent. No new raster assets, competitor assets or dependencies.

System type: 28sp state, 24sp session, 20sp app bar, 16sp section titles,
14–16sp reading text. User scaling is never capped. Shared shapes are 4/8/12/16/20dp;
main buttons and outlined actions explicitly use8dp corners. Native navigation
indicators remain Material components. No decorative charts or ticking service.

Today is flat: date, written IN/OUT, labelled session, complete daily table,
prescribed target/unknown message, optional tray and fixed accounting zone.
One reachable56dp tracking button and a48dp correction route remain. At constrained
heights secondary facts scroll and correction is inside that scroll region.
History has48dp previous/next controls around a readable date chooser, with bounded
native calendar or large-text input; event rows retain contextual correction.
Reports wrap period selectors as available width permits, retain units/coverage,
show unknown days as unknown and group only the aggregate range.

Setup uses radio rows with explicit selection and clearly secondary restore/zone.
Settings groups target, reminders, data and watch information; the watch paragraph
no longer separates an input from its save action. Notes, appointments and photos
use neutral outlined records, and shared form/dialog/navigation styling carries
across supporting screens. All capabilities and copy resources remain available.

## Behavior boundary and material review

No Room schema, domain model, repository, backup codec, reminder schedule,
widget contract, Wear protocol or connection implementation changed. Historical
targets, estimates, unknown data and explicit tray actions retain their meanings.
No account, analytics, INTERNET permission, cloud sync, medical guidance or automatic
advancement was added. Phone and watch versions advance together to code5.

One material source/interaction review covered the changed surfaces and state
boundaries. Corrected: initial History coverage note could squeeze the event row;
keep that initial record stacked. Move unrelated Wear explanation out of target
editing. Explicit neutral dialog surface removes elevation tint. Preserve heading
semantics in the compact app bar. A date-browsing regression verifies bounds and
unchanged records; existing tests keep transactional, backup and stale-action gates.
No recursive review or speculative domain cleanup was performed.

See [validation and screenshots](validation-neutral.md) for final outcomes,
artifact identity, same-signer upgrade and remaining owner/device gates.
