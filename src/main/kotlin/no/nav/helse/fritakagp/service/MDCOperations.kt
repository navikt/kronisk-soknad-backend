package no.nav.helse.fritakagp.service

import no.nav.helsearbeidsgiver.utils.log.logger
import org.slf4j.MDC
import kotlin.random.Random

class MDCOperations {
    companion object {
        private val logger = this.logger()
        const val MDC_CALL_ID = "callId"
        const val MDC_USER_ID = "userId"
        const val MDC_CONSUMER_ID = "consumerId"

        fun generateCallId(): String = "CallId_${getRandomNumber()}_${getSystemTime()}"

        fun getFromMDC(key: String): String? {
            val value = MDC.get(key)
            logger.debug("Getting key: $key from MDC with value: $value")
            return value
        }

        fun putToMDC(key: String, value: String) {
            logger.debug("Putting value: $value on MDC with key: $key")
            MDC.put(key, value)
        }

        fun remove(key: String) {
            logger.debug("Removing key: $key")
            MDC.remove(key)
        }

        private fun getRandomNumber(): Int = Random.nextInt(Int.MAX_VALUE)
        private fun getSystemTime(): Long = System.currentTimeMillis()
    }
}
