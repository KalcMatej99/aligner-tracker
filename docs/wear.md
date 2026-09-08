# Wear OS companion

The Wear OS app is a companion to the phone app. The phone remains the only authority for treatment state and history. The companion sends a requested IN or OUT state and displays the phone's acknowledged result. It does not copy event history, reports, notes, appointments, or photos to the watch.

## Install and connect

Build the phone and watch packages with the same application ID and signing certificate. Android's Wear OS Data Layer only connects matching application IDs and signatures. Install the phone APK on the paired phone and the `wear` APK on the watch, then open the phone app before the first watch sync.

The watch app shows one of these distinct states:

- `Acknowledged: IN` or `Acknowledged: OUT` is the last state confirmed by the phone.
- `Pending phone acknowledgement` is a locally durable request that has not been accepted or rejected.
- A stale or completed-treatment rejection states that nothing was recorded and asks the user to review the phone.
- `Phone status unknown` means the watch has no acknowledged snapshot and therefore cannot queue a change.

The main watch screen has large Record IN or Record OUT and Sync nearby phone controls. A Tile opens the same one-step action. The short-text complication displays IN, OUT, Setup, Done, or Sync from the last acknowledged local snapshot. Neither surface runs a ticking service.

## Reconciliation and privacy

Protocol version 1 uses request paths under `/aligner/v1`. Requests and responses are capped at 8 KiB. The phone publishes only:

- opaque generation and monotonic revision;
- wearing and completed state;
- whether a plan exists, current tray, and the last event timestamp;
- a durable accepted or rejected outcome for a command.

Every watch command has a UUID idempotency ID and the generation/revision of the last phone snapshot. The watch writes it to its Room outbox before attempting transport. It keeps the row pending through process death, a missing nearby phone, or a lost response, and retries the same ID on an explicit sync or reconnect. The phone stores the outcome for that ID, so retrying cannot create a second event.

Before applying a command, the phone checks its clock guard and then validates the command against the current generation and revision in the same repository transaction. An accepted state change uses the phone's receive time. A queued watch timestamp is informational and never inserts or reorders phone history. If phone state changed while the watch was offline, the phone rejects the stale request, sends its current snapshot, and the watch displays the rejection rather than claiming the requested state was recorded.

Transport uses `MessageClient.sendRequest` and `sendMessage`. Both sides resolve a capability and require `Node.isNearby` immediately before accepting or sending data. This excludes nodes that Play services reports as remote, and avoiding DataClient also avoids its persistent cloud-backed state synchronization. No DataClient state, backend, account, analytics, app `INTERNET` permission, event history, or photo transfer is used. Google documents that MessageClient has no persistence or retry; the durable outbox supplies that retry behavior.

`Node.isNearby` is an application acceptance boundary, not a cryptographic guarantee that Google infrastructure is never involved. Google's current Data Layer overview says all Data Layer clients may use Google-owned servers depending on available connections and advises assuming that this can happen. A release requirement that no payload may ever transit Google infrastructure is incompatible with Play services Data Layer; it would require removing the watch transport because Google also advises against opening low-level phone/watch sockets.

## Build and verification

Use the repository's configured JDK and SDK:

```bash
./gradlew :app:assembleDebug :wear:assembleDebug
./gradlew :app:testDebugUnitTest :wear:testDebugUnitTest
./gradlew :app:lintDebug :wear:lintDebug spotlessCheck
```

The watch tests use a real in-memory Room database. They verify that an offline command stays pending, reconnect resends the same ID, accepted responses alone become acknowledged, non-nearby pushes are ignored, and stale rejection adopts the current phone snapshot while preserving a visible rejected outcome.

For emulator acceptance, use a paired phone and Wear OS emulator with Google APIs/Play services. Google's supported virtual-device path is Android Studio's Wear OS emulator pairing assistant. The documented manual port-forward path, `adb -d forward tcp:5601 tcp:5601`, still requires the Wear OS companion app on the phone and its interactive **Pair with emulator** action; port forwarding alone does not create a Data Layer association.

Exercise OUT and IN from the app and Tile, disconnect the pair before creating a command, change the phone state, reconnect, and verify the watch shows a stale rejection while the phone history contains no watch-timestamp insertion. Add and tap the complication, restart the watch app with a pending command, and confirm the pending state survives. Physical-watch install, reconnect, Tile, complication, accessibility, and battery observations remain a separate release gate until recorded on hardware.

## Distribution implication

The Data Layer dependency is Google Play services. F-Droid's main repository inclusion policy requires application dependencies to be free and open source, so this Wear-enabled variant is not suitable for the main F-Droid repository. If F-Droid distribution is required, add a separate FOSS phone flavor that excludes the Wear bridge, listener, capability resource, and Play services dependency. A direct or Play-distributed phone/watch pair can retain the nearby Wear feature. There is no backend fallback in either variant.

## Primary references

- [Wear OS Data Layer overview](https://developer.android.com/training/wearables/data/overview)
- [Send and receive messages](https://developer.android.com/training/wearables/data/messages)
- [Connect a watch to a phone](https://developer.android.com/training/wearables/get-started/connect-phone)
- [MessageClient reference](https://developers.google.com/android/reference/com/google/android/gms/wearable/MessageClient)
- [Node.isNearby reference](https://developers.google.com/android/reference/com/google/android/gms/wearable/Node#isNearby())
- [Build Tiles](https://developer.android.com/training/wearables/tiles)
- [Expose complication data](https://developer.android.com/training/wearables/watch-faces/complications/data-sources)
- [F-Droid inclusion policy](https://f-droid.org/docs/Inclusion_Policy/)
