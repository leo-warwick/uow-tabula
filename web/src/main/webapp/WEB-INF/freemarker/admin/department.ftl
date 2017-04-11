<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#function route_anchor route>
	<#return "route-${route.code}" />
</#function>

<#if department??>
	<#assign can_manage_dept=can.do("RolesAndPermissions.Create", department) />

	<div class="btn-toolbar dept-toolbar">
		<div class="btn-group dept-settings">
			<a class="btn btn-default dropdown-toggle" data-toggle="dropdown" href="#">
				Manage
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<li><a href="<@routes.admin.deptperms department/>">
					Edit departmental permissions
				</a></li>

				<li class="divider"></li>

				<#if modules?has_content || departmentRoutes?has_content || !department.children?has_content>
					<li><a href="<@routes.admin.displaysettings department />?returnTo=${(info.requestedUri!"")?url}">
						Department settings
					</a></li>
					<li><a href="<@routes.admin.notificationsettings department />?returnTo=${(info.requestedUri!"")?url}">
						Notification settings
					</a></li>

					<li class="divider"></li>
				</#if>

				<#if can.do("Department.Manage", department)>
					<li><a href="<@routes.admin.createsubdepartment department />">
						Create sub-department
					</a></li>
				</#if>
				<#if can.do("Department.Manage", department)>
					<li><a href="<@routes.admin.editdepartment department />">
						Edit department</a>
					</li>
				</#if>
				<#if can.do("Module.Create", department)>
					<li><a href="<@routes.admin.createmodule department />">
						Create module
					</a></li>
				</#if>
				<#if department.children?has_content && can.do("Department.ArrangeRoutesAndModules", department)>
					<li class="divider"></li>

					<li><a href="<@routes.admin.sortmodules department />">
						Arrange modules
					</a></li>
				</#if>
				<#if department.children?has_content && can.do("Department.ArrangeRoutesAndModules", department)>
					<li><a href="<@routes.admin.sortroutes department />">
						Arrange routes
					</a></li>
				</#if>
			</ul>
		</div>
	</div>

	<#function route_function dept>
		<#local result><@routes.admin.departmenthome dept /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader activeDepartment.name route_function  />

	<div class="row">
		<div class="col-md-6">
			<h3>Modules</h3>

			<#if !modules?has_content>
				<#if department.children?has_content>
					<p class="alert alert-info"><i class="fa fa-info-circle"></i> This department doesn't directly contain any modules. Check subdepartments.</p>
				<#else>
					<p class="alert alert-info"><i class="fa fa-info-circle"></i> This department doesn't contain any modules.</p>
				</#if>
			</#if>

			<#list modules as module>
				<#assign can_manage=can.do("RolesAndPermissions.Create", module) />

				<a id="${module_anchor(module)}"></a>
				<div class="striped-section" data-name="${module_anchor(module)}">
					<div class="clearfix">
						<div class="btn-group section-manage-button">
							<#if can_manage>
								<a class="btn btn-default dropdown-toggle" data-toggle="dropdown"> Manage <span class="caret"></span></a>
								<ul class="dropdown-menu pull-right">
									<li><a href="<@routes.admin.moduleperms module />">
										Edit module permissions
									</a></li>
								</ul>
							<#else>
								<a class="btn btn-default dropdown-toggle disabled" title="You do not have permission to manage this module"> Manage <span class="caret"></span></a>
							</#if>
						</div>

						<h4 class="section-title with-button"><@fmt.module_name module /></h4>
					</div>
				</div>
			</#list>
		</div>
		<div class="col-md-6">
			<h3>Routes</h3>

			<#if !departmentRoutes?has_content>
				<#if department.children?has_content>
					<p class="alert alert-info"><i class="fa fa-info-circle"></i> This department doesn't directly contain any routes. Check subdepartments.</p>
				<#else>
					<p class="alert alert-info"><i class="fa fa-info-circle"></i> This department doesn't contain any routes.</p>
				</#if>
			</#if>

			<#list departmentRoutes as route>
				<#assign can_manage=can.do("RolesAndPermissions.Create", route) />

				<a id="${route_anchor(route)}"></a>
				<div class="striped-section" data-name="${route_anchor(route)}">
					<div class="clearfix">
						<div class="btn-group section-manage-button">
							<#if can_manage>
								<a class="btn btn-default dropdown-toggle" data-toggle="dropdown"> Manage <span class="caret"></span></a>
								<ul class="dropdown-menu pull-right">
									<li><a href="<@routes.admin.routeperms route />">
										Edit route permissions
									</a></li>
								</ul>
							<#else>
								<a class="btn btn-default dropdown-toggle disabled" title="You do not have permission to manage this route"> Manage <span class="caret"></span></a>
							</#if>
						</div>

						<h4 class="section-title with-button"><@fmt.route_name route true /></h4>
					</div>
				</div>
			</#list>
		</div>
	</div>
<#else>
	<p>No department.</p>
</#if>

</#escape>
