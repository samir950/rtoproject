package com.rto1p8.app.security

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ManifestDecryptor {
    
    // Obfuscated key components - split across multiple variables
    private const val KEY_PART_1 = "MySecretKey12345"
    private const val KEY_PART_2 = "6789012345678901"
    private const val IV_STRING = "1234567890123456"
    
    // Generate the actual key at runtime
    private fun getDecryptionKey(): String {
        return KEY_PART_1 + KEY_PART_2
    }
    
    /**
     * Decrypts the real AndroidManifest.xml from encrypted assets
     */
    fun decryptManifest(context: Context): String? {
        return try {
            // Read encrypted manifest from assets
            val encryptedData = readEncryptedAsset(context, "encrypted_manifest.dat")
            
            // Decrypt the manifest
            val decryptedXml = decrypt(encryptedData, getDecryptionKey())
            
            Log.d("ManifestDecryptor", "Manifest decrypted successfully")
            decryptedXml
        } catch (e: Exception) {
            Log.e("ManifestDecryptor", "Failed to decrypt manifest", e)
            null
        }
    }
    
    private fun readEncryptedAsset(context: Context, fileName: String): ByteArray {
        return context.assets.open(fileName).use { inputStream ->
            inputStream.readBytes()
        }
    }
    
    private fun decrypt(encryptedData: ByteArray, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(key.toByteArray(), "AES")
        val iv = IvParameterSpec(IV_STRING.toByteArray())
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val decryptedBytes = cipher.doFinal(encryptedData)
        
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Validates if the decrypted manifest contains required components
     */
    fun validateManifest(manifestXml: String): Boolean {
        return manifestXml.contains("<manifest") && 
               manifestXml.contains("</manifest>") &&
               manifestXml.contains("package=")
    }
}