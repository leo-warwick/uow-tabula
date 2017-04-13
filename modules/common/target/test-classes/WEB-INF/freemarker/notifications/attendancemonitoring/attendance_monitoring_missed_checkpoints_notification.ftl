The following student has missed at least ${level} monitoring points in the academic year ${academicYear.toString}:

Name: ${student.fullName!'Unknown'}
University ID: ${student.universityId}
Course: ${student.mostSignificantCourse.course.name}
<#list relationships?keys as relationshipType><@fmt.p number=mapGet(relationships, relationshipType)?size singular=relationshipType.agentRole?cap_first shownumber=false/>: <#list mapGet(relationships, relationshipType) as relationship>${relationship.agentName}<#if relationship_has_next>, </#if></#list>
</#list>

Please refer to the Teaching Quality website for guidance on the appropriate action to take:

http://warwick.ac.uk/studentattendanceguide/principles