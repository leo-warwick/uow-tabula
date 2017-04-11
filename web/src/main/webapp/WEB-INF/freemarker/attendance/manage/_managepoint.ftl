<@bs3form.labelled_form_group path="name" labelText="Name">
	<@f.input path="name" cssClass="form-control"/>
</@bs3form.labelled_form_group>

<div class="dateTimePair">
	<#if command.pointStyle.dbValue == "week">

		<#assign label>
			Start
			<@fmt.help_popover id="startWeek" content="You cannot mark a point as attended or missed (unauthorised) before its start date" />
		</#assign>
		<@bs3form.labelled_form_group path="startWeek" labelText="${label}">
			<@f.select path="startWeek" cssClass="startDateTime selectOffset form-control">
				<#list 1..52 as week>
					<@f.option value="${week}"><@fmt.monitoringPointWeeksFormat week week command.academicYear command.department /></@f.option>
				</#list>
			</@f.select>
		</@bs3form.labelled_form_group>

		<#assign label>
			End
			<@fmt.help_popover id="endWeek" content="A warning will appear for unrecorded attendance after its end date" />
		</#assign>
		<@bs3form.labelled_form_group path="endWeek" labelText="${label}">
			<@f.select path="endWeek" cssClass="endDateTime selectOffset form-control">
				<#list 1..52 as week>
					<@f.option value="${week}"><@fmt.monitoringPointWeeksFormat week week command.academicYear command.department /></@f.option>
				</#list>
			</@f.select>
		</@bs3form.labelled_form_group>

	<#else>

		<#assign label>
			Start
			<@fmt.help_popover id="startDate" content="You cannot mark a point as attended or missed (unauthorised) before its start date" />
		</#assign>
		<@bs3form.labelled_form_group path="startDate" labelText="${label}">
			<@f.input type="text" path="startDate" cssClass="form-control date-picker startDateTime" placeholder="Pick the start date" />
			<input class="endoffset" type="hidden" data-end-offset="0" />
		</@bs3form.labelled_form_group>

		<#assign label>
			End
			<@fmt.help_popover id="endDate" content="A warning will appear for unrecorded attendance after its end date" />
		</#assign>
		<@bs3form.labelled_form_group path="endDate" labelText="${label}">
			<@f.input type="text" path="endDate" cssClass="form-control date-picker endDateTime" placeholder="Pick the end date" />
		</@bs3form.labelled_form_group>

	</#if>
</div>

<@bs3form.labelled_form_group path="pointType" labelText="Type">
	<@bs3form.radio>
		<@f.radiobutton path="pointType" value="standard" />
		Standard
		<@fmt.help_popover id="pointType-standard" content="This is a basic monitoring point which will create a register for you to mark monitored attendance" />
	</@bs3form.radio>
	<@bs3form.radio>
		<@f.radiobutton path="pointType" value="meeting" />
		Meeting
		<@fmt.help_popover id="pointType-meeting" content="This monitoring point will be marked as 'attended' if there is a record in Tabula of a meeting taking place between the start and end dates" />
	</@bs3form.radio>
	<#if features.attendanceMonitoringSmallGroupPointType>
		<@bs3form.radio>
			<@f.radiobutton path="pointType" value="smallGroup" />
			Teaching event
			<@fmt.help_popover id="pointType-smallGroup" content="This monitoring point will be marked as 'attended' if the student attends a small group teaching event recorded in Tabula between the start and end dates" />
		</@bs3form.radio>
	</#if>
	<#if features.attendanceMonitoringAssignmentSubmissionPointType>
		<@bs3form.radio>
			<@f.radiobutton path="pointType" value="assignmentSubmission" />
			Coursework
			<@fmt.help_popover id="pointType-assignmentSubmission" content="This monitoring point will be marked as 'attended' if the student submits coursework via Tabula to an assignment with a close date between the start and end dates" />
		</@bs3form.radio>
	</#if>
</@bs3form.labelled_form_group>

