<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<div class="deptheader">
		<h1>Create events from Syllabus+</h1>
		<h4 class="with-related"><span class="muted">for</span> ${smallGroupSet.name}</h4>
	</div>

	<p>Here are all the small group events for <@fmt.module_name module false /> in the central timetabling system Syllabus+.</p>

	<@f.form method="post" action="" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<table class="table table-bordered table-striped">
			<thead>
				<tr>
					<th>Event</th>
					<th>Group</th>
				</tr>
			</thead>
			<tbody>
				<#list command.eventsToImport as eventToImport>
					<@spring.nestedPath path="eventsToImport[${eventToImport_index}]">
						<tr>
							<td><@components.timetableEventDetails eventToImport.timetableEvent smallGroupSet.academicYear, module.adminDepartment /></td>
							<td>
								<@f.select path="group" cssClass="form-control">
									<@f.option value="">Do not import</@f.option>
									<@f.options items=groups itemLabel="name" itemValue="id" />
								</@f.select>
							</td>
						</tr>
					</@spring.nestedPath>
				</#list>
			</tbody>
		</table>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
				/>
			<a class="btn btn-default" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</#escape>