SET DATABASE SQL SYNTAX ORA TRUE;
DROP TABLE MEN_ADD IF EXISTS;

CREATE TABLE MEN_ADD
(
  ADD_AENT VARCHAR(255) NOT NULL,
  ADD_ADID VARCHAR(20)  NOT NULL,
  ADD_SEQN VARCHAR(4)   NOT NULL,
  ADD_MST1 VARCHAR(12),
  ADD_ATYC VARCHAR(6),
  ADD_RADC VARCHAR(12),
  ADD_ADD1 VARCHAR(50),
  ADD_ADD2 VARCHAR(50),
  ADD_ADD3 VARCHAR(50),
  ADD_ADD4 VARCHAR(50),
  ADD_ADD5 VARCHAR(50),
  ADD_PCOD VARCHAR(12),
  ADD_MSRT VARCHAR(5),
  ADD_TELN VARCHAR(35),
  ADD_TEL2 VARCHAR(35),
  ADD_TEL3 VARCHAR(35),
  ADD_FAXN VARCHAR(35),
  ADD_EMAD VARCHAR(255),
  ADD_DETS VARCHAR(255),
  ADD_BEGD DATE,
  ADD_ENDD DATE,
  ADD_TEMP VARCHAR(255),
  ADD_TTAC VARCHAR(6),
  ADD_ACTV VARCHAR(255),
  ADD_FLAG VARCHAR(6),
  ADD_NAM1 VARCHAR(50),
  ADD_NAM2 VARCHAR(50),
  ADD_CODC VARCHAR(6),
  ADD_CNTY VARCHAR(50),
  ADD_UPDD DATE,
  ADD_STAT VARCHAR(255),
  ADD_MST2 VARCHAR(12),
  ADD_USRC VARCHAR(12)
);

Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0028', '1234567', 'CER', null, '1 Church Road', 'Kessingland', 'Lowestoft', null, null, 'NR33 7TH', null, '01502 654321', null,
        'M1:07777777777', null, 'reynardfox@hotmail.co.uk', 'C', null, null, 'N', null, 'C', '123456', null, null, '5826', 'England', null, 'N', '123456',
        'STU');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0024', '1234567', 'CER-2', null, null, null, null, null, null, null, null, null, null, 'M1:07777777777', null,
        'reynardfox@hotmail.co.uk', 'C', null, null, 'N', null, 'C', null, null, null, null, null, null, 'N', '123456', 'STU');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0027', '1234567', 'CMY', null, '1 Church Road', 'Kessingland', 'Lowestoft', null, null, 'NR33 7TH', null, '01502 654321', null,
        'M2:07777777777', null, 'reynardfox@hotmail.co.uk', 'C', null, null, 'N', null, 'C', '123456', null, null, '5826', 'England', null, 'N', '123456',
        'STU');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0025', '1234567', 'CMY-2', null, null, null, null, null, null, null, null, null, null, 'M1:07777777777', null,
        'reynardfox@hotmail.co.uk', 'C', null, null, 'N', null, 'C', null, null, null, null, null, null, 'N', '123456', 'STU');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0029', '1234567', 'CORR', null, '138 Cherry Tree Drive', 'Coventry', null, null, null, 'CV4 8LZ', null, '07777777777', null,
        '07777777777', null, 'reynard.fox@warwick.ac.uk', 'C', null, null, 'Y', null, 'H', '123456', null, null, '5826', 'England', null, 'V', 'ACDJBS',
        'ACDJBS');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0019', '1234567', 'CORR', null, null, null, 'Canley', null, null, null, null, null, null, null, null, 'reynard.fox@warwick.ac.uk',
        'C', null, null, 'N', 'O', 'H', '1', null, null, null, null, null, 'N', '123456', 'STU');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0031', '1234567', 'CORR', null, 'HB1406', 'Heronbank', 'University of Warwick', 'Coventry', null, 'CV4 7ES', null, null, null, null,
        null, 'reynard.fox@warwick.ac.uk', 'C', null, null, 'N', 'C', 'C', '1', null, null, null, null, null, 'V', 'ACDJBS', 'ACDJBS');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0020', '1234567', 'H', null, '1 Church Road', 'Kessingland', 'Lowestoft', null, null, 'NR33 7TH', null, '01502 654321', null,
        '07777777777', null, 'reynardfox@hotmail.co.uk', 'C', null, null, 'N', null, 'H', '4', null, null, '5826', 'England', null, 'N', '123456', 'STU');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0021', '1234567', 'H', null, '1 CHURCH ROAD', 'KESSINGLAND', 'LOWESTOFT', 'SUFFOLK', null, 'NR33 7TH', null, '01502 654321', null,
        '07777777777', null, 'reynard.fox@warwick.ac.uk', 'C', null, null, 'N', null, 'C', '1', null, null, null, null, null, 'V', null, 'SITS');
Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '1234567', '0003', '1234567', 'REFU', null, 'SIR JOHN LEMAN HIGH SCHOOL', 'RINGSFIELD ROAD', 'BECCLES', 'SUFFOLK', 'NR34 9PG', null, null,
        '01502-123456', null, null, '01502-123456', 'RNFX@SJLHS.SUFFOLK.SCH.UK', 'C', null, null, 'N', null, 'C', '123456', 'Miss K Bill',
        'Deputy Director of KS5', null, null, null, null, 'HERCULES', 'HERCULES');

Insert into MEN_ADD (ADD_AENT, ADD_ADID, ADD_SEQN, ADD_MST1, ADD_ATYC, ADD_RADC, ADD_ADD1, ADD_ADD2, ADD_ADD3, ADD_ADD4, ADD_ADD5, ADD_PCOD, ADD_MSRT, ADD_TELN,
                     ADD_TEL2, ADD_TEL3, ADD_FAXN, ADD_EMAD, ADD_DETS, ADD_BEGD, ADD_ENDD, ADD_TEMP, ADD_TTAC, ADD_ACTV, ADD_FLAG, ADD_NAM1, ADD_NAM2, ADD_CODC,
                     ADD_CNTY, ADD_UPDD, ADD_STAT, ADD_MST2, ADD_USRC)
values ('STU', '7654321', '0031', '7654321', 'CORR', null, '21 Spencer Ave', 'Earlsdon', null, 'Coventry', null, 'CV5 6BS', null, null, null, null, null,
        'canard.duck@warwick.ac.uk', 'C', null, null, 'N', 'C', 'C', '1', null, null, null, null, null, 'V', 'ACDJBS', 'ACDJBS');
