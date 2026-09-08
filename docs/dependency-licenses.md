# Dependency and distribution license review

Status: evidence snapshot for release preparation, generated 2026-09-08. It is not a legal clearance or a release approval.

## Audited graph

The audit covers the resolved `releaseRuntimeClasspath` graphs for `:app` and `:wear`. It excludes build plugins, test-only dependencies, Android platform files and tools that are not packaged at runtime. The source reports and generated inventory had these SHA-256 hashes:

| Evidence | SHA-256 |
|---|---|
| Phone Gradle report | `555df6c982686eaa190e1bb53a2e47d09021cc044def56d5e6da6b14e28a798f` |
| Wear Gradle report | `74f53e0df0f4ad5e95485f9f7d5e6949f7e204538cddccb3f1ffcfb72869bf5d` |
| Markdown inventory | `64421f4fdbba6220b1249eff8a675699d858d6395ab4a545437e4e8e5885f888` |

The combined graph contains 166 unique coordinates: 22 direct and 144 transitive. Local Gradle-cache POMs declared a license for every coordinate. The declarations group as 160 Apache-2.0 variants, two BSD-3-Clause components and four Google components declaring the Android Software Development Kit License. “Declared” describes the POM evidence; it does not independently prove copyright ownership, notice completeness or compatibility with this project's GPL-3.0-only license.

The non-Apache entries are:

| Dependency | Relationship | POM declaration |
|---|---|---|
| `androidx.datastore:datastore-preferences-external-protobuf:1.2.1` | transitive | BSD-3-Clause |
| `androidx.wear.protolayout:protolayout-external-protobuf:1.4.2` | transitive | BSD-3-Clause |
| `com.google.android.gms:play-services-wearable:20.0.1` | direct in phone and watch | Android Software Development Kit License |
| `com.google.android.gms:play-services-base:18.5.0` | transitive | Android Software Development Kit License |
| `com.google.android.gms:play-services-basement:18.9.0` | transitive | Android Software Development Kit License |
| `com.google.android.gms:play-services-tasks:18.2.0` | transitive | Android Software Development Kit License |

The 22 direct coordinates are the Compose BOM, Activity Compose, Compose Foundation, Material 3, UI tooling preview, Core KTX, Concurrent Futures, DataStore Preferences, ExifInterface, Lifecycle runtime/view-model Compose, Room runtime/KTX, WorkManager runtime KTX, Wear ProtoLayout/Tiles/complication data-source KTX, Kotlin standard library, coroutines Android/Play Services, serialization JSON, and `play-services-wearable`. The inventory script records the exact resolved version, direct/transitive relationship, POM declaration and any local archive `META-INF/LICENSE*` or `META-INF/NOTICE*` evidence for every coordinate.

Regenerate the evidence from a clean release candidate after dependency resolution, without committing the reports:

```bash
./gradlew --no-daemon :app:dependencies --configuration releaseRuntimeClasspath \
  > /tmp/aligner-phone-dependencies.txt
./gradlew --no-daemon :wear:dependencies --configuration releaseRuntimeClasspath \
  > /tmp/aligner-wear-dependencies.txt
python3 scripts/license-inventory.py \
  /tmp/aligner-phone-dependencies.txt \
  /tmp/aligner-wear-dependencies.txt \
  > /tmp/aligner-license-inventory.md
sha256sum /tmp/aligner-{phone,wear}-dependencies.txt \
  /tmp/aligner-license-inventory.md
```

The script exits nonzero if a coordinate, POM or POM license declaration is unresolved. Review the complete output manually, archive it beside the candidate artifacts, and compare its hashes and counts with the release manifest. A successful run does not replace review of the actual license texts and required notices.

## Full resolved inventory

This is the exact generated union of both audited runtime reports. Duplicate coordinates shared by phone and watch appear once.

