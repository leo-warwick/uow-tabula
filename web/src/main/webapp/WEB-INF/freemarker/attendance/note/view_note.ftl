<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<#import "../attendance_macros.ftl" as attendance_macros />

<@modal.wrapper isModal>
	<#if isModal>
		<@modal.header>
			<h3 class="modal-title">Attendance note for ${attendanceNote.student.fullName}</h3>
		</@modal.header>
	<#else>
		<h2>Attendance note for ${attendanceNote.student.fullName}</h2>
	</#if>

	<#assign content>
		<p>
			<#if checkpoint??>
				${checkpoint.state.description}:
			<#else>
				Unrecorded:
			</#if>
			${attendanceNote.point.name}
			<#if point.scheme.pointStyle.dbValue == "week">
				(<@fmt.wholeWeekDateFormat
				point.startWeek
				point.endWeek
				point.scheme.academicYear
				/>)
			<#else>
				(<@fmt.interval point.startDate point.endDate />)
			</#if>
		</p>

		<#if checkpoint??>
			<@attendance_macros.checkpointDescription department=checkpoint.point.scheme.department checkpoint=checkpoint point=point student=attendanceNote.student/>
		</#if>

		<p>Absence type: ${attendanceNote.absenceType.description}</p>

		<#if attendanceNote.note?has_content>
			<#noescape>${attendanceNote.escapedNote}</#noescape>
		</#if>

		<#if attendanceNote.attachment?has_content>
			<p>
				<@fmt.download_link
					filePath="/attendance/note/${academicYear}/${attendanceNote.student.universityId}/${attendanceNote.point.id}/attachment/${attendanceNote.attachment.name}"
					mimeType=attendanceNote.attachment.mimeType
					title="Download file ${attendanceNote.attachment.name}"
					text="Download ${attendanceNote.attachment.name}"
				/>
			</p>
		</#if>

		<p class="hint">
			Attendance note updated
			<#if updatedBy?? && updatedBy?has_content>by ${updatedBy}, </#if>
			<#noescape>${updatedDate}</#noescape>
		</p>
	</#assign>

	<#if isModal>
		<@modal.body>
			<#noescape>${content}</#noescape>
		</@modal.body>

		<@modal.footer>
			<span class="submit-buttons">
				<#if can.do("MonitoringPoints.Record", attendanceNote.student) >
					<a href="<@routes.attendance.noteEdit academicYear attendanceNote.student attendanceNote.point />" class="btn btn-primary spinnable spinner-auto">
						Edit
					</a>
				</#if>
				<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Close</button>
			</span>
		</@modal.footer>

	<#else>

		<#noescape>${content}</#noescape>

		<#if can.do("MonitoringPoints.Record", attendanceNote.student) >
			<a href="<@routes.attendance.noteEdit academicYear attendanceNote.student attendanceNote.point />" class="btn btn-primary spinnable spinner-auto">Edit</a>
		</#if>

	</#if>

</@modal.wrapper>
</#escape>