<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<div class="pull-right">
	<button class="btn btn-default" data-toggle="modal" data-target="#copy-loads-modal">
		Copy from other academic year
	</button>
</div>

<div id="copy-loads-modal" class="modal fade">
	<@modal.wrapper cssClass="modal-lg">
		<@modal.header>
			<h3 class="modal-title">Copy from other academic year</h3>
		</@modal.header>
		<@modal.body>
			<div class="alert alert-info">
				Copying from another academic year will overwrite any changes you have made but not saved,
				but you can review and update the copy before it is saved.
			</div>
			<@bs3form.labelled_form_group path="" labelText="Academic year">
				<select class="form-control" name="academicYear" data-href="<@routes.exams.manageNormalLoads department academicYear />/fetch/">
					<option value="" disabled selected></option>
					<#list availableAcademicYears as academicYear>
						<option value="${academicYear.startYear?c}">${academicYear.toString}</option>
					</#list>
				</select>
			</@bs3form.labelled_form_group>
			<div class="loading hide">
				<i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em>
			</div>
			<div class="content"></div>
		</@modal.body>
		<@modal.footer>
			<div class="submit-buttons">
				<button class="btn btn-primary" name="copy" disabled>Copy</button>
				<button class="btn btn-default" data-dismiss="modal">Cancel</button>
			</div>
		</@modal.footer>
	</@modal.wrapper>
</div>

<#function route_function dept>
	<#local result><@routes.exams.manageNormalLoads dept academicYear /></#local>
	<#return result />
</#function>

<@fmt.id7_deptheader title="Manage normal CATS loads for ${academicYear.toString}" route_function=route_function preposition="in" />

<p>
	The "Normal CATS load" is the expected number of CATS take by each student on a given route
	on a particular year of study in an academic year. Any student who is registered for modules
	whose CATS total more than this are considered to have overcatted.
</p>

<p>
	For each route and year of study you can specify the normal CAT load for ${academicYear.toString}.
	If the normal CATS load is not specified the following defaults will be used, based on the value of INS_PWY.PWY_PWTC in SITS:
</p>

<ul>
	<#list allDegreeTypes as degreeType>
		<li>${degreeType.description} (${degreeType.dbValue}): ${degreeType.normalCATSLoad}</li>
	</#list>
</ul>

<#assign formUrl><@routes.exams.manageNormalLoads department academicYear /></#assign>

<div class="fix-area normal-load-editor">

	<@f.form method="post" action="${formUrl}" commandName="command" cssClass="dirty-check">

		<@spring.bind path="command">
			<#if (status.errors.allErrors?size > 0)>
				<div class="alert alert-danger">
					<@f.errors path="*" />
				</div>
			</#if>
		</@spring.bind>

		<div class="header fix-header">
			<div class="row">
				<div class="col-md-9 col-md-offset-2">
					<div class="pull-right">
						<div class="checkbox">
							<label>
								<input name="showAdditionalYears" type="checkbox" />
								Show additional years of study
							</label>
						</div>
					</div>
					<label>Year of study</label>
				</div>
			</div>
			<div class="row last">
				<div class="col-md-2">
					<label>Route</label>
				</div>
				<div class="col-md-9">
					<#list allYearsOfStudy as year>
						<div class="col-md-1"><label>${year}</label></div>
					</#list>
				</div>

				<div class="col-md-2">
					<input type="text" class="form-control" name="filter" placeholder="Filter routes" />
				</div>
				<div class="col-md-9">
					<#list allYearsOfStudy as year>
						<div class="col-md-1">
							<input type="text" class="form-control" name="bulk" data-year="${year}" placeholder="All" />
						</div>
					</#list>
				</div>
				<div class="col-md-1">
					<button type="button" name="bulkapply" class="btn btn-default btn-sm" disabled>Apply</button>
					<@fmt.help_popover id="bulkapply" content="Enter a CATS load and click Apply to change all filtered routes" />
				</div>
			</div>
		</div>

		<#list command.allRoutes as route>
			<div class="row">
				<div class="col-md-2">
					<label title="${route.code?upper_case} ${route.name}" class="use-tooltip">${route.code?upper_case} ${route.name}</label>
				</div>
				<div class="col-md-9">
					<#list allYearsOfStudy as year>
						<#assign value = "" />
						<#if mapGet(command.normalLoads, route)?? && mapGet(mapGet(command.normalLoads, route), year)?? >
							<#assign value = mapGet(mapGet(command.normalLoads, route), year) />
						</#if>
						<div class="col-md-1">
							<input title="${route.code?upper_case} ${route.name} Year ${year}"
								data-container="body"
								type="text"
								class="use-tooltip form-control"
								name="normalLoads[${route.code}][${year}]"
								value="${value}"
								placeholder="${route.degreeType.normalCATSLoad}"
							/>
						</div>
					</#list>
				</div>
			</div>
		</#list>

		<div class="submit-buttons fix-footer">
			<button class="btn btn-primary" type="submit" name="confirm">Save</button>
			<a class="btn btn-default dirty-check-ignore" href="<@routes.exams.gridsDepartmentHomeForYear department academicYear />">Cancel</a>
		</div>
	</@f.form>

</div>

<script>
	window.ExamGrids.manageNormalLoads();
</script>

</#escape>