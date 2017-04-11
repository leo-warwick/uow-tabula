<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro pagination currentPage totalResults resultsPerPage>
	<#local totalPages = (totalResults / resultsPerPage)?ceiling />
	<nav class="pull-right">
		<ul class="pagination pagination-sm">
			<#if currentPage lte 1>
				<li class="disabled"><span>&laquo;</span></li>
			<#else>
				<li><a data-page="${currentPage - 1}">&laquo;</a></li>
			</#if>

			<#list 1..totalPages as page>
				<#if page == currentPage>
					<li class="active"><span>${page}</span></li>
				<#else>
					<li><a data-page="${page}">${page}</a></li>
				</#if>
			</#list>

			<#if currentPage gte totalPages>
				<li class="disabled"><span>&raquo;</span></li>
			<#else>
				<li><a data-page="${currentPage + 1}">&raquo;</a></li>
			</#if>
		</ul>
	</nav>
</#macro>

<@spring.bind path="persistanceCommand.staticStudentIds">
	<#if status.error>
		<div class="alert alert-danger"><@f.errors path="persistanceCommand.staticStudentIds" cssClass="error"/></div>
	</#if>
</@spring.bind>

<#if summaryString?has_content>
	<p><#noescape>${summaryString}</#noescape></p>
</#if>

<div class="striped-section collapsible" data-populate=".striped-section-contents .item-info" data-form="form.add-student-to-scheme" data-href="<@routes.attendance.manageAddStudentsAllStudents scheme />">
	<h3 class="section-title">View all students on this scheme</h3>
	<div class="striped-section-contents">
		<div class="item-info">

		</div>
	</div>
</div>

