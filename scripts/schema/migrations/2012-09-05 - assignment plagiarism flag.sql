-- HFC-236
ALTER TABLE SUBMISSION
ADD (
	suspectPlagiarised NUMBER(1,0) DEFAULT 0
);