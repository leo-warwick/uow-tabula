<#import "*/marking_macros.ftl" as marking_macros />

<#if marker??>
	<#assign formUrl><@routes.exams.markerAddMarks exam marker/></#assign>
	<#assign templateUrl><@routes.exams.markerMarksTemplate exam marker/></#assign>
	<#assign cancelUrl><@routes.exams.examsHome /></#assign>
<#else>
	<#assign formUrl><@routes.exams.addMarks exam /></#assign>
	<#assign templateUrl><@routes.exams.markstemplate exam=exam /></#assign>
	<#assign cancelUrl><@routes.exams.moduleHomeWithYear module=exam.module academicYear=exam.academicYear /></#assign>
</#if>
<#assign generateUrl><@routes.exams.generateGradesForMarks exam=exam /></#assign>
<@marking_macros.marksForm
	assignment = exam
	templateUrl = templateUrl
	formUrl = formUrl
	commandName = "adminAddMarksCommand"
	cancelUrl = cancelUrl
	generateUrl = generateUrl
	seatNumberMap = seatNumberMap
	showAddButton = false
/>