<#if features.attendanceMonitoringMeetingPointType>

	<#assign meetingRelationshipsStrings = extractParam(command.meetingRelationships, 'urlPart') />
	<#assign meetingFormatsStrings = extractParam(command.meetingFormats, 'code') />
	<div class="pointTypeOption meeting row" <#if ((command.pointType.dbValue)!'null') != 'meeting'>style="display:none"</#if>>
		<div class="col-md-5">
			<@bs3form.labelled_form_group path="meetingRelationships" labelText="Meeting with">
				<#list command.department.displayedStudentRelationshipTypes as relationship>
					<@bs3form.checkbox>
						<input type="checkbox" name="meetingRelationships" id="meetingRelationships-${relationship.urlPart}" value="${relationship.urlPart}" <#if meetingRelationshipsStrings?seq_contains(relationship.urlPart)>checked</#if> />
						${relationship.agentRole?capitalize}
						<@fmt.help_popover id="meetingRelationships-${relationship.urlPart}" content="This monitoring point will be marked as 'attended' when a meeting record is created or approved by the student's ${relationship.agentRole?capitalize}" />
					</@bs3form.checkbox>
				</#list>
			</@bs3form.labelled_form_group>

			<#assign label>
				Number of meetings
				<@fmt.help_popover id="meetingQuantity" content="The student must have this many meetings between the start and end dates in order to meet this monitoring point" />
			</#assign>
			<@bs3form.labelled_form_group path="meetingQuantity" labelText="${label}">
				<@f.input path="meetingQuantity" cssClass="form-control"/>
			</@bs3form.labelled_form_group>
		</div>

		<div class="col-md-6">
			<#assign label>
				Meeting formats
				<@fmt.help_popover id="meetingFormats" content="Only selected meeting formats will count towards this monitoring point" />
			</#assign>
			<@bs3form.labelled_form_group path="meetingFormats" labelText="${label}">
				<#list allMeetingFormats as format>
					<@bs3form.checkbox>
						<input type="checkbox" name="meetingFormats" id="meetingFormats-${format.code}" value="${format.code}" <#if meetingFormatsStrings?seq_contains(format.code)>checked</#if> />
						${format.description}
					</@bs3form.checkbox>
				</#list>
			</@bs3form.labelled_form_group>
		</div>
	</div>

</#if>

<#if features.attendanceMonitoringSmallGroupPointType>

<div class="pointTypeOption smallGroup" <#if ((command.pointType.dbValue)!'null') != 'smallGroup'>style="display:none"</#if>>

	<div class="module-choice">
		<@bs3form.labelled_form_group path="smallGroupEventModules" labelText="Modules">
			<@bs3form.radio>
				<input type="radio" <#if (command.anySmallGroupEventModules)>checked </#if> value="true" name="isAnySmallGroupEventModules"/>
				Any
				<@fmt.help_popover id="isAnySmallGroupEventModules" content="Attendance at any module recorded in Tabula will count towards this monitoring point" />
			</@bs3form.radio>

			<@bs3form.radio>
				<input class="specific" type="radio" <#if (!command.anySmallGroupEventModules)>checked </#if> value="false" name="isAnySmallGroupEventModules"/>
				Specific
				<@fmt.help_popover id="isAnySmallGroupEventModules" content="Attendance at any of the specified modules recorded in Tabula will count towards this monitoring point" />
				<div class="module-search input-group">
					<input class="module-search-query smallGroup form-control" type="text" value="" placeholder="Search for a module" />
					<span class="input-group-addon"><i class="fa fa-search"></i></span>
				</div>
				<div class="modules-list">
					<input type="hidden" name="_smallGroupEventModules" value="false" />
					<ol>
						<#list command.smallGroupEventModules![] as module>
							<li>
								<input type="hidden" name="smallGroupEventModules" value="${module.id}" />
								<#if command.moduleHasSmallGroups(module)><i class="fa fa-fw"></i><#else><i class="fa fa-fw fa-exclamation-circle" title="This module has no small groups set up in Tabula"></i></#if><span title="<@fmt.module_name module false />"><@fmt.module_name module false /></span> <button class="btn btn-danger btn-xs">Remove</button>
							</li>
						</#list>

					</ol>

				</div>
			</@bs3form.radio>

		</@bs3form.labelled_form_group>

	</div>

	<#assign label>
		Number of events
		<@fmt.help_popover id="smallGroupEventQuantity" content="The student must have attended this many events for any of the specified modules between the start and end dates in order to meet this monitoring point" />
	</#assign>
	<@bs3form.labelled_form_group path="smallGroupEventQuantityAll" labelText="${label}">
		<input class="form-control" type="text" <#if (command.smallGroupEventQuantity?? && command.smallGroupEventQuantity > 0)>value="${command.smallGroupEventQuantity}"</#if> name="smallGroupEventQuantity" />
	</@bs3form.labelled_form_group>

