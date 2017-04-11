<#escape x as x?html>
<div class="fix-area">

	<h1 class="with-settings">Students in ${department.name} with no ${relationshipType.agentRole}</h1>

	<form action="<@routes.profiles.relationship_allocate department relationshipType />" method="post">

		<#if studentCount gt 0>
			<#if missingStudents?has_content>
				<table class="related_students table table-striped table-condensed">
					<thead>
						<tr>
							<th><@bs3form.selector_check_all /></th>
							<th class="student-col">First name</th>
							<th class="student-col">Last name</th>
							<th class="id-col">ID</th>
							<th class="type-col">Type</th>
							<th class="year-col">Year</th>
							<th class="course-col">Course</th>
						</tr>
					</thead>

					<tbody>
						<#list missingStudents as student>
							<tr class="student">
								<td><@bs3form.selector_check_row name="preselectStudents" value="${student.universityId}" /></td>
								<td><h6>${student.firstName}</h6></td>
								<td><h6>${student.lastName}</h6></td>
								<td><a class="profile-link" href="<@routes.profiles.profile student />">${student.universityId}</a></td>
								<td>${student.groupName!""}</td>
								<td>
									${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
								</td>
								<td>
									${(student.mostSignificantCourseDetails.currentRoute.name)!}
								</td>
							</tr>
						</#list>
					</tbody>
				</table>

				<div class="fix-footer">
					<#if features.personalTutorAssignment && !relationshipType.readOnly(department)>
						<button type="submit" class="btn btn-primary">
							Allocate ${relationshipType.description}s
						</button>
					</#if>
					<@fmt.bulk_email_students students=missingStudents subject="${relationshipType.agentRole?cap_first}" />
				</div>
			<#else>
				<p class="alert alert-info">All students in ${department.name} have ${relationshipType.agentRole}s recorded.</p>
			</#if>
		<#else>
			<p class="alert alert-info">No students are currently visible for ${department.name} in Tabula.</p>
		</#if>

	</form>
</div>

<script type="text/javascript">
(function($) {
	$(function() {
		$('.fix-area').fixHeaderFooter();
		$('.related_students').tablesorter({
			sortList: [[2,0], [1,0], [3,0]],
			headers: { 0: { sorter: false} }
		}).bigList();
	});
})(jQuery);
</script>
</#escape>