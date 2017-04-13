<#assign tiles=JspTaglibs["/WEB-INF/tld/tiles-jsp.tld"]>
<!doctype html>
<html lang="en-GB">
	<head>
		<#include "_head.ftl" />
	</head>
	<body class="horizontal-nav layout-100 tabula-page ${component.bodyClass?default('component-page')} ${bodyClasses?default('')}">
		<div id="container">
			<!--[if IE]>
				<div id="ie8-notice">
					Your web browser is unsupported, please upgrade.
					<a href="http://warwick.ac.uk/tabula/faqs/browser-support" class="btn btn-mini btn-info">
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
						<button class="btn btn-mini btn-info"><i class="icon-eye-close fa fa-eye-slash"></i> Disable God mode</button>
					</@f.form>
				</div>
			</#if>
			<#if (info.hasEmergencyMessage)!false>
				<div id="emergency-message" class="sysadmin-only-content">${info.emergencyMessage}</div>
			</#if>
			<#if (user.masquerading)!false>
				<div id="masquerade-notice" class="sysadmin-only-content">
					Masquerading as <strong>${user.apparentUser.fullName}</strong>. <a href="<@url page="/masquerade?returnTo=${info.requestedUri}" context="/admin"/>">Change</a>
				</div>
			</#if>
			<#if isProxying!false && proxyingAs??>
				<div id="proxy-notice" class="sysadmin-only-content">
					Proxying as <strong>${proxyingAs.fullName}</strong>.
				</div>
			</#if>
			<#-- Change this to header-medium or header-large as necessary - large is for homepages only -->
			<div id="header" class="<#if jumbotron?? && jumbotron>header-medium<#else>header-small</#if>" data-type="image">
				<div id="masthead">
					<div class="access-info">
						<a href="#main-content" accesskey="c" title="Skip to content [c]">Skip to content</a>
						<a href="#navigation" accesskey="n" title="Skip to navigation [n]">Skip to navigation</a>
					</div>

					<#-- The on-hover class here specifies that the links should only be displayed on hover -->
					<div id="warwick-logo-container" class="on-hover">
						<a id="warwick-logo-link" href="http://www.warwick.ac.uk" title="University of Warwick homepage">
							<img id="warwick-logo" src="<@url resource="/static/images/logo.png" />" alt="University of Warwick">
						</a>
					</div>

					<div id="utility-container">
						<div id="utility-bar">
							<ul>
								<#if IS_SSO_PROTECTED!true>
									<li>
										<#if user?? && user.loggedIn>
											Signed in as ${user.fullName}
											| <a href="/settings">Settings</a>
											| <a href="http://warwick.ac.uk/tabula/manual/" target="_blank">Manual</a>
											| <a href="http://warwick.ac.uk/tabula/whatsnew/" target="_blank">What's new?</a>
											| <a class="sso-link" href="<@sso.logoutlink target="${component.rootUrl?default(rootUrl)}" />">Sign out</a>
										<#else>
											<a class="sso-link" href="<@sso.loginlink />">Sign in</a>
										</#if>
									</li>
								</#if>
							</ul>
						</div>
					</div>
				</div>

				<div id="page-header">
					<div class="content">
						<div id="site-header-container">
							<h1 id="site-header">
							<#if component.subsite>
								<span id="parent-site-header">
									<a href="/" title="Tabula home page">Tabula</a>
								</span>
								<span id="subsite-character">&raquo;</span>
							</#if>
								<span id="current-site-header"><#compress>
									<#if component.homeUrl??>
										<#assign homeUrl=component.homeUrl />
									<#else>
										<#assign homeUrl><@routes.home /></#assign>
									</#if>
									<#if (info.requestedUri != homeUrl)!false>
										<a href="${homeUrl}">${component.siteHeader?default('Tabula')}</a>
									<#else>
										<span>${component.siteHeader?default('Tabula')}</span>
									</#if>
								</#compress></span>
								<#if !(jumbotron!false)><span class="more-link"><i class="icon-caret-down fa fa-caret-down"></i></span></#if>
							</h1>

							<h2 id="strapline">
								<#if jumbotron!false>
									Student management and administration system
								</#if>
							</h2>
						</div>

						<div id="custom-header">
							<div>
							<#if (info.maintenance)!false>
								<span id="maintenance-mode-label" class="label label-warning" rel="popover" title="System read-only" data-placement="left" data-content="This system has been placed in a read-only mode. You will be able to download files, but other operations are not currently possible. Normal access will be restored very soon.">Read-only</span>
								<script>
									jQuery(function($){
										$('#maintenance-mode-label').popover();
									});
								</script>
							</#if>

							<#if (activeSpringProfiles!"") == "sandbox">
								<span id="sandbox-label" class="label label-warning" rel="popover" title="Tabula Sandbox" data-placement="left" data-content="This instance of Tabula is a sandbox instance, and doesn't use any real data."><i class="icon-sun fa fa-sun-o"></i> Sandbox</span>
								<script>
									jQuery(function($){
										$('#sandbox-label').popover();
									});
								</script>
							</#if>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div id="navigation-and-content">
				<#if !component.nonav>
					<div id="navigation-wrapper">
						<div id="navigation" class="horizontal">
							<div id="primary-navigation-wrapper">
								<div id="before-primary-navigation"></div>

								<div id="primary-navigation-container" >
									<ul id="primary-navigation">
										<li class="section rendered-link">
											<div class="link-content">
												<div class="title rendered-link-content">
													<#if component.homeUrl??>
														<#assign homeUrl=component.homeUrl />
													<#else>
														<#assign homeUrl><@routes.home /></#assign>
													</#if>
													<#if (info.requestedUri != homeUrl)!false>
														<a href="${homeUrl}">${component.title?default('Tabula')}</a>
													<#else>
														<span>${component.title?default('Tabula')}</span>
													</#if>
												</div>
											</div>
										</li>
										<#if breadcrumbs??><#list breadcrumbs as crumb>
											<#if crumb.linked!false>
												<li class="section rendered-link">
													<div class="link-content">
														<div class="title rendered-link-content">
															<a href="<@url page=crumb.url />" <#if crumb.tooltip??>title="${crumb.tooltip}"</#if>>${crumb.title}</a>
														</div>
													</div>
												</li>
											</#if>
										</#list></#if>
									</ul>
								</div>

								<div id="after-primary-navigation"></div>
							</div>
						</div>
					</div>
				</#if>

				<div id="content-wrapper">
					<div id="main-content">
						<#--
						<div id="page-title">

							<h1>
								<span id="after-page-title"></span>
							</h1>

							<div id="page-title-bottom"></div>
						</div>
						-->

						<#-- column-1 and column-2 may not stick around as IDs - don't use them in a site design -->
						<div id="column-1"><div id="column-1-content">

						<@tiles.insertAttribute name="body" />

						</div></div>

						<div style="clear:both;"></div>
					</div>
				</div>

				<div style="clear:both;"></div>
			</div>

			<div style="clear:both;"></div>

			<div id="footer">
				<div class="content">
					<div id="custom-footer">
						<#-- Enter any custom footer content here (like contact details et al) -->
					</div>

					<div style="clear:both;"></div>

					<div id="common-footer">
						<div id="page-footer-elements" class="nofollow">
							<span class="footer-left"></span> <span class="footer-right"></span>

							<div style="clear:both;"></div>
						</div>

						<div id="footer-utility">
							<ul>
								<#if IS_SSO_PROTECTED!true>
									<li id="sign-inout-link">
										<#if user?? && user.loggedIn>
											<a class="sso-link" href="<@sso.logoutlink target="${component.rootUrl?default(rootUrl)}" />">Sign out</a>
										<#else>
											<a class="sso-link" href="<@sso.loginlink />">Sign in</a>
										</#if>
									</li>
			          				<li class="spacer">|</li>
								</#if>
			          			<li id="copyright-link"><a href="http://go.warwick.ac.uk/terms" title="Copyright Statement">&copy; <@warwick.copyright /></a></li>
			          			<li class="spacer">|</li>
			          			<li id="privacy-link"><a href="http://go.warwick.ac.uk/terms#privacy" title="Privacy statement">Privacy</a></li>
			          			<li class="spacer">|</li>
			          			<li id="accessibility-link"><a href="http://go.warwick.ac.uk/accessibility" title="Accessibility information [0]" accesskey="0">Accessibility</a></li>
			          			<li class="spacer subtle">|</li>
			          			<li id="faqs-link"><a href="http://warwick.ac.uk/tabula/manual/" target="_blank">Manual</a></li>
			          			<li class="spacer subtle">|</li>
								<li id="whatsnew-link"><a href="http://warwick.ac.uk/tabula/whatsnew/" target="_blank">What's new?</a></li>
								<li class="spacer subtle">|</li>
			          			<li class="subtle">
			          				App last built <@warwick.formatDate value=appBuildDate pattern="d MMMM yyyy HH:mm" />
			          			</li>
		          			</ul>

		          			<a id="app-feedback-link" href="/comments<#if info??>?currentPage=${info.requestedUri}</#if>"><span>Give feedback</span></a>

									<#if user?? && user.sysadmin>
										<div id="sysadmin-link">
											<div class="btn-group">
												<a id="sysadmin-button" class="btn btn-inverse dropdown-toggle dropup" data-toggle="dropdown" href="<@url page="/sysadmin/" context="/" />"><i class="icon-cog fa fa-cog icon-white fa fa-white"></i> System <span class="caret"></span></a>
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
											  jQuery('#sysadmin-link').fadeOut('slow')
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
		          		</div>
					</div>
				</div>
			</div>
		</div>

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
