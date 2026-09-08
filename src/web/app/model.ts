export class StatisticsModel {
  public anmeldungen: number;
  public empfehlungen: number;
  public consents: number;
  public primaerfaelle: PrimaerfaelleModel[];

  constructor() {
    this.anmeldungen = 0;
    this.empfehlungen = 0;
    this.consents = 0;
    this.primaerfaelle = [];
  }
}

export class PrimaerfaelleModel {
  public year: number;
  public count: number;

  constructor() {
    this.year = 2000;
    this.count = 0;
  }
}

export class CaseId {
  public pid: string;
  public patientGuid: string;
  public procedureGuid: string;

  constructor() {
    this.pid = "0";
    this.patientGuid = "00000000-0000-0000-0000-000000000000";
    this.procedureGuid = "00000000-0000-0000-0000-000000000000";
  }
}

export class CaseModel {
  public pid: string;
  public icd: string;
  public patientGuid: string;
  public procedureGuid: string;
  public zaehlzeitpunkt: string | null;
  public anmeldedatum: string | null;
  public internextern: string | null;
  public consent: Consent;
  public molgen: MolGen;
  public empfehlungsdatum: string | null;
  public latestDokuDatum: string | null;
  public offlabel: boolean;
  public studie: boolean;
  public einschlussMvh: boolean;
  public warnings: boolean;
  public warningDetails: WarningDetails;
  public aufgaben: Aufgabe[];

  constructor() {
    this.pid = "";
    this.icd = "";
    this.patientGuid = "00000000-0000-0000-0000-000000000000";
    this.procedureGuid = "00000000-0000-0000-0000-000000000000";
    this.zaehlzeitpunkt = null;
    this.anmeldedatum = null;
    this.internextern = null;
    this.consent = new Consent();
    this.molgen = new MolGen();
    this.empfehlungsdatum = null;
    this.latestDokuDatum = null;
    this.offlabel = false;
    this.studie = false;
    this.einschlussMvh = false;
    this.warnings = false;
    this.warningDetails = new WarningDetails();
    this.aufgaben = [];
  }
}

export class Consent {
    public datum: string | null;
    public zustimmung: boolean;

    constructor() {
        this.datum = null;
        this.zustimmung = false;
    }
}

export class MolGen {
  public datum: string | null;
  public korrekt: boolean;

  constructor() {
    this.datum = null;
    this.korrekt = false;
  }
}

export class WarningDetails {
  public invalidPrimaerfall: boolean;
  public noMolgen: boolean;
  public noDisease: boolean;

  constructor() {
    this.invalidPrimaerfall = false;
    this.noMolgen = false;
    this.noDisease = false;
  }
}

export class Aufgabe {
  public akteur: string;
  public date: string;
  public diseaseGuid: string;
  public formName: string | null;
  public formDate: string | null;
  public formGuid: string | null;
  public text: string;
  public dueDate: string | null;

  constructor() {
    this.akteur = "";
    this.date = "";
    this.diseaseGuid = "";
    this.formName = null;
    this.formDate = null;
    this.formGuid = null;
    this.text = "";
    this.dueDate = null;
  }
}

