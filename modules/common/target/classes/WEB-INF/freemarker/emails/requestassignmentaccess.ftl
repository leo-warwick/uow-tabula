${student.fullName}<#if student.warwickId?has_content> (${student.warwickId})</#if>, who is <#if student.student>a student<#elseif student.staff>a member of staff<#else>an external user</#if><#if student.department?has_content> in ${student.department}</#if>, has requested access to the assignment "${assignment.name}" for ${assignment.module.code?upper_case}.

If you agree that they should be able to submit to this assignment, you should enrol them on the assignment via the assignment properties.<#if student.email?has_content>

You can contact the <#if student.student>student<#elseif student.staff>member of staff<#else>external user</#if> via email at ${student.email}.</#if>