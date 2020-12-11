<#ftl strip_text=true />
<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the routes.groups object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose routes.groups directly.

-->
<#macro _u page context='/groups'><@url context=context page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro departmenthome department year=""><#compress>
  <#local p>/admin/department/${department.code}/<#if year?has_content>${year.startYear?c}/</#if></#local>
  <@_u page=p />
</#compress></#macro>
<#macro modulehome module><@_u page="/admin/module/${module.code}/" /></#macro>
<#macro depthome module academicYear=""><#compress>
  <#local p>/admin/department/${module.adminDepartment.code}/<#if academicYear?has_content>${academicYear.startYear?c}/</#if>?moduleFilters=Module(${module.code})</#local>
  <@_u page=p />
</#compress></#macro>
<#macro single_set_info set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/" /></#macro>

<#macro tutor academicYear><@_u page="/tutor/${academicYear.startYear?c}" /></#macro>

<#macro moduleperms module><@_u page="/module/${module.code}/permissions" context="/admin" /></#macro>

<#macro displaysettings department><@_u page="/department/${department.code}/settings/display" context="/admin" /></#macro>
<#macro notificationsettings department><@_u page="/department/${department.code}/settings/notification" context="/admin" /></#macro>
<#macro batchnotify department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/groups/release"  /></#macro>
<#macro batchopen department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/groups/selfsignup/open"  /></#macro>
<#macro batchclose department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/groups/selfsignup/close"  /></#macro>

<#macro createset module><@_u page="/admin/module/${module.code}/groups/new" /></#macro>
<#macro editset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/edit" /></#macro>
<#macro deleteset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/delete" /></#macro>
<#macro archiveset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/archive" /></#macro>
<#macro releaseset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/release" /></#macro>
<#macro enrolment set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/enrolment"/></#macro>
<#macro openset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/selfsignup/open" /></#macro>
<#macro closeset set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/selfsignup/close" /></#macro>

<#macro createeditproperties set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}" /></#macro>
<#macro createsetstudents set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/students" /></#macro>
<#macro createsetgroups set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/groups" /></#macro>
<#macro createsetevents set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/events" /></#macro>
<#macro createsetallocate set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/allocate" /></#macro>
<#macro editsetproperties set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}" /></#macro>
<#macro editsetstudents set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/students" /></#macro>
<#macro editsetgroups set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/groups" /></#macro>
<#macro editsetevents set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/events" /></#macro>
<#macro editsetallocate set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/allocate" /></#macro>
<#macro templatespreadsheet set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/allocate/template" /></#macro>

<#macro createseteventsnewevent group><@_u page="/admin/module/${group.groupSet.module.code}/groups/new/${group.groupSet.id}/events/${group.id}/new" /></#macro>
<#macro createseteventseditevent event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/new/${event.group.groupSet.id}/events/${event.group.id}/edit/${event.id}" /></#macro>
<#macro editseteventsnewevent group><@_u page="/admin/module/${group.groupSet.module.code}/groups/edit/${group.groupSet.id}/events/${group.id}/new" /></#macro>
<#macro editseteventseditevent event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/edit/${event.group.groupSet.id}/events/${event.group.id}/edit/${event.id}" /></#macro>

<#macro createseteventscopyevent event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/new/${event.group.groupSet.id}/events/${event.id}/copy" /></#macro>
<#macro editseteventscopyevent event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/edit/${event.group.groupSet.id}/events/${event.id}/copy" /></#macro>

<#macro createseteventdefaults set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/events/defaults" /></#macro>
<#macro editseteventdefaults set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/events/defaults" /></#macro>

<#macro createimportfromexternal set><@_u page="/admin/module/${set.module.code}/groups/new/${set.id}/events/import" /></#macro>
<#macro editimportfromexternal set><@_u page="/admin/module/${set.module.code}/groups/edit/${set.id}/events/import" /></#macro>

<#macro createseteventsediteventimport event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/new/${event.group.groupSet.id}/events/${event.group.id}/edit/${event.id}/import" /></#macro>
<#macro editseteventsediteventimport event><@_u page="/admin/module/${event.group.groupSet.module.code}/groups/new/${event.group.groupSet.id}/events/${event.group.id}/edit/${event.id}/import" /></#macro>

