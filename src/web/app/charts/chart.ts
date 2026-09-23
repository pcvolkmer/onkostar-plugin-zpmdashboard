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

import {Component, DestroyRef, inject, Input, SimpleChanges} from '@angular/core';
import {NgxEchartsModule} from 'ngx-echarts';
import type {ECharts, EChartsOption} from 'echarts';
import {debounceTime, Subject, Subscription} from "rxjs";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
    selector: 'pie-chart',
    standalone: true,
    imports: [NgxEchartsModule],
    templateUrl: './chart.html',
    styleUrl: './chart.css',
})
export class PieChartComponent {
    @Input() title!: string
    @Input() data!: { name: string; value: number }[];

    private readonly destroyRef = inject(DestroyRef);
    private chart?: ECharts;
    private dataSubject = new Subject<{ name: string; value: number }[]>();
    private subscription: Subscription;

    constructor() {
        this.subscription = this.dataSubject
            .pipe(
                debounceTime(500),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe(data => {
                this.updateChart(data);
            });
    }

    onChartInit(chart: ECharts) {
        this.chart = chart;
    }

    chartOptions: EChartsOption = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {c}'
        },

        legend: {
            orient: 'horizontal',
            left: 0
        },

        series: [{
            type: 'pie',
            avoidLabelOverlap: true,
            itemStyle: {
                borderRadius: 3,
                borderColor: '#fff',
                borderWidth: 2
            },
            label: {
                show: false
            },
            radius: ['40%', '70%'],
            center: ['50%', '40%'],
            emphasis: {
                itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.5)'
                }
            }
        }]
    };

    ngOnChanges(changes: SimpleChanges) {
        if (changes['data']) {
            this.dataSubject.next(changes['data'].currentValue)
        }
    }

    private updateChart(data: { name: string; value: number }[]) {
        this.chart?.setOption({
            title: {
                text: this.title,
                left: 'left',
                top: 10
            },
            series: [{data}]
        });
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }
}
