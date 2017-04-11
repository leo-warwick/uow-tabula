<#import "attendance_variables.ftl" as attendance_variables />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#macro attendanceButtons>
	<div style="display:none;" class="forCloning">
		<div class="btn-group" data-toggle="radio-buttons">
			<button
					type="button"
					class="btn btn-default use-tooltip"
					data-state=""
					title="Set to 'Not recorded'"
					data-html="true"
					data-container="body"
					>
				<i class="fa fa-fw fa-minus"></i>
			</button>
			<button
					type="button"
					class="btn btn-default btn-unauthorised use-tooltip"
					data-state="unauthorised"
					title="Set to 'Missed (unauthorised)'"
					data-html="true"
					data-container="body"
					>
				<i class="fa fa-fw fa-times"></i>
			</button>
			<button
					type="button"
					class="btn btn-default btn-authorised use-tooltip"
					data-state="authorised"
					title="Set to 'Missed (authorised)'"
					data-html="true"
					data-container="body"
					>
				<i class="fa fa-fw fa-times-circle-o"></i>
			</button>
			<button
					type="button"
					class="btn btn-default btn-attended use-tooltip"
					data-state="attended"
					title="Set to 'Attended'"
					data-html="true"
					data-container="body"
					>
				<i class="fa fa-fw fa-check"></i>
			</button>
		</div>
	</div>
</#macro>

<#function sortClass field command>
	<#list command.sortOrder as order>
		<#if order.propertyName == field>
			<#if order.ascending>
				<#return "headerSortDown" />
			<#else>
				<#return "headerSortUp" />
			</#if>
		</#if>
	</#list>
	<#return "" />
</#function>

<#macro pagination currentPage totalResults resultsPerPage>
	<#local totalPages = (totalResults / resultsPerPage)?ceiling />
	<nav>
		<ul class="pagination pagination-sm">
			<#if currentPage lte 1>
				<li class="disabled"><span>&laquo;</span></li>
			<#else>
				<li><a href="?page=${currentPage - 1}" data-page="${currentPage - 1}">&laquo;</a></li>
			</#if>

			<#list 1..totalPages as page>
				<#if page == currentPage>
					<li class="active"><span>${page}</span></li>
				<#else>
					<li><a href="?page=${page}" data-page="${page}">${page}</a></li>
				</#if>
			</#list>

			<#if currentPage gte totalPages>
				<li class="disabled"><span>&raquo;</span></li>
			<#else>
				<li><a href="?page=${currentPage + 1}" data-page="${currentPage + 1}">&raquo;</a></li>
			</#if>
		</ul>
	</nav>
</#macro>

<#macro manageStudentTable
	membershipItems
	doSorting=false
	command=""
	checkboxName=""
	onlyShowCheckboxForStatic=false
	checkAll=false
	showRemoveButton=false
	showResetButton=false
