import {inject, Service} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CaseId, CaseModel, StatisticsModel} from "./model";

@Service()
export class OnkostarService {
  http: HttpClient;

  constructor() {
    this.http = inject(HttpClient);
  }

  getStatistics(year: string): Observable<StatisticsModel> {
    return this.http.get<StatisticsModel>(`/onkostar/zpm-dashboard/statistics?year=${year}`);
  }

  getCases(year: string): Observable<CaseId[]> {
    return this.http.get<CaseId[]>(`/onkostar/zpm-dashboard/cases?year=${year}`);
  }

  getCase(guid: string): Observable<CaseModel> {
    return this.http.get<CaseModel>(`/onkostar/zpm-dashboard/cases/${guid}`);
  }
}
