<#import "*/group_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/modal_macros.ftl" as modal />

<@modal.wrapper cssClass="modal-lg">
<@modal.header>
	<#if command.group??>
		<h3 class="modal-title">Students in ${command.group.name}</h3>
	<#else>
		<h3 class="modal-title">Students <#if unallocated!false>not </#if>in ${command.smallGroupSet.name}</h3>
	</#if>

</@modal.header>

<@modal.body>

	<#if command.group??>
		<ul>
		<#list command.group.events as event>
		<#assign tutorUsers=event.tutors.users />
		<li>
			<div>
				<@components.event_schedule_info event />
			</div>

			<#if studentsCanSeeTutorName>
			<div>
				<@fmt.p number=tutorUsers?size singular="Tutor" shownumber=false />:
				<#if !tutorUsers?has_content>
					<em>None</em>
				</#if>
				<#list tutorUsers as tutorUser>
					${tutorUser.fullName}<#if tutorUser_has_next>,</#if>
				</#list>
			</div>
			</#if>

			<div>
				<@fmt.p number=students?size singular="student" plural="students" /><#if userIsMember> including you</#if>.
			</div>
		</li>
		</#list>
		</ul>
	</#if>

	<#if students?has_content>
	<ul class="profile-user-list">
	<#list students as student>
	<#-- If the current user is a member no need to show them their own photo -->
	<#if student.universityId != userUniId!"">
		<li>
			<div class="profile clearfix">
				<#if student.isMember()>
					<@fmt.member_photo student "tinythumbnail" false />
				<#else>
					<#-- `student` is actually only backed by a User here, so no Member photo, so let's explicitly serve the default photo -->
					<@fmt.member_photo {} "tinythumbnail" false />
				</#if>

				<div class="name">
					<h6>${student.fullName} <@pl.profile_link student.universityId /></h6>
					<#if student.isMember()>
						${(student.asMember.mostSignificantCourseDetails.currentRoute.code?upper_case)!""} ${(student.asMember.mostSignificantCourseDetails.currentRoute.name)!""}<br />
					</#if>
					${student.shortDepartment!""}
				</div>
			</div>
		</li>
	</#if>
	</#list>
	</ul>

	<#--
		FIXME This doesn't really make sense. If I can take a register for a small group,
		I can email all the members in it? This is mostly to avoid showing the link to students
	-->
	<#if command.group?? && can.do("SmallGroupEvents.ViewRegister", command.group)>
		<p>
			<@fmt.bulk_email_students students=students />
		</p>
	</#if>

	<#else>

	<p>No students have been allocated to this group.</p>

	</#if>

</@modal.body>
</@modal.wrapper>