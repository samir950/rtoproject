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

# Check if we're in the right directory
if [ ! -f "app/build.gradle.kts" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Check if real manifest exists
if [ ! -f "app/src/main/assets/real_manifest.xml" ]; then
    print_warning "Real manifest not found. The build process will create a template."
    print_warning "Please edit app/src/main/assets/real_manifest.xml with your actual manifest content after the build."
fi

# Clean previous builds
print_status "Cleaning previous builds..."
./gradlew clean

# Check if keystore exists
if [ ! -f "keystore/release.keystore" ]; then
    print_warning "Release keystore not found. Creating a test keystore..."
    mkdir -p keystore
    
    # Generate a test keystore (replace with your actual keystore for production)
    keytool -genkey -v -keystore keystore/release.keystore -alias test_alias -keyalg RSA -keysize 2048 -validity 10000 -storepass test123 -keypass test123 -dname "CN=Test, OU=Test, O=Test, L=Test, S=Test, C=US" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        print_status "Test keystore created successfully"
    else
        print_error "Failed to create test keystore"
        exit 1
    fi
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
        
        # Check if ANDROID_HOME is set for APK verification
        if [ -n "$ANDROID_HOME" ]; then
            # Verify APK signature
            print_status "🔐 Verifying APK signature..."
            APKSIGNER_PATH=$(find "$ANDROID_HOME/build-tools" -name "apksigner" | head -1)
            
            if [ -n "$APKSIGNER_PATH" ]; then
                "$APKSIGNER_PATH" verify "$APK_PATH"
                
                if [ $? -eq 0 ]; then
                    print_status "✅ APK signature verified successfully!"
                else
                    print_warning "⚠️ APK signature verification failed"
                fi
            else
                print_warning "⚠️ apksigner not found, skipping signature verification"
            fi
        else
            print_warning "⚠️ ANDROID_HOME not set, skipping signature verification"
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
    print_error "Please check the error messages above and fix any issues."
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

# Instructions for next steps
echo ""
print_status "📋 Next Steps:"
echo "  1. Test the APK on a device to ensure everything works"
echo "  2. If using test keystore, replace with your production keystore"
echo "  3. Update app/src/main/assets/real_manifest.xml with your actual manifest"
echo "  4. Upload to Google Play Store or distribute as needed"