import {Component, Input, OnInit, output, signal} from '@angular/core';
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
  @Input() pid!: string
  @Input() patientGuid!: string;
  @Input() procedureGuid!: string;
  @Input() year!: string;

  protected loadingError = false;
  protected data = signal<CaseModel>(new CaseModel());

  public warningsChange = output();
  public invalidPrimaerfallChange = output();
  public internexternChange = output<string | null>();
  public offlabelCountChange = output();
  public studyCountChange = output();

  constructor(readonly onkostarService: OnkostarService) {
    this.onkostarService = onkostarService;
  }

  ngOnInit() {
    this.onkostarService.getCase(this.patientGuid, this.procedureGuid, this.year).subscribe(res => {
      if (null === res) {
        this.warningsChange.emit();
        let res = new CaseModel();
        res.pid = this.pid;
        res.patientGuid = this.patientGuid;
        res.procedureGuid = this.procedureGuid;
        this.data.set(res);
        this.loadingError = true;
        return;
      }

      res.patientGuid = btoa(res.patientGuid);
      res.procedureGuid = btoa(res.procedureGuid);
      this.data.set(res);

      if (res.warnings) {
        this.warningsChange.emit();
      }
      if (res.warningDetails.invalidPrimaerfall) {
        this.invalidPrimaerfallChange.emit();
      }
      this.internexternChange.emit(res.internextern)
      if (res.offlabel) {
        this.offlabelCountChange.emit();
      }
      if (res.studie) {
        this.studyCountChange.emit();
      }
    });
  }
}
