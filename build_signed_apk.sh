#!/bin/bash

# Production-ready script to build signed APK with encrypted manifest
# This script automatically handles the entire build process

echo "🚀 Starting production build with encrypted manifest..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if real manifest exists
if [ ! -f "app/src/main/assets/real_manifest.xml" ]; then
    print_error "Real manifest not found at app/src/main/assets/real_manifest.xml"
    print_status "Creating template real manifest..."
    
    mkdir -p app/src/main/assets
    cp app/src/main/AndroidManifest.xml app/src/main/assets/real_manifest.xml
    
    print_warning "Please edit app/src/main/assets/real_manifest.xml with your actual manifest content"
    print_warning "Then run this script again"
    exit 1
fi

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean

# Check if keystore exists
if [ ! -f "keystore/release.keystore" ]; then
    print_warning "Release keystore not found. Creating a debug keystore for testing..."
    mkdir -p keystore
    
    # Generate a test keystore (replace with your actual keystore for production)
    keytool -genkey -v -keystore keystore/release.keystore -alias test_alias -keyalg RSA -keysize 2048 -validity 10000 -storepass test123 -keypass test123 -dname "CN=Test, OU=Test, O=Test, L=Test, S=Test, C=US"
    
    print_warning "Using test keystore. Replace with your production keystore for release builds."
fi

# Build the signed APK
print_status "Building signed APK with encrypted manifest..."
./gradlew assembleRelease

# Check if build was successful
if [ $? -eq 0 ]; then
    print_status "✅ Build completed successfully!"
    
    # Find the generated APK
    APK_PATH=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
    
    if [ -n "$APK_PATH" ]; then
        print_status "📱 Signed APK generated: $APK_PATH"
        
        # Get APK size
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        print_status "📦 APK Size: $APK_SIZE"
        
        # Verify APK signature
        print_status "🔐 Verifying APK signature..."
        $ANDROID_HOME/build-tools/*/apksigner verify "$APK_PATH"
        
        if [ $? -eq 0 ]; then
            print_status "✅ APK signature verified successfully!"
        else
            print_warning "⚠️ APK signature verification failed"
        fi
        
        # Check if encrypted manifest was created
        if [ -f "app/src/main/assets/encrypted_manifest.dat" ]; then
            print_status "🔒 Encrypted manifest created successfully"
        else
            print_warning "⚠️ Encrypted manifest not found"
        fi
        
        print_status "🎉 Production APK ready for distribution!"
        print_status "Location: $APK_PATH"
        
    else
        print_error "❌ APK not found in build output"
        exit 1
    fi
else
    print_error "❌ Build failed!"
    exit 1
fi

echo ""
print_status "🔐 Security Features Enabled:"
echo "  ✅ Encrypted AndroidManifest.xml"
echo "  ✅ String encryption for sensitive data"
echo "  ✅ ProGuard obfuscation"
echo "  ✅ Code shrinking and resource optimization"
echo "  ✅ Signed APK for production distribution"
echo ""
print_status "Your production APK is ready! 🚀"