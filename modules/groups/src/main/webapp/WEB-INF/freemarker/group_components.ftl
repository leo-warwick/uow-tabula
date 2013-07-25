<#ftl strip_text=true />
<#-- Common template parts for use in other small groups templates. -->

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#-- Output a dropdown menu only if there is anything in it. -->
<#macro dropdown_menu text icon>
	<#-- Capture the content between the macro tags into a string -->
	<#local content><#nested /></#local>
	<#if content?trim?has_content>
	<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-${icon}"></i> ${text} <span class="caret"></span></a>
	<ul class="dropdown-menu pull-right">
	${content}
	</ul>
	</#if>
</#macro>

<#-- module_info: takes a GroupsViewModel.ViewModules and renders out
 	a collection of modules with group sets and groups.

 	How the data is organised (which modules/sets/groups) is up to
 	the command generating the view model. No user checks in here!
 -->
<#macro module_info data>
<div class="small-group-modules-list">
<#list data.moduleItems as moduleItem>

<#assign module=moduleItem.module />

<#assign has_groups=(moduleItem.setItems!?size gt 0) />
<#assign has_archived_groups=false />
<#list moduleItem.setItems as setItem>
	<#if setItem.set.archived>
		<#assign has_archived_groups=true />
	</#if>
</#list>


<a id="${module_anchor(module)}"></a>
<div class="module-info striped-section<#if has_groups> collapsible expanded</#if><#if data.canManageDepartment && !has_groups> empty</#if>"
	 data-name="${module_anchor(module)}">
	<div class="clearfix">

		<div class="btn-group section-manage-button">
			<@dropdown_menu "Manage" "wrench">
				<#if moduleItem.canManageGroups>
					<li><a href="<@routes.moduleperms module />">
						<i class="icon-user icon-fixed-width"></i> Edit module permissions
					</a></li>
					<li><a href="<@routes.createset module />"><i class="icon-group icon-fixed-width"></i> Add small groups</a></li>
				</#if>
				<#if has_archived_groups>
					<li><a class="show-archived-small-groups" href="#">
						<i class="icon-eye-open icon-fixed-width"></i> Show archived small groups
					</a>
					</li>
				</#if>
			</@dropdown_menu>
		</div>

		<h2 class="section-title with-button"><@fmt.module_name module /></h2>

		<#if moduleItem.setItems?has_content>
		<div class="striped-section-contents">
		<#list moduleItem.setItems as setItem>
			<#assign groupSet=setItem.set />
			<#if !groupSet.deleted>
				<div class="item-info row-fluid<#if groupSet.archived> archived</#if> groupset-${groupSet.id}">
					<div class="span3">
						<h3 class="name">
							<small>
							${groupSet.name}
								<#if groupSet.archived>
									(Archived)
								</#if>
							</small>
						</h3>

						<span class="format">
						${groupSet.format.description}
						</span>
					</div>

					<div class="span7">
						<#if allocated?? && allocated.id == groupSet.id>
							<div class="alert alert-success">
								<a class="close" data-dismiss="alert">&times;</a>
								<p>Changes saved.</p>
							</div>
						</#if>

						<#list setItem.groups as group>
							<div class="group">
								<h4 class="name">
								${group.name!""}
								<#-- modal not currently working
								<a data-url="<@routes.studentslist group />" data-toggle="modal"  href="#students-list-modal">
								-->

								<a href="<@routes.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
								<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
								</a>
								</h4>

								<ul class="unstyled">
									<#list group.events as event>
										<li>
										<#-- Tutor, weeks, day/time, location -->
										<@fmt.weekRanges event />,
										${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
										${event.location!"[no location]"}
										</li>
									</#list>
								</ul>
							</div>
						</#list>

						<#-- Only show warnings to users that can do somthing about them -->
						<#if moduleItem.canManageGroups>
							<#assign unallocatedSize = groupSet.unallocatedStudentsCount />
							<#if unallocatedSize gt 0>
								<div class="alert">
									<i class="icon-info-sign"></i> <@fmt.p unallocatedSize "student has" "students have" /> not been allocated to a group
								</div>
							</#if>

							<#if groupSet.hasAllocated >
								 <#-- not released at all -->
								  <#if (!groupSet.releasedToStudents && !groupSet.releasedToTutors)>
								<p class="alert">
									<i class="icon-info-sign"></i> Notifications have not been sent for these groups
								</p>
								 <#-- only released to tutors-->
								 <#elseif (!groupSet.releasedToStudents && groupSet.releasedToTutors)>
								  <p class="alert">
									   <i class="icon-info-sign"></i> Notifications have not been sent to students for these groups
								   </p>
								  <#-- only released to students-->
								  <#elseif (groupSet.releasedToStudents && !groupSet.releasedToTutors)>
									  <p class="alert">
										  <i class="icon-info-sign"></i> Notifications have not been sent to tutors for these groups
									  </p>
								 </#if>
							</#if>
						</#if>
                    </div>

                    <div class="span2">
                        <div class="btn-toolbar pull-right">
                            <div class="btn-group">

                                <@dropdown_menu "Actions" "cog">
                                    <#if moduleItem.canManageGroups>
                                    <li><a href="<@routes.editset groupSet />"><i class="icon-wrench icon-fixed-width"></i> Edit properties</a></li>
                                     <#if features.smallGroupTeachingStudentSignUp>
										 <#if groupSet.openForSignups>
										 <li  ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
                                         		   (''," class='disabled use-tooltip' title='Not a self-signup group' ")
                                         }>
                                         <a  class="close-group-link" href="/TODO-TAB-934"><i class="icon-lock icon-fixed-width"></i> Close</a></li>

										 <#else>
										<li  ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
												   (''," class='disabled use-tooltip' title='Not a self-signup group' ")
										}>
										<a  class="open-group-link" data-toggle="modal" data-target="#modal-container"
										href="<@routes.openset groupSet />"><i class="icon-unlock-alt icon-fixed-width"></i> Open</a></li>
										</#if>
									</#if>
                                    <li><a href="<@routes.allocateset groupSet />"><i class="icon-random icon-fixed-width"></i> Allocate students</a></li>
                                    <li ${groupSet.fullyReleased?string(" class='disabled use-tooltip' title='Already notified' ",'')} >
                                            <a class="notify-group-link" data-toggle="modal" data-target="#modal-container" href="<@routes.releaseset groupSet />">
                                            <i class="icon-envelope-alt icon-fixed-width"></i>
                                            Notify
                                        </a></li>
                                    <li>
                                        <#if groupSet.archived>
                                           <#assign archive_caption>Unarchive groups</#assign>
                                        <#else>
                                            <#assign archive_caption>Archive groups</#assign>
                                        </#if>
                                        
                                        <#assign archive_url><@routes.archiveset groupSet /></#assign>
                                        
                                        <@fmt.permission_button permission='SmallGroups.Archive' scope=module action_descr='${archive_caption}'?lower_case classes='archive-group-link ajax-popup' href=archive_url 
                                        						tooltip='Archive small group' data_attr='data-popup-target=.btn-group data-container=body'> 
                                        <i class="icon-folder-close icon-fixed-width"></i> ${archive_caption} 
                                        </@fmt.permission_button>
                                    </a></li> 
                                    </#if>
                                </@dropdown_menu>
                            </div>
                        </div>
                    </div>
                </div>
            </#if>
        </#list>
        </div>
        </#if>
    </div>
</div>

</#list>

<#-- List of students modal -->
<div id="students-list-modal" class="modal fade">
</div>

</#macro>
