<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<h1>Create assignments from previous for ${title}</h1>

	<form action="" method="post" class="form-horizontal copy-assignments">

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class='btn' href='<@url page=cancel />'>Cancel</a>
		</div>

		<#assign modules = copyAssignmentsCommand.modules />
		<#assign path = "copyAssignmentsCommand.assignments" />
		<#include "_assignment_list.ftl" />


		<@form.labelled_row "copyAssignmentsCommand.archive" "Archive old assignments">
		<div class="checkbox"><#compress>
			<label class="checkbox">
				<input type="checkbox"  name="archive">
			</label>
		</#compress></div>
		<div class="help-block">
			<small>
				Archive all of the assignments selected above.
			</small>
		</div>
		</@form.labelled_row>

		<@form.labelled_row "copyAssignmentsCommand.academicYear" "Set academic year">
			<@f.select path="copyAssignmentsCommand.academicYear" cssClass="span2">
				<@f.options items=copyAssignmentsCommand.academicYear.yearsSurrounding(2, 2) itemLabel="label" itemValue="storeValue" />
			</@f.select>
			<div class="help-block">
				<small>
					The new assignments' open and close dates will be offset by the appropriate number of years. You should check the open and close dates
					of all new assignments.
				</small>
			</div>
		</@form.labelled_row>

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class='btn' href='<@url page=cancel />'>Cancel</a>
		</div>

	</form>
</#escape>