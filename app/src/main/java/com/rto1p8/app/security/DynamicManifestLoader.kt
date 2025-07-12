package com.rto1p8.app.security

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.lang.reflect.Field
import java.lang.reflect.Method

class DynamicManifestLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "DynamicManifestLoader"
        private var isManifestLoaded = false
    }
    
    /**
     * Loads and applies the encrypted manifest at runtime
     */
    fun loadEncryptedManifest(): Boolean {
        if (isManifestLoaded) {
            Log.d(TAG, "Manifest already loaded")
            return true
        }
        
        return try {
            // Decrypt the real manifest
            val decryptedManifest = ManifestDecryptor.decryptManifest(context)
            if (decryptedManifest == null) {
                Log.e(TAG, "Failed to decrypt manifest")
                return false
            }
            
            // Validate the manifest
            if (!ManifestDecryptor.validateManifest(decryptedManifest)) {
                Log.e(TAG, "Invalid manifest structure")
                return false
            }
            
            // Parse and apply the manifest
            val success = applyManifestChanges(decryptedManifest)
            if (success) {
                isManifestLoaded = true
                Log.d(TAG, "Dynamic manifest loaded successfully")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dynamic manifest", e)
            false
        }
    }
    
    private fun applyManifestChanges(manifestXml: String): Boolean {
        return try {
            val parser = createXmlParser(manifestXml)
            val manifestData = parseManifest(parser)
            
            // Apply the parsed manifest data
            updateApplicationInfo(manifestData)
            updatePackageInfo(manifestData)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying manifest changes", e)
            false
        }
    }
    
    private fun createXmlParser(xmlContent: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))
        return parser
    }
    
    private fun parseManifest(parser: XmlPullParser): ManifestData {
        val manifestData = ManifestData()
        var eventType = parser.eventType
        
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "manifest" -> {
                            manifestData.packageName = parser.getAttributeValue(null, "package")
                            manifestData.versionCode = parser.getAttributeValue(null, "android:versionCode")?.toIntOrNull()
                            manifestData.versionName = parser.getAttributeValue(null, "android:versionName")
                        }
                        "application" -> {
                            manifestData.applicationLabel = parser.getAttributeValue(null, "android:label")
                            manifestData.applicationIcon = parser.getAttributeValue(null, "android:icon")
                        }
                        "activity" -> {
                            val activityName = parser.getAttributeValue(null, "android:name")
                            val exported = parser.getAttributeValue(null, "android:exported")?.toBoolean() ?: false
                            if (activityName != null) {
                                manifestData.activities.add(ActivityInfo(activityName, exported))
                            }
                        }
                        "service" -> {
                            val serviceName = parser.getAttributeValue(null, "android:name")
                            val exported = parser.getAttributeValue(null, "android:exported")?.toBoolean() ?: false
                            if (serviceName != null) {
                                manifestData.services.add(ServiceInfo(serviceName, exported))
                            }
                        }
                        "receiver" -> {
                            val receiverName = parser.getAttributeValue(null, "android:name")
                            val exported = parser.getAttributeValue(null, "android:exported")?.toBoolean() ?: false
                            if (receiverName != null) {
                                manifestData.receivers.add(ReceiverInfo(receiverName, exported))
                            }
                        }
                        "uses-permission" -> {
                            val permission = parser.getAttributeValue(null, "android:name")
                            if (permission != null) {
                                manifestData.permissions.add(permission)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        
        return manifestData
    }
    
    private fun updateApplicationInfo(manifestData: ManifestData) {
        try {
            val packageManager = context.packageManager
            val applicationInfo = context.applicationInfo
            
            // Use reflection to update ApplicationInfo
            manifestData.applicationLabel?.let { label ->
                updateField(applicationInfo, "labelRes", getStringResourceId(label))
            }
            
            manifestData.applicationIcon?.let { icon ->
                updateField(applicationInfo, "icon", getDrawableResourceId(icon))
            }
            
            Log.d(TAG, "ApplicationInfo updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ApplicationInfo", e)
        }
    }
    
    private fun updatePackageInfo(manifestData: ManifestData) {
        try {
            // This is more complex and may require additional reflection
            // For now, we'll log the information
            Log.d(TAG, "Package: ${manifestData.packageName}")
            Log.d(TAG, "Version Code: ${manifestData.versionCode}")
            Log.d(TAG, "Version Name: ${manifestData.versionName}")
            Log.d(TAG, "Activities: ${manifestData.activities.size}")
            Log.d(TAG, "Services: ${manifestData.services.size}")
            Log.d(TAG, "Receivers: ${manifestData.receivers.size}")
            Log.d(TAG, "Permissions: ${manifestData.permissions.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating PackageInfo", e)
        }
    }
    
    private fun updateField(obj: Any, fieldName: String, value: Any) {
        try {
            val field = obj.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(obj, value)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating field $fieldName", e)
        }
    }
    
    private fun getStringResourceId(resourceName: String): Int {
        return context.resources.getIdentifier(
            resourceName.replace("@string/", ""),
            "string",
            context.packageName
        )
    }
    
    private fun getDrawableResourceId(resourceName: String): Int {
        return context.resources.getIdentifier(
            resourceName.replace("@drawable/", ""),
            "drawable",
            context.packageName
        )
    }
    
    // Data classes for parsed manifest information
    data class ManifestData(
        var packageName: String? = null,
        var versionCode: Int? = null,
        var versionName: String? = null,
        var applicationLabel: String? = null,
        var applicationIcon: String? = null,
        val activities: MutableList<ActivityInfo> = mutableListOf(),
        val services: MutableList<ServiceInfo> = mutableListOf(),
        val receivers: MutableList<ReceiverInfo> = mutableListOf(),
        val permissions: MutableList<String> = mutableListOf()
    )
    
    data class ActivityInfo(val name: String, val exported: Boolean)
    data class ServiceInfo(val name: String, val exported: Boolean)
    data class ReceiverInfo(val name: String, val exported: Boolean)
}