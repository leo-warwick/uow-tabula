<#escape x as x?html>
	<input type="hidden" name="action" value="submit" id="action-submit">

	<@bs3form.labelled_form_group path="format" labelText="Type">
		<@f.select path="format" id="format" cssClass="form-control">
			<@f.options items=allFormats itemLabel="description" itemValue="code" />
		</@f.select>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
		<p class="form-control-static">${status.actualValue.label} <span class="very-subtle">(can't be changed)</span></p>
		<@f.hidden path="academicYear" id="academicYear" value="${status.actualValue.label}" cssClass="form-control-static" />
	</@bs3form.labelled_form_group>

	<#assign label>
		Set name
		<@fmt.help_popover id="name" content="Give this set of groups a name to distinguish it from any other sets - eg. UG Year 1 seminars and UG Year 2 seminars" />
	</#assign>
	<@bs3form.labelled_form_group path="name" labelText="Set name">
		<@f.input path="name" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<#if features.smallGroupTeachingStudentSignUp || features.smallGroupTeachingRandomAllocation>
		<@bs3form.labelled_form_group path="allocationMethod" labelText="Allocation method">
			<@bs3form.radio>
				<@f.radiobutton path="allocationMethod" value="Manual" />
				Manual allocation
				<@fmt.help_popover id="allocationMethod-manual" content="Allocate students by drag and drop or spreadsheet upload" />
			</@bs3form.radio>
			<#if features.smallGroupTeachingStudentSignUp>
				<@bs3form.radio>
					<@f.radiobutton path="allocationMethod" value="StudentSignUp" selector=".student-sign-up-options" />
					Self sign-up
					<@fmt.help_popover id="allocationMethod-ssu" content="Allow students to sign up for groups (you can edit group allocation later" />
				</@bs3form.radio>
			</#if>
			<#if features.smallGroupCrossModules>
				<#if departmentSmallGroupSets?size gt 0>
					<@bs3form.radio>
						<@f.radiobutton path="allocationMethod" value="Linked" selector=".linked-options" />
						Linked to
						<span class="linked-options" style="display: inline-block;">
							<@f.select path="linkedDepartmentSmallGroupSet" id="linkedDepartmentSmallGroupSet" cssClass="form-control">
								<@f.options items=departmentSmallGroupSets itemLabel="name" itemValue="id" />
							</@f.select>
						</span>
						<@fmt.help_popover id="allocationMethod-linked" content="Link these groups to a reusable set of small groups" />
					</@bs3form.radio>
				<#else>
					<@bs3form.radio>
						<span class="disabled use-tooltip" title="There are no reusable small group sets to link to">
							<input type="radio" disabled="disabled">
							Linked
							<@fmt.help_popover id="allocationMethod-linked" content="Link these groups to a reusable set of small groups" />
						</span>
					</@bs3form.radio>
				</#if>
			</#if>
			<#if features.smallGroupTeachingRandomAllocation>
				<@bs3form.radio>
					<@f.radiobutton path="allocationMethod" value="Random" />
					Randomly allocate students to groups
					<@fmt.help_popover id="allocationMethod-random" content="Students in the allocation list are randomly assigned to groups. Administrators can still assign students to groups. There may be a delay between students being added to the allocation list and being allocated to a group." />
				</@bs3form.radio>
			</#if>
		</@bs3form.labelled_form_group>
	</#if>

	<#if features.smallGroupTeachingStudentSignUp>
		<@bs3form.checkbox path="studentsCanSeeTutorName">
			<@f.checkbox path="studentsCanSeeTutorName" id="studentsCanSeeTutorName" />
			Students can see tutor name
			<@fmt.help_popover id="studentsCanSeeTutorName" content="Students can see tutor names when deciding which group to sign up for" />
		</@bs3form.checkbox>
	</#if>

	<#if features.smallGroupTeachingStudentSignUp>
		<@bs3form.checkbox path="studentsCanSeeOtherMembers">
			<@f.checkbox path="studentsCanSeeOtherMembers" id="studentsCanSeeOtherMembers" />
			Students can see student names
			<@fmt.help_popover id="studentsCanSeeTutorName" content="Students can see the names of any other students in the group when deciding which group to sign up for" />
		</@bs3form.checkbox>
	</#if>

	<#if features.smallGroupTeachingSelfGroupSwitching>
		<div class="student-sign-up-options">
			<@bs3form.checkbox path="allowSelfGroupSwitching">
				<@f.checkbox path="allowSelfGroupSwitching" id="allowSelfGroupSwitching" />
				Allow students to switch groups
				<@fmt.help_popover id="allowSelfGroupSwitching" content="When self sign up is enabled students will be able to switch groups." />
			</@bs3form.checkbox>
		</div>
	</#if>

	<#if features.smallGroupTeachingLectures>
		<@bs3form.checkbox path="collectAttendance">
			<@f.checkbox path="collectAttendance" id="collectAttendance" />
			Collect attendance at events
		</@bs3form.checkbox>
	</#if>

	<script type="text/javascript">
		jQuery(function($) {
			// Set up radios to show/hide self-sign up options fields.
			$("input:radio[name='allocationMethod']").radioControlled({mode: 'hidden'});
		});
	</script>
</#escape>
