package no.nav.helse.fritakagp.integration

import no.nav.helse.arbeidsgiver.utils.SimpleHashMapCache

fun <T : Any> SimpleHashMapCache<T>.use(cacheKey: String, fn: () -> T): T =
    if (this.hasValidCacheEntry(cacheKey)) this.get(cacheKey)
    else fn().also { this.put(cacheKey, it) }
