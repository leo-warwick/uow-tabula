<#escape x as x?html>

	<#if user.loggedIn && user.firstName??>
		<h5>Hello, ${user.firstName}</h5>
	</#if>

	<#if !user.loggedIn>
		<#if IS_SSO_PROTECTED!true>
		<p>
			You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
			to see a personalised view.
		</p>
		</#if>
	<#else>
		<#noescape>${userNavigation.expanded}</#noescape>

		<#if features.activityStreams>
			<#import "*/activity_macros.ftl" as activity />
		<div class="home-page-activity">
			<h3>Activity stream</h3>
			<@activity.activity_stream max=5 />
		</div>
		</#if>
	</#if>

	<#if (activeSpringProfiles!"") == "sandbox">
		<div class="alert alert-block">
			<h4><i class="fa fa-sun-o"></i> Tabula Sandbox</h4>

			<p>This instance of Tabula is a sandbox, used for testing Tabula's features and functionality without affecting
				any real data.</p>

			<p>There are some important differences in this version of Tabula:</p>

			<ul>
				<li>Features and functionality may be enabled that are not enabled on the live system
				<li>No staff data exists on the system at all
				<li>Student data is automatically generated, using fake names
				<li>No emails will be sent by the system, so feel free to play around without worrying
			</ul>

			<p>Please make sure you do not upload any sensitive data to this system, such as anything relating to real students.</p>

		<p>To get access to administration on this system, please send an email to <a href="mailto:tabula@warwick.ac.uk?subject=Sandbox access">tabula@warwick.ac.uk</a></p>

	</div>
	</#if>

</#escape>