<#escape x as x?html>
<#import "*/group_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />

<@modal.wrapper cssClass="modal-lg">
	<@modal.header>
		<h3 class="modal-title">Students with small group event conflicts</h3>
	</@modal.header>
	<@modal.body>
		<#if students?has_content>
			<ul class="profile-user-list">
				<#list students as student>
					<li>
						<div class="profile clearfix">
							<@fmt.member_photo student "tinythumbnail" false />
							<div class="name">
								<h6>${student.fullName} <@pl.profile_link student.universityId /></h6>
								${(student.mostSignificantCourseDetails.currentRoute.code?upper_case)!""} ${(student.mostSignificantCourseDetails.currentRoute.name)!""}<br />
								${student.shortDepartment!""}
							</div>
						</div>
					</li>
				</#list>
			</ul>
			<p>
				<@fmt.bulk_email_students students=students />
			</p>
		</#if>
	</@modal.body>
</@modal.wrapper>
</#escape>