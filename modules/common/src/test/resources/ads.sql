-- let's make a fake ADS
DROP TABLE MODULE_REGISTRATION IF EXISTS;
DROP TABLE MODULE_AVAILABILITY IF EXISTS;
DROP TABLE MODULE_ASSESSMENT_DETAILS IF EXISTS;
DROP TABLE MODULE IF EXISTS;

CREATE TABLE IF NOT EXISTS MODULE_REGISTRATION
(
  ACADEMIC_YEAR_CODE VARCHAR(6) NOT NULL 
, ASSESSMENT_GROUP VARCHAR(2) 
, CATS integer
, MAV_OCCURRENCE VARCHAR(6) NOT NULL 
, MODULE_CODE VARCHAR(10) NOT NULL 
, MOD_REG_TYPE_CODE integer 
, REGISTRATION_STATUS integer
, SITS_OR_OMR varCHAR(1) 
, SPR_CODE VARCHAR(12) NOT NULL 
);

CREATE TABLE IF NOT EXISTS MODULE_AVAILABILITY
(
  ACADEMIC_YEAR_CODE VARCHAR(6) NOT NULL
, MODULE_CODE VARCHAR(10) NOT NULL
, MAV_OCCURRENCE VARCHAR(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS MODULE_ASSESSMENT_DETAILS
(
  MODULE_CODE VARCHAR(10) NOT NULL
, SEQ VARCHAR(3) NOT NULL
, ASSESSMENT_CODE VARCHAR(6)
, ASSESSMENT_GROUP VARCHAR(2)
, NAME VARCHAR(255)
)

CREATE TABLE IF NOT EXISTS MODULE
(
  MODULE_CODE VARCHAR(10) NOT NULL
, IN_USE CHAR(1)
, DEPARTMENT_CODE VARCHAR(5)
);

-- unique constraint as found on ADS
CREATE UNIQUE INDEX AS16_CSITE ON MODULE_REGISTRATION(ACADEMIC_YEAR_CODE, MODULE_CODE, MAV_OCCURRENCE, SPR_CODE);

-- Thoughts - only the assignment importer test really needs all this data,
-- so perhaps move it into a separate file. Alternatively, just don't invoke
-- ads.sql at all in the regular PersistenceTestBase since we only require
-- an empty but functional datasource there.

INSERT INTO MODULE VALUES ('CH115-30', 'Y', 'CH'); -- live module, students
INSERT INTO MODULE VALUES ('CH120-15', 'Y', 'CH'); -- live module, students
INSERT INTO MODULE VALUES ('CH130-15', 'Y', 'CH'); -- live module, no students
INSERT INTO MODULE VALUES ('CH130-20', 'Y', 'CH'); -- live module, no students
INSERT INTO MODULE VALUES ('XX101-30', 'N', 'XX'); -- inactive module

-- no students registered on CH130, so should show up in list of empty groups
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH130-15', 'A');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH130-20', 'A');
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH130-15', 'A01', 'A', 'A', 'Chem 130 A01');
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH130-20', 'A01', 'A', 'A', 'Chem 130 A01 (20 CATS)');

-- some more items that don't have corresponding students,
-- but don't have the right data in other tables to form a complete entry
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('XX100-30', 'A01', 'A', 'A', 'Mystery Meat');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'XX100-30', 'A');
INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('XX101-30', 'A01', 'A', 'A', 'Danger Zone');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'XX101-30', 'A');

INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH115-30', 'A01', 'A', 'A', 'Chemicals Essay');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH115-30', 'A');

INSERT INTO MODULE_ASSESSMENT_DETAILS VALUES ('CH120-15', 'A01', 'A', 'A', 'Chemistry Dissertation');
INSERT INTO MODULE_AVAILABILITY VALUES ('11/12', 'CH120-15', 'A');

insert into module_registration values ('11/12','A',30,'A','CH115-30',1,1,'S','0123456/1');
insert into module_registration values ('11/12','A',30,'A','CH120-15',1,1,'S','0123458/1');
insert into module_registration values ('11/12','A',30,'A','CH115-30',1,1,'S','0123457/1');
insert into module_registration values ('11/12','A',30,'A','CH115-30',1,1,'S','0123458/1');

