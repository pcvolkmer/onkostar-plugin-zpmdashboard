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

import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

@RestController
public class ZpmDashboardController {
    private final ZpmDashboardService zpmDashboardService;
    private final ResourceLoader resourceLoader;

    public ZpmDashboardController(
            final ZpmDashboardService zpmDashboardService,
            final ResourceLoader resourceLoader

            ) {
        this.zpmDashboardService = zpmDashboardService;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping(path = {
            "/zpm-dashboard",
            "/zpm-dashboard/prime",
            "/zpm-dashboard/all",
            "/zpm-dashboard/mv"
    }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> getIndexPage(
            @RequestParam(required = false, defaultValue = "") String year
    ) {
        try {
            final var indexPage = resourceLoader.getResource("classpath:static/index.html").getInputStream().readAllBytes();

            return ResponseEntity
                    .ok(indexPage);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/zpm-dashboard/statistics")
    public ResponseEntity<Statistics> getStatistics(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "pf") String context
    ) {
        final var currentYear = LocalDate.now(TimeZone.getDefault().toZoneId()).getYear();

        List<StatisticCases> pf;
        if (context.equals("all")) {
            pf = List.of(
                    new StatisticCases(currentYear - 2, this.zpmDashboardService.findAllCaseId(currentYear - 2).size()),
                    new StatisticCases(currentYear - 1, this.zpmDashboardService.findAllCaseId(currentYear - 1).size()),
                    new StatisticCases(currentYear, this.zpmDashboardService.findAllCaseId(currentYear).size())
            );
        } else if (context.equals("mv")) {
            pf = List.of(
                    new StatisticCases(currentYear - 2, this.zpmDashboardService.findModellvorhabenCaseId(currentYear - 2).size()),
                    new StatisticCases(currentYear - 1, this.zpmDashboardService.findModellvorhabenCaseId(currentYear - 1).size()),
                    new StatisticCases(currentYear, this.zpmDashboardService.findModellvorhabenCaseId(currentYear).size())
            );
        } else {
            pf = List.of(
                    new StatisticCases(currentYear - 2, this.zpmDashboardService.findPrimaerfaelleCaseId(currentYear - 2).size()),
                    new StatisticCases(currentYear - 1, this.zpmDashboardService.findPrimaerfaelleCaseId(currentYear - 1).size()),
                    new StatisticCases(currentYear, this.zpmDashboardService.findPrimaerfaelleCaseId(currentYear).size())
            );
        }

        final var statistics = new Statistics(
                this.zpmDashboardService.countMtbAnmeldungInYear(year),
                this.zpmDashboardService.countMtbEmpfehlungInYear(year),
                this.zpmDashboardService.countConsents(year),
                pf);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/zpm-dashboard/cases")
    public ResponseEntity<List<ZpmDashboardService.CaseId>> getCases(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "pf") String context
    ) {
        if (context.equals("all")) {
            final var cases = this.zpmDashboardService.findAllCaseId(year);
            return ResponseEntity.ok(cases);
        } else if (context.equals("mv")) {
            final var cases = this.zpmDashboardService.findModellvorhabenCaseId(year);
            return ResponseEntity.ok(cases);
        }
        final var cases = this.zpmDashboardService.findPrimaerfaelleCaseId(year);
        return ResponseEntity.ok(cases);
    }

    @GetMapping(value = "/zpm-dashboard/cases.xlsx")
    public ResponseEntity<byte[]> getCasesXls(
            @RequestParam int year,
            @RequestParam(required = false, defaultValue = "pf") String context,
            @RequestParam(required = false, defaultValue = "") List<String> pid
    ) {
        final var cases = this.zpmDashboardService.casesXsl(year, context, pid);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=Primaerfaelle_%d.xlsx", year))
                .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cases);
    }

    @GetMapping("/zpm-dashboard/cases/{patientGuid}/{procedureGuid}")
    public ResponseEntity<?> getCase(
            @PathVariable String patientGuid,
            @PathVariable String procedureGuid,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "pf") String context
    ) {
        if (null == year) {
            year = LocalDate.now(ZoneId.systemDefault()).getYear();
        }

        try {
            ZpmDashboardService.Case theCase;
            if ("mv".equals(context) || "all".equals(context)) {
                theCase = this.zpmDashboardService.findAnmeldungCase(patientGuid, procedureGuid, year);
            } else {
                theCase = this.zpmDashboardService.findZpmCase(patientGuid, procedureGuid, year);
            }
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
        public List<StatisticCases> cases;

        public Statistics(int anmeldungen, int empfehlungen, int consents, List<StatisticCases> cases) {
            this.anmeldungen = anmeldungen;
            this.empfehlungen = empfehlungen;
            this.consents = consents;
            this.cases = cases;
        }
    }

    public static class StatisticCases {
        public Integer year;
        public Integer count;

        public StatisticCases(int year, int count) {
            this.year = year;
            this.count = count;
        }
    }

}
