<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

	<@modal.wrapper isModal!false "modal-lg">

		<#if success??>
			<#assign isAdd = !command.attendanceNote.note?has_content && !command.attendanceNote.attachment?has_content && !command.attendanceNote.absenceType?has_content />
			<div
				class="attendance-note-success"
				data-linkid="#attendanceNote-${student.universityId}-${command.point.id}"
				data-state="<#if isAdd>Add<#else>Edit</#if>"
			></div>
		</#if>

		<#assign heading>
			<#if command.isNew()>
				<h3 class="modal-title">Create attendance note for ${student.fullName}</h3>
			<#else>
				<h3 class="modal-title">Edit attendance note for ${student.fullName}</h3>
			</#if>
		</#assign>

		<#if isModal!false>
			<@modal.header>
				<#noescape>${heading}</#noescape>
			</@modal.header>
		<#elseif !isIframe>
			<#noescape>${heading}</#noescape>
		</#if>

		<#if isModal!false>
			<@modal.body />

			<@modal.footer>
				<form class="double-submit-protection">
					<span class="submit-buttons">
						<button class="btn btn-primary spinnable" type="submit" name="submit" data-loading-text="Saving&hellip;">
							Save
						</button>
						<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
					</span>
				</form>
			</@modal.footer>
		<#else>
			<#if command.isNew() && command.customState?? && command.customState.description == "Missed (authorised)">
				<div class="alert alert-info">
					Points marked as Missed (authorised) must have an attendance note explaining why the absence was authorised.
				</div>
			</#if>
			<p>
				<#if command.customState??>
					${command.customState.description}:
				<#elseif command.checkpoint??>
					${command.checkpoint.state.description}:
				<#else>
					Not recorded:
				</#if>

				${command.point.name}

				${command.attendanceNote.point.name}
				<#if point.scheme.pointStyle.dbValue == "week">
					(<@fmt.wholeWeekDateFormat
						point.startWeek
						point.endWeek
						point.scheme.academicYear
					/>)
				<#else>
					(<@fmt.interval point.startDate point.endDate />)
				</#if>

				<#if command.customState?? && command.checkpoint?? && command.customState.dbValue != command.checkpoint.state.dbValue>
					<small class="subtle help-block">
						This attendance has not yet been saved.
					</small>
				</#if>
			</p>
			<#if checkpoint??>
				<@attendance_macros.checkpointDescription department=checkpoint.point.scheme.department checkpoint=checkpoint point=point student=attendanceNote.student/>
			</#if>

			<@f.form id="attendance-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="double-submit-protection">

				<#include "_shared_fields.ftl" />

				<#if !isIframe>

					<div class="form-actions">
						<div class="pull-right">
							<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
							<a class="btn" href="${returnTo}">Cancel</a>
						</div>
					</div>

				</#if>

			</@f.form>

		</#if>

	</@modal.wrapper>

</#escape>