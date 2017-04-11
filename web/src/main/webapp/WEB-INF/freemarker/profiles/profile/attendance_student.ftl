<#import "*/modal_macros.ftl" as modal />

<#import "../../attendance/attendance_variables.ftl" as attendance_variables />
<#import "../../attendance/attendance_macros.ftl" as attendance_macros />

<#escape x as x?html>

<#if !isSelf>
	<details class="indent">
		<summary>${student.officialName}</summary>
		<#if student.userId??>
			${student.userId}<br/>
		</#if>
		<#if student.email??>
			<a href="mailto:${student.email}">${student.email}</a><br/>
		</#if>
		<#if student.phoneNumber??>
			${phoneNumberFormatter(student.phoneNumber)}<br/>
		</#if>
		<#if student.mobileNumber??>
			${phoneNumberFormatter(student.mobileNumber)}<br/>
		</#if>
	</details>
</#if>

<h1>Attendance</h1>

<#if hasMonitoringPointAttendancePermission>
	<#assign groupedPointMap=monitoringPointAttendanceCommandResult.groupedPointMap />
	<#if groupedPointMap?keys?size == 0>
		<div class="seminar-attendance-profile striped-section collapsible">
			<h3 class="section-title">Monitoring points</h3>
			<p><em>No monitoring points found for this academic year.</em></p>
		</div>
	<#else>
		<div class="monitoring-points-profile striped-section collapsible expanded">
			<#if can.do("MonitoringPoints.Record", student)>
				<#assign returnTo><@routes.profiles.profile_attendance studentCourseDetails academicYear /></#assign>
				<a class="pull-right btn btn-primary btn-sm" href="<@routes.attendance.profileRecord student academicYear returnTo />">Record attendance</a>
			</#if>
			<h3 class="section-title">
				Monitoring points
			</h3>
			<div class="missed-info">
				<#if !monitoringPointAttendanceCommandResult.hasAnyMissedPoints>
					<#if isSelf>
						You have missed 0 monitoring points.
					<#else>
						${student.firstName} has missed 0 monitoring points.
					</#if>
				<#else>
					<#macro missedWarning term>
						<#if (monitoringPointAttendanceCommandResult.missedPointCountByTerm[term]?? && monitoringPointAttendanceCommandResult.missedPointCountByTerm[term] > 0)>
							<div class="missed">
								<i class="icon-warning-sign"></i>
								<#if isSelf>
									You have
								<#else>
								${student.firstName} has
								</#if>
								missed <@fmt.p monitoringPointAttendanceCommandResult.missedPointCountByTerm[term] "monitoring point" /> in ${term}
							</div>
						</#if>
					</#macro>
					<#list attendance_variables.monitoringPointTermNames as term>
						<@missedWarning term />
					</#list>
					<#list monthNames as month>
						<@missedWarning month />
					</#list>
				</#if>
			</div>

			<div class="striped-section-contents">
				<#list attendance_variables.monitoringPointTermNames as term>
					<#if groupedPointMap[term]??>
						<div class="item-info row-fluid term">
							<div>
								<h4>${term}</h4>
								<table class="table table-hover">
									<tbody>
										<#list groupedPointMap[term] as pointPair>
											<#assign point = pointPair._1() />
											<tr class="point">
												<td class="point" title="${point.name} <@fmt.wholeWeekDateFormat startWeek=point.startWeek endWeek=point.endWeek academicYear=point.scheme.academicYear stripHtml=true/>">
													${point.name} (<@fmt.wholeWeekDateFormat point.startWeek point.endWeek	point.scheme.academicYear/>)
												</td>
												<td class="state">
													<#if pointPair._2()??>
														<@attendance_macros.checkpointLabel
															department=point.scheme.department
															checkpoint=pointPair._2()
															urlProfile=true/>
													<#else>
														<@attendance_macros.checkpointLabel
															department=point.scheme.department
															point=pointPair._1()
															student=student
															urlProfile=true/>
													</#if>
												</td>
											</tr>
										</#list>
									</tbody>
								</table>
							</div>
						</div>
					</#if>
				</#list>
				<#list monthNames as month>
					<#if groupedPointMap[month]??>
						<div class="item-info row-fluid term">
							<div>
								<h4>${month}</h4>
								<table class="table table-hover">
									<tbody>
										<#list groupedPointMap[month] as pointPair>
											<#assign point = pointPair._1() />
											<tr class="point">
												<td class="point" title="${point.name} (<@fmt.interval point.startDate point.endDate  true/>)">
													${point.name} (<@fmt.interval point.startDate point.endDate />)
												</td>
												<td class="state">
													<#if pointPair._2()??>
														<@attendance_macros.checkpointLabel department=point.scheme.department checkpoint=pointPair._2() urlProfile=true />
													<#else>
														<@attendance_macros.checkpointLabel department=point.scheme.department point=pointPair._1() student=student urlProfile=true />
													</#if>
												</td>
											</tr>
										</#list>
									</tbody>
								</table>
							</div>
						</div>
					</#if>
				</#list>
			</div>
		</div>
		<#assign notes = monitoringPointAttendanceCommandResult.notes />
		<#assign noteCheckpoints = monitoringPointAttendanceCommandResult.noteCheckpoints />
		<div class="monitoring-points-profile attendance-notes striped-section collapsible">
			<h3 class="section-title">Attendance notes</h3>
			<div class="attendance-note-info">
				<#if isSelf>
					You have <@fmt.p notes?size "attendance note" />
				<#else>
					${student.firstName} has <@fmt.p notes?size "attendance note" />
				</#if>
			</div>
			<div class="striped-section-contents">
				<#if notes?has_content>
					<div class="row item-info">
						<div class="col-md-12 form-inline">
							<div class="form-group">
								<label>Filter options</label>
								<label class="checkbox-inline"><input type="checkbox" class="collection-check-all" checked />All</label>
								<#list allCheckpointStates as state>
									<label class="checkbox-inline"><input type="checkbox" class="collection-checkbox" name="checkpointState" value="${state.dbValue}" checked />${state.description}</label>
								</#list>
							</div>
						</div>
					</div>
					<#list notes as note>
						<#if mapGet(noteCheckpoints, note)??>
							<#assign checkpoint = mapGet(noteCheckpoints, note) />
						<#else>
							<#assign checkpoint = '' />
						</#if>
						<div class="row item-info checkpoint-state-<#if checkpoint?has_content>${checkpoint.state.dbValue}<#else>not-recorded</#if>">
							<div class="col-md-12">
								<p>
									<#if checkpoint?has_content>
										<strong>${checkpoint.state.description}</strong>:
									<#else>
										<strong>Unrecorded</strong>:
									</#if>
									${note.point.name}
									<#if point.scheme.pointStyle.dbValue == "week">
										(<@fmt.wholeWeekDateFormat point.startWeek point.endWeek point.scheme.academicYear />)
									<#else>
										(<@fmt.interval point.startDate point.endDate />)
									</#if>.
									<#if checkpoint?has_content>
										<@attendance_macros.checkpointDescription department=checkpoint.point.scheme.department checkpoint=checkpoint point=point student=note.student withParagraph=false />
									</#if>
								</p>

								<p>Absence type: ${note.absenceType.description}</p>

								<#if note.note?has_content>
									<blockquote><#noescape>${note.escapedNote}</#noescape></blockquote>
								</#if>

								<#if note.attachment?has_content>
									<p>
										<@fmt.download_link
											filePath="/attendance/note/${academicYear.startYear?c}/${note.student.universityId}/${note.point.id}/attachment/${note.attachment.name}"
											mimeType=note.attachment.mimeType
											title="Download file ${note.attachment.name}"
											text="Download ${note.attachment.name}"
										/>
									</p>
								</#if>

								<p class="hint">
									Attendance note updated
									<#if note.updatedBy?? && note.updatedBy?has_content>
										by
										<@userlookup id=note.updatedBy>
											<#if returned_user.foundUser>
												${returned_user.fullName}
											<#else>
												${note.updatedBy}
											</#if>
										</@userlookup>
										,
									</#if>
									<@fmt.date note.updatedDate />
								</p>
							</div>
						</div>
					</#list>
				<#else>
					<div class="row item-info"><div class="col-md-12"><em>There are no notes.</em></div></div>
				</#if>
			</div>
		</div>

	</#if>
<#else>
	<div class="alert alert-info">
		You do not have permission to see the monitoring point attendance for this course.
	</div>
</#if>

<#if hasSeminarAttendancePermission>
	<#include "../../groups/students_group_attendance.ftl" />
<#else>
	<div class="alert alert-info">
		You do not have permission to see the seminar attendance for this course.
	</div>
</#if>

<script>
	jQuery(function($) {
		$('.attendance-notes').bigList({
			onChange: function(){
				$('[name=checkpointState]').each(function(){
					var $this = $(this), state = $this.val();
					if ($this.is(':checked')) {
						$('.attendance-notes .checkpoint-state-' + state).show();
					} else {
						$('.attendance-notes .checkpoint-state-' + state).hide();
					}

				});
			}
		});
	});
</script>
</#escape>