-- TAB-6551

create index idx_smallgroupeventattendance_replaces on smallgroupeventattendance (replaces_attendance_id);

-- TAB-6552

alter table recipientnotificationinfo add constraint recipientnotificationinfo_pk primary key (id);