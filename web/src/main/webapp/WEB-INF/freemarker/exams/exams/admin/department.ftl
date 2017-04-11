<#escape x as x?html>
<#import "modules/admin_components.ftl" as components />

<#if department??>

<#assign can_manage_dept=can.do("Department.ManageExtensionSettings", department) />
<#assign expand_by_default = (!can_manage_dept && modules?size lte 5) />

<div class="btn-toolbar dept-toolbar">
	<#if !modules?has_content && department.children?has_content>
	<div class="use-tooltip btn btn-default disabled" title="This department doesn't directly contain any modules. Check subdepartments.">
		<a class="btn-default disabled">
			Manage
		</a>
	</div>
	<#else>
		<div class="btn-group">
			<a class="btn btn-default dropdown-toggle" data-toggle="dropdown" href="#">
				Manage
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<li>
					<#assign settings_url><@routes.exams.departmentDisplaySettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageDisplaySettings'
						scope=department
						action_descr='manage department settings'
						href=settings_url
					>
						Department settings
					</@fmt.permission_button>
				</li>
				<li>
					<#assign settings_url><@routes.exams.departmentNotificationSettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageNotificationSettings'
						scope=department
						action_descr='manage department notification settings'
						href=settings_url
					>
						Notification settings
					</@fmt.permission_button>
				</li>
				<#if features.markingWorkflows>
					<li class="divider"></li>
					<li>
						<#assign markingflow_url><@routes.exams.markingWorkflowList department /></#assign>
						<@fmt.permission_button permission='MarkingWorkflow.Read' scope=department action_descr='manage marking workflows' href=markingflow_url>
							Marking workflows
						</@fmt.permission_button>
					</li>
				</#if>
			</ul>
		</div>
	</#if>

	<#if modules?has_content && !expand_by_default>
	<div class="btn-group dept-show">
		<a class="btn btn-default use-tooltip" href="#" data-container="body" title="Modules with no exams are hidden. Click to show all modules." data-title-show="Modules with no exams are hidden. Click to show all modules." data-title-hide="Modules with no exams are shown. Click to hide them">
			Show
		</a>
	</div>
	</#if>
</div>


<#function route_function dept>
	<#local result><@routes.exams.departmentHomeWithYear dept academicYear /></#local>
	<#return result />
</#function>

<@fmt.id7_deptheader title=department.name route_function=route_function />

<#if !modules?has_content>
	<#if department.children?has_content>
		<p class="alert-danger">This department doesn't directly contain any modules. Check subdepartments.</p>
	<#else>
		<p class="alert-danger">This department doesn't contain any modules.</p>
	</#if>
</#if>

<div id="feedback-report-modal" class="modal fade"></div>

<script type="text/javascript">
	<#-- Immediately start waiting for collapsibles to load - don't wait to wire this handler in, because we initialise collapsibles before the DOM has loaded below -->
	jQuery(document.body).on('loaded.collapsible', '.module-info', function() {
		var $module = jQuery(this);
		Exams.zebraStripeExams($module);
		$module.find('.use-tooltip').tooltip();
	});
</script>

<#list modules as module>
	<@components.admin_section module=module expand_by_default=expand_by_default />

	<#if !expand_by_default>
		<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
		<script type="text/javascript">
			GlobalScripts.initCollapsible(jQuery('#module-${module.code}').filter(':not(.empty)'));
		</script>
	</#if>
</#list>

<#else>
	<p>No department.</p>
</#if>
</#escape>
