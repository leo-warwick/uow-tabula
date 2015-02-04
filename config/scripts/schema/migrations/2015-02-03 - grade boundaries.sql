-- TAB-3245
ALTER TABLE UPSTREAMASSIGNMENT
ADD (
  MARKSCODE nvarchar2(100)
);

CREATE TABLE GRADEBOUNDARY
(
  "ID" NVARCHAR2(250) NOT NULL,
  "MARKSCODE" NVARCHAR2(100) NOT NULL,
  "GRADE" NVARCHAR2(100) NOT NULL,
  "MINIMUMMARK" NUMBER(3,0) NOT NULL,
  "MAXIMUMMARK" NUMBER(3,0) NOT NULL,
  CONSTRAINT "GRADEBOUNDARY_PK" PRIMARY KEY ("ID")
);

CREATE INDEX IDX_GRADEBOUNDARY_MARKSCODE ON GRADEBOUNDARY(MARKSCODE);