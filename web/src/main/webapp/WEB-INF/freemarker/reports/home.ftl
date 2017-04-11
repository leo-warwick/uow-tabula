<#escape x as x?html>

<p>
	This is a service for viewing reports for various aspects of Tabula.
</p>

<#if departmentsWithPermission?size == 0 && user.staff>
	<p>
		You do not currently have permission to view any reports. Please contact your
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

<#if departmentsWithPermission?has_content>
	<h2>View reports</h2>
	<ul>
		<#list departmentsWithPermission as department>
			<#list academicYears as year>
				<li><h3><a href="<@routes.reports.departmentWithYear department year />">${department.name} ${year.toString}</a></h3></li>
			</#list>
		</#list>
	</ul>
</#if>

</#escape>