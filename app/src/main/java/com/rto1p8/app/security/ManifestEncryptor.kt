package com.rto1p8.app.security

import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class to encrypt the real AndroidManifest.xml
 * This should be run during build process to generate encrypted_manifest.dat
 */
object ManifestEncryptor {
    
    private const val KEY_PART_1 = "MySecretKey12345"
    private const val KEY_PART_2 = "6789012345678901"
    private const val IV_STRING = "1234567890123456"
    
    private fun getEncryptionKey(): String {
        return KEY_PART_1 + KEY_PART_2
    }
    
    /**
     * Encrypts the real AndroidManifest.xml content
     */
    fun encryptManifest(manifestContent: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKey = SecretKeySpec(getEncryptionKey().toByteArray(), "AES")
        val iv = IvParameterSpec(IV_STRING.toByteArray())
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        return cipher.doFinal(manifestContent.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Main function to encrypt and save manifest
     * Call this during build process
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != 2) {
            println("Usage: ManifestEncryptor <input_manifest_path> <output_encrypted_path>")
            return
        }
        
        val inputPath = args[0]
        val outputPath = args[1]
        
        try {
            val manifestContent = File(inputPath).readText()
            val encryptedData = encryptManifest(manifestContent)
            
            FileOutputStream(outputPath).use { fos ->
                fos.write(encryptedData)
            }
            
            println("Manifest encrypted successfully: $outputPath")
        } catch (e: Exception) {
            println("Error encrypting manifest: ${e.message}")
            e.printStackTrace()
        }
    }
}