</div>

</#if>

<#if features.attendanceMonitoringAssignmentSubmissionPointType>

<div class="pointTypeOption assignmentSubmission" <#if ((command.pointType.dbValue)!'null') != 'assignmentSubmission'>style="display:none"</#if>>

	<@bs3form.labelled_form_group path="assignmentSubmissionType" labelText="Assignments">
		<@bs3form.radio>
			<@f.radiobutton path="assignmentSubmissionType" value="any" />
			Any module or assignment
			<@fmt.help_popover id="isSpecificAssignmentsFalse" content="Submission to any assignment for any module with a close date between the start and end dates will count towards this monitoring point" />
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="assignmentSubmissionType" value="modules" />
			By module
			<@fmt.help_popover id="isSpecificAssignmentsFalse" content="Submission to any assignment for the specified modules with a close date between the start and end dates will count towards this monitoring point" />
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="assignmentSubmissionType" value="assignments" />
			Specific assignments
			<@fmt.help_popover id="isSpecificAssignmentsTrue" content="Submissions to the specified assignments that have a close date between the start and end dates will count towards this monitoring point" />
		</@bs3form.radio>
	</@bs3form.labelled_form_group>

	<div class="assignmentSubmissionType-any" <#if command.assignmentSubmissionType != 'any'>style="display:none"</#if>>
		<#assign label>
			Number of assignments
			<@fmt.help_popover id="assignmentSubmissionTypeAnyQuantity" content="The student must submit coursework to this many assignments for any module with a close date between the start and end dates in order to meet this monitoring point" />
		</#assign>
		<@bs3form.labelled_form_group path="assignmentSubmissionTypeAnyQuantity" labelText="${label}">
			<input class="form-control" type="text" <#if (command.assignmentSubmissionTypeAnyQuantity?? && command.assignmentSubmissionTypeAnyQuantity > 0)>value="${command.assignmentSubmissionTypeAnyQuantity}"</#if> name="assignmentSubmissionTypeAnyQuantity" />
		</@bs3form.labelled_form_group>
	</div>

	<div class="assignmentSubmissionType-modules" <#if command.assignmentSubmissionType != 'modules'>style="display:none"</#if>>
		<div class="module-choice">
			<@bs3form.labelled_form_group path="assignmentSubmissionModules" labelText="">
				<div class="module-search input-group">
					<input class="module-search-query assignment form-control" type="text" value="" placeholder="Search for a module"/>
					<span class="input-group-addon"><i class="fa fa-search"></i></span>
				</div>
				<div class="modules-list">
					<input type="hidden" name="_assignmentSubmissionModules" value="false" />
					<ol>
						<#list command.assignmentSubmissionModules![] as module>
							<li>
								<input type="hidden" name="assignmentSubmissionModules" value="${module.id}" />
								<#if command.moduleHasAssignments(module)><i class="fa fa-fw"></i><#else><i class="fa fa-fw fa-exclamation-circle" title="This module has no assignments set up in Tabula"></i></#if><span title="<@fmt.module_name module false />"><@fmt.module_name module false /></span><button class="btn btn-danger btn-xs"><i class="fa fa-times"></i></button>
							</li>
						</#list>
					</ol>
				</div>
			</@bs3form.labelled_form_group>
		</div>

		<#assign label>
			Number of assignments
			<@fmt.help_popover id="assignmentSubmissionTypeModulesQuantity" content="The student must submit coursework to this many assignments for any of the specified modules with a close date between the start and end dates in order to meet this monitoring point" />
		</#assign>
		<@bs3form.labelled_form_group path="assignmentSubmissionTypeModulesQuantity" labelText="${label}">
			<input class="form-control" type="text" <#if (command.assignmentSubmissionTypeModulesQuantity?? && command.assignmentSubmissionTypeModulesQuantity > 0)>value="${command.assignmentSubmissionTypeModulesQuantity}"</#if> name="assignmentSubmissionTypeModulesQuantity" />
		</@bs3form.labelled_form_group>
	</div>

	<div class="assignmentSubmissionType-assignments" <#if command.assignmentSubmissionType != 'assignments'>style="display:none"</#if>>
		<div class="assignment-choice">
			<@bs3form.labelled_form_group path="assignmentSubmissionAssignments" labelText="">
				<div class="assignment-search input-group">
					<input class="assignment-search-query form-control" type="text" value="" placeholder="Search for an assignment"/>
					<span class="input-group-addon"><i class="fa fa-search"></i></span>
				</div>
				<div class="assignments-list">
					<input type="hidden" name="_assignmentSubmissionAssignments" value="false" />
					<ol>
						<#list command.assignmentSubmissionAssignments![] as assignment>
							<li>
								<input type="hidden" name="assignmentSubmissionAssignments" value="${assignment.id}" />
								<span title="<@fmt.assignment_name assignment false />"><@fmt.assignment_name assignment false /></span><button class="btn btn-danger btn-xs"><i class="fa fa-times"></i></button>
							</li>
						</#list>
					</ol>
				</div>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group path="assignmentSubmissionDisjunction" labelText="">
				<@bs3form.radio>
					<input name="isAssignmentSubmissionDisjunction" type="radio" value="true" <#if command.isAssignmentSubmissionDisjunction()>checked</#if>>
					Any
					<@fmt.help_popover id="isAssignmentSubmissionDisjunctionTrue" content="The student must submit coursework to any specified assignment with a close date between the start and end dates in order to meet this monitoring point" />
				</@bs3form.radio>
				<@bs3form.radio>
					<input name="isAssignmentSubmissionDisjunction" type="radio" value="false" <#if !command.isAssignmentSubmissionDisjunction()>checked</#if>>
					All
					<@fmt.help_popover id="isAssignmentSubmissionDisjunctionFalse" content="The student must submit coursework to all of the specified assignments with a close date between the start and end dates in order to meet this monitoring point" />
				</@bs3form.radio>
			</@bs3form.labelled_form_group>
		</div>
	</div>

