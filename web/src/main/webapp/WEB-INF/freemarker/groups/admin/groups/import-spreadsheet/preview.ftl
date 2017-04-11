<#import "*/group_components.ftl" as components />

<#macro command_type_label commandType>
	<#if commandType == "Create">
		<span class="label label-success">${commandType}</span>
	<#elseif commandType == "Edit">
		<span class="label label-warning">${commandType}</span>
	<#elseif commandType == "Delete">
		<span class="label label-danger">${commandType}</span>
	<#else>
		<span class="label label-default">${commandType}</span>
	</#if>
</#macro>

<#macro set_command_details holder>

	<#assign setError><#compress>
		<@spring.bind path="command.*">
			<#if status.error>true</#if>
		</@spring.bind>
	</#compress></#assign>

	<div class="set-info striped-section collapsible">
		<div class="clearfix">
			<div class="section-title row ">
				<div class="col-md-6 icon-container">
					<#if setError == "true">
						<span class="label label-danger">Has errors</span>
					</#if>
					<@command_type_label holder.commandType />
					<span class="h6 colour-h6">
						${holder.command.module.code?upper_case} ${holder.command.format.description}s: "${holder.command.name}"
					</span>
				</div>
				<div class="col-md-2">
					${holder.command.allocationMethod.description}
				</div>
				<div class="col-md-4">
					<#if holder.command.allocationMethod.dbValue == "Linked" && holder.command.linkedDepartmentSmallGroupSet??>
						<i class="fa fa-link"></i>
						${holder.command.linkedDepartmentSmallGroupSet.name}
					</#if>
				</div>
			</div>

			<div class="striped-section-contents">
				<@f.errors path="command.*" cssClass="error form-errors text-danger" />

				<h4>Settings</h4>

				<ul class="fa-ul">
					<#if holder.command.studentsCanSeeTutorName>
						<li><i class="fa-li fa fa-check"></i> Students can see tutor name</li>
					<#else>
						<li><i class="fa-li fa fa-times"></i> Students cannot see tutor name</li>
					</#if>

					<#if holder.command.studentsCanSeeOtherMembers>
						<li><i class="fa-li fa fa-check"></i> Students can see names of other students</li>
					<#else>
						<li><i class="fa-li fa fa-times"></i> Students cannot see names of other students</li>
					</#if>

					<#if holder.command.allocationMethod.dbValue == "StudentSignUp">
						<#if holder.command.allowSelfGroupSwitching>
							<li><i class="fa-li fa fa-check"></i> Students can switch groups</li>
						<#else>
							<li><i class="fa-li fa fa-times"></i> Students cannot switch groups</li>
						</#if>
					</#if>

					<#if holder.command.collectAttendance>
						<li><i class="fa-li fa fa-check"></i> Collect attendance at events</li>
					<#else>
						<li><i class="fa-li fa fa-times"></i> Do not collect attendance at events</li>
					</#if>
				</ul>

				<h4>Groups</h4>

				<#list holder.modifyGroupCommands as gHolder>
					<@spring.nestedPath path="modifyGroupCommands[${gHolder_index}]">
						<@group_command_details gHolder />
					</@spring.nestedPath>
				</#list>

				<#list holder.deleteGroupCommands as deleteCommand>
					<@spring.nestedPath path="deleteGroupCommands[${deleteCommand_index}]">
						<@delete_group_command_details deleteCommand />
					</@spring.nestedPath>
				</#list>
			</div>
		</div>
	</div> <!-- module-info striped-section-->
</#macro>

<#macro group_command_details holder>
	<div class="item-info row">
		<div class="col-md-4">
			<@command_type_label holder.commandType />

			<span class="h4 colour-h4 name">
				<#if holder.commandType == "Delete">
					${holder.command.group.name}
				<#else>
					${holder.command.name}
				</#if>
			</span>
		</div>

		<div class="col-md-8">
			<@f.errors path="command.*" cssClass="error form-errors text-danger" />

			<h5>Events</h5>

			<ul class="list-unstyled">
				<#list holder.modifyEventCommands as eHolder>
					<@spring.nestedPath path="modifyEventCommands[${eHolder_index}]">
						<li><@event_command_details eHolder /></li>
					</@spring.nestedPath>
				</#list>

				<#list holder.deleteEventCommands as deleteCommand>
					<@spring.nestedPath path="deleteEventCommands[${deleteCommand_index}]">
						<li><@delete_event_command_details deleteCommand /></li>
					</@spring.nestedPath>
				</#list>
			</ul>
		</div>
	</div>
</#macro>

