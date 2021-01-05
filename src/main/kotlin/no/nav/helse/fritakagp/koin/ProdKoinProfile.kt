package no.nav.helse.fritakagp.koin

import io.ktor.config.*
import io.ktor.util.*
import no.nav.helse.fritakagp.gcp.BucketStorage
import no.nav.helse.fritakagp.gcp.BucketStorageImp
import no.nav.helse.fritakagp.virusscan.ClamavVirusScannerImp
import no.nav.helse.fritakagp.virusscan.VirusScanner
import org.koin.dsl.bind
import org.koin.dsl.module


@KtorExperimentalAPI
fun prodConfig(config: ApplicationConfig) = module {
    single { ClamavVirusScannerImp(
        get(),
        config.getString("clamav_url")
    ) } bind VirusScanner::class
    single { BucketStorageImp() } bind BucketStorage::class
}
