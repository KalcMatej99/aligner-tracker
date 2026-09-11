# Play Console declarations worksheet

Preparation, not submitted declarations or legal certification. Check the live questions against the exact uploaded artifact and SDK behavior before submitting. The app has no developer-operated treatment backend or INTERNET permission, but that alone does not settle every Data safety question.

## Health apps

Declare health-related functionality: aligner wear and treatment schedule tracking, appointment reminders and private comparison photos. In the current form choose the category whose definition covers treatment/health management; inspect the actual wording rather than guessing a category ID. No Health Connect integration, research study, diagnostic model, sensor-based measurement, clinical decision support, prescription sale or claimed regulatory approval. The listing includes the health disclaimer and the app reminds users to follow their professional's instructions.

## Data inventory and declaration reasoning

| Surface | Data/use | Transfer and proposed treatment |
| --- | --- | --- |
| Phone database | Wear events, treatment, goals, schedule, notes, appointments | Local-only processing; not developer collection. Health information can occur in all these fields. |
| Photo copies | User-selected photos and captions | Local copies with metadata removed. No analysis or developer upload. |
| Preferences | Reminder/display choices | Local-only processing. |
| Exports | Records/photos chosen by user | Explicit user-selected document provider; may be cloud-backed. Assess Google’s user-initiated sharing exception; do not say exports can never leave the device. |
| Optional Wear Data Layer | IN/OUT, completion/setup status, tray number, last event time; command timestamps, generation/revision and random command IDs | Health-related status leaves the phone when a user pairs/uses a compatible watch. Google’s installed transport may use cloud routing. No photos/notes/appointments/full history are exchanged. Google documents encrypted Bluetooth transport and end-to-end encryption for its cloud relay. On that supported runtime, the documented end-to-end-encryption exception supports excluding this exchange from collection. This is documentation-based reasoning, not an independent transport audit. |
| Public support issues | Whatever a user deliberately posts | Separate public web service, not an automatic in-app report. Policy explicitly warns against posting health records, photos or backups. |

**Proposed Console answer: no user data collected or shared**, based on local-only processing, Google’s documented end-to-end-encrypted Wear relay, and explicit user-selected exports under the user-initiated-sharing exception. The [official Wear documentation](https://developer.android.com/training/wearables/data/overview#cloud) states the cloud relay is end-to-end encrypted; the [Data safety definition](https://support.google.com/googleplay/android-developer/answer/10787469) excludes qualifying end-to-end-encrypted transfers from collection. This is an inference applying those definitions to the inspected code, not independent cryptographic certification.

The phone includes the Wear listener even when the Wear APK is not published, so omission of a Wear listing does not remove that path. Confirm the live Console questions and supported runtime before submitting; if another transport/runtime or an SDK collects readable data, revise the answer. No third-party service behavior is silently assumed equivalent to Google's documented service.

No ads, analytics, sale of data or developer tracking. Do not claim external security review, a security certification, or full encryption of all local records. Password-protected archives are encrypted; plaintext exports and the local database are not separately encrypted by this app.

## Permissions

- POST_NOTIFICATIONS: optional break, tray and appointment reminders; test denial and revocation.
- SCHEDULE_EXACT_ALARM: optional special access, distinct from restricted USE_EXACT_ALARM (which is absent). Existing inexact behavior must remain usable when denied.
- RECEIVE_BOOT_COMPLETED: rebuild reminder scheduling after reboot/update.
- System photo/document pickers and external camera: chosen media and explicit exports; no broad image-library permission or direct camera permission requested.
- No INTERNET, location, contacts, Health Connect or full-screen-intent permission requested by the phone manifest. Verify the merged bundle manifest as well.

## Other Console answers

- App access: no credentials needed; provide a sample setup walkthrough.
- Ads: no.
- Account deletion: the app does not create app accounts. Existing local delete-data functionality remains available; this is not a new account-deletion web service requirement.
- Financial features, government affiliation, news: none advertised; answer actual form questions truthfully.
- Content rating and target audience: complete with owner; local photos/notes are not a public user-generated-content feed.
- Public support email: me@matejkalc.com, explicitly supplied by owner. Developer identity still requires Console verification.

## Primary sources checked 2026-09-11

- https://support.google.com/googleplay/android-developer/answer/10787469 — Data safety definitions and exceptions.
- https://support.google.com/googleplay/android-developer/answer/16679511 — health policy, privacy and disclaimer (read in the preceding review; direct reload was rate-limited during implementation).
- https://support.google.com/googleplay/android-developer/answer/14738291 — health declaration.
- https://developer.android.com/training/wearables/data/overview — installed Wear service transport boundary.
- https://developer.android.com/develop/background-work/services/alarms — alarm access behavior.
