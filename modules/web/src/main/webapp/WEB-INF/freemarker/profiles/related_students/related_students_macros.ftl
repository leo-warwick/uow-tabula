<#escape x as x?html>

<#macro row student studentCourseDetails="" showMeetings=false meetingInfoPair="">
	<#if studentCourseDetails?has_content>
		<#assign courseDetails=studentCourseDetails.urlSafeId>
	<#else>
		<#assign courseDetails="">
	</#if>

	<tr class="related_student">
		<td><input class="collection-checkbox" type="checkbox" name="students" data-fullname="${student.fullName}" value="${student.universityId}"  data-student-course-details ="${courseDetails}" /></td>
		<td>
			<@fmt.member_photo student "tinythumbnail" />
		</td>
		<td>${student.firstName}</td>
		<td>${student.lastName}</td>
		<#if studentCourseDetails?has_content>
			<td><a class="profile-link" href="/profiles/view/course/${studentCourseDetails.urlSafeId}">${studentCourseDetails.student.universityId}</a></td>
		<#else>
			<td><a class="profile-link" href="<@routes.profiles.profile student />">${student.universityId}</a></td>
		</#if>
		<td>${student.groupName!""}</td>
		<#if studentCourseDetails?has_content>
			<td>${(studentCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
			<td>${(studentCourseDetails.currentRoute.name)!""}</td>
		<#else>
			<td>${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
			<td>${(student.mostSignificantCourseDetails.currentRoute.name)!""}</td>
		</#if>
		<#if showMeetings>
			<#if meetingInfoPair._1()?has_content>
				<#assign lastMeeting = meetingInfoPair._1() />
				<#assign pendingApprovals = meetingInfoPair._2() />
				<td data-datesort="${(lastMeeting.meetingDate.millis?c)!''}">
					<@fmt.date date=lastMeeting.meetingDate shortMonth=true includeTime=true /><br/>
					<span class="badge progress-bar-<#if (pendingApprovals > 2)>danger<#elseif (pendingApprovals > 0)>warning<#else>success</#if>  use-tooltip" data-original-title="Number of unapproved meetings">${pendingApprovals}</span>
				</td>
			<#else>
				<td></td>
			</#if>
		</#if>
	</tr>
</#macro>

<#macro tableWithMeetingsColumn items meetingsMap>
	<@table items=items showMeetings=true meetingsMap=meetingsMap />
</#macro>
<#-- Print out a table of students/agents.-->
<#macro table items showMeetings=false meetingsMap="">
	<table class="related_students student-list table table-striped table-condensed">
		<thead>
			<tr>
			<tr>
				<th class ="check-col no-sort"><input type="checkbox" class="collection-check-all use-tooltip" title="Select/unselect all"></th>
				<th class="photo-col">Photo</th>
				<th class="student-col">First name</th>
				<th class="student-col">Last name</th>
				<th class="id-col">ID</th>
				<th class="type-col">Type</th>
				<th class="year-col">Year</th>
				<th class="course-but-photo-col">Course</th>
				<#if showMeetings><th class="meetings-col">Last met</th></#if>
			</tr>
		</thead>
		<tbody>
			<#list items as item>
				<#if item.scjCode??>
					<@row item.student item showMeetings=showMeetings meetingsMap[item.student.universityId]/>
				<#else>
					<@row item />
				</#if>

			</#list>
		</tbody>
	</table>

	<#if !student_table_script_included??>
		<script type="text/javascript">

			(function($) {
				$('.student-list').bigList({});
				// add a custom parser for the date column
				$.tablesorter.addParser({
					id: 'customdate',
					is: function(s, table, cell, $cell){return false; /*return false so this parser is not auto detected*/},
					format: function(s, table, cell, cellIndex) {
						var $cell = $(cell);
						return $cell.attr('data-datesort') || s;
					},
					parsed: false,
					type: 'numeric'
				});

				$(function() {
					$('.related_students').tablesorter({
						sortList: [[3,0], [5,0], [6,0]],
						headers: {
							8:{sorter: 'customdate'},
							0:{sorter:false}
						},
						sortForce: [[3,0]]
					});

					$('.related_student').on('mouseover', function(e) {
						$(this).find('td').addClass('hover');
					}).on('mouseout', function(e) {
						$(this).find('td').removeClass('hover');
					}).on('click', function(e) {
						if (!$(e.target).is('a') && !$(e.target).is('img') && !$(e.target).is('input')) {
							window.location = $(this).find('a.profile-link').attr('href');
						}
					});
				});

			})(jQuery);
		</script>
		<#-- deliberately a global assign, so only the first macro includes script. -->
		<#assign student_table_script_included=true />
	</#if>
</#macro>

<#macro myStudents viewerRelationshipTypes smallGroups="">
<section id="relationship-details" class="clearfix" >
	<h4>My students</h4>
	<ul>
		<#list viewerRelationshipTypes as relationshipType>
			<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.profiles.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></h5></li>
		</#list>
		<#if smallGroups?has_content>
			<#list smallGroups as smallGroup>
				<#assign _groupSet=smallGroup.groupSet />
				<#assign _module=smallGroup.groupSet.module />
				<li><a href="<@routes.profiles.smallgroup smallGroup />">
					${_module.code?upper_case} (${_module.name}) ${_groupSet.name}, ${smallGroup.name}
				</a></li>
			</#list>
		</#if>
	</ul>
</section>


</#macro>

</#escape>