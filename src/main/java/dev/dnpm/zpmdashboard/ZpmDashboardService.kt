/*
 * This file is part of zpmdashboard
 *
 * Copyright (C) 2026  the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package dev.dnpm.zpmdashboard

import de.itc.onkostar.api.IOnkostarApi
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.text.SimpleDateFormat
import javax.sql.DataSource

@Service
class ZpmDashboardService(private val onkostarApi: IOnkostarApi, dataSource: DataSource?) {
    private val jdbcTemplate: NamedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)

    fun findMtbAnmeldungInYear(year: Int): List<Int> {
        val sql = """SELECT DISTINCT p.id FROM dk_mtb_anmeldung a 
            JOIN prozedur p ON (a.id = p.id) 
            WHERE p.geloescht <> 1 AND YEAR(a.anmeldedatum) = :year""".trimIndent()

        try {
            val params = MapSqlParameterSource().apply {
                addValue("year", year)
            }
            return jdbcTemplate.queryForList(sql, params, Int::class.java)
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun findMtbEmpfehlungInYear(year: Int): List<Int> {
        val sql = """SELECT DISTINCT p.id FROM dk_mtb_empfehlung e 
            JOIN prozedur p ON (e.id = p.id) 
            WHERE p.geloescht <> 1 AND YEAR(e.mtbdatum) = :year""".trimMargin()
        try {
            val params = MapSqlParameterSource().apply {
                addValue("year", year)
            }
            return jdbcTemplate.queryForList(sql, params, Int::class.java)
        } catch (_: Exception) {
            return emptyList()
        }
    }

    fun countConsents(year: Int): Int {
        return findMtbAnmeldungInYear(year).flatMap {
            onkostarApi.getProceduresForDiseaseByForm(
                it,
                "MolPath Consent",
                null
            )
        }.count()
    }

    fun findPrimaerfaelleCaseId(year: Int): List<CaseId> {
        val sql = """SELECT DISTINCT pat.patienten_id, pat.guid FROM dk_zpm_auswertungen zpm
            JOIN prozedur p ON (zpm.id = p.id)
            JOIN patient pat ON (p.patient_id = pat.id)
            WHERE YEAR(zaehlzeitpunkt) = :year AND p.geloescht <> 1 AND zpm.primaerfall = 1
            ORDER BY zaehlzeitpunkt, pat.patienten_id;
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("year", year)
        }

        return jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet? ->
            val caseIds = mutableListOf<CaseId>()
            while (rs!!.next()) {
                caseIds.add(CaseId(rs.getString("patienten_id"), rs.getString("guid")))
            }
            return@ResultSetExtractor caseIds
        })
    }

    fun findCase(guid: String): Case? {
        val sql =
            """SELECT patient.patienten_id, patient.guid, ep.erkrankung_id, e.diagnose AS icd10, a.anmeldedatum, molgen.datum AS molgen_datum, e.mtbdatum, e.modellvorhaben FROM dk_mtb_empfehlung e 
                    JOIN prozedur p ON (e.id = p.id) 
                    JOIN patient ON (p.patient_id = patient.id) 
                    LEFT JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
                    JOIN dk_mtb_anmeldung a ON (a.id = e.anmeldung) 
                    LEFT JOIN dk_molekulargenetik molgen ON (e.einsendenummer = molgen.einsendenummer)
                    WHERE p.geloescht <> 1 AND patient.guid = :guid LIMIT 1;""".trimIndent()

        try {
            val params = MapSqlParameterSource().apply {
                addValue("guid", guid)
            }

            return jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet? ->
                if (rs!!.next()) {
                    return@ResultSetExtractor Case(
                        rs.getString("patienten_id"),
                        rs.getString("icd10"),
                        rs.getString("guid"),
                        rs.getString("anmeldedatum"),
                        findMolPathConsent(rs.getString("guid")),
                        rs.getString("molgen_datum"),
                        rs.getString("mtbdatum"),
                        findLatestDokuDatum(rs.getInt("erkrankung_id")),
                        rs.getBoolean("modellvorhaben")
                    )
                }
                null
            })
        } catch (_: Exception) {
            return null
        }
    }

    private fun findMolPathConsent(patientGuid: String?): String? {
        if (patientGuid == null) {
            return null
        }
        try {
            val sql =
                """SELECT consentdatummolpath FROM dk_mr_consent c 
                JOIN prozedur p ON (c.id = p.id) 
                JOIN patient ON (p.patient_id = patient.id) 
                WHERE patient.guid = :guid AND consentstatusmolpath = 'z' ORDER BY datum DESC LIMIT 1;""".trimIndent()
            val params = MapSqlParameterSource().apply {
                addValue("guid", patientGuid)
            }
            return jdbcTemplate.queryForObject(sql, params, String::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun findLatestDokuDatum(erkrankungId: Int): String? {
        val sql = """SELECT erstelldatum FROM prozedur 
            JOIN erkrankung_prozedur ON (prozedur.id = erkrankung_prozedur.prozedur_id) 
            WHERE geloescht <> 1 AND erkrankung_id = :id ORDER BY erstelldatum DESC LIMIT 1;""".trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("id", erkrankungId)
        }

        val date = jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet? ->
            if (rs!!.next()) {
                return@ResultSetExtractor rs.getDate("erstelldatum")
            }
            null
        })

        if (date == null) {
            return null
        }

        val format = SimpleDateFormat("yyyy-MM-dd")
        return format.format(date)
    }

    data class CaseId(
        val pid: String,
        val guid: String
    )

    data class Case(
        var pid: String?,
        var icd: String?,
        var guid: String?,
        var anmeldedatum: String?,
        var consentdatum: String?,
        var molgenDatum: String?,
        var empfehlungsdatum: String?,
        var latestDokuDatum: String?,
        var einschlussMvh: Boolean
    )
}
