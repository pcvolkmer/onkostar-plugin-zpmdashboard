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
import java.sql.Date
import java.sql.ResultSet
import java.text.SimpleDateFormat
import javax.sql.DataSource

@Service
class ZpmDashboardService(private val onkostarApi: IOnkostarApi, dataSource: DataSource?) {
    private val jdbcTemplate: NamedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)

    fun countMtbAnmeldungInYear(year: Int): Int {
        val sql = """SELECT DISTINCT pat.id FROM dk_mtb_anmeldung a 
            JOIN prozedur p ON (a.id = p.id) 
            JOIN patient pat ON (pat.id = p.patient_id) 
            WHERE p.geloescht <> 1 AND pat.nachname <> 'Momentum' AND YEAR(a.anmeldedatum) = :year""".trimIndent()

        try {
            val params = MapSqlParameterSource().apply {
                addValue("year", year)
            }
            return jdbcTemplate.queryForList(sql, params, Int::class.java).size
        } catch (_: Exception) {
            return 0
        }
    }

    fun countMtbEmpfehlungInYear(year: Int): Int {
        val sql = """SELECT DISTINCT pat.id FROM dk_mtb_empfehlung e 
            JOIN prozedur p ON (e.id = p.id) 
            JOIN patient pat ON (pat.id = p.patient_id) 
            WHERE p.geloescht <> 1 AND pat.nachname <> 'Momentum' AND YEAR(e.mtbdatum) = :year""".trimMargin()
        try {
            val params = MapSqlParameterSource().apply {
                addValue("year", year)
            }
            return jdbcTemplate.queryForList(sql, params, Int::class.java).size
        } catch (_: Exception) {
            return 0
        }
    }

    fun countConsents(year: Int): Int {
        val sql = """SELECT DISTINCT p.id FROM dk_mr_consent c 
            JOIN prozedur p ON (c.id = p.id) 
            WHERE p.geloescht <> 1 AND YEAR(c.consentdatummolpath) = :year""".trimMargin()
        try {
            val params = MapSqlParameterSource().apply {
                addValue("year", year)
            }
            return jdbcTemplate.queryForList(sql, params, Int::class.java).size
        } catch (_: Exception) {
            return 0
        }
    }

    fun findPrimaerfaelleCaseId(year: Int): List<CaseId> {
        val sql =
            """SELECT DISTINCT pat.patienten_id, pat.guid AS pat_guid, p.guid AS proc_guid, e.guid AS e_guid FROM dk_zpm_auswertungen zpm
            JOIN prozedur p ON (zpm.id = p.id)
            JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
            JOIN erkrankung e ON (ep.erkrankung_id = e.id)
            JOIN patient pat ON (p.patient_id = pat.id)
            WHERE YEAR(zaehlzeitpunkt) = :year AND p.geloescht <> 1 AND zpm.primaerfall = 1 AND pat.nachname <> 'Momentum'
            ORDER BY zaehlzeitpunkt, pat.patienten_id;
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("year", year)
        }

        return jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet? ->
            val caseIds = mutableListOf<CaseId>()
            while (rs!!.next()) {
                caseIds.add(CaseId(rs.getString("patienten_id"), rs.getString("pat_guid"), rs.getString("proc_guid"), rs.getString("e_guid")))
            }
            return@ResultSetExtractor caseIds.distinctBy { it.patientGuid + it.erkrankungGuid }
        })
    }

    fun findCase(patientGuid: String, procedureGuid: String, year: Int): Case? {
        val sql =
            """SELECT patient.patienten_id, ep.erkrankung_id, e.diagnose AS icd10, a.anmeldedatum, zpm.internextern, molgen.datum AS molgen_datum, molgenp.status = 0 AS molgen_korrekt, e.mtbdatum, zpm.zaehlzeitpunkt, zpm.offlabel, zpm.studie, e.modellvorhaben FROM dk_mtb_empfehlung e 
                    JOIN prozedur p ON (e.id = p.id) 
                    JOIN patient ON (p.patient_id = patient.id) 
                    LEFT JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
                    LEFT JOIN dk_mtb_anmeldung a ON (a.id = e.anmeldung) 
                    LEFT JOIN dk_molekulargenetik molgen ON (e.einsendenummer = molgen.einsendenummer)
                    LEFT JOIN prozedur molgenp ON (molgen.id = molgenp.id)
                    LEFT JOIN prozedur zpmp ON (zpmp.guid = :zpm_guid)
                    LEFT JOIN dk_zpm_auswertungen zpm ON (zpm.id = zpmp.id)
                    WHERE p.geloescht <> 1 AND patient.guid = :pat_guid  
                      AND YEAR(p.beginndatum) = :year AND YEAR(zpm.zaehlzeitpunkt) = :year 
                      LIMIT 1;""".trimIndent()

        try {
            val params = MapSqlParameterSource().apply {
                addValue("pat_guid", patientGuid)
                addValue("zpm_guid", procedureGuid)
                addValue("year", year)
            }

            return jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet ->
                if (rs.next()) {
                    return@ResultSetExtractor Case(
                        rs.getString("patienten_id"),
                        rs.getString("icd10"),
                        patientGuid,
                        procedureGuid,
                        rs.getString("zaehlzeitpunkt"),
                        rs.getString("anmeldedatum"),
                        rs.getString("internextern"),
                        findMolPathConsent(patientGuid),
                        MolGen(rs.getString("molgen_datum"), rs.getBoolean("molgen_korrekt")),
                        rs.getString("mtbdatum"),
                        findLatestDokuDatum(rs.getInt("erkrankung_id")),
                        rs.getBoolean("offlabel"),
                        rs.getBoolean("studie"),
                        rs.getBoolean("modellvorhaben"),
                        hasWarnings(rs.getInt("erkrankung_id"), year)
                    )
                }
                null
            })
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun findMolPathConsent(patientGuid: String?): Consent {
        if (patientGuid == null) {
            return Consent(null, false)
        }
        try {
            val sql =
                """SELECT consentdatummolpath, consentstatusmolpath FROM dk_mr_consent c 
                JOIN prozedur p ON (c.id = p.id) 
                JOIN patient ON (p.patient_id = patient.id) 
                WHERE patient.guid = :guid ORDER BY datum DESC LIMIT 1;""".trimIndent()
            val params = MapSqlParameterSource().apply {
                addValue("guid", patientGuid)
            }
            return jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet ->
                if (rs.next()) {
                    return@ResultSetExtractor Consent(
                        rs.getString("consentdatummolpath"),
                        rs.getString("consentstatusmolpath") == "z"
                    )
                }
                Consent(null, false)
            })
        } catch (_: Exception) {
            return Consent(null, false)
        }
    }

    private fun findLatestDokuDatum(erkrankungId: Int): String? {
        // Nur: Studiensekretariat Hämatologie (1527)?
        val sql = """SELECT MAX(beginndatum) FROM prozedur 
            JOIN erkrankung_prozedur ON (prozedur.id = erkrankung_prozedur.prozedur_id) 
            WHERE geloescht <> 1 AND erkrankung_id = :id;""".trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("id", erkrankungId)
        }

        try {
            val date = jdbcTemplate.queryForObject(sql, params, Date::class.java) ?: return null
            val format = SimpleDateFormat("yyyy-MM-dd")
            return format.format(date)
        } catch (_: Exception) {
            return null
        }
    }

    private fun hasWarnings(erkrankungId: Int, year: Int): Boolean {
        val sql =
            """SELECT COUNT(*) FROM dk_zpm_auswertungen zpm
            JOIN prozedur p ON (zpm.id = p.id)
            JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
            WHERE (YEAR(zaehlzeitpunkt) = :year OR YEAR(zaehlzeitpunkt) = :lastyear) 
              AND ep.erkrankung_id = :erkrankung_id
              AND p.geloescht <> 1 AND zpm.primaerfall = 1;
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("year", year)
            addValue("lastyear", year - 1)
            addValue("erkrankung_id", erkrankungId)
        }

        // Multiple PF in this and last year
        return jdbcTemplate.queryForObject(sql, params, Integer::class.java) > 1
    }

    data class CaseId(
        val pid: String,
        val patientGuid: String,
        val procedureGuid: String,
        val erkrankungGuid: String
    )

    data class Case(
        var pid: String?,
        var icd: String?,
        var patientGuid: String,
        var procedureGuid: String,
        var zaehlzeitpunkt: String?,
        var anmeldedatum: String?,
        var internextern: String?,
        var consent: Consent,
        var molgen: MolGen,
        var empfehlungsdatum: String?,
        var latestDokuDatum: String?,
        var offlabel: Boolean,
        var studie: Boolean,
        var einschlussMvh: Boolean,
        var warnings: Boolean = false
    )

    data class Consent(
        val datum: String?,
        val zustimmung: Boolean = false
    )

    data class MolGen(
        val datum: String?,
        val korrekt: Boolean = false
    )
}
