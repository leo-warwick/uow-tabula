Further details related to this request:

<#if moduleManagers?has_content >
Module Manager<#if (moduleManagers?size > 1)>s</#if>
<#list moduleManagers as manager>
${manager.fullName} (${manager.warwickId!}) (${manager.email})
</#list>
</#if>

Student Contact Details

* Mobile Number: ${(student.mobileNumber)!"Not available"}
* Telephone Number: ${(student.phoneNumber)!"Not available"}
* Email Address: ${(student.email)!"Not available"}

<#if relationships?has_content >
<#list relationships?keys as key>
<#if relationships[key]?has_content>
${key}<#if (relationships[key]?size > 1)>s</#if>

<#list relationships[key] as agent>
*${agent.agentName} (${agent.agent})<#if agent.agentMember??>, ${agent.agentMember.description}</#if>
</#list>
</#if>
</#list>
</#if>

<#if scdRoute?has_content >
Student Course Details

* Route:	${(scdRoute.name)!} (${(scdRoute.code?upper_case)!})
* Course:	<#if (scdRoute.name) != (scdCourse.name)>${(scdCourse.name)!} (${(scdCourse.code?upper_case)!})<#else>${(scdCourse.code?upper_case)!}</#if>
* Intended award and type: ${(scdAward.name)!} (${(scdRoute.degreeType.toString)!})
</#if>