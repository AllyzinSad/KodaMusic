#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ "$(uname -s)" != Linux ]]; then
  echo 'Este build precisa rodar em Linux x64.' >&2; exit 1
fi
if ! command -v java >/dev/null || ! command -v jpackage >/dev/null; then
  echo 'Instale JDK 21 (incluindo jpackage) antes de compilar.' >&2; exit 1
fi
if ! command -v mpv >/dev/null; then
  echo 'Para reproduzir no Linux, instale mpv: sudo apt install mpv' >&2
fi
chmod +x ./gradlew
./gradlew :desktop:linuxPortableTar --stacktrace
printf '\nPacote: release/KodaMusic-3.11.0-Linux-x64.tar.gz\n'
