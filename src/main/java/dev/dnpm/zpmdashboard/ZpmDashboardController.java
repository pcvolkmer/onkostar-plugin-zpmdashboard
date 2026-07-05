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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ZpmDashboardController {

    private final ZpmDashboardService zpmDashboardService;

    public ZpmDashboardController(final ZpmDashboardService zpmDashboardService) {
        this.zpmDashboardService = zpmDashboardService;
    }

    @GetMapping("/zpm-dashboard")
    public ResponseEntity<Void> getIndexPage() {
        return ResponseEntity
                .status(HttpStatus.TEMPORARY_REDIRECT)
                .header(HttpHeaders.LOCATION, "/onkostar/zpm-dashboard/index.html")
                .build();
    }

    @GetMapping("/zpm-dashboard/statistics")
    public ResponseEntity<Statistics> getStatistics(@RequestParam int year) {
        final var pf = List.of(
                new Primaerfaelle(year - 2, this.zpmDashboardService.findPrimaerfaelle(year - 2).size()),
                new Primaerfaelle(year - 1, this.zpmDashboardService.findPrimaerfaelle(year - 1).size()),
                new Primaerfaelle(year, this.zpmDashboardService.findPrimaerfaelle(year).size())
        );

        final var statistics = new Statistics(
                this.zpmDashboardService.findDiseasesWithMtbAnmeldungInYear(year).size(),
                this.zpmDashboardService.findDiseasesWithMtbEmpfehlungInYear(year).size(),
                (int)this.zpmDashboardService.countConsents(year),
                pf);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/zpm-dashboard/cases")
    public ResponseEntity<List<CaseId>> getCases(@RequestParam int year) {
        final var cases = this.zpmDashboardService.findPrimaerfaelle(year).stream()
                .map(disease -> new CaseId(disease.getPatient().getPatientId(), disease.getPatient().getGuid()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/zpm-dashboard/cases/{guid}")
    public ResponseEntity<ZpmDashboardService.Case> getCase(@PathVariable String guid) {
        try {
            final var theCase = this.zpmDashboardService.findCase(guid);
            if (theCase == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(theCase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    public static class Statistics {
        public Integer anmeldungen;
        public Integer empfehlungen;
        public Integer consents;
        public List<Primaerfaelle> primaerfaelle;

        public Statistics(int anmeldungen, int empfehlungen, int consents, List<Primaerfaelle> primaerfaelle) {
            this.anmeldungen = anmeldungen;
            this.empfehlungen = empfehlungen;
            this.consents = consents;
            this.primaerfaelle = primaerfaelle;
        }
    }

    public static class Primaerfaelle {
        public Integer year;
        public Integer count;

        public Primaerfaelle(int year, int count) {
            this.year = year;
            this.count = count;
        }
    }

    public static class CaseId {
        public String pid;
        public String guid;

        public CaseId(String pid, String guid) {
            this.pid = pid;
            this.guid = guid;
        }
    }

}
