<#escape x as x?html>

<#if user.staff>
	<#include "search/form.ftl" />
	<hr class="full-width" />
</#if>

<h1>Identity</h1>

<div class="row">
	<div class="col-md-6">
		<h2>${member.fullName}</h2>

		<div class="row">
			<div class="col-md-5 col-lg-4">
				<@fmt.member_photo member />
			</div>
			<div class="col-md-7 col-lg-8">
				<strong>Official name:</strong> ${member.officialName}<br/>
				<strong>Preferred name:</strong> ${member.fullName}<br/>
				<#if member.jobTitle??>
					<strong>Job title:</strong> ${member.jobTitle}<br/>
				</#if>

				<br/>

				<#if member.email??>
					<strong>Warwick email:</strong> <a href="mailto:${member.email}">${member.email}</a><br/>
				</#if>
				<#if member.universityId??>
					<strong>University ID: </strong> ${member.universityId}<br/>
				</#if>
				<#if member.userId??>
					<strong>Username:</strong> ${member.userId}<br/>
				</#if>
			</div>
		</div>

		<#if can.do("RolesAndPermissions.Create", member)>
			<#assign permissions_url><@routes.profiles.permissions member /></#assign>
			<p><@fmt.permission_button
			permission='RolesAndPermissions.Create'
			scope=member
			action_descr='modify permissions'
			classes='btn btn-primary'
			href=permissions_url
			tooltip='Permissions'
			>
				Permissions
			</@fmt.permission_button></p>
		</#if>

	</div>
	<div class="col-md-6"></div>
</div>
</#escape>