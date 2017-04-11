<#function sanitise text>
	<#return text?lower_case?replace("[^a-z0-9]", "", "r") />
</#function>

<#escape x as x?html>
<div class="pull-right">
	<#if features.personalTutorAssignment && !relationshipType.readOnly(department)>
		<a href="<@routes.profiles.relationship_allocate department relationshipType />" class="btn btn-default pull-right">
			Allocate ${relationshipType.description}s
		</a>
	</#if>
</div>

<h1 class="with-settings">${relationshipType.description}s for ${department.name}</h1>

<#if agentRelationships?has_content || (missingCount > 0)>
	<#if agentRelationships?has_content>
		<div id="agents">
			<#list agentRelationships?keys as key>
				<#assign students = mapGet(agentRelationships,key) />
				<#assign agent = students?first.agent /><#-- always a string. -->
				<#assign agentMember = students?first.agentMember />
				<#assign studentKey = "rel-" + sanitise(key.sortkey) + "-students" />

				<div class="striped-section collapsible">
					<h4 class="section-title" id="${studentKey}-title" title="Expand">
						<span class="very-subtle pull-right"><@fmt.p students?size "${relationshipType.studentRole}" /></span>
						<#if agentMember??>
							${agentMember.fullName}
							<#if ((agentMember.homeDepartment.code)!'') != department.code>
								<span class="very-subtle">${(agentMember.homeDepartment.name)!''}</span>
							</#if>
							<#assign agentId = agentMember.universityId />
						<#else>
							${agent}
							<#if !agent?starts_with("Not ")>
								<span class="very-subtle">External to Warwick</span>
							</#if>
							<#assign agentId = agent />
						</#if>
					</h4>

					<div id="${studentKey}" class="striped-section-contents">
						<div class="item-info">
							<div class="clearfix">
								<div class="pull-right">
									<@fmt.bulk_email_student_relationships relationships=students subject="${relationshipType.agentRole?cap_first}" />
								</div>
							</div>
							<form class="" action="<@routes.profiles.relationship_reallocate department relationshipType agentId />" method="post">
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
										<#list students as studentRelationship>
											<#assign studentCourseDetails = studentRelationship.studentCourseDetails />
											<tr class="student">
												<#assign readOnly=(studentCourseDetails.department.code!=department.code) />
												<td>
													<#if readOnly>
														<#assign studentDepartment=studentCourseDetails.department />
														<div class="use-tooltip" data-html="true" data-container ="body" data-title= "This student can be reallocated from their profile page or from within the ${studentDepartment.name} department.">
													</#if>
													<@bs3form.selector_check_row
														name="preselectStudents"
														value="${studentCourseDetails.student.universityId}"
														readOnly=readOnly
													/>
													<#if readOnly></div></#if>
												</td>
												<td><h6>${studentCourseDetails.student.firstName}</h6></td>
												<td><h6>${studentCourseDetails.student.lastName}</h6></td>
												<td><a class="profile-link" href="/profiles/view/course/${studentCourseDetails.urlSafeId}">${studentCourseDetails.student.universityId}</a></td>
												<td>${studentCourseDetails.student.groupName!""}</td>
												<td>${(mapGet(yearOfStudyMap, studentCourseDetails))!""}</td>
												<td>${(mapGet(courseMap, studentCourseDetails).name)!""}</td>
											</tr>
										</#list>
									</tbody>
								</table>

								<p>
									<#if canReallocateStudents><button type="submit" class="btn btn-primary reallocate">Reallocate students</button></#if>
								</p>
							</form>
						</div>
					</div>
				</div>
			</#list>
		</div>
	<#else>
		<p class="alert alert-info">No ${relationshipType.agentRole}s are currently visible for ${department.name} in Tabula.</p>
	</#if>

	<#if missingCount == 0>
		<h4 class="subtle">All students in ${department.name} have ${relationshipType.agentRole}s recorded</h4>
	<#else>
		<h4><a href="<@routes.profiles.relationship_missing department relationshipType />">View <@fmt.p missingCount "student" /> with no ${relationshipType.agentRole}</a></h4>
	</#if>

	<#if scheduledCount == 0>
		<h4 class="subtle">No scheduled ${relationshipType.agentRole} changes</h4>
	<#else>
		<h4><a href="<@routes.profiles.relationship_scheduled department relationshipType />">View ${scheduledCount} scheduled ${relationshipType.agentRole} <@fmt.p number=scheduledCount singular="change" shownumber=false/></a></h4>
	</#if>
<#else>
	<p class="alert alert-info">No students are currently visible for ${department.name} in Tabula.</p>
</#if>

<script type="text/javascript">
	(function($) {
		$(function() {
			$('.related_students').tablesorter({
				sortList: [[2,0], [1,0], [3,0]],
				headers: { 0: { sorter: false} }
			}).bigList();
			$('div.striped-section').on('click', function(e){
				var $currentExpandedDiv =  $(this);
				var departmentalStudentsExist = !!$currentExpandedDiv.find('.collection-checkbox').length;
				if (!departmentalStudentsExist) {
				    var $reallocateButton = $currentExpandedDiv.find('button.reallocate');
					if(!$reallocateButton.hasClass('disabled')){
						$reallocateButton.addClass('disabled');
					}
					var $checkAll = $currentExpandedDiv.find('.collection-check-all');
						$checkAll.attr('disabled', true);
				}
			});
		});
	})(jQuery);
</script>
<style>
	div.tooltip-inner {
		max-width: 350px;
	}
</style>
</#escape>