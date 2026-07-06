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

import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;

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
        final var currentYear = LocalDate.now(TimeZone.getDefault().toZoneId()).getYear();
        final var pf = List.of(
                new Primaerfaelle(currentYear - 2, this.zpmDashboardService.findPrimaerfaelleCaseId(currentYear - 2).size()),
                new Primaerfaelle(currentYear - 1, this.zpmDashboardService.findPrimaerfaelleCaseId(currentYear - 1).size()),
                new Primaerfaelle(currentYear, this.zpmDashboardService.findPrimaerfaelleCaseId(currentYear).size())
        );

        final var statistics = new Statistics(
                this.zpmDashboardService.findMtbAnmeldungInYear(year).size(),
                this.zpmDashboardService.findMtbEmpfehlungInYear(year).size(),
                this.zpmDashboardService.countConsents(year),
                pf);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/zpm-dashboard/cases")
    public ResponseEntity<List<ZpmDashboardService.CaseId>> getCases(@RequestParam int year) {
        final var cases = this.zpmDashboardService.findPrimaerfaelleCaseId(year);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/zpm-dashboard/cases/{guid}")
    public ResponseEntity<?> getCase(@PathVariable String guid) {
        try {
            final var theCase = this.zpmDashboardService.findCase(guid);
            if (theCase == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(theCase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
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

}
