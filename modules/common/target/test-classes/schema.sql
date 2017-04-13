drop table auditevent if exists;
drop sequence auditevent_seq if exists;
create table auditevent (
	id integer,
	eventid varchar(36),
	eventdate timestamp,
	eventType varchar(255) not null,
	eventStage varchar(64) not null,
	real_user_id varchar(255),
	masquerade_user_id varchar(255),
	data varchar(4000) not null
);
create sequence auditevent_seq increment by 1 minvalue 1 maxvalue 999999999 start with 1;

SET DATABASE TRANSACTION CONTROL MVCC;
SET DATABASE REFERENTIAL INTEGRITY FALSE;