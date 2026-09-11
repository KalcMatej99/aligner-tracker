#!/usr/bin/env python3
"""Render the public policy from the exact English text shipped offline in the app."""
from pathlib import Path
import argparse
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
strings = {s.attrib['name']: ''.join(s.itertext()) for s in ET.parse(root / 'app/src/main/res/values/publishing_strings.xml').getroot()}
parts = ['# Aligner Tracker privacy policy', strings['publishing_updated']]
for section in ['owner', 'records', 'photos', 'exports', 'watch', 'permissions', 'deletion', 'contact']:
    parts.extend(['## ' + strings[f'policy_{section}_title'], strings[f'policy_{section}_body']])
text = '\n\n'.join(parts) + '\n'
p = argparse.ArgumentParser()
p.add_argument('--check', action='store_true')
args = p.parse_args()
output = root / 'docs/google-play/privacy-policy.md'
if args.check:
    if output.read_text() != text:
        raise SystemExit('Public and in-app policy differ: run scripts/render-privacy-policy.py')
else:
    output.write_text(text)
