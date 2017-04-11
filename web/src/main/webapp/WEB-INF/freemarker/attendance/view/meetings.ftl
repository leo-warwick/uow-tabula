<#escape x as x?html>
	<#if !command.point.pointType?? || command.point.pointType.dbValue != "meeting">
		<div class="alert alert-danger">
			Specified monitoring point is not a Meeting point
		</div>
	<#else>
		<h3 class="modal-header">Meetings with ${student.fullName}</h3>

		<p>
			Monitoring point requires ${command.point.meetingQuantity}
			<#if command.point.meetingFormats?size == allMeetingFormats?size>
				meeting of any format
			<#else>
				<#list command.point.meetingFormats as format>
					${format.description}<#if format_has_next> or </#if>
				</#list>
			</#if>
			with the student's
			<#list command.point.meetingRelationships as relationship>
				${relationship.description}<#if relationship_has_next> or </#if>
			</#list>

			<#if command.point.scheme.pointStyle.dbValue == "week">
				<#if command.point.startWeek == command.point.endWeek>
					in
					<a class="use-tooltip" data-html="true" data-placement="bottom" title="<@fmt.wholeWeekDateFormat command.point.startWeek command.point.endWeek command.point.scheme.academicYear />">
						<@fmt.monitoringPointWeeksFormat command.point.startWeek command.point.endWeek command.point.scheme.academicYear command.point.scheme.department />
					</a>
				<#else>
					between
					<a class="use-tooltip" data-html="true" data-placement="bottom" title="<@fmt.wholeWeekDateFormat command.point.startWeek command.point.startWeek command.point.scheme.academicYear />">
						<@fmt.monitoringPointWeeksFormat command.point.startWeek command.point.startWeek command.point.scheme.academicYear command.point.scheme.department />
					</a>
					and
					<a class="use-tooltip" data-html="true" data-placement="bottom" title="<@fmt.wholeWeekDateFormat command.point.endWeek command.point.endWeek command.point.scheme.academicYear />">
						<@fmt.monitoringPointWeeksFormat command.point.endWeek command.point.endWeek command.point.scheme.academicYear command.point.scheme.department />
					</a>
				</#if>
			<#else>
				(<@fmt.interval command.point.startDate command.point.endDate />)
			</#if>
		</p>

		<#if meetingsStatuses?size == 0>
			<p><em>There were no meetings found.</em></p>
		<#else>

			<table class="table table-condensed table-hover">
				<thead>
					<tr>
						<th>Meeting</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
					<#list meetingsStatuses as meetingStatus>
						<#assign meeting = meetingStatus._1() />
						<#assign reasons = meetingStatus._2() />
						<tr>
							<td>
								${(meeting.format.description)!"Unknown format"} with
								${meeting.relationship.relationshipType.agentRole}
								on <@fmt.date date=meeting.meetingDate includeTime=false/>
								created <@fmt.date meeting.lastUpdatedDate />
							</td>
							<td>
								<#if reasons?size == 0>
									<i class="fa fa-fw fa-check"></i>
								<#else>
									<#assign popoverContent>
										<#list reasons as reason>
											<#if reason == "Took place before">
												Took place before
												<#if command.point.scheme.pointStyle.dbValue == "week">
													<@fmt.monitoringPointWeeksFormat command.point.startWeek command.point.startWeek command.point.scheme.academicYear command.point.scheme.department />
												<#else>
													<@fmt.interval command.point.startDate command.point.startDate/>

												</#if>
											<#elseif reason == "Took place after">
												Took place after
												<#if command.point.scheme.pointStyle.dbValue == "week">
													<@fmt.monitoringPointWeeksFormat command.point.endWeek command.point.endWeek command.point.scheme.academicYear command.point.scheme.department />
												<#else>
													<@fmt.interval command.point.endDate command.point.endDate/>
												</#if>
											<#else>
												${reason}
											</#if>
											<#if reason_has_next><br /></#if>
										</#list>
									</#assign>
									<a class="use-popover" id="popover-meeting-status-${meetingStatus_index}" data-placement="left" data-html="true" data-content="${popoverContent}">
										<i class="fa fa-fw fa-times"></i>
									</a>
								</#if>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>

		</#if>
	</#if>
	<script>
		jQuery(function($){
			$('.use-tooltip').tooltip();
		})
	</script>
</#escape>