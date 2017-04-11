<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.wrapper isModal>
	<@modal.header isModal>
		<h3 class="modal-title">${student.fullName} small group teaching events</h3>
	</@modal.header>

	<@modal.body isModal>

		<#if !point.pointType?? || point.pointType.dbValue != "smallGroup">
			<div class="alert alert-danger">
				Specified monitoring point is not a Teaching event point
			</div>
		<#else>

			<p>
				Monitoring point '${point.name}'
				requires attendance at <strong><@fmt.p point.smallGroupEventQuantity 'event' /></strong>
				for <strong>
					<#if point.smallGroupEventModules?size == 0>
						any module
					<#else>
						<#list point.smallGroupEventModules as module>${module.code?upper_case}<#if module_has_next>, </#if></#list>
					</#if>
				</strong>
				during <strong>
					<#if point.pointType.dbValue == 'date'>
						<@fmt.interval point.startDate point.endDate />
					<#else>
						<@fmt.monitoringPointWeeksFormat
							point.startWeek
							point.endWeek
							point.scheme.academicYear
							point.scheme.department
						/>
					</#if>
				</strong>
			</p>

			<details>
				<summary>
					<span class="legend">Course: ${result.course.name}</span>
				</summary>

				<table class="table table-condensed table-striped">
					<tbody>
						<tr>
							<th>Route:</th>
							<td>${result.course.route}</td>
						</tr>
						<tr>
							<th>Department:</th>
							<td>${result.course.department}</td>
						</tr>
						<tr>
							<th>Status on Route:</th>
							<td>${result.course.status}</td>
						</tr>
						<tr>
							<th>Attendance:</th>
							<td>${result.course.attendance}</td>
						</tr>
						<tr>
							<th>UG/PG:</th>
							<td>${result.course.courseType}</td>
						</tr>
						<tr>
							<th>Year of study:</th>
							<td>${result.course.yearOfStudy}</td>
						</tr>
					</tbody>
				</table>
			</details>

			<details>
				<summary>
					<span class="legend">Modules</span>
				</summary>

				<#if result.modules?size == 0>
					<em>No module registrations found.</em>
				<#else>
					<table class="table table-condensed table striped">
						<thead>
							<tr>
								<td></td>
								<td>Code</td>
								<td>Title</td>
								<td>Department</td>
								<td>CATS</td>
								<td>Status</td>
							</tr>
						</thead>
						<tbody>
							<#list result.modules as module>
								<tr>
									<td>
										<#if !module.hasGroups>
											<i class="fa fa-fw fa-exclamation-circle" title="This module has no small groups set up in Tabula"></i>
										</#if>
									</td>
									<td>${module.code}</td>
									<td>${module.title}</td>
									<#--This is a ViewSmallGroupsForPointCommandResult.Module which has .department rather than .adminDepartment -->
									<td>${module.department}</td>
									<td>${module.cats}</td>
									<td>${module.status}</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</#if>
			</details>

			<details class="groups" open>
				<summary>
					<span class="legend">Project groups, Seminars</span>
				</summary>
				<div>

				<#macro instanceFormat instance academicYear department><#compress>
					<#local event = instance._1() />
					<#local week = instance._2() />
					${event.day.shortName} <@fmt.time event.startTime />, <@fmt.singleWeekFormat week academicYear department />
				</#compress></#macro>

				<#macro groupsInATerm term>
					<h4>${term.name}</h4>

					<table class="table table-striped table-condensed">
						<thead>
							<tr>
								<th class="sortable nowrap"></th>
								<#list term.weeks as weekPair>
									<th class="instance-date-header <#if !weekPair._2()>irrelevant</#if>">
										<#if currentMember.homeDepartment??>
											<@fmt.singleWeekFormat week=weekPair._1() academicYear=academicYear dept=currentMember.homeDepartment short=true />
										</#if>
									</th>
								</#list>
							</tr>
						</thead>
						<tbody>
							<#list term.groups as group>
								<tr <#if !group.relevant>class="irrelevant"</#if>>
									<td>
										${group.name}
									</td>
									<#list term.weeks as weekPair>
										<td <#if !weekPair._2()>class="irrelevant"</#if>>
											<#list mapGet(group.attendance, weekPair._1()) as attendance>
												<#if !attendance.instance??>
													<i class="use-popover fa fa-fw irrelevant" data-content="${attendance.reason}"></i>
												<#else>
													<#local titles = [] />

													<#if attendance.reason?has_content>
														<#local reason><#noescape><strong>${attendance.reason}</strong></#noescape></#local>
														<#local titles = titles + [reason] />
													</#if>

													<#local title><@instanceFormat attendance.instance academicYear currentMember.homeDepartment /></#local>
													<#if attendance.state.name == 'Attended'>
														<#local class = "fa fa-check attended" />
														<#local title = "${student.fullName} attended: " + title />
													<#elseif attendance.state.name == 'MissedAuthorised'>
														<#local class = "fa fa-times-circle-o authorised" />
														<#local title = "${student.fullName} did not attend (authorised absence): " + title />
													<#elseif attendance.state.name == 'MissedUnauthorised'>
														<#local class = "fa fa-times unauthorised" />
														<#local title = "${student.fullName} did not attend (unauthorised): " + title />
													<#elseif attendance.state.name == 'Late'>
														<#local class = "fa fa-exclamation-triangle late" />
														<#local title = "No data: " + title />
													<#else>
														<#local class = "fa fa-minus" />
													</#if>

													<#local titles = titles + [title] />

													<#if attendance.note??>
														<#local studentNote = attendance.note />
														<#local note>
															${studentNote.absenceType.description}<br />
															${studentNote.truncatedNote}
															<#if (studentNote.truncatedNote?length > 0)>
																<br/>
															</#if>
															<a class='attendance-note-modal' href='<@routes.attendance.groupsNoteView studentNote.student studentNote.occurrence />'>View attendance note</a>
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

													<i class="use-popover fa fa-fw ${class} <#if !attendance.relevant>irrelevant</#if>" data-content="<#noescape>${renderedTitle}</#noescape>" data-html="true"></i>
												</#if>
											</#list>
										</td>
									</#list>
								</tr>
							</#list>
						</tbody>
					</table>
				</#macro>

				<#if result.groupData.terms?size == 0>
					<em>There are no small group events defined for this academic year.</em>
				<#else>
					<#list result.groupData.terms as term>
						<@groupsInATerm term />
					</#list>
				</#if>
				</div>
			</details>

		</#if>

	</@modal.body>
</@modal.wrapper>

</#escape>