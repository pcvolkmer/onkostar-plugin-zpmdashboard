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
  public anmeldedatum: string | null;
  public internextern: string | null;
  public consent: Consent;
  public molgen: MolGen;
  public empfehlungsdatum: string | null;
  public latestDokuDatum: string | null;
  public offlabel: boolean;
  public studie: boolean;
  public einschlussMvh: boolean;

  constructor() {
    this.pid = "0";
    this.icd = "C00.0";
    this.patientGuid = "00000000-0000-0000-0000-000000000000";
    this.procedureGuid = "00000000-0000-0000-0000-000000000000";
    this.anmeldedatum = null;
    this.internextern = null;
    this.consent = new Consent();
    this.molgen = new MolGen();
    this.empfehlungsdatum = null;
    this.latestDokuDatum = null;
    this.offlabel = false;
    this.studie = false;
    this.einschlussMvh = false;
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