<#macro registerForWeek event week><@_u page="/event/${event.id}/register?week=${week?c}" /></#macro>
<#macro registerPdf event><@_u page="/event/${event.id}/register.pdf" /></#macro>
<#macro addAdditionalStudent event week><@_u page="/event/${event.id}/register/additional?week=${week?c}" /></#macro>
<#macro groupAttendance group><@_u page="/group/${group.id}/attendance" /></#macro>
<#macro setAttendance set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/attendance" /></#macro>
<#macro moduleAttendance module><@_u page="/admin/module/${module.code}/attendance" /></#macro>
<#macro moduleAttendanceInYear module academicYear><@_u page="/admin/module/${module.code}/${academicYear.startYear?c}/attendance" /></#macro>
<#macro departmentAttendance department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/attendance" /></#macro>
<#macro printRegisters department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/registers" /></#macro>
<#macro viewNote student occurrence returnTo=""><@_u page="/note/${student.universityId}/${occurrence.id}/?returnTo=${returnTo}" /></#macro>
<#macro editNote student occurrence returnTo="">
  <#local returnTo><#if returnTo?has_content>?returnTo=${returnTo}</#if></#local>
  <@_u page="/note/${student.universityId}/${occurrence.id}/edit${returnTo}" />
</#macro>
<#macro bulkEditNote occurrence><@_u page="/note/bulk/${occurrence.id}/edit" /></#macro>

<#macro signup_to_group set><@_u page="/module/${set.module.code}/groups/${set.id}/signup" /></#macro>
<#macro leave_group set><@_u page="/module/${set.module.code}/groups/${set.id}/leave" /></#macro>
<#macro signup_to_group_timetableclash_info set><@_u page="/module/${set.module.code}/groups/${set.id}/signuptimetableclashinfo" /></#macro>

<#macro timetableclashstudentslist set><@_u page="/${set.id}/timetableclashstudentspopup?" /></#macro>
<#macro studenttimetableconflict set><@_u page="/module/${set.module.code}/groups/${set.id}/timetableconflict" /></#macro>

<#macro photo profile><#if ((profile.universityId)!)?has_content><@_u page="/view/photo/${profile.universityId}.jpg" context="/profiles" /><#else><@_u resource="/static/images/no-photo.jpg" /></#if></#macro>
<#macro relationshipPhoto profile relationship><@_u page="/view/photo/${profile.universityId}/${relationship.relationshipType.urlPart}/${relationship.agent}.jpg" context="/profiles" /></#macro>

<#macro studentslist group><@_u page="/group/${group.id}/studentspopup" /></#macro>
<#macro studentsinsetlist groupset><@_u page="/${groupset.id}/studentspopup" /></#macro>
<#macro unallocatedstudentslist groupset><@_u page="/${groupset.id}/unallocatedstudentspopup" /></#macro>

<#macro crossmodulegroups department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/groups/reusable"  /></#macro>
<#macro createcrossmodulegroups department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/groups/reusable/new"  /></#macro>
<#macro createcrossmodulegroupsstudents set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/new/${set.id}/students"  /></#macro>
<#macro createcrossmodulegroupsgroups set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/new/${set.id}/groups"  /></#macro>
<#macro createcrossmodulegroupsallocate set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/new/${set.id}/allocate"  /></#macro>
<#macro editcrossmodulegroups set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/edit/${set.id}"  /></#macro>
<#macro editcrossmodulegroupsstudents set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/edit/${set.id}/students"  /></#macro>
<#macro editcrossmodulegroupsgroups set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/edit/${set.id}/groups"  /></#macro>
<#macro editcrossmodulegroupsallocate set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/edit/${set.id}/allocate"  /></#macro>
<#macro deletecrossmodulegroups set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/delete/${set.id}"  /></#macro>
<#macro crossmodulegroupstemplate set><@_u page="/admin/department/${set.department.code}/${set.academicYear.startYear?c}/groups/reusable/${set.id}/template"  /></#macro>

<#macro students_json set><@_u page="/module/${set.module.code}/groups/${set.id}/students/search.json" /></#macro>

<#macro import_groups department><@_u page="/admin/department/${department.code}/import-groups" /></#macro>
<#macro import_groups_for_year department academicYear><@import_groups department />?academicYear=${academicYear.startYear?c}</#macro>

<#macro import_spreadsheet department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/import-spreadsheet" /></#macro>
<#macro import_spreadsheet_template department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear?c}/import-spreadsheet/template" /></#macro>

<#macro permissions scope><@_u page="/permissions/${scope.urlCategory}/${scope.urlSlug}" context="/admin" /></#macro>

<#macro deregisteredStudents set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/deregistered" /></#macro>
<#macro withdrawnStudents set><@_u page="/admin/module/${set.module.code}/groups/${set.id}/withdrawn" /></#macro>
<#macro courseworkDeptHome module><@url context="/coursework" page="/admin/department/${module.adminDepartment.code}/#module-${module.code}" /></#macro>

<#macro copyDepartment department><@_u page="/admin/department/${department.code}/groups/copy" /></#macro>
<#macro copyModule module><@_u page="/admin/module/${module.code}/groups/copy" /></#macro>

<#macro missing_map_locations department academicYear><@_u page="/admin/department/${department.code}/${academicYear.startYear}/missing-map-locations" /></#macro>
