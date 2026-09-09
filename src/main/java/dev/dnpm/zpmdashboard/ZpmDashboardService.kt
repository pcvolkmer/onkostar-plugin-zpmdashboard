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

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.sql.Date
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource


@Service
class ZpmDashboardService(dataSource: DataSource?) {
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
            LEFT JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
            LEFT JOIN erkrankung e ON (ep.erkrankung_id = e.id)
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
                caseIds.add(
                    CaseId(
                        rs.getString("patienten_id"),
                        rs.getString("pat_guid"),
                        rs.getString("proc_guid"),
                        rs.getString("e_guid").orEmpty()
                    )
                )
            }
            return@ResultSetExtractor caseIds.distinctBy { it.patientGuid + it.erkrankungGuid }
        })
    }

    fun findCase(patientGuid: String, procedureGuid: String, year: Int): Case? {
        val sql =
            """SELECT patient.patienten_id, ep.erkrankung_id, e.diagnose AS icd10, a.anmeldedatum, zpm.internextern, molgen.datum AS molgen_datum, molgenp.status = 0 AS molgen_korrekt, e.mtbdatum, zpmep.erkrankung_id IS NOT NULL AS zpm_erkrankung, zpm.zaehlzeitpunkt, zpm.offlabel, zpm.studie, e.modellvorhaben, YEAR(p.beginndatum) = YEAR(zpm.zaehlzeitpunkt) AS sameyear FROM dk_mtb_empfehlung e  
                    JOIN prozedur p ON (e.id = p.id) 
                    JOIN patient ON (p.patient_id = patient.id) 
                    JOIN dk_zpm_auswertungen zpm ON (zpm.zaehlzeitpunkt = p.beginndatum) 
                    JOIN prozedur zpmp ON (zpmp.id = zpm.id) 
                    LEFT JOIN dk_mtb_anmeldung a ON (a.id = e.anmeldung) 
                    LEFT JOIN dk_molekulargenetik molgen ON (e.einsendenummer = molgen.einsendenummer) 
                    LEFT JOIN prozedur molgenp ON (molgen.id = molgenp.id) 
                    LEFT JOIN erkrankung_prozedur zpmep ON (zpmep.prozedur_id = zpm.id) 
                    LEFT JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
                    WHERE p.geloescht <> 1 
                      AND patient.guid = :pat_guid 
                      AND zpmp.guid = :zpm_guid 
                      AND YEAR(p.beginndatum) = :year  
                      ORDER BY p.beginndatum  
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
                                || !rs.getBoolean("sameyear")
                                || !rs.getBoolean("zpm_erkrankung")
                                || null == rs.getString("molgen_datum"),
                        WarningDetails(
                            hasPFWarnings(rs.getInt("erkrankung_id"), year),
                            null == rs.getString("molgen_datum"),
                            !rs.getBoolean("zpm_erkrankung")
                        ),
                        findAufgabenForPatient(patientGuid)
                    )
                }
                null
            })
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun findAufgabenForPatient(patientGuid: String): List<Aufgabe> {
        val sql = """SELECT
            CONCAT(akteur.name, ', ',akteur.vorname) AS user,
            data_form.name AS form_name,
            prozedur.beginndatum AS form_date,
            prozedur.guid AS form_guid,
            aufnahme_akteur_zeit,
            hinweis,
            faelligkeit
            FROM aufgabe
            JOIN aufgabenliste ON (aufgabe.aufgabenliste_id = aufgabenliste.id AND aufgabenliste.name = 'ZPM-Dashboard')
            JOIN akteur ON (aufgabe.aufnahme_akteur_id = akteur.id)
            JOIN patient ON (patient.id = aufgabe.patient_id)
            LEFT JOIN prozedur ON (prozedur.id = aufgabe.prozedur_id)
            LEFT JOIN data_form ON (data_form.id = prozedur.data_form_id)
            WHERE aufgabe.status < 1 AND patient.guid = :pat_guid;"""

        try {
            val params = MapSqlParameterSource().apply {
                addValue("pat_guid", patientGuid)
            }

            return jdbcTemplate.query(sql, params, ResultSetExtractor { rs: ResultSet ->
                val result = mutableListOf<Aufgabe>()
                while (rs.next()) {
                    result.add(
                        Aufgabe(
                            rs.getString("user"),
                            rs.getString("aufnahme_akteur_zeit"),
                            rs.getString("hinweis"),
                            rs.getString("faelligkeit"),
                            rs.getString("form_name"),
                            rs.getString("form_date"),
                            rs.getString("form_guid")
                        )
                    )
                }
                return@ResultSetExtractor result
            })
        } catch (_: Exception) {
            // Nop
        }
        return emptyList()
    }

    fun casesXsl(year: Int): ByteArray {
        val workbook: Workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(
            "Primärfälle - Stand %s".format(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH.mm"))
            )
        )
        sheet.createFreezePane(2, 1)

        val headerFont = workbook.createFont()
        headerFont.bold = true
        val headerStyle = workbook.createCellStyle()
        headerStyle.setFont(headerFont)
        headerStyle.borderTop = BorderStyle.THIN
        headerStyle.borderBottom = BorderStyle.THIN
        headerStyle.borderLeft = BorderStyle.THIN
        headerStyle.borderRight = BorderStyle.THIN
        headerStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        headerStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        val cellStyle = workbook.createCellStyle()
        cellStyle.borderTop = BorderStyle.THIN
        cellStyle.borderBottom = BorderStyle.THIN
        cellStyle.borderLeft = BorderStyle.THIN
        cellStyle.borderRight = BorderStyle.THIN

        val dateStyle = workbook.createCellStyle()
        dateStyle.borderTop = BorderStyle.THIN
        dateStyle.borderBottom = BorderStyle.THIN
        dateStyle.borderLeft = BorderStyle.THIN
        dateStyle.borderRight = BorderStyle.THIN
        val createHelper = workbook.creationHelper
        dateStyle.dataFormat = createHelper.createDataFormat().getFormat("dd.MM.yyyy")

        val headers = listOf(
            "PID",
            "ICD10",
            "intern/extern",
            "Studie",
            "off-label",
            "Zählzeitpunkt",
            "Consent-Datum",
            "Consentzustimmung",
            "TuDok Stand",
            "Warnung?",
            "Kein Primärfall?",
            "Kein MolGen?",
            "Keine Erkrankung?"
        )
        val headRow = sheet.createRow(0)
        headers.forEachIndexed { idx, value ->
            val cell = headRow.createCell(idx)
            cell.setCellValue(value)
            cell.cellStyle = headerStyle
        }

        this.findPrimaerfaelleCaseId(year)
            .mapNotNull {
                this.findCase(it.patientGuid, it.procedureGuid, year)
            }
            .forEachIndexed { row, case ->
                val row = sheet.createRow(row + 1)

                val pidCell = row.createCell(0)
                pidCell.setCellValue(case.pid.orEmpty())
                pidCell.cellStyle = cellStyle

                val icd10Cell = row.createCell(1)
                icd10Cell.setCellValue(case.icd.orEmpty())
                icd10Cell.cellStyle = cellStyle

                val internExternColumn = row.createCell(2)
                internExternColumn.setCellValue(
                    if (case.internextern == "E") {
                        "extern"
                    } else {
                        "intern"
                    }
                )
                internExternColumn.cellStyle = cellStyle

                val studieColumn = row.createCell(3)
                studieColumn.setCellValue(
                    if (case.studie) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                studieColumn.cellStyle = cellStyle

                val offLabelColumn = row.createCell(4)
                offLabelColumn.setCellValue(
                    if (case.offlabel) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                offLabelColumn.cellStyle = cellStyle

                val zZeitpunktCell = row.createCell(5)
                try {
                    val date = LocalDate.parse(case.zaehlzeitpunkt.orEmpty())
                    zZeitpunktCell.setCellValue(Date.valueOf(date))
                } catch (_: Exception) { /* Do not set a value */
                }
                zZeitpunktCell.cellStyle = dateStyle

                val consentCell = row.createCell(6)
                try {
                    val date = LocalDate.parse(case.consent.datum.orEmpty())
                    consentCell.setCellValue(Date.valueOf(date))
                } catch (_: Exception) { /* Do not set a value */
                }
                consentCell.cellStyle = dateStyle

                val consentAcceptedCell = row.createCell(7)
                consentAcceptedCell.setCellValue(
                    if (case.consent.zustimmung) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                consentAcceptedCell.cellStyle = cellStyle

                val todokDateCell = row.createCell(8)
                try {
                    val date = LocalDate.parse(case.latestDokuDatum.orEmpty())
                    todokDateCell.setCellValue(Date.valueOf(date))
                } catch (_: Exception) { /* Do not set a value */
                }
                todokDateCell.cellStyle = dateStyle

                val warningCell = row.createCell(9)
                warningCell.setCellValue(
                    if (case.warnings) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                warningCell.cellStyle = cellStyle

                val keinPF = row.createCell(10)
                keinPF.setCellValue(
                    if (case.warningDetails?.invalidPrimaerfall == true) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                keinPF.cellStyle = cellStyle

                val noMolGen = row.createCell(11)
                noMolGen.setCellValue(
                    if (case.warningDetails?.noMolgen == true) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                noMolGen.cellStyle = cellStyle

                val noDisease = row.createCell(12)
                noDisease.setCellValue(
                    if (case.warningDetails?.noDisease == true) {
                        "Ja"
                    } else {
                        "Nein"
                    }
                )
                noDisease.cellStyle = cellStyle
            }

        headers.forEachIndexed { idx, _ -> sheet.autoSizeColumn(idx) }

        val os = ByteArrayOutputStream()
        workbook.write(os)
        workbook.close()

        return os.toByteArray()
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
            WHERE (YEAR(zpm.zaehlzeitpunkt) = :year OR YEAR(zpm.zaehlzeitpunkt) = :lastyear) 
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

    private fun hasPFWarnings(erkrankungId: Int, year: Int): Boolean {
        val sql =
            """SELECT DISTINCT YEAR(zpm.zaehlzeitpunkt) FROM dk_zpm_auswertungen zpm
            JOIN prozedur p ON (zpm.id = p.id)
            JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) 
            WHERE (YEAR(zpm.zaehlzeitpunkt) = :year OR YEAR(zpm.zaehlzeitpunkt) = :lastyear) 
              AND ep.erkrankung_id = :erkrankung_id
              AND p.geloescht <> 1 AND zpm.primaerfall = 1;
        """.trimIndent()

        val params = MapSqlParameterSource().apply {
            addValue("year", year)
            addValue("lastyear", year - 1)
            addValue("erkrankung_id", erkrankungId)
        }

        // Multiple PF in this and last year
        return jdbcTemplate.queryForList(sql, params, Integer::class.java).size > 1
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
        var warnings: Boolean = false,
        var warningDetails: WarningDetails? = null,
        val aufgaben: List<Aufgabe> = emptyList()
    )

    data class Consent(
        val datum: String?,
        val zustimmung: Boolean = false
    )

    data class MolGen(
        val datum: String?,
        val korrekt: Boolean = false
    )

    data class WarningDetails(
        val invalidPrimaerfall: Boolean = false,
        val noMolgen: Boolean = false,
        val noDisease: Boolean = false
    )

    data class Aufgabe(
        val akteur: String,
        val date: String,
        val text: String,
        val dueDate: String?,
        val formName: String?,
        val formDate: String?,
        val formGuid: String?
    )
}
