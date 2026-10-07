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

import {Component, OnInit, signal} from '@angular/core';
import {CaseId, Context, StatisticsModel} from "../model";
import {OnkostarService} from "../onkostar.service";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
    template: ''
})
export abstract class AbstractCasesComponent implements OnInit {
    protected statistics = signal<StatisticsModel>(new StatisticsModel());
    protected cases = signal<CaseId[]>([]);
    protected year = signal<string>(new Date().getFullYear().toString());

    protected warningCount = 0;
    protected invalidPrimaerfallCount = 0;
    protected internCount = 0;
    protected externCount = 0;
    protected offlabelCount = 0;
    protected studyCount = 0;
    protected taskCount = 0;
    protected entitaetCounts = new Map<string, number>;
    protected dueFollowUpCount = 0;

    protected hideNoneWarnings = false;
    protected hideNonePFWarnings = false;
    protected hideNoneTasks = false;
    protected hideNoneDueFollowUps = false;

    protected exportMarkedCases = new Array<string>();

    constructor(readonly onkostarService: OnkostarService, readonly route: ActivatedRoute, readonly router: Router) {
        this.onkostarService = onkostarService;
    }

    ngOnInit() {
        this.route.queryParams.subscribe((params) => {
            if (params['year']) {
                this.year.set(params['year']);
            }

            this.loadData();
        });
    }

    protected years(): string[] {
        let year = new Date().getFullYear();
        let result = [];
        for (let i = 0; i < 3; i++) {
            result.push(`${year - i}`);
        }
        return result;
    }

    protected onYearChangeEvent($event: Event) {
        this.onYearChange(($event.target as HTMLSelectElement).value);
    }

    protected onYearChange(year: string) {
        this.router.navigate([], {
            queryParams: {
                year: year
            },
            queryParamsHandling: 'merge', // Preserve other query parameters
        });
    }

    protected onContextChange(context: string) {
        this.router.navigate([], {
            queryParams: {
                context: context
            },
            queryParamsHandling: 'merge', // Preserve other query parameters
        });
    }

    protected abstract loadData(): void;

    protected updateWarningCount() {
        this.warningCount++;
    }

    protected updateInvalidPrimaerfallCount() {
        this.invalidPrimaerfallCount++;
    }

    protected updateInternexternCount(value: string | null) {
        if (value === 'E') {
            this.externCount++;
        } else {
            this.internCount++;
        }
    }

    protected updateOfflabelCount() {
        this.offlabelCount++;
    }

    protected updateStudyCount() {
        this.studyCount++;
    }

    protected updateTaskCount() {
        this.taskCount++;
    }

    protected updateEntitaetCounts(value: string | null) {
        if (value == null) {
            return;
        }
        let currentCount = this.entitaetCounts.get(value);
        if (currentCount) {
            this.entitaetCounts.set(value, ++currentCount);
        } else {
            this.entitaetCounts.set(value, 1);
        }
    }

    protected updateDueFollowUp() {
        this.dueFollowUpCount++;
    }

    protected entitaetenChartData(): Array<any> {
        const sortedMap = new Map(
            [...this.entitaetCounts.entries()].sort(([, a], [, b]) => b - a)
        );
        let result = []
        let allCount = 0;
        for (let [name, count] of sortedMap) {
            if (result.length < 5 || count > 10) {
                result.push({name: name, value: count});
                allCount += count;
            }
        }
        result.push({name: 'sonstiges', value: this.selectedPFcount() - allCount})
        return result;
    }

    protected selectedPFcount(): number {
        for (let i in this.statistics().cases) {
            if (`${this.statistics().cases[i].year}` == this.year()) {
                return this.statistics().cases[i].count;
            }
        }
        return 0;
    }

    protected switchNonWarnings() {
        this.hideNoneWarnings = !this.hideNoneWarnings;
        this.hideNonePFWarnings = false;
        this.hideNoneTasks = false;
        this.hideNoneDueFollowUps = false;
        if (this.hideNoneWarnings) {
            document.querySelectorAll('.dashboard-entry').forEach(elem => {
                (elem as HTMLElement).style.display = 'none';
            });
            document.querySelectorAll('.dashboard-entry:has(.check-required)').forEach(elem => {
                (elem as HTMLElement).style.display = '';
            });
            return;
        }
        document.querySelectorAll('.dashboard-entry').forEach(elem => {
            (elem as HTMLElement).style.display = '';
        });
    }

    protected switchNonPFWarnings() {
        this.hideNonePFWarnings = !this.hideNonePFWarnings;
        this.hideNoneWarnings = false;
        this.hideNoneTasks = false;
        this.hideNoneDueFollowUps = false;
        if (this.hideNonePFWarnings) {
            document.querySelectorAll('.dashboard-entry').forEach(elem => {
                (elem as HTMLElement).style.display = 'none';
            });
            document.querySelectorAll('.dashboard-entry:has(.check-nopf)').forEach(elem => {
                (elem as HTMLElement).style.display = '';
            });
            return;
        }
        document.querySelectorAll('.dashboard-entry').forEach(elem => {
            (elem as HTMLElement).style.display = '';
        });
    }

    protected switchNonTasks() {
        this.hideNoneTasks = !this.hideNoneTasks;
        this.hideNonePFWarnings = false;
        this.hideNoneWarnings = false;
        this.hideNoneDueFollowUps = false;
        if (this.hideNoneTasks) {
            document.querySelectorAll('.dashboard-entry').forEach(elem => {
                (elem as HTMLElement).style.display = 'none';
            });
            document.querySelectorAll('.dashboard-entry:has(.check-tasks)').forEach(elem => {
                (elem as HTMLElement).style.display = '';
            });
            return;
        }
        document.querySelectorAll('.dashboard-entry').forEach(elem => {
            (elem as HTMLElement).style.display = '';
        });
    }

    protected switchDueFollowUps() {
        this.hideNoneDueFollowUps = !this.hideNoneDueFollowUps;
        this.hideNoneTasks = false;
        this.hideNonePFWarnings = false;
        this.hideNoneWarnings = false;
        if (this.hideNoneDueFollowUps) {
            document.querySelectorAll('.dashboard-entry').forEach(elem => {
                (elem as HTMLElement).style.display = 'none';
            });
            document.querySelectorAll('.dashboard-entry:has(.due-followup)').forEach(elem => {
                (elem as HTMLElement).style.display = '';
            });
            return;
        }
        document.querySelectorAll('.dashboard-entry').forEach(elem => {
            (elem as HTMLElement).style.display = '';
        });
    }

    protected onExportMarkChanged($event: {pid: string, checked: boolean}) {
        if ($event.checked) {
            if (!this.exportMarkedCases.includes($event.pid)) {
                this.exportMarkedCases.push($event.pid);
            }
        } else {
            this.exportMarkedCases = this.exportMarkedCases.filter(pid => pid !== $event.pid);
        }
    }

    protected readonly Context = Context;
}
