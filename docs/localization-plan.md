# Multilingual implementation plan

Prepared 2026-09-09 for [Forgejo #31](https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker/issues/31). Planning only: no translations, application changes or new APK publication have been performed. Inspection baseline: `31347ee` (the published phone pilot's application code is `82a11d0`). The KB had no relevant project localization decision; the findings below come from the checkout and current official documentation.

## Outcome and language scope

A user can run the complete phone experience in their chosen language, including reminders, widgets, errors, accessibility descriptions and exported photo presentations. Changing language preserves treatment history, pending actions and drafts. The watch presents translated acknowledged/pending/rejected states without changing synchronization semantics.

The owner requested major languages plus English and explicitly excluded Slovene. The following concrete selection interprets that request; the owner did not individually select these languages or regional variants. Target one multilingual release with English and 11 new languages, implemented and reviewed in batches. Additional locales should require resources and review rather than language-specific branches in tracking code.

| Language | Locale / resource directory | Picker label |
| --- | --- | --- |
| English (complete fallback) | `en` / `values` | English |
| Spanish | `es` / `values-es` | Español |
| French | `fr` / `values-fr` | Français |
| German | `de` / `values-de` | Deutsch |
| Italian | `it` / `values-it` | Italiano |
| Portuguese (Brazil) | `pt-BR` / `values-pt-rBR` | Português (Brasil) |
| Russian | `ru` / `values-ru` | Русский |
| Chinese (Simplified script) | `zh-Hans` / `values-b+zh+Hans` | 简体中文 |
| Japanese | `ja` / `values-ja` | 日本語 |
| Korean | `ko` / `values-ko` | 한국어 |
| Arabic | `ar` / `values-ar` | العربية |
| Hindi | `hi` / `values-hi` | हिन्दी |

Slovene is out of scope. Portuguese for Portugal and Traditional Chinese are not implicitly covered by the Brazilian/Simplified variants; test Android locale matching/fallback and advertise the precise supported variants. These can be distinct follow-up translations. This is a practical language set, not a claim of a statistically optimal market ranking.

Bundle all approved translations in the APK so language selection works offline. Keep “Aligner Tracker” as the product name. Default to **Follow system**, with the native language names above in the phone picker. English remains an explicit choice even on a translated device. A supported language may still have regional number/date conventions; avoid equating language with country or treatment timezone.

## Current repository findings

| Surface | Evidence | Work needed |
| --- | --- | --- |
| Phone resources | Six `app/src/main/res/values/*strings.xml` files: 359 string entries and 2 plural resources; no translated resource directories | Review all translatable entries, including accessibility, reminder, control and error resources; inventory is a baseline, not a claim that every visible message is extracted |
| Phone locale selection | `MainActivity.kt` extends `ComponentActivity`; no locale configuration or picker; theme is a platform Material theme | Add supported-language declaration and compatible activity/theme setup for a picker |
| Formatting | `Screens.kt` already reads Compose configuration locale and uses localized dates/BidiFormatter; `JournalScreen.kt` uses `Locale.getDefault()`; reports have separate formatters | Consolidate display formatting around the effective app locale and explicit treatment zone |
| Watch | Only 4 string resources; English branches in `WearMainActivity.kt`, `AlignerTileService.kt`, `AlignerComplicationService.kt` | Extract main UI, rejection/sync errors, Tile actions, short complication text and full spoken descriptions |
| Background text | `ReminderScheduler.kt`, `TrackerWidget.kt`, `ResourceUserErrorText` resolve resources through supplied contexts; ViewModel gets application context | Resolve the current app locale outside activities and refresh visible surfaces after a language change |
| Exports | `PhotoTimeLapse.kt` embeds English HTML/JS and `lang="en"`; `BackupCodec.kt` has fixed CSV headers | Translate the presentation export safely; preserve machine-readable contracts |
| Existing readiness | Both manifests support RTL; both debug builds enable pseudolocales; `AccessibilityLocalizationTest.kt` covers some dates, Unicode numeric input and RTL | Extend the existing coverage to real translated resources and runtime switches; do not count readiness tests as translated-language acceptance |

## Implementation decisions and best practices

**Use native resources.** Keep complete English defaults under `values/`, with the approved locale directories above matching existing file boundaries. Missing locale resources fall back to defaults, but the release gate should require completeness for advertised languages. Use `stringResource`/`pluralStringResource` in Compose and resources in other surfaces. Keep complete messages, positional placeholders, translator comments and examples; do not assemble translated sentences from English fragments. Mark genuine identifiers/product names non-translatable. [Android localization](https://developer.android.com/guide/topics/resources/localization), [Compose resources](https://developer.android.com/develop/ui/compose/resources), [string resources](https://developer.android.com/guide/topics/resources/string-resource).

**Use Android's language selection.** AGP 8.13.2 supports generated LocaleConfig: enable `androidResources.generateLocaleConfig`, add `res/resources.properties` with the chosen English fallback tag, and restrict packaged release locales to the approved set. Verify the merged output, because dependencies can contribute languages. Do not also supply a manual LocaleConfig. For the phone's Android 8+ picker, use a pinned compatible AppCompat release, `AppCompatActivity`, an AppCompat-compatible host theme and `AppCompatDelegate.setApplicationLocales`; use an empty locale list for Follow system. Opt into `autoStoreLocales` for older Android and test synchronization with Android 13+ system app-language settings. Account for activity recreation. [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages).

Project-specific choice: use one platform/AppCompat locale authority rather than a second DataStore language preference. Preserve the existing disabled Android backup policy; the language setting does not require enabling treatment-data backup. The first implementation step must prove the activity/theme transition on the pinned Compose stack before expanding to all screens.

**Resolve non-activity text correctly.** Use the existing AndroidX Core support (`ContextCompat.getContextForLanguage`, available before the pinned Core 1.17.0) when retrieving strings from receivers, widgets, errors and exports. Obtain a fresh locale-aware context at rendering time rather than holding translated strings indefinitely. [ContextCompat reference](https://developer.android.com/reference/androidx/core/content/ContextCompat#getContextForLanguage(android.content.Context)).

Project-specific behavior: refresh widget text, notification channel labels and eligible currently displayed notifications when the app language changes or the system locale changes while following system. Keep channel IDs, user notification settings, alarm deadlines and command IDs unchanged; changing language must not make an alert sound again. Android permits updating existing channel names/descriptions; do not delete/recreate channels. [NotificationManager reference](https://developer.android.com/reference/android/app/NotificationManager#createNotificationChannel(android.app.NotificationChannel)).

**Translate quantities, not just words.** Audit counts beyond the two existing duration plurals. Arabic requires coverage of `zero`, `one`, `two`, `few`, `many`, `other`; Russian has distinct cardinal forms too. Test locale-specific examples including 0, 1, 2, 3, 5, 11, 21, 22, 25, 100 and 102, and verify the categories against CLDR. Japanese/Chinese/Korean must not inherit English singular/plural assumptions. Supply forms required by each target locale, including `other`, and preserve argument indices/types. Use Android's quantity selection, not an English `count == 1` branch. [Unicode CLDR plural rules](https://www.unicode.org/cldr/charts/48/supplemental/language_plural_rules.html), [Android plurals](https://developer.android.com/guide/topics/resources/string-resource#Plurals).

**Separate display from stored meaning.** This is a project invariant: locale changes must not modify timestamps, treatment-zone accounting, ISO dates, JSON field names, schema versions, enums, sync error codes or command receipts. Retain stable CSV headers/values and document it as an interoperable data export. Localize human-readable on-screen reports and HTML export text. Use locale-aware formatting for dates, numbers, decimal input and calendar labels, while preserving explicit treatment timezone and DST behavior. Existing user-entered notes, appointment titles and phase names remain exactly as entered; localize default labels at presentation time instead of rewriting saved text.

**Treat translation as reviewed product copy.** Prepare a glossary for aligner, tray, refinement, retention, prescribed target, tracked/untracked time, pending acknowledgement and confirmed recording. Include screen context and short-space limits for watch strings. Machine-generated drafts need fluent review, especially delete/replace, backup/password, permissions and treatment wording. Use only application copy and synthetic examples in translation work. Keep translations in Git; a hosted translation service is optional later, not a prerequisite or an in-app network dependency.

**Make layout flexible.** Preserve start/end layout and appropriate directional icons. Keep identifiers/timezone tokens readable in mixed-direction text, and do not mirror photographs or data indiscriminately. Test translated text at 200% font rather than shrinking labels to fit. Android's `en-XA` and `ar-XB` pseudolocales expose expansion, hardcoded strings and bidirectional problems; retain these in debug testing and exclude them from release language choices. [Language and culture support](https://developer.android.com/training/basics/supporting-devices/languages), [pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales).

## Ordered work packages

| Step | Files / scope | Acceptance before moving on |
| --- | --- | --- |
| 1. Inventory and freeze source copy | Existing resource files, all user-facing Kotlin/XML literals, watch surfaces, `PhotoTimeLapse.kt`; new translation guide/glossary | Classify visible text versus protocol/internal messages; complete English defaults; named placeholders/context; explicit intentional exceptions |
| 2. Prove phone locale lifecycle | `app/build.gradle.kts`, `MainActivity.kt`, manifest/theme, new locale helper, `SettingsScreen.kt` | Follow system and explicit language work on API26/32 and API33/36; picker/system settings agree; recreation/relaunch preserves tracking and form state; no duplicate operations |
| 3. Finish presentation extraction | Watch resource files/surfaces; `Screens.kt`, journal/reports, reminder/widget/error adapters; HTML exporter and its caller | No unintended English UI literals; no stale error/success strings after switching; background surfaces use effective locale; translated export remains valid/offline |
| 4. Translate complete locales | Mirrored approved locale resources in phone and wear; translator guide | All 11 new locales have full key coverage with reviewed exclusions, valid placeholders/plurals, consistent terminology and fluent review status per language |
| 5. Validate integration | Resource audit script, existing unit/instrumentation suites, locale-switch tests, layout snapshots, HTML browser checks | Matrix below passes; no changes to stored records/backup/protocol; no mixed-language or clipped critical UI |
| 6. Prepare a signed update | Release notes/version metadata, same permanent app key, source/license bundle, private store metadata | Higher versionCode than installed pilot 2; same-key upgrade preserves records; only verified completed languages advertised; distribution tracked under #21 |

Translation batches: first Spanish/French/German/Italian/Brazilian Portuguese, then Russian/Chinese/Japanese/Korean, then Arabic/Hindi. Prototype Arabic RTL and CJK/Indic rendering during step 2 so layout problems are discovered early; do not defer them until the final translation batch. With 359 existing phone string entries, 11 new languages mean roughly 3,949 string translations before new extraction, plural forms and watch resources. Budget fluent review per language explicitly. A language is not complete merely because an agent generated XML. If reviewer availability limits delivery, report that language as pending rather than silently reducing the requested set or advertising it as ready.

Dependencies: step 1 precedes extraction/translations; step 2 validates the risky lifecycle choice early; step 5 follows integration. Use one primary agent by default. If helpers are used later, give one phone/background ownership and one watch/translation ownership after the shared locale/glossary contract is fixed; no concurrent edits to the same resource files.

Watch policy for the first iteration: translate all watch surfaces and follow the watch's effective device locale. Do not silently copy the phone preference or add a language field to the Wear protocol. A separate watch picker is a later UX choice; phone/watch may legitimately use different languages. Request Tile/complication updates when locale changes and check their own service contexts. Short complication labels need compact translations plus complete accessibility descriptions. Phone publication need not wait for unavailable physical watch acceptance, which remains explicit in #20.

HTML export policy: capture the selected display language at export time, populate title/instructions/controls/alt text and correct `lang`/`dir`. Use proper HTML escaping and safe JSON serialization for strings inserted into scripts, including protection against closing-script sequences. Avoid interpolating translated apostrophes directly into JavaScript literals. Keep canonical frame dates available, preserve private-photo limits and verify Play/Pause/slider and non-Latin text in a browser.

## Verification and completion

- Resource checks: complete default set; missing/extra translation keys; format-argument compatibility; required plural cases; no release pseudolocales or unintended dependency languages. Enable relevant missing/extra translation and format lint checks. A source-literal audit complements lint, which alone does not prove full Compose/HTML extraction.
- Phone matrix: API26, API32, API33 and API36; English plus each released language; supported/unsupported system languages; regional variants; Follow system and explicit override; process restart and system settings changes. Exercise language changes with OUT timer, pending reminders, widget, unsaved note and backup preview. Preserve exact synthetic records and no repeated actions.
- Formatting: 21.5/21,5-hour entry, existing Unicode digits, month/year boundaries, treatment timezone different from device timezone, DST and numeric count cases. Display locale must not change recorded totals or import results.
- Visual/accessibility: main and secondary journeys, empty/loading/error states, destructive/restore dialogs, 320dp/200% font, landscape, dark/light, en-XA and ar-XB. Real Arabic RTL is mandatory for this release, including mixed Latin app names and timezone/date tokens; test Cyrillic, CJK, Korean and Devanagari glyphs, wrapping and shaping on minimum supported APIs, not just pseudolocales. Human review of the actual wording of every advertised language and TalkBack remains distinct from automation.
- Wear: round small/large emulator layouts, acknowledged/pending/rejected/unknown/completed states, Tile and complication text/descriptions. Validate offline/retry semantics unchanged. Physical paired tests stay open until hardware evidence exists.
- Run focused checks during implementation, then `scripts/check.sh` at integration; execute the relevant instrumentation matrix and retain results/screenshots. Verify a same-key installed upgrade from the published phone pilot with synthetic records, plus cross-language backup export/import. Locale preference stays device-local; portable treatment archives retain their current schema.

Finish condition: each advertised language is complete and reviewed, selection and every covered surface behave correctly, data/upgrade checks pass, and exact remaining physical gates are documented. No new translation backend, schema migration, app permission or stable-release claim is needed. This plan itself does not publish an update.
