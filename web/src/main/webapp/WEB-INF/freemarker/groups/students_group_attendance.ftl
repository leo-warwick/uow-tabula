<#import "*/group_components.ftl" as components />
<#escape x as x?html>
<div id="student-groups-view">
	<#macro eventsInATerm term>
		<#local weekNumbers = mapGet(seminarAttendanceCommandResult.termWeeks, term) />
		<#local groupsMap = mapGet(seminarAttendanceCommandResult.attendance, term) />

		<div class="item-info row-fluid term">
			<div class="span12">
				<h4>${term.termTypeAsString}</h4>
				<div class="row-fluid term">
					<table id="group_attendance_${term.termTypeAsString}" class="table table-striped table-condensed attendance-table">
						<thead>
							<tr>
								<th class="sortable nowrap">
									<#-- TAB-1124 don't show Group header on gadget view -->
									<#if defaultExpand!false>
										Group
									</#if>
								</th>
								<#list (weekNumbers.minWeek)..(weekNumbers.maxWeek) as weekNumber>
									<th class="instance-date-header">
										<div class="instance-date">
											<#if student.homeDepartment?has_content>
												<@fmt.singleWeekFormat week=weekNumber academicYear=academicYear dept=student.homeDepartment short=!(defaultExpand!false) />
											</#if>
										</div>
									</th>
								</#list>
								<th class="sortable">
									<span class="fa-stack fa-stack-original-size fa-stack-right " >
										<i class="fa fa-fw fa-stack-2x fa-times unauthorised" title="Missed count"></i>
									</span>
								</th>
							</tr>
						</thead>
						<tbody>
							<#list groupsMap?keys as group>
								<#local set = group.groupSet />
								<#local module = set.module />
								<#local department = module.adminDepartment />

								<#local weeksMap = mapGet(groupsMap, group) />
								<#local missedCount = 0 />

								<tr>
									<td class="nowrap" title="${group.groupSet.name} - ${group.name}">
										<#if defaultExpand!false>
											${group.groupSet.name}
										<#else>
											${group.groupSet.module.code?upper_case}
										</#if>
									</td>
									<#list (weekNumbers.minWeek)..(weekNumbers.maxWeek) as weekNumber>
										<#if weeksMap?keys?seq_contains(weekNumber)>
											<#local weekMap = mapGet(weeksMap, weekNumber) />
											<td>
												<#list weekMap?keys as instance>
													<#local state = mapGet(weekMap, instance) />

													<#local title><@components.instanceFormat instance academicYear department /></#local>

													<#if state.name == 'Attended'>
														<#local class = "fa-check attended" />
														<#local title = "${student.fullName} attended: " + title />
													<#elseif state.name == 'MissedAuthorised'>
														<#local class = "fa-times-circle-o authorised" />
														<#local title = "${student.fullName} did not attend (authorised absence): " + title />
													<#elseif state.name == 'MissedUnauthorised'>
														<#local class = "fa-times unauthorised" />
														<#local title = "${student.fullName} did not attend (unauthorised): " + title />
														<#local missedCount = missedCount + 1 />
													<#elseif state.name == 'Late'>
														<#local class = "fa-exclamation-triangle late" />
														<#local title = "No data: " + title />
													<#elseif state.name == 'NotExpected'>
														<#local class = "" />
														<#local title = "No longer in group" />
													<#else>
														<#local class = "fa-minus" />
													</#if>

													<#local titles = [title] />
													<#if mapGet(seminarAttendanceCommandResult.notes, instance)??>
														<#local studentNote = mapGet(seminarAttendanceCommandResult.notes, instance) />
														<#local note>
															${studentNote.absenceType.description}<br />
															${studentNote.truncatedNote}
															<#if (studentNote.truncatedNote?length > 0)>
																<br/>
															</#if>
															<a class='attendance-note-modal' href='<@routes.groups.viewNote studentNote.student studentNote.occurrence />'>View attendance note</a>
														</#local>
														<#local titles = titles + [note] />
													</#if>

													<#local renderedTitle>
														<#list titles as t>
															<#if (titles?size > 1)>
																<p>${t}</p>
															<#else>
															${t}
															</#if>
														</#list>
													</#local>

														<span class="fa-stack fa-stack-original-size fa-stack-right fa-fw use-popover" data-content="<#noescape>${renderedTitle}</#noescape>" data-html="true">
															<i class="fa fa-fw fa-stack-2x ${class}"></i>
															<#if mapGet(notes, instance)??><i class="fa fa-fw fa-stack-1x fa-envelope-o fa-filled-white"></i></#if>
														</span>
												</#list>
											</td>
										<#else>
											<td title="No event in this week">
												&nbsp;
											</td>
										</#if>
									</#list>
									<td>
										<span class="badge progress-bar-<#if (missedCount > 2)>danger<#elseif (missedCount > 0)>warning<#else>success</#if>">${missedCount}</span>
									</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</#macro>

	<#if !seminarAttendanceCommandResult.attendance?? || !seminarAttendanceCommandResult.hasGroups>
		<div class="seminar-attendance-profile striped-section collapsible <#if defaultExpand!false>expanded</#if>">
			<h3 class="section-title"><#if title?has_content>${title}<#else>Small groups</#if></h3>
			<p><em>There are no small group events defined for this academic year.</em></p>
		</div>
	<#else>
		<div class="seminar-attendance-profile striped-section collapsible <#if defaultExpand!false>expanded</#if>">
			<h3 class="section-title"><#if title?has_content>${title}<#else>Small groups</#if></h3>
			<div class="missed-info">
				<#if seminarAttendanceCommandResult.missedCount == 0>
					<#if isSelf>
						You have missed 0 small group events.
					<#else>
						${student.firstName} has missed 0 small group events.
					</#if>
				<#else>
					<#list seminarAttendanceCommandResult.missedCountByTerm?keys as term>
						<#if mapGet(seminarAttendanceCommandResult.missedCountByTerm, term) != 0>
							<div class="missed">
								<span class="fa-stack fa-stack-original-size fa-stack-right fa-fw">
									<i class="fa fa-fw fa-stack-2x fa-warning"></i>
								</span>
								<#if isSelf>
									You have
								<#else>
									${student.firstName} has
								</#if>
								 missed
								<#if mapGet(seminarAttendanceCommandResult.missedCountByTerm, term) == 1>
									1 small group event
								<#else>
									${mapGet(seminarAttendanceCommandResult.missedCountByTerm, term)} small group events
								</#if>
								in ${term.termTypeAsString}
							</div>
						</#if>
					</#list>
				</#if>
			</div>

			<div class="striped-section-contents">
				<#if seminarAttendanceCommandResult.attendance?keys?size == 0>
					<div class="item-row row-fluid">
						<div class="span12"><em>There are no small group events for this route and year of study.</em></div>
					</div>
				<#else>
					<#list seminarAttendanceCommandResult.attendance?keys as term>
						<@eventsInATerm term />
					</#list>
				</#if>
			</div>
		</div>
	</#if>
</div>
</#escape>
