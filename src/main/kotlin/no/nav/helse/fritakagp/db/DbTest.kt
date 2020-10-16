package no.nav.helse.fritakagp.db

import org.slf4j.LoggerFactory

class DbTest(val repo: Repository) {
    val logger = LoggerFactory.getLogger("main")

    fun lagre() {
        logger.info("Lagrer til databasen..")
        val testString = repo.insert("Privyet mir", 4)

        logger.info("fikk lagret $testString i databasen!")
        logger.info("slettet ${repo.delete(4)} fra databasen")
    }

}