<div class="find-students striped-section collapsible <#if expandFind>expanded</#if>" data-submitparam="${ManageSchemeMappingParameters.findStudents}">
	<h3 class="section-title">
		Find students
		<span class="very-subtle">Select students by route, year of study etc.</span>
	</h3>
	<div class="striped-section-contents">
		<div class="item-info">
			<p>Use the filters to add students to this scheme:</p>

			<@f.hidden path="findCommand.page" />
			<@f.hidden path="findCommand.sortOrder" />

			<div class="student-filter btn-group-group well well-sm">

				<button type="button" class="clear-all-filters btn btn-link">
				<span class="fa-stack">
					<i class="fa fa-filter fa-stack-1x"></i>
					<i class="fa fa-ban fa-stack-2x"></i>
				</span>
				</button>

				<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="">
					<@spring.bind path=path>
						<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
							<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
								<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
								<span class="caret"></span>
							</a>
							<div class="dropdown-menu filter-list">
								<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
								<ul>
									<#if customPicker?has_content>
										<li>
											<#noescape>${customPicker}</#noescape>
										</li>
									</#if>
									<#if allItems?has_content>
										<#list allItems as item>
											<#local isValid = (allItems?size == validItems?size)!true />
											<#if !isValid>
												<#list validItems as validItem>
													<#if ((validItem.id)!0) == ((item.id)!0)>
														<#local isValid = true />
													</#if>
												</#list>
											</#if>
											<li class="checkbox check-list-item" data-natural-sort="${item_index}">
												<label class="checkbox <#if !isValid>disabled</#if>"><#compress><#nested item isValid/></#compress></label>
											</li>
										</#list>
									<#else>
										<li><small class="very-subtle" style="padding-left: 5px;">N/A for this department</small></li>
									</#if>
								</ul>
							</div>
						</div>
					</@spring.bind>
				</#macro>

				<#macro current_filter_value path placeholder><#compress>
					<@spring.bind path=path>
						<#if status.actualValue?has_content>
							<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
						<#else>
							${placeholder}
						</#if>
					</@spring.bind>
				</#compress></#macro>

				<#function contains_by_code collection item>
					<#list collection as c>
						<#if c.code == item.code>
							<#return true />
						</#if>
					</#list>
					<#return false />
				</#function>

				<#assign placeholder = "All course types" />
				<#assign currentfilter><@current_filter_value "findCommand.courseTypes" placeholder; courseType>${courseType.code}</@current_filter_value></#assign>
				<@filter "findCommand.courseTypes" placeholder currentfilter findCommand.allCourseTypes; courseType>
					<input type="checkbox" name="${status.expression}" value="${courseType.code}" data-short-value="${courseType.code}" ${contains_by_code(findCommand.courseTypes, courseType)?string('checked','')}>
					${courseType.description}
				</@filter>

				<#-- Non-standard drop-down for routes -->
				<#assign placeholder = "All routes" />
				<#assign currentFilter><@current_filter_value "findCommand.routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
				<#assign routesCustomPicker>
					<div class="route-search input-group">
						<input class="route-search-query route prevent-reload form-control" type="text" value="" placeholder="Search for a route" />
						<span class="input-group-addon"><i class="fa fa-search"></i></span>
					</div>
				</#assign>
				<@spring.bind path="findCommand.routes">
					<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
						<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">
							<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="">${currentFilter}</span>
							<span class="caret"></span>
						</a>
						<div class="dropdown-menu filter-list">
							<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
							<ul>
								<li>
									<#noescape>${routesCustomPicker}</#noescape>
								</li>
								<#macro routeLi route route_index>
									<li class="checkbox check-list-item" data-natural-sort="${route_index}">
										<label class="checkbox"><#compress>
											<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(findCommand.routes, route)?string('checked','')}>
											<@fmt.route_name route false />
										</#compress></label>
									</li>
								</#macro>
								<#list findCommand.outOfDepartmentRoutes as route>
									<@routeLi route route_index />
								</#list>
								<#if findCommand.allRoutes?has_content>
									<#list findCommand.allRoutes as route>
										<@routeLi route route_index />
									</#list>
								<#else>
									<li><small class="muted" style="padding-left: 5px;">N/A for this department</small></li>
								</#if>
							</ul>
						</div>
					</div>
				</@spring.bind>

				<#assign placeholder = "All attendance" />
				<#assign currentfilter><@current_filter_value "findCommand.modesOfAttendance" placeholder; moa>${moa.shortName?capitalize}</@current_filter_value></#assign>
				<@filter "findCommand.modesOfAttendance" placeholder currentfilter findCommand.allModesOfAttendance; moa>
					<input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}"
					${contains_by_code(findCommand.modesOfAttendance, moa)?string('checked','')}>
					${moa.fullName}
				</@filter>

				<#assign placeholder = "All years" />
				<#assign currentfilter><@current_filter_value "findCommand.yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
				<@filter "findCommand.yearsOfStudy" placeholder currentfilter findCommand.allYearsOfStudy findCommand.allYearsOfStudy "Year "; yearOfStudy>
					<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
					${findCommand.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
					${yearOfStudy}
				</@filter>

				<#assign placeholder = "All statuses" />
				<#assign currentfilter><@current_filter_value "findCommand.sprStatuses" placeholder; sprStatus>${sprStatus.shortName?capitalize}</@current_filter_value></#assign>
				<@filter "findCommand.sprStatuses" placeholder currentfilter findCommand.allSprStatuses; sprStatus>
					<input type="checkbox" name="${status.expression}" value="${sprStatus.code}" data-short-value="${sprStatus.shortName?capitalize}" ${contains_by_code(findCommand.sprStatuses, sprStatus)?string('checked','')}>
					${sprStatus.fullName}
				</@filter>

				<#assign placeholder = "All modules" />
				<#assign modulesCustomPicker>
					<div class="module-search input-group">
						<input class="module-search-query module prevent-reload form-control" type="text" value="" placeholder="Search for a module" />
						<span class="input-group-addon"><i class="fa fa-search"></i></span>
					</div>
				</#assign>
				<#assign currentfilter><@current_filter_value "findCommand.modules" placeholder; module>${module.code?upper_case}</@current_filter_value></#assign>
				<@filter path="findCommand.modules" placeholder=placeholder currentFilter=currentfilter allItems=findCommand.allModules customPicker=modulesCustomPicker; module>
					<input type="checkbox" name="${status.expression}"
						   value="${module.code}"
						   data-short-value="${module.code?upper_case}"
					${contains_by_code(findCommand.modules, module)?string('checked','')}>
					<@fmt.module_name module false />
				</@filter>

				<@f.hidden path="findCommand.filterChanged" class="student-filter-change" />

				<div class="btn-group">
					<button class="btn btn-xs btn-primary search" type="submit" name="${ManageSchemeMappingParameters.findStudents}" value="true">Find</button>
				</div>

			</div>

			<#if findCommand.filterChanged>
				<div class="well well-small student-filter-changed-box">
					<span class="legend">Filter Update</span>
					<div>
						You have changed the filters for this search, to keep these results for the next time you sign in you must select Save at the bottom of the page. If you do not select Save the last filter will still apply next time you sign in.
					</div>
				</div>
			</#if>

			<#if findCommand.staticStudentIds?has_content>
				<p>To remove the filter, click the button below. This will remove all students currently linked from this filter.</p>
				<p><button class="btn btn-xs btn-default" type="submit" name="${ManageSchemeMappingParameters.resetFilter}" value="true">Remove filter</button></p>
			</#if>

			<#if (findCommandResult.membershipItems?size > 0)>
				<div class="pull-right">
					<@pagination
						currentPage=findCommand.page
						resultsPerPage=findCommand.studentsPerPage
						totalResults=findCommand.totalResults
					/>
				</div>

				<#assign startIndex = ((findCommand.page - 1) * findCommand.studentsPerPage) />
				<#assign endIndex = startIndex + findCommandResult.membershipItems?size />
				<p>
					Results ${startIndex + 1} - ${endIndex} of ${findCommand.totalResults}
				</p>

				<@attendance_macros.manageStudentTable
					membershipItems=findCommandResult.membershipItems
					doSorting=true
					command=findCommand
					checkboxName="excludeIds"
					onlyShowCheckboxForStatic=true
					showRemoveButton=true
				/>
			<#elseif (findCommand.findStudents?has_content)>
				<p style="padding-bottom: 20px;">No students were found.</p>
			</#if>
		</div>
	</div>
</div>

<div class="manually-added striped-section collapsible <#if expandManual>expanded</#if>">
	<h3 class="section-title">
		Manually add students
		<span class="very-subtle">Add a list of students by university ID or username</span>
	</h3>
	<div class="striped-section-contents">
		<div class="item-info">
			<p style="margin-bottom: 1em;">
				<input style="margin-right: 8px;" class="btn btn-default" type="submit" name="${ManageSchemeMappingParameters.manuallyAddForm}" value="Add students manually" />
				<@fmt.p editMembershipCommandResult.includedStudentIds?size "student" />
				added manually and
				<@fmt.p editMembershipCommandResult.excludedStudentIds?size "student" />
				removed manually
			</p>

			<#if (addUsersResult.missingMembers?size > 0 || addUsersResult.noPermissionMembers?size > 0)>
				<div class="alert alert-danger">
					<#if (addUsersResult.missingMembers?size > 0)>
						The following students could not be added as they were not found:
						<ul>
							<#list addUsersResult.missingMembers as member>
								<li>${member}</li>
							</#list>
						</ul>
					</#if>
					<#if (addUsersResult.noPermissionMembers?size > 0)>
						The following students could not be added as you do not have permission to manage their attendance:
						<ul>
							<#list addUsersResult.noPermissionMembers as member>
								<li>${member.fullName} (${member.universityId})</li>
							</#list>
						</ul>
					</#if>
				</div>
			</#if>

			<@attendance_macros.manageStudentTable
				membershipItems=editMembershipCommandResult.membershipItems
				checkboxName="resetStudentIds"
				checkAll=true
				showResetButton=true
			/>
		</div>
	</div>
</div>

<input type="hidden" name="returnTo" value="${returnTo}">
<@f.hidden path="findCommand.doFind" />

</#escape>