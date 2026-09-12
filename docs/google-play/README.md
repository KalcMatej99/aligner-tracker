# Google Play closed-test preparation

Owner-authorized preparation for issue #60, based on private 1.1.0 (code 12).
Current candidate: **1.1.1, versionCode 14**; see [release 14](../release14.md).
The original runtime validation below covers **1.1.0, versionCode 13**. The higher code permits an in-place update;
it does not publish a new private-store version or indicate Play approval.

## Prepared deliverables

- Settings → Privacy policy: full policy available offline, with accessible headings, scrolling and a fixed close control. Settings → About health tracking: explicit role/health disclaimer and professional-advice reminder. No permission or network request to read either.
- [Public policy text](privacy-policy.md), generated from the exact English app resources using `python3 scripts/render-privacy-policy.py`. Run with `--check` to verify parity. Public contact: **me@matejkalc.com**, explicitly provided by the owner.
- [Listing and original source assets](listing.md): title, short/full description, release notes, app icon, feature graphic and actual synthetic-record phone screenshots.
- [Declaration worksheet](declarations.md): health features, data inventory, Wear/export boundaries and owner-dependent Console fields.
- Local signed AAB, universal APK and device-specific split APK set, with exact committed source and notices. See [validation](validation.md). The signing process uses the existing owner key locally; no key is uploaded/exported to Google.
- Reusable `scripts/build-play-candidate.sh`, `scripts/verify-play-native.py` and public/in-app policy parity check. Output is outside Git; no APKs, keys or passwords are committed.

## Public policy location

Candidate policy URL:
https://matejkalc.com/aligner-tracker/privacy.html

The policy is a standalone HTML page on the owner's public website. It has no
JavaScript, analytics, form or login. The candidate source is also available at
https://matejkalc.com/aligner-tracker/source/1.1.0-13.tar.gz with build instructions,
GPL license and dependency notices. Source commit and checksum accompany it at
https://matejkalc.com/aligner-tracker/source/1.1.0-13.txt.

Forgejo's public DNS resolves to a Tailscale address: its repository URL is **not**
a suitable globally accessible Play policy or source link. Neither private service
was exposed or reconfigured. Regenerate/copy the HTML to portfolio
`public/aligner-tracker/privacy.html` when the canonical policy changes; verify
byte parity and public HTTPS after deployment.

## Owner steps before uploading

1. Create your own Play Console account, pay Google's one-time registration fee and
   complete identity/contact and any Android-device verification. Do not give an
   agent your Google password, payment details or verification documents.
2. Confirm developer identity, public support email (provided), target age groups,
   countries, content rating questionnaire and the health/data declarations against
   the live Console. The declaration worksheet is not a submitted certification.
3. Choose Play App Signing deliberately. To preserve upgrades between Kalc Apps and
   Play under `org.alignertracker.app`, retain the existing signing identity through
   Google's supported existing-key enrollment process. That transfers a private-key
   copy to Google and needs explicit owner approval; no such transfer has occurred.
   A separate upload key can then be configured. Do not accidentally accept an
   unrelated signing key and assume existing installations can update from Play.
4. Upload the approved phone AAB to an internal/closed test, then install **Google
   Play's delivered split APKs** and run the smoke checklist below. Local bundletool
   APKs exercise the bundle format but are not proof of Google's delivery/signing.
5. For a new personal account, recruit at least 12 genuine testers opted in for 14
   consecutive days, gather actual usage/feedback and apply for production access.
   Passing the duration/count alone is not guaranteed approval. No testers or
   testimonials are fabricated by this preparation.

## Test session checklist — record actual outcomes

Use synthetic data first. Record device, Android version, build code, date and outcome.
Do not uninstall the owner's existing app or clear its data to make a test pass.

| Journey | What to verify | Evidence boundary |
| --- | --- | --- |
| Fresh install | Deliberate IN/OUT startup, optional target, native pickers | Local emulator and future Play build |
| Upgrade | Existing records, preferences and photos retained | Local signed bundle splits; repeat on Play delivery |
| Tracking | Both directions, correction validation, process restart | Emulator plus real-device use |
| Reminders | Deny/grant notifications, exact access denied/granted, reboot, snooze and stale actions | Accelerated emulator tests supplement physical delivery |
| Backup | Encrypted export/restore, wrong password, cancellation, photo restoration | Never replace personal records during a smoke test |
| Photos | Pick/camera, cancel, compare, deletion and chosen export destination | External picker/camera behavior needs real-device review |
| Privacy | Policy and health notice readable offline; close and Back work at large font | Semantic and screenshot tests are not TalkBack speech acceptance |
| Accessibility | Screen reader order, magnification, 200% font, dark mode and RTL | Human TalkBack and physical gestures remain open |
| 16 KB | ELF/ZIP/bundle alignment, explicit native-library load, normal app flows | x86_64 16 KB emulator; arm64 hardware remains separate |
| Watch | Do not advertise or publish the Wear companion in this phone-only test | Paired/physical Wear acceptance remains open |

Do not close the existing physical acceptance issues based on this checklist or a
version label. No account, analytics, Health Connect, backend, schema migration,
background service or network permission is added by this work.

## Source guidance

- [Play account setup](https://support.google.com/googleplay/android-developer/answer/6112435)
- [New personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [Preview assets](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Bundletool](https://developer.android.com/tools/bundletool)
- [16 KB compatibility](https://developer.android.com/guide/practices/page-sizes)

Checked 2026-09-11. The current Android page lists February 1, 2027 for its update
submission enforcement date, superseding older November 2025 articles. This candidate
checks compatibility now; no deadline extension is used as a substitute for validation.
