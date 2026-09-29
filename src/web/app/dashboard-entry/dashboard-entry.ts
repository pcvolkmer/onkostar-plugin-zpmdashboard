import {Component, Input, OnInit, output, signal} from '@angular/core';
import {CaseModel, Context, FollowUpState} from "../model";
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
  @Input() context!: Context | null;
  @Input() duplicate!: boolean;

  protected loadingError = false;
  protected showAufgaben = false;
  protected data = signal<CaseModel>(new CaseModel());

  public warningsChange = output();
  public invalidPrimaerfallChange = output();
  public internexternChange = output<string | null>();
  public offlabelCountChange = output();
  public studyCountChange = output();
  public taskCountChange = output();
  public entitaetChange = output<string | null>();
  public exportMarkChanged = output<{pid: string, checked: boolean}>();

  protected readonly FollowUpState = FollowUpState;

  constructor(readonly onkostarService: OnkostarService) {
    this.onkostarService = onkostarService;
  }

  ngOnInit() {
    this.onkostarService.getCase(this.patientGuid, this.procedureGuid, this.year, this.context).subscribe(res => {
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

      if (res.warnings || this.duplicate) {
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
      if (res.aufgaben.length > 0) {
        this.taskCountChange.emit();
      }
      this.entitaetChange.emit(res.entitaet);
    });
  }

  get followUpState(): FollowUpState {
    let latestFollowUp = this.data().latestFollowUp;
    if (latestFollowUp == null) {
      return FollowUpState.NO_FOLLOW_UP;
    }
    const dueDate = new Date();
    dueDate.setMonth(dueDate.getMonth() -3);
    const followUpDate = new Date(latestFollowUp);
    if (followUpDate < dueDate) {
      return FollowUpState.FOLLOW_UP_PENDING;
    }
    return FollowUpState.FOLLOW_UP_OK;
  }

  protected onExportMarkChanged(e: Event) {
    const checked = (e.target as HTMLInputElement).checked;
    this.exportMarkChanged.emit({pid: this.pid, checked});
  }
}
