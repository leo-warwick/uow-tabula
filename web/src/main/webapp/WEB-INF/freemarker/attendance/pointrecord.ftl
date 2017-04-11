<#escape x as x?html>

<#import "*/attendance_macros.ftl" as attendance_macros />
<#import "*/modal_macros.ftl" as modal />

<#assign titleHeader>
	<div class="deptheader">
		<h1>Record attendance</h1>
		<h5 class="with-related"><span class="muted">for</span> ${command.templatePoint.name}</h5>
	</div>
</#assign>
<#assign numberOfStudents = command.checkpointMap?keys?size />

<#if numberOfStudents == 0>
	<#noescape>${titleHeader}</#noescape>

	<p class="alert alert-info">
		There are no students registered to these points.
	</p>
<#elseif (numberOfStudents > 600)>
	<#noescape>${titleHeader}</#noescape>

	<div class="alert alert-danger">
		There are too many students to display at once. Please go back and choose a more restrictive filter.
	</div>
<#else>

	<div class="pull-right">
		<a href="${uploadUrl}" class="btn btn-default upload-attendance">Upload attendance from CSV</a>
	</div>

	<#noescape>${titleHeader}</#noescape>

	<#import "*/_profile_link.ftl" as pl />
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<script>
		(function ($) {
			$(function() {
				$('.fix-area').fixHeaderFooter();

				$('.select-all').change(function(e) {
					$('.attendees').selectDeselectCheckboxes(this);
				});

			});
		} (jQuery));
	</script>

	<div class="recordCheckpointForm">

		<@attendance_macros.attendanceButtons />

		<div class="fix-area">
			<div class="fix-header pad-when-fixed">
				<div class="striped-section expanded spacing-only">
					<div class="striped-section-contents">
						<div class="row item-info check-all">
							<div class="col-md-12">
									<span class="studentsLoadingMessage" style="display: none;">
										<i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em>
									</span>

								<script>
									jQuery('.studentsLoadingMessage').show();
									jQuery(function($){
										$('.studentsLoadingMessage').hide();
									})
								</script>

								<div class="pull-right" style="display: none;">
									<span class="checkAllMessage">
										Record all attendance
										<#assign popoverContent>
											<p><i class="fa-minus fa fa-fw"></i> Not recorded</p>
											<p><i class="fa-times fa fa-fw unauthorised"></i> Missed (unauthorised)</p>
											<p><i class="fa-times-circle-o fa fa-fw authorised"></i> Missed (authorised)</p>
											<p><i class="fa-check fa fa-fw attended"></i> Attended</p>
										</#assign>
										<a class="use-popover"
										   data-title="Key"
										   data-placement="bottom"
										   data-container="body"
										   data-content='${popoverContent}'
										   data-html="true">
											<i class="fa fa-question-circle"></i>
										</a>
									</span>
									<div class="btn-group">
										<button
												type="button"
												class="btn btn-default use-tooltip"
												title="Set all to 'Not recorded'"
												data-html="true"
												data-container="body"
												>
											<i class="fa-minus fa fa-fw"></i>
										</button>
										<button
												type="button"
												class="btn btn-default btn-unauthorised use-tooltip"
												title="Set all to 'Missed (unauthorised)'"
												data-html="true"
												data-container="body"
												>
											<i class="fa-times fa fa-fw"></i>
										</button>
										<button
												type="button"
												class="btn btn-default btn-authorised use-tooltip"
												title="Set all to 'Missed (authorised)'"
												data-html="true"
												data-container="body"
												>
											<i class="fa-times-circle-o fa fa-fw"></i>
										</button>
										<button
												type="button"
												class="btn btn-default btn-attended use-tooltip"
												title="Set all to 'Attended'"
												data-html="true"
												data-container="body"
												>
											<i class="fa-check fa fa-fw"></i>
										</button>
									</div>
									<#if features.attendanceMonitoringNote>
										<#assign students = command.checkpointMap?keys?sort_by("lastName") />
										<a class="btn btn-default use-tooltip attendance-note bulk-attendance-note"
										   title="Add an attendance note for all students"
										   href="<@routes.attendance.bulkNoteEdit academicYear command.templatePoint students />">
											<i class="fa fa-pencil-square-o"></i>
										</a>
									</#if>
									<i class="fa fa-fw"></i>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>

			<form id="recordAttendance" action="" method="post" class="dirty-check">
				<div class="striped-section expanded no-title">
					<div class="striped-section-contents attendees">
						<script>
							AttendanceRecording.bindButtonGroupHandler();
						</script>
						<input type="hidden" name="returnTo" value="${returnTo}"/>

						<#list command.checkpointMap?keys?sort_by("lastName") as student>
							<#list mapGet(command.checkpointMap, student)?keys as point>
								<div class="row item-info">
									<div class="col-md-8">
										<#if numberOfStudents <= 50>
											<@fmt.member_photo student "tinythumbnail" true />
										</#if>
										${student.fullName}&nbsp;<@pl.profile_link student.universityId />
										<#if mapGet(command.hasReportedMap, student)>
											<br/><span class="hint">A checkpoint cannot be set as this student's attendance data has already been submitted for this period.</span>
										</#if>
										<@spring.bind path="command.checkpointMap[${student.universityId}][${point.id}]">
											<#if status.error>
												<div class="text-error"><@f.errors path="command.checkpointMap[${student.universityId}][${point.id}]" cssClass="error"/></div>
											</#if>
										</@spring.bind>
									</div>
									<div class="col-md-4">
										<div class="pull-right">
											<div class="visible-print-block">
												<@attendance_macros.checkpointLabel
													department=department
													student=student
													checkpoint=(mapGet(mapGet(command.studentPointCheckpointMap, student), point))!""
													note=(mapGet(mapGet(command.attendanceNoteMap, student), point))!""
													point=point
												/>
											</div>
											<#if mapGet(command.hasReportedMap, student)>
												<@attendance_macros.checkpointLabel
													department=department
													student=student
													checkpoint=(mapGet(mapGet(command.studentPointCheckpointMap, student), point))!""
													note=(mapGet(mapGet(command.attendanceNoteMap, student), point))!""
													point=point
												/>
											<#else>
												<@attendance_macros.checkpointSelect
													id="checkpointMap-${student.universityId}-${point.id}"
													name="checkpointMap[${student.universityId}][${point.id}]"
													department=department
													checkpoint=(mapGet(mapGet(command.studentPointCheckpointMap, student), point))!""
													note=(mapGet(mapGet(command.attendanceNoteMap, student), point))!""
													student=student
													point=point
												/>
											</#if>

											<#if mapGet(mapGet(command.attendanceNoteMap, student), point)??>
												<#assign note = mapGet(mapGet(command.attendanceNoteMap, student), point) />
												<#if note.hasContent>
													<a id="attendanceNote-${student.universityId}-${point.id}" class="btn btn-default use-tooltip attendance-note edit" title="Edit attendance note" href="<@routes.attendance.noteEdit academicYear student point />?dt=${.now?string('iso')}">
														<i class="fa fa-pencil-square-o attendance-note-icon"></i>
													</a>
												<#else>
													<a id="attendanceNote-${student.universityId}-${point.id}" class="btn btn-default use-tooltip attendance-note" title="Add attendance note" href="<@routes.attendance.noteEdit academicYear student point />">
														<i class="fa fa-pencil-square-o attendance-note-icon"></i>
													</a>
												</#if>
											<#else>
												<a id="attendanceNote-${student.universityId}-${point.id}" class="btn btn-default use-tooltip attendance-note" title="Add attendance note" href="<@routes.attendance.noteEdit academicYear student point />">
													<i class="fa fa-pencil-square-o attendance-note-icon"></i>
												</a>
											</#if>

											<#if point.pointType.dbValue == "meeting">
												<a class="meetings" title="Meetings with this student" href="<@routes.attendance.profileMeetings student academicYear point />"><i class="fa fa-fw fa-info-circle"></i></a>
											<#elseif point.pointType.dbValue == "smallGroup">
												<a class="small-groups" title="Small group teaching events for this student" href="<@routes.attendance.profileGroups student academicYear point />"><i class="fa fa-fw fa-info-circle"></i></a>
											<#else>
												<i class="fa fa-fw"></i>
											</#if>
										</div>
									</div>
									<script>
										AttendanceRecording.createButtonGroup('#checkpointMap-${student.universityId}-${point.id}');
									</script>
								</div>
							</#list>
						</#list>
					</div>
				</div>
				<div class="fix-footer submit-buttons">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
						<a class="btn btn-default dirty-check-ignore" href="${returnTo}">Cancel</a>
					</div>
					<#if command.checkpointMap?keys?size < 500>
						<@fmt.bulk_email_students students=command.checkpointMap?keys />
					</#if>
				</div>
			</form>
		</div>

	</div>

</#if>

<div id="meetings-modal" class="modal fade" style="display:none;">
	<@modal.header>
		<h3>Meetings</h3>
	</@modal.header>
	<@modal.body></@modal.body>
</div>
<div id="small-groups-modal" class="modal fade" style="display:none;"></div>
<div id="upload-attendance-modal" class="modal fade" style="display:none;"></div>


</#escape>