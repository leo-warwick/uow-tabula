<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
	<h2 class="section">Administration</h2>

	<div class="row-fluid">
		<div class="span6">
			<#if features.activityStreams>
				<h6>Late &amp; unusual activity</h6>

				<#import "*/activity_macros.ftl" as activity />
				<div class="home-page-activity">
					<@activity.activity_stream max=5 minPriority=0.5 types="SubmissionReceived,MarkedPlagarised"/>
				</div>
			</#if>
		</div>

		<div class="span6">
			<#if nonempty(ownedModuleDepartments)>
				<h6>My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h6>

				<ul class="links">
					<#list ownedModuleDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>

			<#if nonempty(ownedDepartments)>
				<h6>My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h6>

				<ul class="links">
					<#list ownedDepartments as department>
						<li>
							<@link_to_department department />
						</li>
					</#list>
				</ul>
			</#if>
		</div>
	</div>
</#if>