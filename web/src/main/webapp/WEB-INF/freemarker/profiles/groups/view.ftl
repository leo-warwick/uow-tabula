<#import "../related_students/related_students_macros.ftl" as related_students_macros />
<#escape x as x?html>

	<#assign _groupSet=smallGroup.groupSet />
	<#assign _module=smallGroup.groupSet.module />

    <h1>${_module.code?upper_case} ${_groupSet.nameWithoutModulePrefix}, ${smallGroup.name}</h1>

	<@related_students_macros.table items=tutees />

	<p><@fmt.bulk_email_students students=tutees subject="${_module.code?upper_case} ${_groupSet.name}, ${smallGroup.name}" /></p>
</#escape>