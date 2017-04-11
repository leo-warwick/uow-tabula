<#include "form.ftl" />
<#escape x as x?html>

<section class="results">
	<h2>Results</h2>

	<#if results?size = 0>
		<p>No profiles were found.</p>
	<#else>
		<ul class="profile-user-list">

			<#list results as profile>
				<li data-id="${profile.universityId}">

					<@fmt.member_photo profile 'tinythumbnail' />

					<h3><a href="<@routes.profiles.profile profile />"><@fmt.profile_name profile /></a></h3>
					<@fmt.profile_description profile />
				</li>
			</#list>
		</ul>
	</#if>
</section>
</#escape>