| Dependency | Scope | POM license declarations | Local notice evidence |
|---|---|---|---|
| `androidx.activity:activity:1.11.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.activity:activity-compose:1.11.0` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.activity:activity-ktx:1.11.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.annotation:annotation:1.9.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.annotation:annotation-experimental:1.5.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.annotation:annotation-jvm:1.9.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.appcompat:appcompat:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.appcompat:appcompat-resources:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.arch.core:core-common:2.2.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.arch.core:core-runtime:2.2.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.asynclayoutinflater:asynclayoutinflater:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.autofill:autofill:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.collection:collection:1.5.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.collection:collection-jvm:1.5.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.collection:collection-ktx:1.5.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose:compose-bom:2025.11.01` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.animation:animation:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.animation:animation-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.animation:animation-core:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.animation:animation-core-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.foundation:foundation:1.9.5` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.foundation:foundation-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.foundation:foundation-layout:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.foundation:foundation-layout-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.material:material-ripple:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.material:material-ripple-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.material3:material3:1.4.0` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.material3:material3-android:1.4.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.runtime:runtime:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.runtime:runtime-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.runtime:runtime-annotation:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.runtime:runtime-annotation-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.runtime:runtime-saveable:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.runtime:runtime-saveable-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-geometry:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-geometry-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-graphics:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-graphics-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-text:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-text-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-tooling-preview:1.9.5` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-tooling-preview-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-unit:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-unit-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-util:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.compose.ui:ui-util-android:1.9.5` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.concurrent:concurrent-futures:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.concurrent:concurrent-futures:1.3.0` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.concurrent:concurrent-futures-ktx:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.concurrent:concurrent-futures-ktx:1.3.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | concurrent-futures-ktx-1.3.0.jar:META-INF/NOTICE.txt |
| `androidx.coordinatorlayout:coordinatorlayout:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.core:core:1.17.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.core:core-ktx:1.17.0` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.core:core-viewtree:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.cursoradapter:cursoradapter:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.customview:customview:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.customview:customview-poolingcontainer:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-android:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-core:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-core-android:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-core-okio:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-core-okio-jvm:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-preferences:1.2.1` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-preferences-android:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-preferences-core:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-preferences-core-android:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.datastore:datastore-preferences-external-protobuf:1.2.1` | transitive | BSD-3-Clause (https://opensource.org/licenses/BSD-3-Clause) | none found |
| `androidx.datastore:datastore-preferences-proto:1.2.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.documentfile:documentfile:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.drawerlayout:drawerlayout:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.emoji2:emoji2:1.4.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.exifinterface:exifinterface:1.4.1` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.fragment:fragment:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.graphics:graphics-path:1.0.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.interpolator:interpolator:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.legacy:legacy-support-core-ui:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.legacy:legacy-support-core-utils:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-common:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-common-java8:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-common-jvm:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-livedata:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-livedata-core:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-livedata-core-ktx:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-process:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-runtime:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-runtime-android:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-runtime-compose:2.9.4` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-runtime-compose-android:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-runtime-ktx:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-runtime-ktx-android:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-service:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel-android:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.9.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.loader:loader:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.localbroadcastmanager:localbroadcastmanager:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.preference:preference:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.print:print:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.profileinstaller:profileinstaller:1.4.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.recyclerview:recyclerview:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.room:room-common:2.8.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.room:room-common-jvm:2.8.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.room:room-ktx:2.8.4` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.room:room-runtime:2.8.4` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.room:room-runtime-android:2.8.4` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.savedstate:savedstate:1.3.3` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.savedstate:savedstate-android:1.3.3` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.savedstate:savedstate-compose:1.3.3` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.savedstate:savedstate-compose-android:1.3.3` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.savedstate:savedstate-ktx:1.3.3` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.slidingpanelayout:slidingpanelayout:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.sqlite:sqlite:2.6.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.sqlite:sqlite-android:2.6.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.sqlite:sqlite-framework:2.6.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.sqlite:sqlite-framework-android:2.6.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.startup:startup-runtime:1.1.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.swiperefreshlayout:swiperefreshlayout:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.tracing:tracing:1.2.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.tracing:tracing-ktx:1.2.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.vectordrawable:vectordrawable:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.vectordrawable:vectordrawable-animated:1.1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.versionedparcelable:versionedparcelable:1.1.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.viewpager:viewpager:1.0.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.protolayout:protolayout:1.4.2` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.protolayout:protolayout-expression:1.4.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.protolayout:protolayout-expression-pipeline:1.4.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.protolayout:protolayout-external-protobuf:1.4.2` | transitive | BSD-3-Clause (https://opensource.org/licenses/BSD-3-Clause) | none found |
| `androidx.wear.protolayout:protolayout-material-core:1.4.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.protolayout:protolayout-material3:1.4.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.protolayout:protolayout-proto:1.4.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.tiles:tiles:1.6.2` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.tiles:tiles-proto:1.6.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.watchface:watchface-complications:1.3.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.watchface:watchface-complications-data:1.3.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.watchface:watchface-complications-data-source:1.3.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.work:work-runtime:2.11.2` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `androidx.work:work-runtime-ktx:2.11.2` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.google.android.gms:play-services-base:18.5.0` | transitive | Android Software Development Kit License (https://developer.android.com/studio/terms.html) | none found |
| `com.google.android.gms:play-services-basement:18.9.0` | transitive | Android Software Development Kit License (https://developer.android.com/studio/terms.html) | none found |
| `com.google.android.gms:play-services-tasks:18.2.0` | transitive | Android Software Development Kit License (https://developer.android.com/studio/terms.html) | none found |
| `com.google.android.gms:play-services-wearable:20.0.1` | direct | Android Software Development Kit License (https://developer.android.com/studio/terms.html) | none found |
| `com.google.guava:listenablefuture:1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.okio:okio:3.9.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.okio:okio-jvm:3.9.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains:annotations:23.0.0` | transitive | The Apache Software License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlin:kotlin-stdlib:2.3.21` | direct | Apache-2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlin:kotlin-stdlib-common:2.3.21` | transitive | Apache-2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2` | direct | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2` | direct | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-bom:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0` | direct | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jspecify:jspecify:1.0.0` | transitive | The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |

Coordinates: 166; unresolved POM license declarations: 0.

## Distribution assessment

The repository is GPL-3.0-only. The [GNU GPL FAQ](https://www.gnu.org/licenses/gpl-faq.en.html) explains that distributing a linked combined work requires compatible terms and corresponding source for the covered work, subject to the GPL's defined exceptions. Publish the exact tagged source and build instructions with any distributed binary; do not rely on a moving branch.

The full phone and watch builds directly include `play-services-wearable`, whose POM declares the proprietary [Android SDK License](https://developer.android.com/studio/terms.html). The [F-Droid developer FAQ](https://f-droid.org/en/docs/FAQ_-_App_Developers/) requires all dependencies to be free/libre software and specifically says apps using proprietary Google Play Services libraries cannot enter the main repository. Therefore the current full build is ineligible for the main F-Droid repository. Making every product feature free of charge does not change this result. A separate stripped flavor is outside the current scope.

Google's [Play Services overview](https://developers.google.com/android/guides/overview) also confirms that the client library communicates with the Google Play Services package and that devices without it are unsupported. Device support and software licensing are separate questions.

The available evidence does not establish that combining and distributing the GPL-3.0-only application with the proprietary wearable client library satisfies the GPL. Whether a GPL-defined exception or license-compatible packaging applies requires a concrete, documented analysis; this repository audit does not resolve it. Public direct-APK distribution remains gated on that resolution or removal/replacement of the proprietary client dependency. If the gate is cleared, the release archive still needs the project GPL text, the exact corresponding source, reproducible build instructions, this inventory, and all dependency notices required by their licenses.

For F-Droid-specific preparation, use its current [build documentation](https://f-droid.org/en/docs/Building_Applications/), [metadata reference](https://f-droid.org/en/docs/Build_Metadata_Reference/) and [anti-feature definitions](https://f-droid.org/en/docs/Anti-Features/). No F-Droid metadata or publication is authorized by this document.
