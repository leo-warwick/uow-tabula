<#import "../../groups/group_components.ftl" as group_components />
<#escape x as x?html>

<#if !isSelf>
	<details class="indent">
		<summary>${member.officialName}</summary>
		<#if member.userId??>
			${member.userId}<br/>
		</#if>
		<#if member.email??>
			<a href="mailto:${member.email}">${member.email}</a><br/>
		</#if>
		<#if member.phoneNumber??>
			${phoneNumberFormatter(member.phoneNumber)}<br/>
		</#if>
		<#if member.mobileNumber??>
			${phoneNumberFormatter(member.mobileNumber)}<br/>
		</#if>
	</details>
</#if>

<h1>Seminars</h1>

	<#if hasPermission>

		<div id="student-groups-view">
			<#if commandResult.moduleItems?size == 0>
				<div class="alert alert-info">
					There are no groups to show right now
				</div>
			</#if>
			<@group_components.module_info commandResult />
		</div>

	<#else>

		<div class="alert alert-info">
			You do not have permission to see the seminars for this course.
		</div>

	</#if>



</#escape>