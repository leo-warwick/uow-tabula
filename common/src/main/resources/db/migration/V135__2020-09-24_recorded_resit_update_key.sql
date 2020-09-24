drop index if exists ck_recordedresit;
create unique index ck_recordedresit on recordedresit (spr_code, academic_year, module_code, sequence, resit_sequence);
