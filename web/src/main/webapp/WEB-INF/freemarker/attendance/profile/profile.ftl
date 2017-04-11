<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />

<#if can.do("MonitoringPoints.Record", student)>
	<div class="pull-right">
		<a class="btn btn-primary" href="<@routes.attendance.profileRecord student academicYear returnTo />">Record attendance</a>
	</div>
</#if>
<h1 class="with-settings"><#if isSelf>My </#if>Monitoring Points<#if !isSelf> for ${student.fullName}</#if></h1>

<#if groupedPointMap?keys?size == 0>
	<p><em>No monitoring points found for this academic year.</em></p>
<#else>

	<div class="monitoring-points-profile striped-section collapsible">
		<h3 class="section-title">Monitoring points</h3>
		<div class="missed-info">
			<#if !hasAnyMissed>
				<#if isSelf>
					You have missed 0 monitoring points.
				<#else>
					${student.firstName} has missed 0 monitoring points.
				</#if>
			<#else>
				<#macro missedWarning term>
					<#if (missedPointCountByTerm[term]?? && missedPointCountByTerm[term] > 0)>
						<div class="missed">
							<#if isSelf>
								You have
							<#else>
								${student.firstName} has
							</#if>
							missed <@fmt.p missedPointCountByTerm[term] "monitoring point" /> in ${term}
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
					<div class="item-info row term">
						<div class="col-md-12">
							<h4>${term}</h4>
							<table class="table">
								<tbody>
									<#list groupedPointMap[term] as pointPair>
										<#assign point = pointPair._1() />
										<tr class="point">
											<td class="point">
												${point.name}
												(<a class="use-tooltip" data-html="true" title="
													<@fmt.wholeWeekDateFormat
														point.startWeek
														point.endWeek
														point.scheme.academicYear
													/>
												"><@fmt.monitoringPointWeeksFormat
													point.startWeek
													point.endWeek
													point.scheme.academicYear
													point.scheme.department
												/></a>)
											</td>
											<td class="state">
												<#if pointPair._2()??>
													<@attendance_macros.checkpointLabel department=point.scheme.department checkpoint=pointPair._2() />
												<#else>
													<@attendance_macros.checkpointLabel department=point.scheme.department point=pointPair._1() student=student />
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
					<div class="item-info row term">
						<div class="col-md-12">
							<h4>${month}</h4>
							<table class="table">
								<tbody>
									<#list groupedPointMap[month] as pointPair>
									<#assign point = pointPair._1() />
									<tr class="point">
										<td class="point" title="${point.name} (<@fmt.interval point.startDate point.endDate />)">
											${point.name}
											(<@fmt.interval point.startDate point.endDate />)
										</td>
										<td class="state">
											<#if pointPair._2()??>
												<@attendance_macros.checkpointLabel department=point.scheme.department checkpoint=pointPair._2() />
											<#else>
												<@attendance_macros.checkpointLabel department=point.scheme.department point=pointPair._1() student=student />
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

	<div class="monitoring-points-profile attendance-notes striped-section collapsible expanded">
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
<script type="text/javascript">

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