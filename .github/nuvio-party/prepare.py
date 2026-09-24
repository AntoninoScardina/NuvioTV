#!/usr/bin/env python3
"""Adatta il build al fork "Nuvio Party" (solo in CI, non va committato il risultato).

- stesso pacchetto di Nuvio ufficiale: lo sostituisce (una sola app), aggiornamenti in-app dal fork
- nome app "Nuvio Party"
- aggiornamenti in-app dalle release del fork
- versione della build automatica

Uso: .github/nuvio-party/prepare.py <versionName> <versionCode> <owner/repo>
"""
import re
import sys
from pathlib import Path

version_name, version_code, repo = sys.argv[1], sys.argv[2], sys.argv[3]
owner, name = repo.split("/", 1)


def patch(path: str, pattern: str, replacement: str, count: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    new_text, found = re.subn(pattern, replacement, text, count=count, flags=re.MULTILINE)
    if found == 0:
        sys.exit(f"::error::Modello non trovato in {path}: {pattern!r} (Nuvio ha cambiato il file?)")
    file.write_text(new_text, encoding="utf-8")


gradle = "app/build.gradle.kts"
patch(gradle, r'^(\s*)versionCode = \d+$', rf"\g<1>versionCode = {version_code}")
patch(gradle, r'^(\s*)versionName = "[^"]*"$', rf'\1versionName = "{version_name}"')
patch(gradle, r'"GITHUB_OWNER", "\\"[^"\\]*\\""', f'"GITHUB_OWNER", "\\\\"{owner}\\\\""')
patch(gradle, r'"GITHUB_REPO", "\\"[^"\\]*\\""', f'"GITHUB_REPO", "\\\\"{name}\\\\""')
patch("app/src/main/res/values/strings.xml", r'<string name="app_name">[^<]*</string>',
      '<string name="app_name">Nuvio Party</string>')

print(f"Nuvio Party {version_name} ({version_code}) → aggiornamenti da {repo}")
