<#escape x as x?html>

<#assign showMyStudents = smallGroups?has_content />
<#list relationshipTypesMap?values as has_relationship>
	<#assign showMyStudents = showMyStudents || has_relationship />
</#list>

<#if !user.loggedIn> <#-- Defensive: For if we ever decide not to force login for /profiles/ -->
	<h1>
		Administration
	</h1>

	<#if IS_SSO_PROTECTED!true>
		<p class="alert">
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
	</#if>
<#else>
	<#assign is_staff=searchProfilesCommand?has_content />
	<#assign is_tutor=showMyStudents />
	<#assign is_admin=adminDepartments?has_content />

	<h1>
		Administration
	</h1>

	<div class="row">
		<div class="col-md-<#if is_admin>6<#else>12</#if>">
			<#if is_staff>
				<div class="header" id="search-header">
					<h2 class="section">Search profiles</h2>
				</div>
				<p class="subtler">Type or paste a name or university ID</p>

				<#include "../profile/search/form.ftl" />

				<#if universityId?has_content>
					<h2><a href="<@routes.profiles.profile_by_id universityId />">My staff profile</a></h2>
					<h2><a href="<@routes.profiles.profile_by_id universityId />/timetable">My timetable</a></h2>
				</#if>
			</#if>

			<#if isPGR>
				<#if universityId?has_content>
					<h2><a href="<@routes.profiles.profile_by_id universityId />">My student profile</a></h2>
				</#if>
			</#if>

			<#if showMyStudents>
				<h2>My students</h2>

				<ul>
					<#list relationshipTypesMap?keys as relationshipType>
						<#if relationshipTypesMapById[relationshipType.id]>
							<li><a href="<@routes.profiles.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></li>
						</#if>
					</#list>

					<#list smallGroups as smallGroup>
						<#assign _groupSet=smallGroup.groupSet />
						<#assign _module=smallGroup.groupSet.module />
						<li><a href="<@routes.profiles.smallgroup smallGroup />">
							${_module.code?upper_case} (${_module.name}) ${_groupSet.nameWithoutModulePrefix}, ${smallGroup.name}
						</a></li>
					</#list>
				</ul>
			<#elseif is_staff>
				<h2>My students</h2>

				<p>
					You are not currently the tutor for any group of students in Tabula. If you think this is incorrect, please contact your
					departmental access manager for Tabula, or email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
				</p>
			</#if>

			<#if searchDepartments??>
				<#list searchDepartments as dept>
					<h2>${dept.name}</h2>
					<ul>
						<li><a href="<@routes.profiles.filter_students dept />">Show all students in ${dept.name}</a></li>
						<li><a href="<@routes.profiles.department_timetables dept />">Show all timetables for ${dept.name}</a></li>
					</ul>
				</#list>
			</#if>
		</div>

		<#if adminDepartments?has_content>
			<div id="profile-dept-admin" class="col-md-6">
				<h2>Departmental administration</h2>

				<#list adminDepartments?sort_by("code") as dept>
					<div class="clearfix">
						<div class="btn-group pull-right">
						  <a class="btn btn-sm btn-default dropdown-toggle" data-toggle="dropdown">Manage <span class="caret"></span></a>
						  <ul class="dropdown-menu pull-right">
								<li><a href="<@routes.profiles.deptperms dept/>">
									Edit departmental permissions
								</a></li>

								<li><a href="<@routes.profiles.filter_students dept/>">
									View students
								</a></li>

								<#list dept.displayedStudentRelationshipTypes as relationshipType>
									<li><a href="<@routes.profiles.relationship_agents dept relationshipType />">
										${relationshipType.description}s
									</a></li>
									<li><a href="<@routes.profiles.relationship_missing dept relationshipType />">
										Students with no ${relationshipType.description}
									</a></li>

									<#if features.personalTutorAssignment && !relationshipType.readOnly(dept)>
										<li><a href="<@routes.profiles.relationship_allocate dept relationshipType />">
											Allocate ${relationshipType.description}s</a>
										</li>
									</#if>

									<li><a href="<@routes.profiles.relationship_unconfirmed_meetings dept relationshipType />">
										${relationshipType.description} meetings with no record</a>
									</li>
								</#list>

								<li><a href="<@routes.profiles.displaysettings dept />?returnTo=${(info.requestedUri!"")?url}">
									Settings</a>
								</li>
						  </ul>
						</div>

						<h5 class="with-settings">${dept.name}</h5>
					</div>

					<#if dept_has_next><hr></#if>
				</#list>
			</div>
		</#if>
	</div>
</#if>
</#escape>