<#macro delete_group_command_details command>
	<div class="item-info row">
		<div class="col-md-4">
			<@command_type_label "Delete" />

			<span class="h4 colour-h4 name">
				${command.group.name}
			</span>
		</div>

		<div class="col-md-8">
			<@f.errors path="command.*" cssClass="error form-errors text-danger" />

			<h5>Events</h5>

			<ul class="list-unstyled">
				<#list command.group.events as event>
					<li>
						<@command_type_label "Delete" />
						<@components.eventShortDetails event />

						<#local popoverContent><@components.eventDetails event /></#local>
						<a class="use-popover"
						   data-html="true"
						   data-content="${popoverContent?html}"><i class="fa fa-question-circle"></i></a>
					</li>
				</#list>
			</ul>
		</div>
	</div>
</#macro>

<#macro event_command_details holder>
	<@command_type_label holder.commandType />
	<@f.errors path="command.*" cssClass="error form-errors text-danger" />

	<#if holder.command.title?has_content><span class="eventTitle">${holder.command.title} - </span></#if>
	<#if holder.command.startTime??><@fmt.time holder.command.startTime /></#if> ${(holder.command.day.name)!""}

	<#local popoverContent>
		<#if holder.command.title?has_content><div class="eventTitle">${holder.command.title}</div></#if>
		<div class="day-time">
			${(holder.command.day.name)!""}
			<#if holder.command.startTime??><@fmt.time holder.command.startTime /><#else>[no start time]</#if>
			-
			<#if holder.command.endTime??><@fmt.time holder.command.endTime /><#else>[no end time]</#if>
		</div>
		<#if holder.command.tutors?size gt 0>
			Tutor<#if holder.command.tutors?size gt 1>s</#if>:
			<@fmt.user_list_csv holder.command.tutors />
		</#if>
		<#if ((holder.command.location)!"")?has_content>
			<div class="location">
				Room: <@fmt.location_decomposed holder.command.location holder.command.locationId!"" />
			</div>
		</#if>
		<div class="running">
			Running: <#compress>
				<#if holder.command.weekRanges?size gt 0 && holder.command.day??>
					${weekRangesFormatter(holder.command.weekRanges, holder.command.day, academicYear, department)}
				<#elseif holder.command.weekRanges?size gt 0>
					[no day of week selected]
				<#else>
					[no dates selected]
				</#if>
			</#compress>
		</div>
	</#local>
	<a class="use-popover"
	   data-html="true"
	   data-content="${popoverContent?html}"><i class="fa fa-question-circle"></i></a>
</#macro>

<#macro delete_event_command_details command>
	<@command_type_label "Delete" />
	<@f.errors path="*" cssClass="error form-errors text-danger" />

	<#local event = command.event />
	<@components.eventShortDetails event />

	<#local popoverContent><@components.eventDetails event /></#local>
	<a class="use-popover"
	   data-html="true"
	   data-content="${popoverContent?html}"><i class="fa fa-question-circle"></i></a>
</#macro>

<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.groups.import_spreadsheet dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Import small groups from spreadsheet" route_function "for" />

<#if errors.hasErrors()>
	<div class="alert alert-danger">
		<#if errors.hasGlobalErrors()>
			<p>There were some problems parsing your spreadsheet:</p>

			<#list errors.globalErrors as e>
				<div><@spring.message message=e /></div>
			</#list>
		</#if>

		<#if errors.hasFieldErrors()>
			<p>Some rows in your spreadsheet weren't valid. See the errors below.</p>
		</#if>
	</div>
<#else>
	<div class="alert alert-info">
		<p>Please review the changes below - note that it is not possible to undo these changes once they have been made!</p>
	</div>
</#if>

<#if command.commands?size gt 0>
	<p>The following sets of small groups were successfully read from the spreadsheet:</p>

	<#list command.commands as sHolder>
		<@spring.nestedPath path="command.commands[${sHolder_index}]">
			<@set_command_details sHolder />
		</@spring.nestedPath>
	</#list>
<#elseif !errors.hasErrors()>
	<p>No sets of small groups were found in your spreadsheet.</p>
</#if>

<#if errors.hasErrors()>
	<@f.form method="post" enctype="multipart/form-data" action="" commandName="command" cssClass="double-submit-protection">
		<@bs3form.filewidget basename="file" types=[] multiple=false labelText="Choose your spreadsheet" />

		<div class="fix-footer">
			<button type="submit" class="btn btn-primary">Upload spreadsheet</button>
			<a class="btn btn-default" href="<@routes.groups.departmenthome department academicYear />">Cancel</a>
		</div>
	</@f.form>
<#else>
	<@f.form method="post" action="" commandName="command" cssClass="double-submit-protection">
		<#list command.file.attached as attached>
			<input type="hidden" name="file.attached" value="${attached.id}">
		</#list>
		<input type="hidden" name="confirm" value="true">

		<div class="fix-footer">
			<button type="submit" class="btn btn-primary">Make changes</button>
			<a class="btn btn-default" href="<@routes.groups.departmenthome department academicYear />">Cancel</a>
		</div>
	</@f.form>
</#if>

</#escape>