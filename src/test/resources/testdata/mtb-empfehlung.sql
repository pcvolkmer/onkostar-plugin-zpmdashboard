/* Testpatient 1 hat Empfehlung */
insert into dk_mtb_empfehlung (id, mtbdatum) values (2001, '2026-08-02');
insert into prozedur (id, patient_id, guid, geloescht, prozedurtyp, data_form_id, hat_dokumente) values (2001, 1, '20000000-0000-0000-0000-000000000001', 0, -3, 0, 0);

/* Testpatient 2 und 3 hat KEINE Empfehlung */

