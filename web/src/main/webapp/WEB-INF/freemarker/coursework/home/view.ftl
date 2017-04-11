<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@routes.coursework.departmenthome department />">
		Go to the ${department.name} admin page
	</a>
</#macro>

<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>

<#if !user.loggedIn>
	<p class="lead muted">
		This is a service for managing coursework assignments and feedback
	</p>

	<#if IS_SSO_PROTECTED!true>
		<p class="alert">
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
	</#if>
</#if>

<#if user.loggedIn>

	<#assign isSelf = true />
	<#include "_student.ftl" />
	<#include "_markers.ftl" />
	<#include "_admin.ftl" />
	<#include "_marks_management_admin.ftl" />

	<#assign is_student=user.student /> <#-- Non-students may also have assignments, but we still show them the intro text -->
	<#assign is_alumni=user.alumni />
	<#assign is_marker=nonempty(assignmentsForMarking) />
	<#assign is_admin=(nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)) />

	<#if !is_alumni && !is_student && !is_marker && !is_admin> <#-- Don't just show an empty page -->
		<p class="lead muted">
			This is a service for managing coursework assignments and feedback
		</p>

		<p>
			<#if userHomeDepartment?has_content>
				<#assign uams = usersWithRole('UserAccessMgrRoleDefinition', userHomeDepartment) />
			</#if>
			You do not currently have permission to manage any assignments or feedback. If you believe this is an error then please
			<#if uams?has_content>
				contact your department's <a href="mailto:${uams?first.email}">User Access Manager</a> for Tabula.
			<#else>
				email <a id="email-support-link" href="mailto:tabula@warwick.ac.uk">tabula@warwick.ac.uk</a>.
			</#if>
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