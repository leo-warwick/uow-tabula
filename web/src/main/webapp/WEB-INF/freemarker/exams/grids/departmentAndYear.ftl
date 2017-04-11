<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.exams.gridsDepartmentHomeForYear dept academicYear /></#local>
	<#return result />
</#function>

<@fmt.id7_deptheader title="Manage exam board grids for ${academicYear.toString}" route_function=route_function preposition="in" />

<#if updatedMarks?has_content>
	<div class="alert alert-info">
		<@fmt.p updatedMarks "mark" /> uploaded to SITS.
	</div>
</#if>

<#if can.do("Department.ExamGrids", department)>
	<h2><a href="<@routes.exams.generateGrid department academicYear />">Generate new grid</a></h2>
	<h2><a href="<@routes.exams.uploadYearMarks department academicYear />">Upload year marks</a></h2>
	<h2><a href="<@routes.exams.manageNormalLoads department academicYear />">Manage normal CATS loads</a></h2>
	<h2><a href="<@routes.exams.manageWeightings department academicYear />">Manage course year weightings</a></h2>
<#else>
	<div class="alert alert-danger">
		You do not have permission to generate exam board grids for ${department.name}.
	</div>
</#if>

</#escape>