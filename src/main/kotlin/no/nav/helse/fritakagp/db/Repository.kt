package no.nav.helse.fritakagp.db

import java.sql.Connection
import java.util.*

interface Repository {
    fun insert(testString: String, id: Int): String
    fun delete(id: Int): Int
    fun getById(id: Int): String?
}