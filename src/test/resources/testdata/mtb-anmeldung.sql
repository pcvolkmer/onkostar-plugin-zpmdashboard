/* Testpatient 1 hat Anmeldung */
insert into dk_mtb_anmeldung (id, anmeldedatum) values (1001, '2026-08-02');
insert into prozedur (id, patient_id, guid, geloescht, prozedurtyp, data_form_id, hat_dokumente) values (1001, 1, '10000000-0000-0000-0000-000000000001', 0, -3, 0, 0);

/* Testpatient 2 hat Anmeldung */
insert into dk_mtb_anmeldung (id, anmeldedatum) values (1002, '2026-08-02');
insert into prozedur (id, patient_id, guid, geloescht, prozedurtyp, data_form_id, hat_dokumente) values (1002, 2, '10000000-0000-0000-0000-000000000002', 0, -3, 0, 0);

/* Testpatient 3 hat KEINE Anmeldung */

