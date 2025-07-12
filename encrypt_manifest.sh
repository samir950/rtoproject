#!/bin/bash

# Script to encrypt the real manifest before building
# This should be run as part of your build process

echo "Encrypting AndroidManifest.xml..."

# Paths
REAL_MANIFEST="app/src/main/assets/real_manifest.xml"
ENCRYPTED_OUTPUT="app/src/main/assets/encrypted_manifest.dat"
TEMP_CLASSES="app/build/tmp/kotlin-classes/debug"

# Ensure the real manifest exists
if [ ! -f "$REAL_MANIFEST" ]; then
    echo "Error: Real manifest not found at $REAL_MANIFEST"
    exit 1
fi

# Create assets directory if it doesn't exist
mkdir -p "app/src/main/assets"

# Compile the encryption class first
echo "Compiling encryption classes..."
./gradlew compileDebugKotlin

# Run the encryption
echo "Running manifest encryption..."
java -cp "$TEMP_CLASSES" com.rto1p8.app.security.ManifestEncryptor "$REAL_MANIFEST" "$ENCRYPTED_OUTPUT"

if [ $? -eq 0 ]; then
    echo "✅ Manifest encrypted successfully!"
    echo "Encrypted file saved to: $ENCRYPTED_OUTPUT"
else
    echo "❌ Manifest encryption failed!"
    exit 1
fi

echo "You can now build your APK with the encrypted manifest."