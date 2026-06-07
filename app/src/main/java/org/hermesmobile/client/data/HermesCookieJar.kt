package org.hermesmobile.client.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class HermesCookieJar(private val store: HermesAuthStore) : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this.cookies) {
            cookies.forEach { incoming ->
                this.cookies.removeAll { it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path }
                if (incoming.expiresAt > System.currentTimeMillis()) {
                    this.cookies.add(incoming)
                }
            }
            persistLocked()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(cookies) {
            if (cookies.isEmpty()) {
                restoreLocked(url)
            }
            val now = System.currentTimeMillis()
            cookies.removeAll { it.expiresAt <= now }
            persistLocked()
            return cookies.filter { it.matches(url) }
        }
    }

    fun clear() {
        synchronized(cookies) {
            cookies.clear()
            store.clearSession()
        }
    }

    fun restoreFor(baseUrl: HttpUrl) {
        synchronized(cookies) {
            restoreLocked(baseUrl)
        }
    }

    private fun restoreLocked(url: HttpUrl) {
        cookies.clear()
        store.cookieLines()
            .mapNotNull { Cookie.parse(url, it) }
            .filter { it.expiresAt > System.currentTimeMillis() }
            .forEach(cookies::add)
    }

    private fun persistLocked() {
        store.saveCookieLines(cookies.map(Cookie::toString))
    }
}
