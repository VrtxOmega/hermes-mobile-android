package org.hermesmobile.client.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.hermesmobile.client.runtime.HermesUrl

class HermesAuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("hermes_mobile_secure_state", Context.MODE_PRIVATE)

    fun backendUrl(): String = prefs.getString(KEY_BACKEND_URL, HermesUrl.DefaultBaseUrl) ?: HermesUrl.DefaultBaseUrl

    fun saveBackendUrl(url: String) {
        prefs.edit().putString(KEY_BACKEND_URL, url).apply()
    }

    fun themeName(): String = prefs.getString(KEY_THEME_NAME, DEFAULT_THEME_NAME) ?: DEFAULT_THEME_NAME

    fun saveThemeName(name: String) {
        prefs.edit().putString(KEY_THEME_NAME, name).apply()
    }

    fun workbenchCwd(): String = prefs.getString(KEY_WORKBENCH_CWD, DEFAULT_WORKBENCH_CWD) ?: DEFAULT_WORKBENCH_CWD

    fun saveWorkbenchCwd(cwd: String) {
        prefs.edit().putString(KEY_WORKBENCH_CWD, cwd).apply()
    }

    fun cookieLines(): List<String> {
        return getEncrypted(KEY_COOKIE_LINES)
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.toList()
            .orEmpty()
    }

    fun saveCookieLines(lines: List<String>) {
        putEncrypted(KEY_COOKIE_LINES, lines.distinct().joinToString("\n"))
    }

    fun clearSession() {
        prefs.edit().remove(KEY_COOKIE_LINES).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun putEncrypted(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(key, encoded).apply()
    }

    private fun getEncrypted(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        val parts = encoded.split(":", limit = 2)
        if (parts.size != 2) return null

        return runCatching {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) {
            return existing.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "org.hermesmobile.client.auth.v1"
        const val KEY_BACKEND_URL = "backend_url"
        const val KEY_COOKIE_LINES = "cookie_lines"
        const val KEY_THEME_NAME = "theme_name"
        const val KEY_WORKBENCH_CWD = "workbench_cwd"
        const val DEFAULT_THEME_NAME = "nous"
        const val DEFAULT_WORKBENCH_CWD = "/home/user/projects"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
