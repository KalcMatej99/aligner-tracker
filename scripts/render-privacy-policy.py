#!/usr/bin/env python3
"""Render the public policy from the exact English text shipped offline in the app."""
from pathlib import Path
import argparse
import html
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
sections = []
for section in ['owner', 'records', 'photos', 'exports', 'watch', 'permissions', 'deletion', 'contact']:
    title = html.escape(strings[f'policy_{section}_title'])
    body = html.escape(strings[f'policy_{section}_body'])
    body = body.replace('me@matejkalc.com', '<a href="mailto:me@matejkalc.com">me@matejkalc.com</a>')
    sections.append(f'<section><h2>{title}</h2><p>{body}</p></section>')
page = '<!doctype html>\n<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Aligner Tracker privacy policy</title><meta name="description" content="Privacy policy for Aligner Tracker: local records, photos, exports, optional watch communication and deletion."><style>html{color-scheme:light dark}body{overflow-wrap:anywhere;font:1.125rem/1.65 system-ui,sans-serif;margin:0;padding:1.5rem;color:#252a32;background:#fff}main{max-width:44rem;margin:auto}h1{font-size:2rem;line-height:1.2}h2{font-size:1.25rem;margin-top:2rem}p{overflow-wrap:anywhere}a{color:#245bc3}a:focus-visible{outline:3px solid currentColor;outline-offset:4px}@media(prefers-color-scheme:dark){body{color:#e5e7ec;background:#11151b}a{color:#acc5ff}}</style></head><body><main><h1>Aligner Tracker privacy policy</h1><p>' + html.escape(strings['publishing_updated']) + '</p>' + ''.join(sections) + '</main></body></html>\n'
outputs = {output: text, root / 'docs/google-play/privacy-policy.html': page}
for path, content in outputs.items():
    if args.check:
        if path.read_text() != content:
            raise SystemExit('Public and in-app policy differ: run scripts/render-privacy-policy.py')
    else:
        path.write_text(content)
