<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">

<#assign group = event.group />
<#assign set = group.groupSet />

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		body {
			font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
		}

		h1 {
			font-size: 20px;
			margin: 0 0 10px 0;
		}

		h2 {
			font-size: 18px;
			margin: 0;
		}

		#header {
			border-bottom: 1px solid #888;
		}

		.sgt-event-details {
			display: block;
			margin-top: -3px;
		}

		.student {
			position: relative;
			display: block;
			border-top: 1px solid #ddd;
			border-bottom: 1px solid #ddd;
			<#if showPhotos>
				padding-top: 10px;
				padding-bottom: 5px;
				min-height: 40px;
			<#else>
				padding-top: 7px;
				padding-bottom: 5px;
			</#if>
			page-break-inside: avoid;
		}

		.student .photo { display: inline-block; width: 10%; vertical-align: middle; }
		.student .name { display: inline-block; width: 50%; vertical-align: middle; }
		.student .name.no-photo { width: 60%; }
		.student .check-box, .student .signature-line { display: inline-block; vertical-align: middle; text-align: right; width: 35%; }

		.student .name { padding: 0 10px; font-size: 16px; }
		.student .name .muted { font-size: 80%; color: #666; }

		.student .check-box input[type="checkbox"] { width: 30px; height: 30px; }
		.student .signature-line { position: absolute; bottom: 0px; right: 0px; }
		.student .signature-line hr { color: #888; }
	</style>
	<title>Register</title>
</head>
<body>
	<div id="header">
		<img src="/static/images/logo-full-black.png" style="width: 30%; float: right;" />
		<h1><#if event.title?has_content>${event.title}, </#if>${set.module.code?upper_case} ${set.nameWithoutModulePrefix}, ${group.name}</h1>
		<h2 class="sgt-event-details">
			${event.day.name} <@fmt.time event.startTime /> - <@fmt.time event.endTime />, <@fmt.singleWeekFormat week=week academicYear=set.academicYear dept=set.module.adminDepartment /> (${formattedEventDate})
			<#if event.tutors.users?has_content>
				<br /><@fmt.p number=event.tutors.users?size singular="Tutor" shownumber=false/>: <#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
			</#if>
		</h2>

		<br style="clear: both;" />
	</div>

	<#if !members?has_content>
		<p><em>There are no students allocated to this group.</em></p>
	<#else>
		<#macro studentRow student>
			<div class="student<#if displayCheck == "line"> with-signature</#if><#if !showPhotos> no-photo</#if>">
				<#if showPhotos>
					<div class="photo">
						<img data-universityid="${student.universityId}" style="width: 100%;" />
					</div>
				</#if>

				<div class="name<#if !showPhotos> no-photo</#if>">
					<#if displayName == "both">
						${student.fullName}<br /><span class="muted">${student.universityId}</span>
					<#elseif displayName == "id">
						${student.universityId}
					<#else>
						${student.fullName}
					</#if>
				</div>

				<#if displayCheck == "line">
					<div class="signature-line"><hr /></div>
				<#else>
					<div class="check-box"><input type="checkbox" /></div>
				</#if>
			</div>
		</#macro>

		<#list members as student>
			<@studentRow student />
		</#list>
	</#if>
</body>
</html>
</#escape>