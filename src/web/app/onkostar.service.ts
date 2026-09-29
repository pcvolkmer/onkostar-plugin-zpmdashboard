import {inject, Service} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CaseId, CaseModel, Context, StatisticsModel} from "./model";

@Service()
export class OnkostarService {
  http: HttpClient;

  constructor() {
    this.http = inject(HttpClient);
  }

  getStatistics(year: string, context: Context | null): Observable<StatisticsModel> {
    if (context === null) {
      return new Observable();
    }
    let c = 'pf';
    if (context === Context.AlleFaelle) {
      c = 'all';
    } else if (context === Context.Modellvorhaben) {
      c = 'mv';
    }
    return this.http.get<StatisticsModel>(`/onkostar/zpm-dashboard/statistics?year=${year}&context=${c}`);
  }

  getCases(year: string, context: Context | null): Observable<CaseId[]> {
    if (context === null) {
      return new Observable();
    }
    let c = 'pf';
    if (context === Context.AlleFaelle) {
      c = 'all';
    } else if (context === Context.Modellvorhaben) {
      c = 'mv';
    }
    return this.http.get<CaseId[]>(`/onkostar/zpm-dashboard/cases?year=${year}&context=${c}`);
  }

  getCase(patientGuid: string, procedureGuid: string, year: string, context: Context | null): Observable<CaseModel> {
    if (context === null) {
      return new Observable();
    }
    let c = 'pf';
    if (context === Context.AlleFaelle) {
      c = 'all';
    } else if (context === Context.Modellvorhaben) {
      c = 'mv';
    }
    return this.http.get<CaseModel>(`/onkostar/zpm-dashboard/cases/${patientGuid}/${procedureGuid}?year=${year}&context=${c}`);
  }
}
