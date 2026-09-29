#!/usr/bin/env bash
# Gera o APK final assinado do MestreScan e publica como release no GitHub.
# O link de download do site aponta sempre para a release mais nova.
#
# Uso:   ./scripts/publicar-release.sh "O que mudou nesta versão"
# Guia:  docs/PUBLICAR-DOWNLOAD.md
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-$HOME/android-dev/jdk/Contents/Home}"
APKSIGNER="$HOME/android-dev/sdk/build-tools/35.0.0/apksigner"
REPO="SandiehMoreira/mestrescan"
NOTES="${1:-Nova versão do MestreScan.}"

if [ ! -f keystore.properties ]; then
    echo "ERRO: falta keystore.properties. Veja docs/PUBLICAR-DOWNLOAD.md (passo 1)."
    exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
    echo "ERRO: há alterações sem commit. Faça o commit antes de publicar."
    exit 1
fi
git fetch -q origin
if [ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]; then
    echo "ERRO: o código local está diferente do GitHub. Rode 'git push' (ou 'git pull') antes."
    exit 1
fi

VERSION=$(grep -E 'versionName *=' app/build.gradle.kts | head -1 | sed -E 's/.*"(.*)".*/\1/')
TAG="v$VERSION"
if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
    echo "ERRO: a versão $TAG já foi publicada. Aumente versionCode e versionName em app/build.gradle.kts."
    exit 1
fi

echo "==> Gerando APK assinado $TAG"
./gradlew -q clean assembleMestrecellRelease
APK=app/build/outputs/apk/mestrecell/release/app-mestrecell-release.apk

echo "==> Conferindo assinatura"
"$APKSIGNER" verify "$APK"

mkdir -p release
cp "$APK" release/MestreScan.apk

echo "==> Publicando release $TAG no GitHub"
gh release create "$TAG" release/MestreScan.apk \
    --repo "$REPO" \
    --title "MestreScan $VERSION" \
    --notes "$NOTES"

echo
echo "Pronto! Link fixo de download (use no site):"
echo "https://github.com/$REPO/releases/latest/download/MestreScan.apk"