>

	<#if (membershipItems?size > 0)>

		<table class="manage-student-table table table-striped table-condensed table-hover tablesorter sb-no-wrapper-table-popout">
			<thead>
			<tr>
				<th class="profile_link-col"></th>
				<th style="width: 50px;" <#if doSorting> class="${sortClass("source", command)} sortable" data-field="source"</#if>>Source</th>
				<th <#if doSorting> class="${sortClass("firstName", command)} sortable" data-field="firstName"</#if>>First name</th>
				<th <#if doSorting> class="${sortClass("lastName", command)} sortable" data-field="lastName"</#if>>Last name</th>
				<th <#if doSorting> class="${sortClass("universityId", command)} sortable" data-field="universityId"</#if>>ID</th>
				<th <#if doSorting> class="${sortClass("userId", command)} sortable" data-field="userId"</#if>>User</th>
				<th>Schemes</th>
				<#if checkboxName?has_content>
					<th style="width: 65px; padding-right: 5px; white-space: nowrap" <#if checkAll>class="for-check-all"</#if>>
						<#if showRemoveButton>
							<input class="btn btn-danger btn-xs use-tooltip"
								<#if findCommandResult.membershipItems?size == 0>disabled</#if>
								type="submit"
								name="${ManageSchemeMappingParameters.manuallyExclude}"
								value="Remove"
								data-container="body"
								title="Remove selected students from this scheme"
								style="margin-left: 0.5em;"
							/>
						</#if>
						<#if (showResetButton && (editMembershipCommandResult.includedStudentIds?size > 0 || editMembershipCommandResult.excludedStudentIds?size > 0))>
							<input class="btn btn-danger btn-xs use-tooltip"
								type="submit"
								style="float: right; padding-left: 5px; padding-right: 5px; margin-left: 5px;"
								name="${ManageSchemeMappingParameters.resetMembership}"
								value="Reset"
								data-container="body"
								title="Restore the manually removed and remove the manually added students selected"
							/>
						</#if>
					</th>
				</#if>
			</tr>
			</thead>
			<tbody>
				<#list membershipItems as item>
					<tr class="${item.itemTypeString}">
						<td class="profile_link"><@pl.profile_link item.universityId /></td>
						<td>
							<#if item.itemTypeString == "static">
								<span class="use-tooltip" title="Automatically linked from SITS" data-placement="right">SITS</span>
							<#elseif item.itemTypeString == "exclude">
								<span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right">Removed</span>
							<#else>
								<span class="use-tooltip" title="Added manually" data-placement="right">Added</span>
							</#if>
						</td>
						<td>${item.firstName}</td>
						<td>${item.lastName}</td>
						<td><a class="profile-link" href="<@routes.attendance.profile item />">${item.universityId}</a></td>
						<td>${item.userId}</td>
						<td>
							<#if item.existingSchemes?size == 0>
								0 schemes
							<#else>
								<#local popovercontent>
									<ul>
										<#list item.existingSchemes as scheme>
											<li>${scheme.displayName}</li>
										</#list>
									<ul>
								</#local>

								<a href="#">
									<span
										class="use-tooltip"
										data-container="body"
										title="See which schemes apply to this student"
									>
										<span
											class="use-popover"
											data-container="body"
											data-html="true"
											data-content="${popovercontent}"
											data-placement="top"
										>
											<@fmt.p item.existingSchemes?size "scheme" />
										</span>
									</span>
								</a>
							</#if>
						</td>
						<#if checkboxName?has_content>
							<td>
								<#if !onlyShowCheckboxForStatic || item.itemTypeString == "static">
									<input type="checkbox" class="collection-checkbox" name="${checkboxName}" value="${item.universityId}" />
								</#if>
							</td>
						</#if>
					</tr>
				</#list>
			</tbody>
		</table>

		<div id="profile-modal" class="modal fade profile-subset"></div>
	</#if>
</#macro>

<#macro groupedPointsBySection pointsMap sectionName>
<div class="striped-section expanded">
	<h2 class="section-title with-contents">${sectionName}</h2>
	<div class="striped-section-contents">
		<#list pointsMap[sectionName] as groupedPoint>
			<div class="item-info row point">
				<#nested groupedPoint />
			</div>
		</#list>
	</div>
</div>
</#macro>

<#macro groupedPointSchemePopover groupedPoint>
	<#local popoverContent>
		<ul>
			<#list groupedPoint.schemes?sort_by("displayName") as scheme>
				<li>${scheme.displayName}</li>
			</#list>
		</ul>
	</#local>
	<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
		<@fmt.p groupedPoint.schemes?size "scheme" />
	</a>
</#macro>

<#function formatResult department checkpoint="" point="" student="" note="", urlProfile=false>
	<#if checkpoint?has_content>
		<#if note?has_content>
			<#return attendanceMonitoringCheckpointFormatter(department, checkpoint, note, urlProfile) />
		<#else>
			<#return attendanceMonitoringCheckpointFormatter(department, checkpoint, urlProfile) />
		</#if>
	<#else>
		<#if note?has_content>
			<#return attendanceMonitoringCheckpointFormatter(department, point, student, note, urlProfile) />
		<#else>
			<#return attendanceMonitoringCheckpointFormatter(department, point, student, urlProfile) />
		</#if>
	</#if>
</#function>

<#macro checkpointDescription department checkpoint="" point="" student="" note="" urlProfile=false withParagraph=true>
	<#local formatResult = formatResult(department, checkpoint, point, student, note, urlProfile) />
	<#if formatResult.metadata?has_content>
		<#if withParagraph><p>${formatResult.metadata}</p><#else>${formatResult.metadata}</#if>
	</#if>
</#macro>

<#macro checkpointLabel department checkpoint="" point="" student="" note="" urlProfile=false>
	<#local formatResult = formatResult(department, checkpoint, point, student, note, urlProfile) />
	<#local popoverContent>
		<#if formatResult.status?has_content><p>${formatResult.status}</p></#if>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
		<#if formatResult.noteUrl?has_content><p><a class='attendance-note-modal' href='${formatResult.noteUrl}?dt=${.now?string('iso')}'>View attendance note</a></p></#if>
	</#local>
	<span class="use-popover label ${formatResult.labelClass}" data-content="${popoverContent}" data-html="true" data-placement="left">${formatResult.labelText}</span>
	<span class="hidden-desktop visible-print visible-print-inline">
		<#if formatResult.metadata?has_content>${formatResult.metadata}<br /></#if>
		<#if formatResult.noteType?has_content>${formatResult.noteType}<br /></#if>
		<#if formatResult.noteText?has_content>${formatResult.noteText}</#if>
	</span>
