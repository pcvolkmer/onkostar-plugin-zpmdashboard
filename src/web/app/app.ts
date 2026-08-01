import {Component, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterOutlet} from '@angular/router';
import {OnkostarService} from './onkostar.service';
import {CaseId, StatisticsModel} from "./model";
import {DashboardEntry} from "./dashboard-entry/dashboard-entry";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, DashboardEntry],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected statistics = signal<StatisticsModel>(new StatisticsModel());
  protected cases = signal<CaseId[]>([]);
  protected year = signal<string>(new Date().getFullYear().toString());

  protected warningCount = 0;
  protected internCount = 0;
  protected externCount = 0;
  protected offlabelCount = 0;
  protected studyCount = 0;

  protected hideNoneWarnings = false;

  constructor(readonly onkostarService: OnkostarService, readonly route: ActivatedRoute, readonly router: Router) {
    this.onkostarService = onkostarService;
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

  protected loadData() {
    this.hideNoneWarnings = false;

    this.warningCount = 0;
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

  protected updateWarningCount() {
    this.warningCount++;
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

  protected switchNonWarnings() {
    this.hideNoneWarnings = !this.hideNoneWarnings;
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
}
