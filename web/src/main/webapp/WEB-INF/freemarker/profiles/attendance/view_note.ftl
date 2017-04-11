<#import "*/modal_macros.ftl" as modal />
<#import "../../attendance/attendance_macros.ftl" as attendance_macros />

<#escape x as x?html>
	<@modal.wrapper enabled=isModal>
		<@modal.header enabled=isModal>
			<h3 class="modal-title">Attendance note for ${attendanceNote.student.fullName}</h3>
		</@modal.header>
		<@modal.body enabled=isModal>
			<#assign content>
				<p>
					<#if checkpoint??>
						${checkpoint.state.description}:
					<#else>
						Not recorded:
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
							filePath="/profiles/attendance/note/${attendanceNote.student.universityId}/${point.id}/attachment/${attendanceNote.attachment.name}"
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
			<#noescape>${content}</#noescape>
		</@modal.body>
		<#if isModal>
			<@modal.footer>
				<span class="submit-buttons">
					<#if can.do("MonitoringPoints.Record", attendanceNote.student) >
						<a href="<@routes.profiles.edit_monitoringpoint_attendance_note attendanceNote.student attendanceNote.point/>" class="btn btn-primary spinnable spinner-auto">
							Edit
						</a>
					</#if>
				</span>
			</@modal.footer>
		</#if>
	</@modal.wrapper>
</#escape>