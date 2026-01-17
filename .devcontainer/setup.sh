#!/bin/bash

# Android SDK installieren
export ANDROID_HOME=/opt/android-sdk
mkdir -p $ANDROID_HOME

# Command Line Tools herunterladen
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip -d $ANDROID_HOME
mkdir -p $ANDROID_HOME/cmdline-tools/latest
mv $ANDROID_HOME/cmdline-tools/* $ANDROID_HOME/cmdline-tools/latest/ 2>/dev/null || true

# SDK Components installieren
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Gradle Wrapper ausführbar machen
chmod +x gradlew

echo "Android SDK Setup abgeschlossen!"
