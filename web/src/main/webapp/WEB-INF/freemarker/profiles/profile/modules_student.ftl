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

<h1>Modules</h1>

<#if hasPermission>

	<#if user.staff>
		<div class="pull-right">
			<@routes.profiles.mrm_link command.studentCourseYearDetails />
				View in MRM<img class="targetBlank" alt="" title="Link opens in a new window" src="/static/images/shim.gif"/>
			</a>
		</div>
	</#if>

	<p>Module Registration Status:
		<#if command.studentCourseYearDetails.moduleRegistrationStatus??>
			${(command.studentCourseYearDetails.moduleRegistrationStatus.description)!}
		<#else>
			Unknown (not in SITS)
		</#if>
	</p>

	<#if moduleRegistrations?has_content>

		<#assign showModuleResults = features.showModuleResults />

		<#list moduleRegistrations as moduleRegistration>
			<div class="striped-section collapsible">
				<h3 class="section-title">
					<@fmt.module_name moduleRegistration.module />
					<span class="mod-reg-summary">
						<#if showModuleResults>
							<span class="mod-reg-summary-item"><strong>Mark:</strong> ${(moduleRegistration.agreedMark)!}</span>
							<span class="mod-reg-summary-item"><strong>Grade:</strong> ${(moduleRegistration.agreedGrade)!}</span>
							<span class="mod-reg-summary-item"><strong>CATS:</strong> ${(moduleRegistration.cats)!}</span>
						</#if>
					</span>
				</h3>
				<div class="striped-section-contents">
					<div class="row item-info">
						<div class="col-md-4">
							<h4><@fmt.module_name moduleRegistration.module false /></h4>
						</div>
						<div class="col-md-8">
							<strong>CATS:</strong> ${(moduleRegistration.cats)!} <br />
							<strong>Assessment group:</strong> ${(moduleRegistration.assessmentGroup)!} <br />
							<strong>Occurrence:</strong> ${(moduleRegistration.occurrence)!} <br />
							<#if showModuleResults>
								<strong>Mark:</strong> ${(moduleRegistration.agreedMark)!} <br />
								<strong>Grade:</strong> ${(moduleRegistration.agreedGrade)!} <br />
							</#if>
							<strong>Status:</strong>
							<#if moduleRegistration.selectionStatus??>
								${(moduleRegistration.selectionStatus.description)!}
							<#else>
								-
							</#if> <br />
						</div>
					</div>
				</div>
			</div>
		</#list>

	<#else>

		<div class="alert alert-info">
			There are no module registrations for this academic year.
		</div>

	</#if>

<#else>

	<div class="alert alert-info">
		You do not have permission to see the module registrations for this course.
	</div>

</#if>



</#escape>