</#macro>

<#macro checkpointSelect department id name checkpoint="" point="" student="" note="">
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#local tooltipContent>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
	</#local>
	<#local startDateInFuture = point.startDateInFuture />
	<select
		id="${id}"
		name="${name}"
		title="${tooltipContent}"
	>
		<option value="" <#if !checkpoint?has_content >selected</#if>>Not recorded</option>
		<option <#if startDateInFuture>class="disabled" title="This event hasn't happened yet so it can't be marked as missed (unauthorised)" </#if> value="unauthorised" <#if checkpoint?has_content && checkpoint.state.dbValue == "unauthorised">selected</#if>>Missed (unauthorised)</option>
		<option value="authorised" <#if checkpoint?has_content && checkpoint.state.dbValue == "authorised">selected</#if>>Missed (authorised)</option>
		<option <#if startDateInFuture>class="disabled" title="This event hasn't happened yet so it can't be marked as attended" </#if> value="attended" <#if checkpoint?has_content && checkpoint.state.dbValue == "attended">selected</#if>>Attended</option>
	</select>
</#macro>

<#macro checkpointIcon department checkpoint="" point="" student="" note="">
	<#local nonActivePoint = checkpoint?has_content && !checkpoint.activePoint />
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#local popoverContent>
		<#if nonActivePoint>
			<p>
				This attendance can no longer be edited, because it was recorded for a monitoring scheme (${checkpoint.point.scheme.displayName})
				that no longer applies to this student.
				<a href='http://warwick.ac.uk/tabula/manual/monitoring-points/recording-viewing-points/#nonactivepoints' target='_blank'><i class='fa fa-question-circle'></i></a>
			</p>
		</#if>
		<#if formatResult.status?has_content><p>${formatResult.status}</p></#if>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
		<#if formatResult.noteUrl?has_content><p><a class='attendance-note-modal' href='${formatResult.noteUrl}?dt=${.now?string('iso')}'>View attendance note</a></p></#if>
	</#local>
	<span class="fa fa-fw fa-stack fa-stack-original-size fa-stack-right use-popover" data-content="${popoverContent}" data-html="true">
		<i class="fa fa-fw fa-stack2x ${formatResult.iconClass} <#if nonActivePoint>non-active</#if>" ></i>
		<#if formatResult.noteUrl?has_content><i class="fa fa-fw fa-stack-1x fa-envelope-o fa-filled-white"></i></#if>
	</span>
</#macro>

<#macro checkpointIconForPointCheckpointPair department student pointCheckpointPair attendanceNotesMap>
	<#if pointCheckpointPair._2()??>
		<#if mapGet(attendanceNotesMap, pointCheckpointPair._1())??>
			<@checkpointIcon
				department=department
				checkpoint=pointCheckpointPair._2()
				note=mapGet(attendanceNotesMap, pointCheckpointPair._1())
			/>
		<#else>
			<@checkpointIcon
				department=department
				checkpoint=pointCheckpointPair._2()
			/>
		</#if>
	<#else>
		<#if mapGet(attendanceNotesMap, pointCheckpointPair._1())??>
			<@checkpointIcon
				department=department
				point=pointCheckpointPair._1()
				student=student
				note=mapGet(attendanceNotesMap, pointCheckpointPair._1())
			/>
		<#else>
			<@checkpointIcon
				department=department
				point=pointCheckpointPair._1()
				student=student
			/>
		</#if>
	</#if>
</#macro>

