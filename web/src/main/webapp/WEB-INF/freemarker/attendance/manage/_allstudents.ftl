<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#if membershipItems?size == 0>
	<div class="students"><p>0 students on this scheme</p></div>
<#else>
	<div class="students">
		<@attendance_macros.manageStudentTable membershipItems />
	</div>
</#if>

</#escape>
