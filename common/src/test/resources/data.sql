insert into usergroup (id, universityids)
values ('1', false);
insert into usergroupinclude (group_id, usercode)
values ('1', 'cusebr');
insert into usergroupinclude (group_id, usercode)
values ('1', 'cusfal');

insert into department (id, code, name)
values ('1', 'cs', 'Computer Science');
insert into grantedrole (id, hib_version, usergroup_id, builtInRoleDefinition, scope_type, scope_id)
values ('1', 0, '1', 'DepartmentalAdministratorRoleDefinition', 'Department', '1');

insert into department (id, code, name)
values ('2', 'ch', 'Chemistry');

insert into department (id, code, name, parent_id)
values ('3', 'cs-subsidiary', 'Computer Science Subsidiary', 1);
insert into department (id, code, name, parent_id)
values ('4', 'cs-subsidiary-2', 'Computer Science Subsidiary 2', 1);


insert into module (id, department_id, code, name, active)
values ('1', '1', 'cs108', 'Introduction to Programming', true);
insert into module (id, department_id, code, name, active)
values ('2', '1', 'cs240', 'History of Computing', true);
insert into module (id, department_id, code, name, active)
values ('3', '3', 'cs241', 'Mystery of Computing', true);
insert into module (id, department_id, code, name, active)
values ('4', '4', 'cs242', 'More Computing', true);

insert into route (id, department_id, code, name, active, degreeType, teachingDepartmentsActive)
values ('1', '1', 'g500', 'BSc Computer Science', true, 'UG', false);
insert into route (id, department_id, code, name, active, degreeType, teachingDepartmentsActive)
values ('2', '1', 'g503', 'MEng Computer Science', true, 'UG', false);
insert into route (id, department_id, code, name, active, degreeType, teachingDepartmentsActive)
values ('3', '3', 'g900', 'Robotics', true, 'PG', false);
insert into route (id, department_id, code, name, active, degreeType, teachingDepartmentsActive)
values ('4', '4', 'g901', 'AI', true, 'PG', false);

-- set up an assignment for the "Intro to Programming" module
insert into assignment(id, name, module_id, academicyear, attachmentlimit,
                       collectmarks, deleted, collectsubmissions, restrictsubmissions,
                       allowlatesubmissions, allowresubmission, displayplagiarismnotice, createdDate)
values ('1', 'Test Assignment', '1', '2011', 1, true, true, true, true, true, true, true, current_timestamp at time zone 'Europe/London');

-- set up an assignment for the "Intro to Programming" module
insert into assignment(id, name, module_id, academicyear, attachmentlimit,
                       collectmarks, deleted, collectsubmissions, restrictsubmissions,
                       allowlatesubmissions, allowresubmission, displayplagiarismnotice, createdDate)
values ('2', 'Test Computing Assignment', '1', '2011', 1, true, true, true, true, true, true, true, current_timestamp at time zone 'Europe/London');

-- set up an assignment for the "History of Computing" module
insert into assignment(id, name, module_id, academicyear, attachmentlimit,
                       collectmarks, deleted, collectsubmissions, restrictsubmissions,
                       allowlatesubmissions, allowresubmission, displayplagiarismnotice, createdDate)
values ('3', 'Programming Assignment', '2', '2011', 1, true, true, true, true, true, true, true, current_timestamp at time zone 'Europe/London');
