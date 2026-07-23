import {Component, Input, OnInit, signal} from '@angular/core';
import {CaseModel} from "../model";
import {OnkostarService} from "../onkostar.service";
import {DatePipe} from "@angular/common";

@Component({
  selector: 'app-dashboard-entry',
  imports: [
    DatePipe
  ],
  templateUrl: './dashboard-entry.html',
  styleUrl: './dashboard-entry.css',
})
export class DashboardEntry implements OnInit {
  @Input() patientGuid!: string;
  @Input() procedureGuid!: string;

  protected data = signal<CaseModel>(new CaseModel());

  constructor(readonly onkostarService: OnkostarService) {
    this.onkostarService = onkostarService;
  }

  ngOnInit() {
    this.onkostarService.getCase(this.patientGuid, this.procedureGuid).subscribe(res => {
      res.patientGuid = btoa(res.patientGuid);
      res.procedureGuid = btoa(res.procedureGuid);
      this.data.set(res);
    });
  }
}
