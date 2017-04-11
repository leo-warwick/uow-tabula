<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>

<p class="lead muted">
	This is a service for generating exam board grids.
</p>

<#if departmentsWithPermission?size == 0 && user.staff>
	<p>
		You do not currently have permission to generate exam board grids. Please contact your
		departmental access manager for Tabula, or email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
	</p>
<#elseif featureFilteredDepartments?size == 0 && user.staff>
	<p>
		Exam board grids are not enabled for your department. Please email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a> for more information.
	</p>
</#if>

<#if featureFilteredDepartments?has_content>
	<h2>Manage Exam Grids</h2>
	<ul>
		<#list featureFilteredDepartments as department>
			<li><h3><a href="<@routes.exams.gridsDepartmentHomeForYear department academicYear />">${department.name}</a></h3></li>
		</#list>
	</ul>
</#if>

<script type="text/javascript">
	jQuery(function($) {
		$('#email-support-link').on('click', function(e) {
			e.stopPropagation();
			e.preventDefault();
			$('#app-feedback-link').click();
		});
	});
</script>

</#escape>