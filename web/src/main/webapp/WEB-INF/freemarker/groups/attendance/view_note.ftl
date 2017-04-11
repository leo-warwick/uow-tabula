<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

	<@modal.wrapper enabled=isModal>
		<@modal.header enabled=isModal>
			<h3 class="modal-title">Attendance note for ${attendanceNote.student.fullName}</h3>
		</@modal.header>

		<@modal.body enabled=isModal>
			<p>
				<#if attendance??>
					${attendance.state.description}:
				<#else>
					Not recorded:
				</#if>
				<#if attendanceNote.occurrence.event.title?has_content>${attendanceNote.occurrence.event.title},</#if>
				${attendanceNote.occurrence.event.group.groupSet.name},
				${attendanceNote.occurrence.event.group.name},
				${attendanceNote.occurrence.event.day.name} <@fmt.time attendanceNote.occurrence.event.startTime /> - <@fmt.time attendanceNote.occurrence.event.endTime />,
				<@fmt.singleWeekFormat week=attendanceNote.occurrence.week academicYear=attendanceNote.occurrence.event.group.groupSet.academicYear dept=attendanceNote.occurrence.event.group.groupSet.module.adminDepartment />
			</p>

			<p>Absence type: ${attendanceNote.absenceType.description}</p>

			<#if attendanceNote.note?has_content>
				<#noescape>${attendanceNote.escapedNote}</#noescape>
			</#if>

			<#if attendanceNote.attachment?has_content>
				<p>
					<@fmt.download_link
						filePath="/profiles/note/${attendanceNote.student.universityId}/${attendanceNote.occurrence.id}/attachment/${attendanceNote.attachment.name}"
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
		</@modal.body>
		<#if isModal>
			<@modal.footer>
				<span class="submit-buttons">
					<#if can.do("SmallGroupEvents.Register", attendanceNote.student) >
						<a href="<@routes.groups.editNote attendanceNote.student attendanceNote.occurrence />" class="btn btn-primary spinnable spinner-auto">
							Edit
						</a>
					</#if>
					<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Close</button>
				</span>
			</@modal.footer>

		<#else>
			<#if can.do("SmallGroupEvents.Register", attendanceNote.student) >
				<a href="<@routes.groups.editNote attendanceNote.student attendanceNote.occurrence />" class="btn btn-primary spinnable spinner-auto">Edit</a>
			</#if>
		</#if>
	</@modal.wrapper>

</#escape>