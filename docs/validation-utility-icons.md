# Utility icon refinement

Owner request: use icons instead of text for some buttons.

Back now uses an RTL-aware arrow in both app-bar layouts. History edits and
note/appointment edits use a pencil; note/appointment/photo delete entry points
use a trash icon. The original vectors share the existing 24-unit, 1.8-unit stroke
style and inherit enabled/disabled theme colors. Native IconButton controls retain
48dp targets and localized accessible names; record names still identify the
specific item. Tracking, saving, appointment completion and delete confirmation
retain text. No data, callback, version or persistence changes.

Inspected emulator captures: [Back](screenshots/utility-icons/settings-back.png),
[History edit](screenshots/utility-icons/history.png). The icon size is subordinate
to content while its touch area remains full-size. Owner aesthetic acceptance is
not inferred from this inspection.

Spotless, phone debug/test builds and debug lint passed
([log](evidence/utility-icons/build-lint.txt)). The focused real-activity journeys,
accessibility/localization and TalkBack semantics suites passed **22/22**, including
correction callbacks, Back/draft recreation and named journal/photo actions
([log](evidence/utility-icons/instrumentation.txt)). Existing test selectors now use
accessible descriptions for Back and History edit. No physical device was changed.
These source changes are not included in published version 5.