</div>

<@spring.bind path="command">
	<#if status.error && status.errorCodes?seq_contains("attendanceMonitoringPoint.overlaps")>
		<div class="alert alert-info">
			There is already a monitoring point that will be met by these criteria.
			If you create this point, it is possible that the same event will mark two points as 'attended'.
			We recommend that you change the settings for this point.
		</div>
		<#assign hasOverlap = true />
	</#if>
</@spring.bind>

<script>
	jQuery(function($) {
		// Show relavant extra options when changing assignment type
		var $specificAssignmentInput = $('form input[name=assignmentSubmissionType]');
		if ($specificAssignmentInput.length > 0) {
			var showAssignmentOptions = function() {
				var value = $('form input[name=assignmentSubmissionType]:checked').val();
				$('.pointTypeOption.assignmentSubmission [class^=assignmentSubmissionType-]').hide();
				$('.pointTypeOption.assignmentSubmission .assignmentSubmissionType-' + value).show();

			};
			$specificAssignmentInput.on('click', showAssignmentOptions);
			showAssignmentOptions();
		}

		// Show relavant extra options when changing point type
		var $pointTypeInput = $('form input[name=pointType]');
		if ($pointTypeInput.length > 0) {
			var showOptions = function() {
				var value = $('form input[name=pointType]:checked').val();
				$('.pointTypeOption').hide();
				if (value != undefined && value.length > 0) {
					$('.pointTypeOption.' + value).show();
				}
			};
			$pointTypeInput.on('click', showOptions);
			showOptions();
		}

		Attendance.bindModulePickers();
		Attendance.bindAssignmentPickers();
	});
</script>

</#if>