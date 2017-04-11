<#import "*/courses_macros.ftl" as courses_macros />
<#assign templateUrl><@routes.coursework.markstemplate assignment=assignment /></#assign>
<#assign formUrl>${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/marks')}</#assign>
<#assign cancelUrl><@routes.coursework.depthome module=assignment.module /></#assign>
<#assign generateUrl><@routes.coursework.generateGradesForMarks assignment=assignment /></#assign>
<@courses_macros.marksForm
	assignment = assignment
	templateUrl = templateUrl
	formUrl = formUrl
	commandName = "adminAddMarksCommand"
	cancelUrl = cancelUrl
	generateUrl = generateUrl
/>
