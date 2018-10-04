<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#escape x as x?html>

<#if isMember>
	<@modal.wrapper>
		<@modal.header>
			<h1 class="modal-title"><@fmt.profile_name profile /></h1>
		</@modal.header>
		<@modal.body>
			<@fmt.member_photo profile "smallthumbnail" false />
			<div class="data clearfix">
				<div class="col1">
					<table class="profile-or-course-info">
						<tbody>
						<tr>
							<th>Official name</th>
							<td>${profile.officialName}</td>
						</tr>

							<#if profile.email??>
							<tr>
								<th>Warwick email</th>
								<td><a href="mailto:${profile.email}">${profile.email}</a></td>
							</tr>
							</#if>

							<#if profile.universityId??>
							<tr>
								<th>University ID</th>
								<td>${profile.universityId}</td>
							</tr>
							</#if>

							<#if profile.userId??>
							<tr>
								<th>Username</th>
								<td>${profile.userId}</td>
							</tr>
							</#if>
						</tbody>
					</table>
				</div>
			</div>

			<!-- basic course details -->
			<div class="data clearfix">
				<div class="col1 basic-course-details">
					<table class="profile-or-course-info subset-course">
						<tbody>
							<#if (studentCourseDetails.currentRoute)??>
							<tr>
								<th>Route</th>
								<td>${(studentCourseDetails.currentRoute.name)!} (${(studentCourseDetails.currentRoute.code?upper_case)!})
								</td>
							</tr>
							</#if>
							<#if (studentCourseDetails.department)??>
							<tr>
								<th>Department</th>
								<td>${(studentCourseDetails.department.name)!} (${((studentCourseDetails.department.code)!)?upper_case})
								</td>
							</tr>
							</#if>
						<tr>
							<th>Status on route</th>
							<td><@fmt.status_on_route studentCourseDetails />
							</td>
						</tr>
						<tr>
							<th>Attendance</th>
							<td>${(studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameAliased)!}
							</td>
						</tr>
						<tr>
							<th>UG/PG</th>
							<td>${(studentCourseDetails.currentRoute.degreeType.toString)!}
							</td>
						</tr>
						<tr>
							<th>Year of study</th>
							<td>${(studentCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
							</td>
						</tr>
						</tbody>
					</table>
				</div>
			</div>

			<div class="link-to-full clearfix">
				<a href="<@routes.profiles.profile profile />" target="_blank">
					View ${profile.fullName}'s full profile<img class="targetBlank" alt="" title="Link opens in a new window" src="/static/images/shim.gif"/>
				</a>
			</div>
		</@modal.body>
	</@modal.wrapper>
<#else>
	<@modal.wrapper>
		<@modal.header>
			<h1 class="modal-title">${studentUser.fullName}</h1>
		</@modal.header>
		<@modal.body>
			<table class="profile-or-course-info">
				<tbody>
					<#if studentUser.email??>
						<tr>
							<th>Email</th>
							<td>
								<a href="mailto:${studentUser.email}">${studentUser.email}</a>
							</td>
						</tr>
					</#if>
					<#if studentUser.warwickId??>
						<tr>
							<th>University ID</th>
							<td>${studentUser.warwickId!}</td>
						</tr>
					</#if>
					<#if studentUser.userId??>
						<tr>
							<th>Username</th>
							<td>${studentUser.userId}</td>
						</tr>
					</#if>
					<#if studentUser.department??>
						<tr>
							<th>Department</th>
							<td>${studentUser.department}</td>
						</tr>
					</#if>
				</tbody>
			</table>
		</@modal.body>
	</@modal.wrapper>
</#if>

</#escape>
