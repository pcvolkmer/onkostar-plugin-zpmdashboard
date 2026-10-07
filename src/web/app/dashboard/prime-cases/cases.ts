/*
 * This file is part of zpm-dashboard
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

import {Component, OnInit} from '@angular/core';
import {DashboardEntry} from "../../dashboard-entry/dashboard-entry";
import {PieChartComponent} from "../../charts/chart";
import {Context, StatisticsModel} from "../../model";
import {OnkostarService} from "../../onkostar.service";
import {ActivatedRoute, Router} from "@angular/router";
import {AbstractCasesComponent} from "../cases";

@Component({
    selector: 'all-cases',
    standalone: true,
    imports: [DashboardEntry, PieChartComponent],
    templateUrl: './cases.html',
    styleUrl: '../../app.css'
})
export class PrimeCasesComponent extends AbstractCasesComponent implements OnInit {
    constructor(onkostarService: OnkostarService, route: ActivatedRoute, router: Router) {
        super(onkostarService, route, router);
    }

    protected loadData() {
        this.hideNoneWarnings = false;
        this.hideNonePFWarnings = false;

        this.warningCount = 0;
        this.invalidPrimaerfallCount = 0;
        this.internCount = 0;
        this.externCount = 0;
        this.offlabelCount = 0;
        this.studyCount = 0;
        this.taskCount = 0;
        this.dueFollowUpCount = 0;

        this.exportMarkedCases = [];

        this.statistics.set(new StatisticsModel());
        this.onkostarService.getStatistics(this.year(), Context.Primaerfaelle).subscribe(res => {
            this.statistics.set(res);
        });
        this.cases.set([]);
        this.onkostarService.getCases(this.year(), Context.Primaerfaelle).subscribe(res => {
            this.cases.set(res);
        });
        this.entitaetCounts.clear();
    }
}
