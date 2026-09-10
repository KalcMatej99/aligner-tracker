# Today recorded-status illustration — #40

Focused refinement of version 7, based on clean main `79a0c50` on 2026-09-10.
Main was fetched before changes; there were no open PRs or existing local edits.
Issues #37, #12 and #18 were inspected; #40 owns this work. The KB search returned
no relevant project design record. Current repository documentation and the running
API36 emulator supplied the baseline. No release, merge or owner-phone installation
is included.

## Original vector source and rationale

The editable source is the pair of Android VectorDrawables:
[IN](../app/src/main/res/drawable/aligner_status_in.xml) and
[OUT](../app/src/main/res/drawable/aligner_status_out.xml).
Original artwork authored by OpenAI Codex for Aligner Tracker on 2026-09-10,
under the repository's GPL-3.0-or-later license. No competitor source, artwork,
image generation, external image package or runtime SVG renderer was used.

Both use the same simplified arch, tooth divisions and aligner outline in a
96 × 72 coordinate system. Only the groups' vertical placement changes. IN fits
the aligner around the arch; OUT separates them with roughly 11 coordinate units
of clear space. The lines are 1.5–2.2 units, with round ends/joins. The illustration
uses a single theme `onSurfaceVariant` color and an 88 × 66dp frame. Geometry carries
the distinction in monochrome. `autoMirrored=false` keeps anatomy independent of RTL.
There are no arrows, checks, crosses, case, implied activity or animation.

This replaces the large status heading. Session context, duration, start timestamp,
recorded wear, daily breakdown, optional treatment invitation and corrections keep
their existing order and type styles. The normal status block is 30dp taller than
the previous 36sp heading at 100% font; at enlarged font the fixed-size illustration
avoids the large heading's wrapping. Bottom tracking and correction controls remain
in their existing layout, including the compact-height correction inside the scroll
area. The illustration has no container, button fill, click,
focus request or tooltip. Keeping it visually separate from the blue labelled action
is a design choice intended to distinguish current fact from next action.

## Accessibility and state ownership

`Image` has exactly one resource-backed content description: `state_in` or
`state_out`. These existing localized resources remain shared; English fallback and
debug pseudolocales are available, while full language delivery remains #31.
The action retains its native Button role and visible Take aligners out / Put
aligners in label. Its redundant stateDescription and live region are removed.
The status picture and timer are not live regions; normal focus traversal can read
the current state without repeated timer announcements. The visible This wear
session / This break text remains an independent reading cue.

The picture consumes the authoritative snapshot's last recorded state. A tap does
not change it; saving keeps the picture and disables tracking/correction as before.
Only a changed snapshot changes the picture. Repository rejection leaves the
picture and next action unchanged and retains the existing error flow.
Completion remains a textual, read-only state. No plan renders no Today content;
a plan with no recorded events gets a resource-backed Current status unavailable
heading instead of an invented OUT picture. Historical tracking gaps continue to
show uncertain duration through the existing rule; they do not erase the latest
explicitly recorded IN/OUT event. Partial prescription details remain unknown and
do not prevent displaying a genuinely recorded status.

No persistence, accounting, schema, backup, reminder, native picker, timezone,
widget/Wear, manifest, network or background-service code changes.

## Research applied, not comprehension evidence

Consulted the four supplied sources on 2026-09-10:

- [NN/g: Icon Usability](https://www.nngroup.com/articles/icon-usability/): unfamiliar
  icons can be ambiguous; keep simple geometry and visible contextual words. Removing
  the large direct status label is an owner-requested design tradeoff, not a finding
  that an illustration alone is universally understood.
- [NN/g: Testing Digital Icons](https://www.nngroup.com/articles/how-to-test-digital-icons/):
  recognition and interpretation require testing with people, both alone and in
  context. Screenshots establish neither. An owner follow-up can ask what is recorded
  now and what tapping the labelled button would record, for both states.
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults):
  provide an image description and retain Material button roles and targets.
- [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics):
  expose the status once, keep state and action separate, and avoid unnecessary live
  regions. Tests inspect semantics and exported nodes, not TalkBack speech quality.

[Validation and review](validation-status.md) separates rendered/test evidence from
human comprehension, physical-device and owner aesthetic acceptance.
