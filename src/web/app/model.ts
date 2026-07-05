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
  public guid: string;

  constructor() {
    this.pid = "0";
    this.guid = "00000000-0000-0000-0000-000000000000";
  }
}

export class CaseModel {
  public pid: string;
  public icd: string;
  public guid: string;
  public anmeldedatum: string | null;
  public empfehlungsdatum: string | null;
  public consentdatum: string | null;
  public latestDokuDatum: string | null;
  public einschlussMvh: boolean;

  constructor() {
    this.pid = "0";
    this.icd = "C00.0";
    this.guid = "00000000-0000-0000-0000-000000000000";
    this.anmeldedatum = null;
    this.empfehlungsdatum = null;
    this.consentdatum = null;
    this.latestDokuDatum = null;
    this.einschlussMvh = false;
  }
}

