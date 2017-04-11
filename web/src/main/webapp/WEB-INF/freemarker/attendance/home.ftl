<#escape x as x?html>

<p class="lead muted">
	This is a service for viewing and managing attendance monitoring points.
</p>

<#if hasProfile>
	<h2>
		<a href="<@routes.attendance.profileHome />">My attendance profile</a>
	</h2>
</#if>

<#if hasAnyRelationships>
	<h2>My students</h2>

	<ul>
		<#list relationshipTypesMap?keys as relationshipType>
			<#if relationshipTypesMapById[relationshipType.id]>
				<li><h3><a id="relationship-${relationshipType.urlPart}" href="<@routes.attendance.agentHomeForYear relationshipType academicYear />">${relationshipType.studentRole?cap_first}s</a></h3></li>
			</#if>
		</#list>
	</ul>
</#if>

<#assign can_record = (viewPermissions?size > 0) />
<#assign can_manage = (managePermissions?size > 0) />

<#if can_record || can_manage>
	<#if can_record>
		<h2>View and record monitoring points</h2>
		<ul class="unstyled">
			<#list viewPermissions as department>
				<li>
					<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department academicYear />">${department.name}</a></h3>
				</li>
			</#list>
		</ul>
	</#if>

	<#if can_manage>
		<h2>Create and edit monitoring schemes</h2>
		<ul class="unstyled">
			<#list managePermissions as department>
				<li>
					<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department academicYear />">${department.name}</a></h3>
				</li>
			</#list>
		</ul>
		<#if showIntro("no-route-departments-intro", "anywhere")>
			<#assign introText>
			<p>
				You can now view or manage monitoring points for any students who are enrolled in your department,
				but whose route is owned by a different department.
				This means that you will no longer see separate links to monitor students in departments
				where you have been granted permissions on a specific route.
			</p>
			</#assign>
			<a href="#"
			   id="no-route-departments-intro"
			   class="use-introductory<#if showIntro("no-route-departments-intro", "anywhere")> auto</#if>"
			   data-hash="${introHash("no-route-departments-intro", "anywhere")}"
			   data-placement="bottom"
			   data-html="true"
			   data-content="${introText}"><i class="fa fa-question-circle"></i></a>
		</#if>
	</#if>
<#else>
	<#if user.staff && !hasAnyRelationships>
		<p>
			You do not currently have permission to view or manage any monitoring points. Please contact your
			departmental access manager for Tabula, or email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
		</p>

		<script type="text/javascript">
			jQuery(function($) {
				$('#email-support-link').on('click', function(e) {
					e.stopPropagation();
					e.preventDefault();
					$('#app-feedback-link').click();
				});
			});
		</script>
	</#if>
</#if>

</#escape>