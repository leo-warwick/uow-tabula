alter table mitigatingcircumstancessubmission
  add column sensitiveEvidenceComments bytea,
  add column sensitiveEvidenceSeenBy varchar(255),
  add column sensitiveEvidenceSeenOn date;