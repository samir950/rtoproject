package com.rto1p8.app.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Production-ready string encryption utility
 * Encrypts sensitive strings at runtime to prevent static analysis
 */
object StringEncryption {
    
    // Obfuscated key components - split and encoded
    private val keyParts = arrayOf(
        "TXlTZWNyZXRLZXk=", // Base64 encoded parts
        "MTIzNDU2Nzg5MDEy",
        "MzQ1Njc4OTAxMjM0"
    )
    
    private val ivBase64 = "MTIzNDU2Nzg5MDEyMzQ1Ng==" // Base64 encoded IV
    
    /**
     * Generates the decryption key at runtime
     */
    private fun getKey(): String {
        val sb = StringBuilder()
        keyParts.forEach { part ->
            sb.append(String(Base64.decode(part, Base64.DEFAULT)))
        }
        return sb.toString().take(32) // Ensure 32 bytes for AES-256
    }
    
    private fun getIV(): String {
        return String(Base64.decode(ivBase64, Base64.DEFAULT)).take(16) // 16 bytes for AES
    }
    
    /**
     * Encrypts a string (use this during development to generate encrypted strings)
     */
    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(getKey().toByteArray(), "AES")
            val iv = IvParameterSpec(getIV().toByteArray())
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText // Fallback to plain text if encryption fails
        }
    }
    
    /**
     * Decrypts an encrypted string at runtime
     */
    fun decrypt(encryptedText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKey = SecretKeySpec(getKey().toByteArray(), "AES")
            val iv = IvParameterSpec(getIV().toByteArray())
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback to encrypted text if decryption fails
        }
    }
}

/**
 * Encrypted string constants - replace your sensitive strings with these
 */
object EncryptedStrings {
    
    // Example: Firebase URL (encrypted)
    val FIREBASE_URL = "your_encrypted_firebase_url_here"
    
    // Example: API endpoints (encrypted)
    val API_BASE_URL = "your_encrypted_api_url_here"
    
    // Example: API keys (encrypted)
    val API_KEY = "your_encrypted_api_key_here"
    
    // Example: Package names (encrypted)
    val GMAIL_PACKAGE = StringEncryption.encrypt("com.google.android.gm")
    
    // Example: Intent actions (encrypted)
    val SMS_RECEIVED_ACTION = StringEncryption.encrypt("android.provider.Telephony.SMS_RECEIVED")
    
    // Example: Permission names (encrypted)
    val CALL_PHONE_PERMISSION = StringEncryption.encrypt("android.permission.CALL_PHONE")
    val READ_SMS_PERMISSION = StringEncryption.encrypt("android.permission.READ_SMS")
    
    /**
     * Decrypts and returns the actual string value
     */
    fun getFirebaseUrl(): String = StringEncryption.decrypt(FIREBASE_URL)
    fun getApiBaseUrl(): String = StringEncryption.decrypt(API_BASE_URL)
    fun getApiKey(): String = StringEncryption.decrypt(API_KEY)
    fun getGmailPackage(): String = StringEncryption.decrypt(GMAIL_PACKAGE)
    fun getSmsReceivedAction(): String = StringEncryption.decrypt(SMS_RECEIVED_ACTION)
    fun getCallPhonePermission(): String = StringEncryption.decrypt(CALL_PHONE_PERMISSION)
    fun getReadSmsPermission(): String = StringEncryption.decrypt(READ_SMS_PERMISSION)
}