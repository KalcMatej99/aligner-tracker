#!/usr/bin/env python3
"""Check 64-bit ELF load segments and APK ZIP alignment without extracting secrets.

Usage: verify-play-native.py APK --zipalign /path/to/build-tools/zipalign
ELF checks apply to libraries bundled transitively as well as first-party code.
This is static evidence; it does not replace runtime testing on a 16 KB device.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import zipfile

p = argparse.ArgumentParser()
p.add_argument('apk', type=Path)
p.add_argument('--zipalign', required=True)
a = p.parse_args()
results = []
with zipfile.ZipFile(a.apk) as archive:
    for name in archive.namelist():
        if not name.endswith('.so') or not name.startswith(('lib/arm64-v8a/', 'lib/x86_64/')):
            continue
        data = archive.read(name)
        if data[:6] != b'\x7fELF\x02\x01':
            raise SystemExit(f'Unexpected ELF format: {name}')
        offset = struct.unpack_from('<Q', data, 32)[0]
        size, count = struct.unpack_from('<HH', data, 54)
        loads, relro = [], []
        for index in range(count):
            kind, flags, file_offset, virtual, physical, filesz, memsz, align = struct.unpack_from('<IIQQQQQQ', data, offset + index * size)
            if kind == 1:
                if align < 16384 or (virtual - file_offset) % 16384:
                    raise SystemExit(f'Unaligned LOAD: {name}')
                loads.append(align)
            if kind == 0x6474e552:
                relro.append((virtual + memsz) % 16384)
        if not loads:
            raise SystemExit(f'No LOAD segments: {name}')
        results.append({'library': name, 'load_alignments': loads, 'relro_end_mod_16384': relro})
subprocess.run([a.zipalign, '-c', '-P', '16', '4', str(a.apk)], check=True, stdout=subprocess.PIPE)
print(json.dumps({'apk_sha256': hashlib.sha256(a.apk.read_bytes()).hexdigest(), 'zip_alignment_16kb': True, 'elf_libraries': results, 'runtime_test_required': True}, indent=2))
