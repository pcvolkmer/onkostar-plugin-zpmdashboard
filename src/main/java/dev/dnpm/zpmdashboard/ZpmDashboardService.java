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

package dev.dnpm.zpmdashboard;

import de.itc.onkostar.api.Disease;
import de.itc.onkostar.api.IOnkostarApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ZpmDashboardService {

    private final IOnkostarApi onkostarApi;
    private final JdbcTemplate jdbcTemplate;

    public ZpmDashboardService(IOnkostarApi onkostarApi, DataSource dataSource) {
        this.onkostarApi = onkostarApi;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<Disease> findDiseasesWithMtbAnmeldungInYear(int year) {
        final var sql = "SELECT a.id FROM dk_mtb_anmeldung a JOIN prozedur p ON (a.id = p.id) WHERE p.geloescht <> 1 AND YEAR(a.anmeldedatum) = ?";
        return jdbcTemplate.queryForList(sql, new Object[]{year}, Integer.class).stream()
                .map(this.onkostarApi::getProcedure)
                .flatMap(procedure -> procedure.getDiseases().stream())
                .collect(Collectors.toList());
    }

    public List<Disease> findDiseasesWithMtbEmpfehlungInYear(int year) {
        final var sql = "SELECT e.id FROM dk_mtb_empfehlung e JOIN prozedur p ON (e.id = p.id) WHERE p.geloescht <> 1 AND YEAR(e.mtbdatum) = ?";
        return jdbcTemplate.queryForList(sql, new Object[]{year}, Integer.class).stream()
                .distinct()
                .map(this.onkostarApi::getProcedure)
                .flatMap(procedure -> procedure.getDiseaseIds().stream())
                .distinct()
                .map(this.onkostarApi::getDiseaseByDiseaseId)
                .collect(Collectors.toList());
    }

    public long countConsents(int year) {
        return findDiseasesWithMtbAnmeldungInYear(year).stream()
                .flatMap(disease ->
                    onkostarApi.getProceduresForPatientByForm(disease.getPatientId(), "MolPath Consent", null).stream()
                )
                .filter(Objects::nonNull)
                .count();
    }

    public List<Disease> findPrimaerfaelle(int year) {
        final var lastTwoYears = Stream.of(year - 2, year - 1)
                .flatMap(y -> this.findDiseasesWithMtbEmpfehlungInYear(y).stream().map(Disease::getId))
                .distinct()
                .collect(Collectors.toList());

        return this.findDiseasesWithMtbEmpfehlungInYear(year).stream()
                .filter(disease -> !lastTwoYears.contains(disease.getId()))
                // TODO Add filter for primaerfaelle - Entität?
                .collect(Collectors.toList());
    }

    public Case findCase(String guid) {
        final var sql = "SELECT patient.patienten_id, patient.guid, ep.erkrankung_id, e.diagnose AS icd10, a.anmeldedatum, e.mtbdatum, e.modellvorhaben FROM dk_mtb_empfehlung e " +
                "    JOIN prozedur p ON (e.id = p.id) " +
                "    JOIN patient ON (p.patient_id = patient.id) " +
                "    JOIN erkrankung_prozedur ep ON (p.id = ep.prozedur_id) " +
                "    JOIN dk_mtb_anmeldung a ON (a.id = e.anmeldung) " +
                "    WHERE p.geloescht <> 1 AND patient.guid = ? LIMIT 1;";
        return jdbcTemplate.query(sql, new Object[]{guid}, rs -> {
            if (rs.next()) {
                return new Case(
                        rs.getString("patienten_id"),
                        rs.getString("icd10"),
                        rs.getString("guid"),
                        rs.getString("anmeldedatum"),
                        rs.getString("mtbdatum"),
                        null,
                        findLatestDokuDatum(rs.getInt("erkrankung_id")) ,
                        rs.getBoolean("modellvorhaben")
                );
            }
            return null;
        });
    }

    private String findLatestDokuDatum(int erkrankungId) {
        final var sql = "SELECT erstelldatum FROM prozedur JOIN erkrankung_prozedur ON (prozedur.id = erkrankung_prozedur.prozedur_id) " +
                "WHERE geloescht <> 1 AND erkrankung_id = ? ORDER BY erstelldatum DESC LIMIT 1;";

        final var date = jdbcTemplate.query(sql, new Object[]{erkrankungId}, rs -> {
            if (rs.next()) {
                return rs.getDate("erstelldatum");
            }
            return null;
        });

        if (date == null) {
            return null;
        }

        final var format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(date);
    }

    public static class Case {
        public String pid;
        public String icd;
        public String guid;
        public String anmeldedatum;
        public String empfehlungsdatum;
        public String consentdatum;
        public String latestDokuDatum;
        public boolean einschlussMvh;

        public Case(String pid, String icd, String guid, String anmeldedatum, String empfehlungsdatum, String consentdatum, String latestDokuDatum, boolean einschlussMvh) {
            this.pid = pid;
            this.icd = icd;
            this.guid = guid;
            this.anmeldedatum = anmeldedatum;
            this.empfehlungsdatum = empfehlungsdatum;
            this.consentdatum = consentdatum;
            this.latestDokuDatum = latestDokuDatum;
            this.einschlussMvh = einschlussMvh;
        }
    }

}
