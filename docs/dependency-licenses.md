# Dependency and distribution license review

Status: evidence snapshot for release preparation, generated 2026-09-08. It is not a legal clearance or a release approval.

## Audited graph

The audit covers the resolved `releaseRuntimeClasspath` graphs for `:app` and `:wear`. It excludes build plugins, test-only dependencies, Android platform files and tools that are not packaged at runtime. The source reports and generated inventory had these SHA-256 hashes:

| Evidence | SHA-256 |
|---|---|
| Phone Gradle report | `07bdc2e59617a611758e573b5f89a6eab54bded71679f15a9e08a96329ecba5d` |
| Wear Gradle report | `38f94d3a6e8a397677688ecb5626f41ef663cdad9b97276704d43108ee0e574c` |
| Markdown inventory | `9720cff8a4ed58e8c118e4a357fc48eb195d08b2716d759441fee3b64542cd3f` |

The combined graph contains 171 unique coordinates: 21 direct and 150 transitive. Local Gradle-cache POMs declared a license for every coordinate: 169 Apache-2.0 variants and two BSD-3-Clause components. No `com.google.android.gms` artifact or Android SDK License declaration appears in either resolved runtime graph. “Declared” describes the POM evidence; it does not independently prove copyright ownership or notice completeness. This project's license remains GPL-3.0-or-later.

The two non-Apache entries are:

| Dependency | Relationship | POM declaration |
|---|---|---|
| `androidx.datastore:datastore-preferences-external-protobuf:1.2.1` | transitive | BSD-3-Clause |
| `androidx.wear.protolayout:protolayout-external-protobuf:1.4.2` | transitive | BSD-3-Clause |

Both modules directly use `org.microg.gms:play-services-wearable:0.3.14.250932`, with Apache-2.0 base, basement and tasks dependencies under the same `org.microg.gms` group. `kotlinx-coroutines-play-services` was removed. The inventory records exact resolved versions, direct/transitive relationships, POM declarations and any local archive `META-INF/LICENSE*` or `META-INF/NOTICE*` evidence. The checked shared `wear-transport` source is also part of the corresponding application source; it is not an additional Maven artifact.

Regenerate the evidence from a clean release candidate after dependency resolution, without committing the reports:

```bash
./gradlew --no-daemon :app:dependencies --configuration releaseRuntimeClasspath \
  > /tmp/aligner-phone-microg-dependencies.txt
./gradlew --no-daemon :wear:dependencies --configuration releaseRuntimeClasspath \
  > /tmp/aligner-wear-microg-dependencies.txt
python3 scripts/license-inventory.py \
  /tmp/aligner-phone-microg-dependencies.txt \
  /tmp/aligner-wear-microg-dependencies.txt \
  > /tmp/aligner-microg-license-inventory.md
sha256sum /tmp/aligner-{phone,wear}-microg-dependencies.txt \
  /tmp/aligner-microg-license-inventory.md
```

First verify both Gradle reports completed successfully and contain no `FAILED` dependency entries: the inventory parser skips those entries. The script exits nonzero when the parsed coordinates have missing or unresolved POM license evidence. Review the complete output manually, archive it beside the candidate artifacts, and compare its hashes and counts with the release manifest. A successful run does not replace review of the actual license texts and required notices.

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
| `com.google.guava:listenablefuture:1.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.okio:okio:3.7.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.okio:okio:3.9.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.okio:okio-jvm:3.7.0` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.okio:okio-jvm:3.9.1` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `com.squareup.wire:wire-runtime:4.9.9` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0) | none found |
| `com.squareup.wire:wire-runtime-jvm:4.9.9` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0) | none found |
| `org.jetbrains:annotations:23.0.0` | transitive | The Apache Software License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlin:kotlin-stdlib:2.3.21` | direct | Apache-2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlin:kotlin-stdlib-common:2.3.21` | transitive | Apache-2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10` | transitive | The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10` | transitive | The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2` | direct | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-bom:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0` | direct | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0` | transitive | Apache-2.0 (https://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.jspecify:jspecify:1.0.0` | transitive | The Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.microg.gms:play-services-base:0.3.14.250932` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.microg.gms:play-services-basement:0.3.14.250932` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.microg.gms:play-services-tasks:0.3.14.250932` | transitive | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |
| `org.microg.gms:play-services-wearable:0.3.14.250932` | direct | The Apache Software License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0.txt) | none found |

Coordinates: 171; unresolved POM license declarations: 0.

## Distribution assessment

The project remains GPL-3.0-or-later, as stated in README and CONTRIBUTING. No license exception or relicensing was added. The bundled Wear client is now microG `0.3.14.250932`: its [tagged source](https://github.com/microg/GmsCore/tree/v0.3.14.250932/play-services-wearable) and [license](https://github.com/microg/GmsCore/blob/v0.3.14.250932/LICENSE) identify Apache-2.0. Its base, basement and tasks artifacts also declare Apache-2.0 in the resolved POMs. The shared `wear-transport` adapter is project source compiled into both APKs; imports under `com.google.android.gms` name microG's compatible API namespace, not evidence of a proprietary Maven artifact.

The fresh graphs above replace the earlier audit of four proprietary Google client artifacts; none of those coordinates or their Android SDK License declarations remains in this runtime inventory. Apache-2.0 code can be included in GPLv3 projects under the [Apache Software Foundation's compatibility guidance](https://www.apache.org/licenses/GPL-compatibility). This resolves the specific bundled proprietary-client blocker. Distribution still requires exact corresponding source, build scripts, GPL text and the applicable Apache/BSD license texts and notices. A POM inventory is not a completed notice bundle; absence of a notice at the script's inspected archive paths does not waive upstream license obligations.

Runtime is a separate boundary. The APKs contain a free client library that communicates with an installed Wear Data Layer service. It neither bundles nor installs the Google Play services application. The phone's treatment features work without that service; paired watch communication requires a compatible service/pairing stack. Current binder, capability/node-query and same-node message/listener acceptance used official Google Play services, not a replacement microG service runtime. Paired phone/watch delivery remains unverified. No claim is made that using a free client removes Google's transport infrastructure; see [privacy](privacy.md) and [Wear validation](wear.md).

F-Droid's current [inclusion policy](https://f-droid.org/en/docs/Inclusion_Policy/) permits freely licensed Maven dependencies from trusted repositories, requires source/build review, and excludes embedded proprietary libraries. The updated runtime graph addresses the old embedded-library problem. It does not establish F-Droid build acceptance. F-Droid also documents [Non-Free Dependencies](https://f-droid.org/en/docs/Anti-Features/#Non-Free-Dependencies) for free apps requiring separately installed non-free software, and Non-Free Addons for promotion of non-free software. These labels do not themselves mean proprietary code is bundled.

Assessment for a future submission: disclose the useful standalone phone app with optional Wear integration separately from the companion's transport runtime requirement. Ask maintainers to determine applicable labels and inclusion after inspecting build provenance and actual functionality. Do not state either definite exclusion based on the removed Google AARs or guaranteed acceptance because the client is Apache-licensed. No F-Droid metadata, build or maintainer approval is recorded; source-built packaging and [release acceptance](release.md) remain separate work before authorized publication.
