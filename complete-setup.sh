#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════╗"
echo "║  Glukose-Rechner - Komplettes Setup         ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

# 1. Android SDK
echo "[1/6] Installiere Android SDK..."
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME && cd $ANDROID_HOME
wget -q --show-progress https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
rm commandlinetools-linux-11076708_latest.zip
echo "✓ Android SDK installiert"

# 2. Umgebungsvariablen
echo "[2/6] Setze Umgebungsvariablen..."
cat >> ~/.bashrc << 'EOF'
export ANDROID_HOME=$HOME/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0
EOF
source ~/.bashrc
echo "✓ Umgebung konfiguriert"

# 3. SDK Komponenten
echo "[3/6] Installiere SDK Komponenten (dauert ca. 5 Min)..."
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "✓ SDK Komponenten installiert"

# 4. Gradle
echo "[4/6] Installiere Gradle..."
cd /tmp
wget -q https://services.gradle.org/distributions/gradle-8.2-bin.zip
unzip -q gradle-8.2-bin.zip
sudo mv gradle-8.2 /opt/gradle
sudo ln -s /opt/gradle/bin/gradle /usr/local/bin/gradle
echo "✓ Gradle installiert"

# 5. Projekt vorbereiten
echo "[5/6] Bereite Projekt vor..."
cd /workspaces/glucose-calculator
gradle wrapper --gradle-version 8.2
chmod +x gradlew
chmod +x build-apk.sh 2>/dev/null || true
echo "✓ Projekt bereit"

# 6. APK bauen
echo "[6/6] Baue APK (dauert 5-10 Min beim ersten Mal)..."
./gradlew assembleDebug

# Erfolgsmeldung
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "╔══════════════════════════════════════════════╗"
    echo "║          ✓✓✓ ERFOLGREICH! ✓✓✓              ║"
    echo "╚══════════════════════════════════════════════╝"
    echo ""
    echo "📦 APK Speicherort:"
    echo "   app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "📥 Nächster Schritt:"
    echo "   1. Rechtsklick auf die APK → Download"
    echo "   2. Auf Android-Gerät übertragen"
    echo "   3. Installieren & xDrip+ aktivieren"
    echo ""
else
    echo "❌ Build fehlgeschlagen - prüfe die Fehler oben"
    exit 1
fi
