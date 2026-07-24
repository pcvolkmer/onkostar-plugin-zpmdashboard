import {Component, OnInit, signal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {OnkostarService} from './onkostar.service';
import {CaseId, StatisticsModel} from "./model";
import {DashboardEntry} from "./dashboard-entry/dashboard-entry";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, DashboardEntry],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected statistics = signal<StatisticsModel>(new StatisticsModel());
  protected cases = signal<CaseId[]>([]);
  protected year = signal<string>(new Date().getFullYear().toString());

  protected internCount = 0;
  protected externCount = 0;
  protected offlabelCount = 0;
  protected studyCount = 0;

  constructor(readonly onkostarService: OnkostarService) {
    this.onkostarService = onkostarService;
  }

  ngOnInit() {
    this.loadData();
  }

  protected years(): string[] {
    let year = new Date().getFullYear();
    let result = [];
    for (let i = 0; i < 3; i++) {
      result.push(`${year - i}`);
    }
    return result;
  }

  protected onYearChange($event: Event) {
    this.year.set(($event.target as HTMLSelectElement).value);
    this.loadData();
  }

  protected loadData() {
    this.internCount = 0;
    this.externCount = 0;
    this.offlabelCount = 0;
    this.studyCount = 0;

    this.statistics.set(new StatisticsModel());
    this.onkostarService.getStatistics(this.year()).subscribe(res => {
      this.statistics.set(res);
    });
    this.cases.set([]);
    this.onkostarService.getCases(this.year()).subscribe(res => {
      this.cases.set(res);
    });
  }

  protected updateInternexternCount(value: string | null) {
    if (value === 'E') {
      this.externCount++;
    } else if (value === 'I') {
      this.internCount++;
    }
  }

  protected updateOfflabelCount() {
    this.offlabelCount++;
  }

  protected updateStudyCount() {
    this.studyCount++;
  }
}
