-- TAB-4099
CREATE TABLE COREREQUIREDMODULE
(
  ID NVARCHAR2(255) NOT NULL,
  ROUTECODE NVARCHAR2(20) NOT NULL,
  ACADEMICYEAR NUMBER(4,0) NOT NULL,
  YEAROFSTUDY NUMBER(2,0) NOT NULL,
  MODULECODE NVARCHAR2(20) NOT NULL,
  CONSTRAINT COREREQUIREDMODULE_PK PRIMARY KEY (ID)
);

CREATE UNIQUE INDEX IDX_COREREQUIREDMODULE_UNIQUE ON COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE);

INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-1', 'g1g3', 2015, 1, 'ma106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-2', 'g1g3', 2015, 1, 'ma137');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-3', 'g1g3', 2015, 1, 'st104');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-4', 'g1g3', 2015, 1, 'st115');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-5', 'g300', 2015, 1, 'ec106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-6', 'g300', 2015, 1, 'ib104');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-7', 'g300', 2015, 1, 'ma106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-8', 'g300', 2015, 1, 'ma137');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-9', 'g300', 2015, 1, 'st115');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-10', 'g302', 2015, 1, 'cs118');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-11', 'g302', 2015, 1, 'cs126');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-12', 'g302', 2015, 1, 'ma106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-13', 'g302', 2015, 1, 'st104');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-14', 'g302', 2015, 1, 'st115');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-15', 'gg14', 2015, 1, 'ma106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-16', 'gg14', 2015, 1, 'ma137');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-17', 'gg14', 2015, 1, 'st104');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-18', 'gg14', 2015, 1, 'st115');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-19', 'y602', 2015, 1, 'ec106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-20', 'y602', 2015, 1, 'ib104');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-21', 'y602', 2015, 1, 'ma106');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-22', 'y602', 2015, 1, 'ma137');
INSERT INTO COREREQUIREDMODULE(ID, ROUTECODE, ACADEMICYEAR, YEAROFSTUDY, MODULECODE) VALUES ('TAB-4099-23', 'y602', 2015, 1, 'st115');