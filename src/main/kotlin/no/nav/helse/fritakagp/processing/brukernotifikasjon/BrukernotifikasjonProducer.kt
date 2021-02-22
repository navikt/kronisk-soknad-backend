package no.nav.helse.fritakagp.processing.brukernotifikasjon

import org.apache.kafka.common.record.Record


interface BrukernotifikasjonProducer {
    fun sendNotifikasjon(record: Record)
}

class KafkaBrukernotifikasjonProducer: BrukernotifikasjonProducer {
    override fun sendNotifikasjon(record: Record) {
        TODO("Not yet implemented")
    }
}