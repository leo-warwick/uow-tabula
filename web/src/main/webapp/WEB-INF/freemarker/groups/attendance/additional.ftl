<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<form action="" method="post">
	<@modal.wrapper>
		<@modal.header>
			<h6 class="modal-title">Add ${student.fullName} to <#if event.title?has_content>${event.title}, </#if>${event.group.groupSet.name}, ${event.group.name}</h6>
		</@modal.header>
		<@modal.body>
			<p>${student.fullName} will be manually added to the register for this occurrence of the event only.</p>

			<p>If the student has been permanently moved into this group, the allocation should be updated rather than
			using this function.</p>

			<p>If the student is attending this occurrence instead of another, select it from the dropdown below
			to automatically authorise absence from the other group:</p>

			<input type="hidden" name="action" value="refresh" />
			<input type="hidden" name="additionalStudent" value="${student.universityId}" />
			<input type="hidden" name="replacedEvent">
			<input type="hidden" name="replacedWeek">

			<select id="replacementEventAndWeek" class="form-control">
				<option></option>
				<#list possibleReplacements as replacement>
					<option value="${replacement.event.id}_${replacement.week}" data-event="${replacement.event.id}" data-week="${replacement.week}">
						<#if replacement.event.title?has_content>${replacement.event.title},</#if>
						${replacement.event.group.groupSet.name}, ${replacement.event.group.name}:
						${replacement.event.day.name} <@fmt.time replacement.event.startTime /> - <@fmt.time replacement.event.endTime />, <@fmt.singleWeekFormat week=replacement.week academicYear=replacement.event.group.groupSet.academicYear dept=replacement.event.group.groupSet.module.adminDepartment />
						<#if replacement.attendance?? && ((replacement.attendance.state.description)!)?has_content>
							(currently ${replacement.attendance.state.description})
						</#if>
					</option>
				</#list>
			</select>
		</@modal.body>
		<@modal.footer>
			<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Adding&hellip;">
				Add student
			</button>
			<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
		</@modal.footer>
	</@modal.wrapper>
</form>

</#escape>