<#escape x as x?html>
	<div id="allocatestudents-tab1" class="tab-pane active fix-area allocate-associations">
		<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="" cssClass="">
			<@spring.bind path=path>
			<div class="btn-group ${cssClass} <#if currentFilter == placeholder> empty-filter</#if>">
				<a class="btn btn-xs btn-default dropdown-toggle" data-toggle="dropdown">
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
								<li class="check-list-item" data-natural-sort="${item_index}">
									<label class="checkbox <#if !isValid>disabled</#if>">
										<#nested item isValid/>
									</label>
								</li>
							</#list>
						<#else>
							<li><p class="very-subtle" style="padding-left: 5px;">N/A for this department</p></li>
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

		<@f.form commandName="command" action="${manualFormAction}" method="POST" cssClass="form-inline fetch">
			<#list command.additions?keys as entity>
				<#list command.additions[entity] as student>
					<input type="hidden" name="additions[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.removals?keys as entity>
				<#list command.removals[entity] as student>
					<input type="hidden" name="removals[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.additionalEntities as entity>
				<input type="hidden" name="additionalEntities" value="${entity}" />
			</#list>
			<div class="row">
				<div class="col-md-6 students">
					<div class="header-with-tooltip">
						<h3>Students</h3>
						<#if command.agentEntityData?has_content>
							<span><@fmt.p unallocated?size "student" /> found for ${command.agentEntityData.displayName}</span>
						<#else>
							<span><@fmt.p unallocated?size "Unallocated student" /> found</span>
						</#if>
					</div>

					<div class="student-filter btn-group-group well well-sm">
						<#assign placeholder = "All routes" />
						<#assign currentfilter><@current_filter_value "routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
						<#assign routesCustomPicker>
							<div class="route-search input-append">
								<input class="route-search-query route prevent-reload" type="text" value="" placeholder="Search for a route" />
								<span class="add-on"><i class="fa fa-search"></i></span>
							</div>
						</#assign>
						<@filter path="routes" placeholder=placeholder currentFilter=currentfilter allItems=command.allRoutes validItems=command.visibleRoutes customPicker=routesCustomPicker cssClass="wide"; route, isValid>
							<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(command.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
							<@fmt.route_name route false />
						</@filter>

						<#assign placeholder = "All years" />
						<#assign currentfilter><@current_filter_value "yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
						<@filter "yearsOfStudy" placeholder currentfilter command.allYearsOfStudy command.allYearsOfStudy "Year " "" "narrow"; yearOfStudy>
							<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
							${command.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
						${yearOfStudy}
						</@filter>

						<button class="btn btn-xs apply" type="submit">Apply</button>

						<br /><br />

						<div class="input-group">
							<input class="form-control" name="query" type="text" placeholder="Search these students" value="${command.query!}"/>
							<span class="input-group-btn">
								<button class="btn btn-default" type="submit"><i class="fa fa-search"></i></button>
							</span>
						</div>
					</div>

					<#assign singleUnallocated = unallocated?has_content && unallocated?size == 1 />
					<#if unallocated?has_content>

						<p>
							<button class="btn btn-default distribute-selected" name="action" value="${commandActions.DistributeSelected}" type="submit">Distribute between selected personal tutors</button>
						</p>
						<p>
							<button class="btn btn-default distribute-all" name="action" value="${commandActions.DistributeAll}" type="submit">Distribute all students between selected personal tutors</button>
						</p>

						<table class="table table-condensed table-striped table-hover scrollable-tbody">
							<thead>
								<tr>
									<th class="check for-check-all"></th>
									<th class="single-name sortable">First name</th>
									<th class="single-name sortable">Last name</th>
									<th class="universityid sortable">ID</th>
								</tr>
							</thead>
							<tbody>
								<#list unallocated as studentData>
									<tr>
										<td class="check">
											<input
												type="checkbox"
												name="allocate"
												value="${studentData.universityId}"
												<#if singleUnallocated || command.preselectStudents?has_content && command.preselectStudents?seq_contains(studentData.universityId)>checked</#if>
											/>
										</td>
										<td class="single-name">${studentData.firstName}</td>
										<td class="single-name">${studentData.lastName}</td>
										<td class="universityid">${studentData.universityId} <@pl.profile_link studentData.universityId /></td>
									</tr>
								</#list>
							</tbody>
						</table>

					</#if>

				</div>

				<div class="col-md-6 entities">
					<div class="header-with-tooltip">
						<h3>${relationshipType.description}s</h3>
						<span><@fmt.p allocated?size "${relationshipType.description}" /> found</span>
					</div>

					<div class="student-filter well well-sm">
						<#assign placeholder = "All ${relationshipType.description}s" />
						<#assign currentfilter><@current_filter_value "entityTypes" placeholder; entityType>${command.allEntityTypesLabels[entityType]}</@current_filter_value></#assign>
						<@filter path="entityTypes" placeholder=placeholder currentFilter=currentfilter allItems=command.allEntityTypes validItems=command.allEntityTypes cssClass="wide"; entityType, isValid>
							<input type="checkbox" name="${status.expression}" value="${entityType}" data-short-value="${command.allEntityTypesLabels[entityType]}"
								${command.entityTypes?seq_contains(entityType)?string('checked','')}
							>
							${command.allEntityTypesLabels[entityType]}
						</@filter>

						<button class="btn btn-xs btn-default apply" type="submit">Apply</button>

						<br /><br />

						<button type="button" class="btn btn-default" data-toggle="modal" data-target="#add-agents">
							Add ${relationshipType.agentRole}s</button>
					</div>

					<p>
						<button class="btn btn-default remove-all" name="action" value="${commandActions.RemoveFromAll}" type="submit" title="You need to select some personal tutors from which to remove students">
							Remove all students from selected ${relationshipType.description}(s)
						</button>
					</p>

					<p>
						<button class="btn invisible">Invisible</button>
					</p>

					<table class="table table-condensed table-striped table-hover scrollable-tbody">
						<thead>
						<tr>
							<th class="check for-check-all"></th>
							<th class="full-name sortable">${relationshipType.description}s name</th>
							<th class="counter sortable">Students</th>
							<th class="edit"></th>
						</tr>
						</thead>
						<tbody>
							<#list allocated?sort_by("sortName") as entityData>
								<tr data-entity="${entityData.entityId}" <#if command.expanded[entityData.entityId]!false>class="expanded"</#if>>
									<td class="check"><input type="checkbox" name="entities" value="${entityData.entityId}" /></td>
									<td class="full-name" data-sortby="${entityData.sortName}">${entityData.displayName}</td>
									<td class="counter">${entityData.students?size}</td>
									<td class="toggle">
										<button title="Edit students allocated to this ${relationshipType.agentRole}" class="btn btn-default btn-xs <#if !entityData.students?has_content>disabled</#if>" type="button">
											Edit
										</button>
										<input type="hidden" name="expanded[${entityData.entityId}]" value="${(command.expanded[entityData.entityId]!false)?string}" />
									</td>
								</tr>
								<#list entityData.students?sort_by("lastName", "firstName") as studentData>
									<tr data-forentity="${entityData.entityId}" class="forentity <#if !studentData_has_next>last</#if>">
										<td class="student" colspan="3">${studentData.firstName} ${studentData.lastName} (${studentData.universityId})  <@pl.profile_link studentData.universityId /></td>
										<td class="remove">
											<button title="Remove" class="btn btn-danger btn-xs" type="submit" name="removeSingleCombined" value="removeSingle-${entityData.entityId}-${studentData.universityId}">
												Remove
											</button>
										</td>
									</tr>
								</#list>
							</#list>
						</tbody>
					</table>

				</div>
			</div>

			<div class="modal fade" id="add-agents" tabindex="-1" role="dialog" aria-labelledby="add-agents-label" aria-hidden="true">
				<@modal.wrapper>
					<@modal.header>
						<h3 id="add-agents-label" class="modal-title">Add ${relationshipType.agentRole}s</h3>
					</@modal.header>

					<@modal.body>
						<p>
							Lookup ${relationshipType.agentRole}s by typing their names, usercodes or university IDs below, then click <code>Add</code>.
						</p>

						<@bs3form.labelled_form_group path="additionalEntityUserIds" labelText="${relationshipType.agentRole?cap_first}s">
							<@form.flexipicker path="additionalEntityUserIds" placeholder="User name" membersOnly="true" list=true multiple=true />
						</@bs3form.labelled_form_group>
					</@modal.body>

					<@modal.footer>
						<button type="submit" class="btn btn-primary" name="action" value="${commandActions.AddAdditionalEntities}">Add</button>
					</@modal.footer>
				</@modal.wrapper>
			</div>
		</@f.form>

		<@f.form commandName="command" action="${previewFormAction}" method="POST" cssClass="form-inline preview">
			<#assign allocatedStudents = [] />
			<#list command.additions?keys as entity>
				<#list command.additions[entity] as student>
					<#assign allocatedStudents = allocatedStudents + [student] />
					<input type="hidden" name="additions[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#list command.removals?keys as entity>
				<#list command.removals[entity] as student>
					<input type="hidden" name="removals[${entity}]" value="${student}" />
				</#list>
			</#list>
			<#if command.agentEntityData?has_content>
				<#list command.dbUnallocated as studentData>
					<#list allocatedStudents as allocatedStudent>
						<#if studentData.universityId == allocatedStudent>
							<input type="hidden" name="removals[${agentId}]" value="${studentData.universityId}" />
						</#if>
					</#list>
				</#list>
			</#if>
			<#list command.additionalEntities as entity>
				<input type="hidden" name="additionalEntities" value="${entity}" />
			</#list>
			<input type="hidden" name="allocationType" value="${allocationTypes.Add}" />

			<div class="submit-buttons fix-footer">
				<button type="submit" class="btn btn-primary">Save</button>
				<a href="<@routes.profiles.home />" class="btn btn-default">Cancel</a>
			</div>
		</@f.form>
	</div>
</#escape>