<#macro listCheckpointIcons department visiblePeriods monthNames result>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if visiblePeriods?seq_contains(term)>
			<td>
				<#if result.groupedPointCheckpointPairs[term]??>
					<#list result.groupedPointCheckpointPairs[term] as pointCheckpointPair>
						<#if pointCheckpointPair??>
							<@checkpointIconForPointCheckpointPair department result.student pointCheckpointPair result.attendanceNotes />
						<#else>
							<i class="fa fa-fw fa-stack fa-stack-right fa-stack-original-size"></i>
						</#if>
					</#list>
				<#else>
					<i class="fa fa-fw"></i>
				</#if>
			</td>
		</#if>
	</#list>
	<#list monthNames as month>
		<#if visiblePeriods?seq_contains(month)>
		<td>
			<#if result.groupedPointCheckpointPairs[month]??>
				<#list result.groupedPointCheckpointPairs[month] as pointCheckpointPair>
					<#if pointCheckpointPair??>
						<@checkpointIconForPointCheckpointPair department result.student pointCheckpointPair result.attendanceNotes />
					<#else>
						<i class="fa fa-fw"></i>
					</#if>
				</#list>
			<#else>
				<i class="fa fa-fw"></i>
			</#if>
		</td>
		</#if>
	</#list>
</#macro>

<#macro scrollablePointsTable command department filterResult visiblePeriods monthNames doCommandSorting=true>
<div class="scrollable-points-table">
	<div class="scrollable-table">
		<div class="scrollable-table-row">
			<div class="left">
				<table class="students table table-striped table-condensed">
					<thead>
						<tr>
							<th class="profile_link-col no-sort"></th>
							<th class="student-col <#if doCommandSorting>${sortClass("firstName", command)}</#if> sortable" data-field="firstName">First name</th>
							<th class="student-col <#if doCommandSorting>${sortClass("lastName", command)}</#if> sortable" data-field="lastName">Last name</th>
							<th class="id-col <#if doCommandSorting>${sortClass("universityId", command)}</#if> sortable" data-field="universityId">ID</th>
							<th class="unrecorded-col <#if doCommandSorting>${sortClass("attendanceCheckpointTotals.unrecorded", command)}</#if> sortable" data-field="attendanceCheckpointTotals.unrecorded">
								<i title="Unrecorded" class="fa fa-fw fa-exclamation-triangle late"></i>
							</th>
							<th class="missed-col <#if doCommandSorting>${sortClass("attendanceCheckpointTotals.unauthorised", command)}</#if> sortable" data-field="attendanceCheckpointTotals.unauthorised">
								<i title="Missed monitoring points" class="fa fa-fw fa-times unauthorised"></i>
							</th>
							<th class="record-col no-sort"></th>
						</tr>
					</thead>

					<tbody>
						<#list filterResult.results as result>
							<tr class="student">
								<td class="profile_link"><@pl.profile_link result.student.universityId /></td>
								<td class="fname" title="${result.student.firstName}">${result.student.firstName}</td>
								<td class="lname" title="${result.student.lastName}">${result.student.lastName}</td>
								<td class="id"><a class="profile-link" href="<@routes.attendance.profile result.student />">${result.student.universityId}</a></td>
								<#if result.groupedPointCheckpointPairs?keys?size == 0>
									<td class="unrecorded">
										0
									</td>
									<td class="missed">
										0
									</td>
									<td>&nbsp;</td>
								<#else>
									<#nested result />
								</#if>
							</tr>
						</#list>
					</tbody>
				</table>
			</div>

			<div class="right">
				<table class="attendance table tablesorter table-striped table-condensed sb-no-wrapper-table-popout">
					<thead>
					<tr>
						<#list attendance_variables.monitoringPointTermNames as term>
							<#if visiblePeriods?seq_contains(term)>
								<th class="${term}-col">${term}</th>
							</#if>
						</#list>
						<#list monthNames as month>
							<#if visiblePeriods?seq_contains(month)>
								<#assign monthMatch = month?matches("([a-zA-Z]{3})[a-zA-Z]*\\s(.*)")[0] />
								<#assign shortMonth>${monthMatch?groups[1]} ${monthMatch?groups[2]}</#assign>
								<th class="${shortMonth}-col">${shortMonth}</th>
							</#if>
						</#list>
						<#if visiblePeriods?size == 0>
							<th>&nbsp;</th>
						</#if>
					</tr>
					</thead>

					<tbody>
						<#list filterResult.results as result>
						<tr class="student">
							<#if visiblePeriods?size == 0 || !result.groupedPointCheckpointPairs?has_content>
								<td colspan="${visiblePeriods?size}"><span class="very-subtle"><em>No monitoring points found</em></span></td>
							<#else>
								<@listCheckpointIcons department visiblePeriods monthNames result />
							</#if>
						</tr>
						</#list>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
</#macro>

<#macro checkpointTotalTitle checkpointTotal>
	<#if checkpointTotal.updatedDate.millis == 0>
		(awaiting update)
	<#else>
		Last updated <@fmt.date checkpointTotal.updatedDate />
	</#if>
</#macro>