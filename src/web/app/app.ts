import {Component, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router, RouterOutlet} from '@angular/router';
import {OnkostarService} from './onkostar.service';
import {CaseId, Context, StatisticsModel} from "./model";
import {DashboardEntry} from "./dashboard-entry/dashboard-entry";
import {PieChartComponent} from "./charts/chart";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, DashboardEntry, PieChartComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected statistics = signal<StatisticsModel>(new StatisticsModel());
  protected cases = signal<CaseId[]>([]);
  protected year = signal<string>(new Date().getFullYear().toString());
  protected context = signal<Context>(Context.Primaerfaelle);

  protected warningCount = 0;
  protected invalidPrimaerfallCount = 0;
  protected internCount = 0;
  protected externCount = 0;
  protected offlabelCount = 0;
  protected studyCount = 0;
  protected taskCount = 0;
  protected entitaetCounts = new Map<string, number>;

  protected hideNoneWarnings = false;
  protected hideNonePFWarnings = false;
  protected hideNoneTasks = false;

  protected exportMarkedCases = new Array<string>();

  constructor(readonly onkostarService: OnkostarService, readonly route: ActivatedRoute, readonly router: Router) {
    this.onkostarService = onkostarService;
  }

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      if (params['year']) {
        this.year.set(params['year']);
      }

      if (params['context'] === 'pf') {
        this.context.set(Context.Primaerfaelle);
      } else if (params['context'] === 'all') {
        this.context.set(Context.AlleFaelle);
      } else if (params['context'] === 'mv') {
        this.context.set(Context.Modellvorhaben);
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

    this.exportMarkedCases = [];

    this.statistics.set(new StatisticsModel());
    this.onkostarService.getStatistics(this.year(), this.context()).subscribe(res => {
      this.statistics.set(res);
    });
    this.cases.set([]);
    this.onkostarService.getCases(this.year(), this.context()).subscribe(res => {
      this.cases.set(res);
    });
    this.entitaetCounts.clear();
  }

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

  protected onExportMarkChanged($event: {pid: string, checked: boolean}) {
    if ($event.checked) {
      if (!this.exportMarkedCases.includes($event.pid)) {
        this.exportMarkedCases.push($event.pid);
      }
    } else {
      this.exportMarkedCases = this.exportMarkedCases.filter(pid => pid !== $event.pid);
    }
  }

  protected get casesName(): string {
    if (this.context() == Context.AlleFaelle) {
      return 'Fälle';
    } else if (this.context() == Context.Modellvorhaben) {
      return 'MV-Fälle';
    } else {
      return 'Primärfälle';
    }
  }

  protected readonly Context = Context;
}
