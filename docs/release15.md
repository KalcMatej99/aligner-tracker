# Version 1.1.2 (15) — clearer day timelines

Owner approved the OUT-block preview, requested removal of inline text, then
explicitly requested merge and deployment. PR #69 implements continuous outlined
OUT periods, the Now marker and explicit legend labels on Today and History.
Phone and Wear version metadata is 1.1.2 (15); app identity and storage schema are
unchanged. [Design, screenshots and focused validation](out-timeline.md).

Distribution scope is the existing Tailscale-only Kalc Apps phone repository at
https://apps.server.matejkalc.com. The installed phone app can update with the
existing owner signing identity. Older APK/source/notices downloads are retained.
Wear remains a local companion artifact. No Google Play upload, public F-Droid
update or physical-phone installation is included.

Release files and operational evidence are stored outside Git in
`/home/matejkalc/.local/share/aligner-tracker/releases/1.1.2-15/`:
signed phone/Wear APKs, signed phone AAB, corresponding source, notices, checksums,
signature checks, signed-emulator upgrade comparison, store publication and live
HTTPS verification. The source commit recorded there and tag `v1.1.2-15` identify
the built release. Issue #68 records the final deployment receipt.
