<#assign tiles=JspTaglibs["/WEB-INF/tld/tiles-jsp.tld"]>
<#import "*/modal_macros.ftl" as modal />
<!DOCTYPE html>
<html lang="en-GB" class="no-js">
<head>
	<#include "_head.ftl" />
</head>
<body class="tabula-page ${component.bodyClass?default('component-page')} ${bodyClasses?default('')}">
<div class="id7-left-border"></div>
<div class="id7-fixed-width-container">
	<a class="sr-only sr-only-focusable" href="#main">Skip to main content</a>

	<header class="id7-page-header">

		<!--[if IE]>
		<div id="ie8-notice">
			Your web browser is unsupported, please upgrade.
			<a href="http://warwick.ac.uk/tabula/faqs/browser-support" class="btn btn-mini btn-primary">
				More information
			</a>
		</div>
		<![endif]-->

		<#if (user.god)!false>
			<div id="god-notice" class="sysadmin-only-content">
				God mode enabled.
				<@f.form id="godModeForm" method="post" action="${url('/sysadmin/god')}">
					<input type="hidden" name="returnTo" value="${info.requestedUri!""}" />
					<input type="hidden" name="action" value="remove" />
					<button class="btn btn-xs btn-info">Disable God mode</button>
				</@f.form>
			</div>
		</#if>
		<#if (info.hasEmergencyMessage)!false>
			<div id="emergency-message" class="sysadmin-only-content">${info.emergencyMessage}</div>
		</#if>
		<#if (user.masquerading)!false>
			<div id="masquerade-notice" class="sysadmin-only-content">
				Masquerading as <strong>${user.apparentUser.fullName}</strong> (${user.apparentUser.userId}<#if user.apparentUser.warwickId?has_content>, ${user.apparentUser.warwickId}</#if>). <a href="<@url page="/masquerade?returnTo=${info.requestedUri}" context="/admin"/>">Change</a>
			</div>
		</#if>
		<#if isProxying!false && proxyingAs??>
			<div id="proxy-notice" class="sysadmin-only-content">
				Proxying as <strong>${proxyingAs.fullName}</strong>.
			</div>
		</#if>

		<div class="id7-utility-masthead">
			<nav class="id7-utility-bar">
				<ul>
					<#if IS_SSO_PROTECTED!true>
						<#if user?? && user.loggedIn>
							<li><a href="http://warwick.ac.uk/tabula/manual/" target="_blank">Manual</a></li>
							<li><a href="http://warwick.ac.uk/tabula/whatsnew/" target="_blank">What's new?</a></li>
							<li>
								<a class="sso-link sign-out" href="<@sso.logoutlink target="${component.rootUrl!rootUrl}" />" data-toggle="id7:account-popover" data-loginlink="<@sso.loginlink />" data-name="${user.fullName}">Sign out</a>
							</li>
						<#else>
							<li>
								<a class="sso-link sign-in" href="<@sso.loginlink />">Sign in</a>
							</li>
						</#if>
					</#if>
				</ul>
			</nav>

			<div class="id7-masthead">
				<div class="id7-masthead-contents">
					<div class="clearfix">
						<div class="id7-logo-column">
							<!-- Don't include the logo row on non-branded sites -->
							<div class="id7-logo-row">
								<div class="id7-logo">
									<a href="http://warwick.ac.uk" title="Warwick homepage">
										<img src="<@url resource="/static/images/shim.gif" />" alt="Warwick">
									</a>
								</div>
								<nav class="id7-site-links">
									<ul>
										<li><a href="http://warwick.ac.uk/study">Study</a></li>
										<li><a href="http://warwick.ac.uk/research">Research</a></li>
										<li><a href="http://warwick.ac.uk/business">Business</a></li>
										<li><a href="http://warwick.ac.uk/alumni">Alumni</a></li>
										<li><a href="http://warwick.ac.uk/news">News</a></li>
										<li><a href="http://warwick.ac.uk/global">Global</a></li>
									</ul>
								</nav>
							</div>
						</div>
					</div>

					<div class="id7-header-text clearfix">
						<div class="pull-right btn-toolbar hidden-xs" style="margin-top: 12px; margin-left: 12px;">
							<#if user?? && user.loggedIn>
								<a class="btn btn-brand btn-sm" href="/settings">Tabula settings</a>
							</#if>
							<a class="btn btn-brand btn-sm" href="/comments<#if info??>?currentPage=${info.requestedUri}</#if>">Problems, questions?</a>
						</div>
						<#if (info.maintenance)!false || (activeSpringProfiles!"") == "sandbox">
							<div class="pull-right" style="line-height: 54px;">
								<#if (info.maintenance)!false>
									<span id="maintenance-mode-label" class="label label-warning" rel="popover" title="System read-only" data-placement="bottom" data-container="body" data-content="This system has been placed in a read-only mode. You will be able to download files, but other operations are not currently possible. Normal access will be restored very soon.">Read-only</span>
									<script>
										jQuery(function($){
											$('#maintenance-mode-label').popover();
										});
									</script>
								</#if>

								<#if (activeSpringProfiles!"") == "sandbox">
									<span id="sandbox-label" class="label label-warning" rel="popover" title="Tabula Sandbox" data-placement="bottom" data-container="body" data-content="This instance of Tabula is a sandbox instance, and doesn't use any real data."><i class="fa fa-sun-o"></i> Sandbox</span>
									<script>
										jQuery(function($){
											$('#sandbox-label').popover();
										});
									</script>
								</#if>
							</div>
						</#if>

						<h1>
							<span class="id7-current-site-link"><a href="/">Tabula</a></span>
						</h1>
					</div>
				</div>
			</div>
		</div>

		<div class="id7-navigation">
			<#assign navigation><#compress>
				<#if userNavigation?has_content>
					${(userNavigation.collapsed)!""}
				<#else>
					${(user.navigation.collapsed)!""}
				</#if>
			</#compress></#assign>
			<#assign navigation><#compress>
				<#if breadcrumbs?has_content>
					${navigation?replace("${component.name!''}-active", "${component.name!''}-active active next-secondary")}
				<#else>
					${navigation?replace("${component.name!''}-active", "${component.name!''}-active active")}
				</#if>
			</#compress></#assign>
			<#if navigation?has_content>
				<nav class="navbar navbar-primary hidden-xs" role="navigation">
					${navigation}
				</nav>
			</#if>
			<#if breadcrumbs?has_content>
				<nav class="navbar navbar-secondary <#if siblingBreadcrumbs!false>sibling-breadcrumbs</#if>" role="navigation">
					<ul class="nav navbar-nav">
						<li class="nav-breadcrumb"><a href="/${component.name}">${component.title}</a></li>
						<#list breadcrumbs as crumb>
							<#if crumb.linked!false>
								<li class="nav-breadcrumb <#if crumb.active!false>active</#if>">
									<a href="<@url page=crumb.url />" <#if crumb.tooltip??>title="${crumb.tooltip}"</#if>>${crumb.title}</a>
								</li>
							</#if>
						</#list>
					</ul>
				</nav>
			</#if>
			<#if secondBreadcrumbs?has_content>
				<nav class="navbar navbar-tertiary" role="navigation">
					<ul class="nav navbar-nav">
						<#list secondBreadcrumbs as crumb>
							<li <#if crumb.active>class="active"</#if>>
								<a
									<#if crumb.active> <#-- can't click active tertiary nav as already on that page -->
										data-page-url="<@url page=crumb.url!"" />"
									<#else>
										href="<@url page=crumb.url!"" />"
									</#if>
									<#if crumb.tooltip??>title="${crumb.tooltip}"</#if>
								>
									${crumb.title}
								</a>
							</li>
						</#list>
					</ul>
				</nav>
			</#if>
		</div>
	</header>

	<!-- Page content of course! -->
	<main class="id7-main-content-area" id="main">
		<header class="id7-main-content-header">
			<div class="id7-horizontal-divider">
				<svg xmlns="http://www.w3.org/2000/svg" x="0" y="0" version="1.1" width="1130" height="40" viewBox="0, 0, 1130, 41">
					<path d="m 0,0.5 1030.48, 0 22.8,40 16.96,-31.4 16.96,31.4 22.8,-40 20,0" class="divider" stroke="#383838" fill="none" />
				</svg>
			</div>
		</header>

		<div class="id7-main-content">
			<@tiles.insertAttribute name="body" />
		</div>
	</main>

	<footer class="id7-page-footer id7-footer-coloured"> <!-- one of id7-footer-coloured or id7-footer-divider -->
		<div class="id7-site-footer">
			<!-- Only included when footer class="id7-footer-divider"
			  <div class="id7-horizontal-divider">
			  <svg xmlns="http://www.w3.org/2000/svg" x="0" y="0" version="1.1" width="1130" height="40" viewBox="0, 0, 1130, 41">
				<path d="m 0,0.5 1030.48, 0 22.8,40 16.96,-31.4 16.96,31.4 22.8,-40 20,0" class="divider" stroke="#383838" fill="none" />
			  </svg>
			  </div>
			-->
		</div>
		<div class="id7-app-footer">
			<!-- Only include the id7-logo-bleed for footer class="id7-footer-coloured" -->
			<div class="id7-logo-bleed"></div>

			<div class="id7-footer-utility">
				<ul>
					<li><a href="http://warwick.ac.uk/copyright">© <@warwick.copyright /></a></li>
					<li><a href="http://warwick.ac.uk/terms">Terms</a></li>
					<li><a href="http://warwick.ac.uk/privacy">Privacy</a></li>
					<li><a href="http://warwick.ac.uk/cookies">Cookies</a></li>
					<li><a href="http://warwick.ac.uk/accessibility">Accessibility</a></li>
					<#if user?? && user.sysadmin>
						<li class="subtle">
							App last built <@warwick.formatDate value=appBuildDate pattern="d MMMM yyyy HH:mm" />
						</li>
					</#if>
				</ul>
			</div>
		</div>

		<div class="modal fade" id="app-comment-modal">
			<@modal.wrapper>
				<@modal.body></@modal.body>
			</@modal.wrapper>
		</div>

		<#if user?? && user.sysadmin>
			<div id="sysadmin-link">
				<div class="btn-group">
					<a id="sysadmin-button" class="btn btn-default dropdown-toggle dropup" data-toggle="dropdown" href="<@url page="/sysadmin/" context="/" />"><i class="icon-cog fa fa-cog icon-white fa fa-white"></i> System <span class="caret"></span></a>
					<ul class="dropdown-menu pull-right">
						<#if user.sysadmin>
							<li><a href="<@url page="/sysadmin/" context="/" />">Sysadmin home</a></li>
						</#if>
						<#if user.sysadmin>
							<li><a href="<@url page="/masquerade?returnTo=${info.requestedUri}" context="/admin" />">Masquerade</a></li>
						</#if>
						<#if user.sysadmin>
							<li><a href="#" id="hide-sysadmin-only-content">Hide sysadmin content</a></li>
						</#if>
					</ul>
				</div>
			</div>
			<script type="text/javascript">
				jQuery('#hide-sysadmin-only-content').on('click', function(){
					jQuery('#sysadmin-link').fadeOut('slow');
					jQuery('.sysadmin-only-content').hide('slow');
					return false;
				});
			</script>
		<#elseif user?? && user.masquerader>
			<div id="sysadmin-link">
				<a id="sysadmin-button" class="btn btn-inverse" href="<@url page="/masquerade" context="/admin" />?returnTo=${(info.requestedUri!"")?url}"><i class="icon-user fa fa-user icon-white fa fa-white"></i> Masquerade</a>
			</div>
		</#if>

		<div style="clear:both;"></div>
		<div class="cog"></div>
	</footer>
</div>
<div class="id7-right-border"></div>
<#if googleAnalyticsCode?has_content>
	<script type="text/javascript">
		var _gaq = _gaq || [];
		_gaq.push(['_setAccount', '${googleAnalyticsCode}']);
		_gaq.push(['_trackPageview']);

		(function() {
			var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
			ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
			var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
		})();
	</script>
</#if>
</body>
</html>