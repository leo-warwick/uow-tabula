<#ftl strip_text=true />

<#-- Common template parts for use in other small groups templates. -->

<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#function set_anchor set>
	<#return "set-${set.id}" />
</#function>

<#macro event_schedule_info event>
<#if event.unscheduled>
	<span class="badge progress-bar-warning use-tooltip" data-toggle="tooltip" data-placement="bottom" data-title="This event has not yet been scheduled">Not scheduled</span>
<#else>
	<#-- Weeks, day/time, location -->
	<#if event.title?has_content><span class="eventTitle">${event.title} - </span></#if>
	<@fmt.weekRanges event />,
	${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime /><#if ((event.location.name)!)?has_content>,</#if>
	<#if ((event.location.name)!)?has_content>
		<@fmt.location event.location />
	</#if>
</#if>
</#macro>

<#-- Output a dropdown menu only if there is anything in it. -->
<#macro dropdown_menu text button_size="btn-medium">
	<#-- Capture the content between the macro tags into a string -->
	<#local content><#nested /></#local>
	<#if content?trim?has_content>
	<a class="btn btn-default ${button_size} dropdown-toggle" data-toggle="dropdown">${text} <span class="caret"></span></a>
	<ul class="dropdown-menu pull-right">
	${content}
	</ul>
	</#if>
</#macro>

<#macro sets_info sets expand_by_default>
	<#list sets as setItem>
		<@single_set setItem expand_by_default />

		<#if !expand_by_default>
			<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
			<script type="text/javascript">
				GlobalScripts.initCollapsible(jQuery('#${set_anchor(setItem.set)}'));
			</script>
		</#if>
	</#list>
</#macro>

<#macro single_set setItem expand_by_default=true>
	<#local set = setItem.set />
	<#local has_groups = setItem.groups?size gt 0 />

	<span id="${set_anchor(set)}-container">
		<div id="${set_anchor(set)}" class="set-info striped-section collapsible<#if expand_by_default> expanded</#if><#if set.archived> archived</#if>"
			<#if !expand_by_default>
				data-populate=".striped-section-contents"
				data-href="<@routes.groups.single_set_info set />"
			</#if>
			 data-name="${set_anchor(set)}">
			<div class="clearfix">
				<div class="section-title row">
					<div class="col-md-4">
						<span class="h4">
							<span class="icon-container"></span>
							<@fmt.groupset_name set />
						</span>
					</div>

					<div class="col-md-2">
						<#if setItem.isStudentSignUp()>
							<#if setItem.set.openForSignups>
								<span class="use-tooltip" title="These groups are open for self sign-up"><i class="fa fa-unlock-alt"></i> Open for self-signup</span>
							<#else>
								<span class="use-tooltip" title="These groups are closed for self sign-up"><i class="fa fa-lock"></i> Closed for self-signup</span>
							</#if>
						<#elseif setItem.isLinked()>
							<span class="use-tooltip" title="These group allocations may be linked to other modules">Linked</span>
						<#else>
							<span class="use-tooltip" title="These groups are manually allocated">Manually allocated</span>
						</#if>

						<#if set.archived>
							<span class="use-tooltip" title="These groups have been archived">Archived</span>
						</#if>
					</div>


					<div class="col-md-2">
						<#if !set.archived>
							<#local progressTooltip><@spring.message code=setItem.progress.messageCode /></#local>

							<dl class="progress use-tooltip" title="${progressTooltip}" style="margin: 0; border-bottom: 0;" data-container="body">
								<dt class="progress-bar progress-bar-${setItem.progress.t}" style="width: <#if setItem.progress.percentage == 0>10<#else>${setItem.progress.percentage}</#if>%;"></dt>
							</dl>
						</#if>
					</div>

					<div class="col-md-4">
						<div class="pull-right">
							<span>
								<@dropdown_menu "Edit" "btn-xs">
									<li>
										<#local edit_url><@routes.groups.editsetproperties set /></#local>
										<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=set
										action_descr='edit small group properties'
										href=edit_url
										>
											Properties
										</@fmt.permission_button>
									</li>

									<li>
										<#local edit_url><@routes.groups.editsetgroups set /></#local>
										<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=set
										action_descr='edit small groups'
										href=edit_url
										>
											Groups
										</@fmt.permission_button>
									</li>

									<li>
										<#local edit_url><@routes.groups.editsetstudents set /></#local>
										<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=set
										action_descr='edit small group students'
										href=edit_url
										>
											Students
										</@fmt.permission_button>
									</li>

									<li>
										<#local edit_url><@routes.groups.editsetevents set /></#local>
										<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=set
										action_descr='edit small group events'
										href=edit_url
										>
											Events
										</@fmt.permission_button>
									</li>

									<li>
										<#local allocateset_url><@routes.groups.editsetallocate set /></#local>
										<@fmt.permission_button
										permission='SmallGroups.Allocate'
										scope=set
										action_descr='allocate students'
										href=allocateset_url
										>
											Allocation
										</@fmt.permission_button>
									</li>
								</@dropdown_menu>
							</span>
							<span>
								<@dropdown_menu "Actions" "btn-xs">
									<#if features.smallGroupTeachingStudentSignUp>
										<#if set.openForSignups>
											<li ${(set.allocationMethod.dbValue == "StudentSignUp")?string
											(''," class='disabled use-tooltip' title='Not a self-signup group' ")
											}>
												<#local closeset_url><@routes.groups.closeset set /></#local>
												<@fmt.permission_button
												permission='SmallGroups.Update'
												scope=set
												classes='close-group-link'
												action_descr='close small group'
												href=closeset_url
												data_attr='data-toggle=modal data-target=#modal-container'
												>
													Close
												</@fmt.permission_button>
											</li>
										<#else>
											<li ${(set.allocationMethod.dbValue == "StudentSignUp")?string
											(''," class='disabled use-tooltip' title='Not a self-signup group' ")
											}>
												<#local openset_url><@routes.groups.openset set /></#local>
												<@fmt.permission_button
												permission='SmallGroups.Update'
												scope=set
												classes='open-group-link'
												action_descr='open small group'
												href=openset_url
												data_attr='data-toggle=modal data-target=#modal-container data-container=body'
												>
													Open
												</@fmt.permission_button>
											</li>
										</#if>
									</#if>

									<li ${set.fullyReleased?string(" class='disabled use-tooltip' title='Already published' ",'')} >
										<#local notifyset_url><@routes.groups.releaseset set /></#local>
										<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=set
										action_descr='publish groups to students and staff'
										href=notifyset_url
										classes='notify-group-link'
										data_attr='data-toggle=modal data-target=#modal-container data-container=body'
										>
											Publish
										</@fmt.permission_button>
									</li>

									<li class="divider"></li>

									<li>
										<#if set.archived>
											<#local archive_caption>Unarchive</#local>
										<#else>
											<#local archive_caption>Archive</#local>
										</#if>

										<#local archive_url><@routes.groups.archiveset set /></#local>

										<@fmt.permission_button permission='SmallGroups.Archive' scope=set action_descr='${archive_caption}'?lower_case classes='archive-group-link' href=archive_url
										tooltip='Archive small group' data_attr='data-toggle=modal data-target=#modal-container'>
											${archive_caption}
										</@fmt.permission_button>
										</a>
									</li>

									<li<#if set.releasedToStudents || set.releasedToTutors> class="disabled use-tooltip" data-container="body" title="Can't delete small groups that have been published to students or tutors"</#if>>
										<#local delete_url><@routes.groups.deleteset set /></#local>
										<@fmt.permission_button
											permission='SmallGroups.Delete'
											scope=set
											action_descr='delete small groups'
											href=delete_url
										>
											Delete
										</@fmt.permission_button>
									</li>

									<li class="divider"></li>

									<#if set.collectAttendance>
										<#local set_attendance_url><@routes.groups.setAttendance set /></#local>
										<li>
											<@fmt.permission_button permission='SmallGroupEvents.ViewRegister' scope=set action_descr='view attendance' href=set_attendance_url
											tooltip='View attendance at groups' data_attr='data-popup-target=.btn-group data-container=body'
											>
												View attendance
											</@fmt.permission_button>
										</li>
									</#if>

									<li>
										<#local permissions_url><@routes.groups.permissions set /></#local>
										<@fmt.permission_button
											permission='RolesAndPermissions.Create'
											scope=set
											action_descr='modify permissions'
											href=permissions_url
										>
											Edit permissions
										</@fmt.permission_button>
									</li>
								</@dropdown_menu>
							</span>
						</div>

						<#if !set.archived>
							<#if setItem.nextStage??>
								<#local nextStageUrl="" />
								<#local nextStageModal="" />
								<#if setItem.nextStage.actionCode == "workflow.smallGroupSet.AddGroups.action">
									<#local nextStageUrl><@routes.groups.editsetgroups set /></#local>
								<#elseif setItem.nextStage.actionCode == "workflow.smallGroupSet.AddStudents.action">
									<#local nextStageUrl><@routes.groups.editsetstudents set /></#local>
								<#elseif setItem.nextStage.actionCode == "workflow.smallGroupSet.AddEvents.action">
									<#local nextStageUrl><@routes.groups.editsetevents set /></#local>
								<#elseif setItem.nextStage.actionCode == "workflow.smallGroupSet.AllocateStudents.action">
									<#local nextStageUrl><@routes.groups.editsetallocate set /></#local>
								<#elseif setItem.nextStage.actionCode == "workflow.smallGroupSet.OpenSignUp.action">
									<#local nextStageUrl><@routes.groups.openset set /></#local>
									<#local nextStageModal = "#modal-container" />
								<#elseif setItem.nextStage.actionCode == "workflow.smallGroupSet.CloseSignUp.action">
									<#local nextStageUrl><@routes.groups.closeset set /></#local>
									<#local nextStageModal = "#modal-container" />
								<#elseif setItem.nextStage.actionCode == "workflow.smallGroupSet.SendNotifications.action">
									<#local nextStageUrl><@routes.groups.releaseset set /></#local>
									<#local nextStageModal = "#modal-container" />
								</#if>

								<#if nextStageUrl?has_content && can.do('SmallGroups.Update', set)>
									<a href="${nextStageUrl}"<#if nextStageModal?has_content> data-toggle="modal" data-target="${nextStageModal}" data-container="body"</#if>><#compress>
										<@spring.message code=setItem.nextStage.actionCode />
									</#compress></a>
								<#else>
									<@spring.message code=setItem.nextStage.actionCode />
								</#if>
							<#elseif setItem.progress.percentage == 100>
								Complete
							</#if>

							<#local notInMembershipCount = set.studentsNotInMembershipCount />

							<#if notInMembershipCount gt 0>
								<#local tooltip><@fmt.p notInMembershipCount "student has" "students have" /> deregistered</#local>

								<a href="<@routes.groups.deregisteredStudents set />" class="use-tooltip warning" style="display: inline;" title="${tooltip}"><i class="fa fa-exclamation-triangle warning"></i></a>
							</#if>
						</#if>
					</div>

				</div>

				<div class="striped-section-contents">
					<#if expand_by_default>
						<@single_set_inner setItem />
					</#if>
				</div>
			</div>
		</div> <!-- module-info striped-section-->
	</span>
</#macro>

<#macro single_set_inner setItem>
	<#local set = setItem.set />
	<#local has_groups = setItem.groups?size gt 0 />
	<#local can_update_groups = can.do('SmallGroups.Update', set) />

	<#-- Overall info -->
	<div class="row">
		<div class="col-md-2">
			<#if can_update_groups>
				<a href="<@routes.groups.editsetgroups set />"><@fmt.p setItem.groups?size "group" /></a>
			<#else>
				<@fmt.p setItem.groups?size "group" />
			</#if>
		</div>
		<div class="col-md-2">
			<#if can_update_groups>
				<a href="<@routes.groups.editsetstudents set />"><@fmt.p set.allStudentsCount "student" /></a>
			<#else>
				<a href="<@routes.groups.studentsinsetlist set />" class="ajax-modal" data-target="#students-list-modal"><@fmt.p set.allStudentsCount "student" /></a>
			</#if>
		</div>
		<div class="col-md-8">
			<#local unallocatedSize = set.unallocatedStudentsCount />
			<#local notInMembershipCount = set.studentsNotInMembershipCount />

			<#if unallocatedSize gt 0>
				<#if can_update_groups>
					<a href="<@routes.groups.editsetallocate set />"><@fmt.p unallocatedSize "unallocated student" /></a>
					<#if notInMembershipCount gt 0><br></#if>
				<#else>
					<a href="<@routes.groups.unallocatedstudentslist set />" class="ajax-modal" data-target="#students-list-modal"><@fmt.p unallocatedSize "unallocated student" /></a>
				</#if>
			</#if>

			<#if notInMembershipCount gt 0 && can_update_groups>
				<a href="<@routes.groups.deregisteredStudents set />"><@fmt.p notInMembershipCount "student has" "students have" /> deregistered</a>
				<i class="fa fa-exclamation-triangle warning"></i>
			</#if>
		</div>
	</div>
	<#-- Fix nth-of-type zebra striping -->
	<div></div>

	<#if has_groups>
		<#list setItem.groups as groupItem>
			<#local group = groupItem.group />
			<#local showAttendanceButton = !(showOccurrenceAttendance!false) &&
				features.smallGroupTeachingRecordAttendance &&
				can.do('SmallGroupEvents.ViewRegister', group) &&
				group.hasScheduledEvents &&
				group.groupSet.collectAttendance
			/>

			<div class="item-info row group-${group.id}" >
				<div class="col-md-2">
					<h4 class="name">
					${group.name!""}
					</h4>
				</div>
				<div class="col-md-2">
					<#if can.do('SmallGroups.ReadMembership', group) && setItem.canViewMembers && ((group.students.size)!0) gt 0>
						<a href="<@routes.groups.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
							<@fmt.p (group.students.size)!0 "student" "students" />
						</a>
					<#else>
						<@fmt.p (group.students.size)!0 "student" "students" />
					</#if>
				</div>

				<div class="${showAttendanceButton?string("col-md-6", "col-md-8")}">
					<ul class="unstyled margin-fix">
						<#list groupItem.events as eventItem>
							<li class="clearfix">
								<#if (showOccurrenceAttendance!false) && eventItem.occurrences?has_content>
									<a href="<@routes.groups.registerForWeek eventItem.event, eventItem.occurrences?first.week />" class="btn btn-primary btn-xs pull-right">
										Attendance
									</a>
								</#if>
								<@eventShortDetails eventItem.event />

								<#local popoverContent><@eventDetails eventItem.event /></#local>
								<a class="use-popover"
								   data-html="true"
								   data-content="${popoverContent?html}"><i class="fa fa-question-circle"></i></a>
							</li>
						</#list>
					</ul>
				</div>

				<#if showAttendanceButton>
					<div class="col-md-2">
						<a href="<@routes.groups.groupAttendance group />" class="btn btn-primary btn-xs pull-right">
							Attendance
						</a>
					</div>
				</#if>
			</div>
		</#list>
	</#if>
</#macro>

<#-- module_info: takes a GroupsViewModel.ViewModules and renders out
	a collection of modules with group sets and groups.

	How the data is organised (which modules/sets/groups) is up to
	the command generating the view model. No user checks in here!
 -->
<#macro module_info data expand_by_default=true>
<div class="small-group-modules-list">
	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade"></div>
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<#-- Immediately start waiting for collapsibles to load - don't wait to wire this handler in, because we initialise collapsibles before the DOM has loaded below -->
	<script type="text/javascript">
		jQuery(document.body).on('loaded.collapsible', '.module-info', function() {
			var $module = jQuery(this);
			Groups.zebraStripeGroups($module);
			$module.mapPopups();
			$module.find('.use-tooltip').tooltip();
			$module.find('.use-popover').tabulaPopover({
				trigger: 'click',
				container: '#container'
			});
		});
	</script>

	<#list data.moduleItems as moduleItem>
		<@single_module moduleItem=moduleItem canManageDepartment=data.canManageDepartment expand_by_default=expand_by_default />

		<#if !expand_by_default>
			<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
			<script type="text/javascript">
				GlobalScripts.initCollapsible(jQuery('#module-${module.code}').filter(':not(.empty)'));
			</script>
		</#if>
	</#list>
</div> <!-- small-group-modules-list-->

</#macro>


<#macro single_module moduleItem canManageDepartment expand_by_default=true>

	<#local module=moduleItem.module />
	<span id="${module_anchor(module)}-container">

		<#local has_groups=(moduleItem.setItems!?size gt 0) />
		<#local has_archived_groups=false />
		<#list moduleItem.setItems as setItem>
			<#if setItem.set.archived>
				<#local has_archived_groups=true />
			</#if>
		</#list>

		<div id="${module_anchor(module)}" class="module-info striped-section<#if has_groups> collapsible<#if expand_by_default> expanded</#if></#if><#if canManageDepartment && !has_groups> empty</#if>"
			<#if has_groups && !expand_by_default>
				data-populate=".striped-section-contents"
				data-href="<@routes.groups.modulehome module />"
			</#if>
			 data-name="${module_anchor(module)}">
			<div class="clearfix">

				<div class="btn-group section-manage-button">
					<@dropdown_menu "Manage" "wrench">
						<#if moduleItem.canManageGroups>
							<li><a href="<@routes.groups.moduleperms module />">
								Edit module permissions
							</a></li>
							<li>
								<#local create_url><@routes.groups.createset module /></#local>
								<@fmt.permission_button
									permission='SmallGroups.Create'
									scope=module
									action_descr='add small groups'
									href=create_url
								>
									Add small groups
								</@fmt.permission_button>
							</li>
						</#if>

						<#if !(showOccurrenceAttendance!false) && can.do('SmallGroupEvents.ViewRegister', module)>
							<#local module_attendance_url><@routes.groups.moduleAttendance module /></#local>
							<li>
								<@fmt.permission_button permission='SmallGroupEvents.ViewRegister' scope=module action_descr='view attendance' href=module_attendance_url
									tooltip='View attendance at groups' data_attr='data-popup-target=.btn-group data-container=body'
								>
									Attendance
								</@fmt.permission_button>
							</li>
						</#if>

						<#if moduleItem.canManageGroups && has_archived_groups>
							<li><a class="show-archived-small-groups" href="#">
								Show archived small groups
							</a></li>
						</#if>
					</@dropdown_menu>
				</div>

				<h4 class="section-title with-button"><@fmt.module_name module /></h4>

				<#if has_groups>
					<div class="striped-section-contents">
						<#if expand_by_default>
							<@groupsets_info moduleItem />
						</#if>
					</div>
				</#if>
			</div>
		</div> <!-- module-info striped-section-->
	</span>
</#macro>

<#macro groupsets_info moduleItem>
	<#list moduleItem.setItems as setItem>
		<span id="groupset-container-${setItem.set.id}">
			<@single_groupset setItem moduleItem/>
		</span>
	</#list>
</#macro>

<#macro single_groupset setItem moduleItem>
	<#local groupSet=setItem.set />
	<#if !groupSet.deleted>
		<div class="item-info row<#if groupSet.archived> archived</#if> groupset-${groupSet.id}" >
		<#if setItem.viewerMustSignUp>
		  <form id="select-signup-${setItem.set.id}" method="post" action="<@routes.groups.signup_to_group setItem.set />">
		</#if>
			<div class="col-md-2">
				<h3 class="name">
					<small>
						${groupSet.name}
						<#if groupSet.archived>
							(Archived)
						</#if>
					</small>
					<#if setItem.viewerIsStudent >
						<#if !setItem.isStudentSignUp()
							 || (setItem.isStudentSignUp() && !setItem.set.allowSelfGroupSwitching)
							 || (setItem.isStudentSignUp() && !setItem.set.openForSignups)
						>
							<span class="use-tooltip" title="You cannot change this group allocation via Tabula. Please speak to your department if you need to change groups"><i class="fa fa-lock"></i></span>
						<#else>
							<span class="use-tooltip" title="This group is open for self sign-up"><i class="fa fa-unlock-alt"></i></span>
						</#if>
					<#else>
						<#if setItem.isStudentSignUp()>
							<#if setItem.set.openForSignups>
								<span class="use-tooltip" title="This group is open for self sign-up"><i class="fa fa-unlock-alt"></i></span>
							<#else>
								<span class="use-tooltip" title="This group is closed for self sign-up"><i class="fa fa-lock"></i></span>
							</#if>
						<#elseif setItem.isLinked()>
							<span class="use-tooltip" title="Allocations for this group are linked and reused"><i class="fa fa-link"></i></span>
						<#else>
							<span class="use-tooltip" title="This is a manually allocated group"><i class="fa fa-random"></i></span>
						</#if>
					</#if>
				</h3>

				<span class="format">
				${groupSet.format.description}
				</span>
			</div>

			<div class="${moduleItem.canManageGroups?string('col-md-8','col-md-10')}">
				<#if allocated?? && allocated.id == groupSet.id>
					<div class="alert alert-info">
						<button type="button" class="close" data-dismiss="alert">&times;</button>
						<p>Changes saved.</p>
					</div>
				</#if>
				<#if notificationSentMessage??>
					<div class="alert alert-info">
						<button type="button" class="close" data-dismiss="alert">&times;</button>
						<p>${notificationSentMessage}</p>
					</div>
				</#if>

				<#list setItem.groups as groupItem>
					<#local group = groupItem.group />
					<div class="row group">
						<div class="col-md-12">
							<#if setItem.viewerMustSignUp>
								<div class="pull-left ${group.full?string('use-tooltip" title="There are no spaces left on this group"','"')}>
									<input type="radio"
										name="group"
										value="${group.id}"
										${group.full?string(' disabled ','')}
										class="radio inline group-selection-radio"/>
								</div>
								<div style="margin-left: 20px;">
							<#else>
								<div>
							</#if>
								<h4 class="name">
									${group.name!""}
									<#if can.do('SmallGroups.ReadMembership', group) && setItem.canViewMembers >
										<a href="<@routes.groups.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
											<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
										</a>
									<#else>
										<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
									</#if>
								</h4>

								<#if !(showOccurrenceAttendance!false) &&
									features.smallGroupTeachingRecordAttendance &&
									can.do('SmallGroupEvents.ViewRegister', group) &&
									group.hasScheduledEvents &&
									group.groupSet.collectAttendance
								>
									<div class="pull-right">
										<a href="<@routes.groups.groupAttendance group />" class="btn btn-primary btn-small">
											Attendance
										</a>
									</div>
								</#if>

								<#if setItem.viewerIsStudent
										&& setItem.isStudentSignUp()
										&& setItem.set.allowSelfGroupSwitching
										&& setItem.set.openForSignups >
									<#if !setItem.viewerMustSignUp >
										<form id="leave-${setItem.set.id}" method="post" action="<@routes.groups.leave_group setItem.set />" >
											<input type="hidden" name="group" value="${group.id}" />
											<input type="submit"
												   class="btn btn-primary pull-right use-tooltip"
												   title='Leave this group. You will need to sign up for a different group.'
												   value="Leave"/>
										 </form>
									</#if>
								</#if>


								<ul class="unstyled margin-fix">
									<#list groupItem.events as eventItem>
										<#local event = eventItem.event />
										<li class="clearfix">
											<#-- Tutor, weeks, day/time, location -->
											<div class="eventWeeks">
												<#if (showOccurrenceAttendance!false) && eventItem.occurrences?has_content>
													<a href="<@routes.groups.registerForWeek eventItem.event, eventItem.occurrences?first.week />" class="btn btn-primary btn-xs pull-right">
														Attendance
													</a>
												</#if>
												<#if setItem.canViewTutors && event.tutors?? && (!(isSelf!false) || event.tutors.size > 1) >
													<h6>Tutor<#if (event.tutors.size > 1)>s</#if>:
														<#if (event.tutors.size < 1)>[no tutor]</#if>
													<#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
													</h6>
												</#if>
												<@event_schedule_info event />
											</div>
										</li>
									</#list>
								</ul>
							</div>
						</div>
					</div>
				</#list>
				<#if setItem.viewerMustSignUp>
					<input type="submit" class="btn btn-primary pull-right sign-up-button" value="Sign Up"/>
					<a data-href="<@routes.groups.signup_to_group_timetableclash_info setItem.set />" class="timetable-clash-link"  data-toggle="modal" data-target="#timetable-clash-modal"></a>
					</form>
				</#if>
				<#-- Only show warnings to users that can do somthing about them -->
				<#if moduleItem.canManageGroups>
					<#local unallocatedSize = groupSet.unallocatedStudentsCount />
					<#if unallocatedSize gt 0>
						<div class="alert">
							<a href="<@routes.groups.unallocatedstudentslist setItem.set />" class="ajax-modal" data-target="#students-list-modal"><@fmt.p unallocatedSize "student has" "students have" /> not been allocated to a group</a>
						</div>
					</#if>

					<#if groupSet.hasAllocated >
						 <#-- not released at all -->
						  <#if (!groupSet.releasedToStudents && !groupSet.releasedToTutors)>
						<p class="alert">
							These groups have not been published
						</p>
						 <#-- only released to tutors-->
						 <#elseif (!groupSet.releasedToStudents && groupSet.releasedToTutors)>
						  <p class="alert">
							   These groups have been published to tutors, but not students
						   </p>
						  <#-- only released to students-->
						  <#elseif (groupSet.releasedToStudents && !groupSet.releasedToTutors)>
							  <p class="alert">
								  These groups have been published to students, but not tutors
							  </p>
						 </#if>
					</#if>
				</#if>
			</div>

			<#if moduleItem.canManageGroups>
				<div class="col-md-2">
					<div class="btn-toolbar pull-right">
						<div class="btn-group">

							<@dropdown_menu "Actions" "cog">
								<li>
									<#local edit_url><@routes.groups.editset groupSet /></#local>
									<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=groupSet
										action_descr='edit small group properties'
										href=edit_url
									>
										Edit properties
									</@fmt.permission_button>
								</li>
								<#if features.smallGroupTeachingStudentSignUp>
									<#if groupSet.openForSignups>
										<li ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
										(''," class='disabled use-tooltip' title='Not a self-signup group' ")
										}>
											<#local closeset_url><@routes.groups.closeset groupSet /></#local>
											<@fmt.permission_button
												permission='SmallGroups.Update'
												scope=groupSet
												classes='close-group-link'
												action_descr='close small group'
												href=closeset_url
												data_attr='data-toggle=modal data-target=#modal-container'
											>
												Close
											</@fmt.permission_button>
										</li>
									<#else>
										<li ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
										(''," class='disabled use-tooltip' title='Not a self-signup group' ")
										}>
											<#local openset_url><@routes.groups.openset groupSet /></#local>
											<@fmt.permission_button
												permission='SmallGroups.Update'
												scope=groupSet
												classes='open-group-link'
												action_descr='open small group'
												href=openset_url
												data_attr='data-toggle=modal data-target=#modal-container data-container=body'
											>
												Open
											</@fmt.permission_button>
										</li>
									</#if>
								</#if>
								<li>
									<#local allocateset_url><@routes.groups.allocateset groupSet /></#local>
									<@fmt.permission_button
										permission='SmallGroups.Allocate'
										scope=groupSet
										action_descr='allocate students'
										href=allocateset_url
									>
										Allocate students
									</@fmt.permission_button>
								</li>
								<li ${groupSet.fullyReleased?string(" class='disabled use-tooltip' title='Already published' ",'')} >
									<#local notifyset_url><@routes.groups.releaseset groupSet /></#local>
									<@fmt.permission_button
										permission='SmallGroups.Update'
										scope=groupSet
										action_descr='publish groups to students and staff'
										href=notifyset_url
										classes='notify-group-link'
										data_attr='data-toggle=modal data-target=#modal-container data-container=body'
									>
										Publish
									</@fmt.permission_button>
								</li>

								<#if groupSet.collectAttendance>
									<#local set_attendance_url><@routes.groups.setAttendance groupSet /></#local>
									<li>
										<@fmt.permission_button permission='SmallGroupEvents.ViewRegister' scope=groupSet action_descr='view attendance' href=set_attendance_url
											tooltip='View attendance at groups' data_attr='data-popup-target=.btn-group data-container=body'
										>
											Attendance
										</@fmt.permission_button>
									</li>
								</#if>


								<li>
									<#if groupSet.archived>
										<#local archive_caption>Unarchive groups</#local>
									<#else>
										<#local archive_caption>Archive groups</#local>
									</#if>

									<#local archive_url><@routes.groups.archiveset groupSet /></#local>

									<@fmt.permission_button permission='SmallGroups.Archive' scope=groupSet action_descr='${archive_caption}'?lower_case classes='archive-group-link' href=archive_url
										tooltip='Archive small group' data_attr='data-toggle=modal data-target=#modal-container'
									>
										${archive_caption}
									</@fmt.permission_button>
								</li>
							</@dropdown_menu>
						</div>
					</div>
				</div>
			</#if>

			<#if setItem.viewerMustSignUp>
			</form>
			</#if>
		</div>
	</#if>
</#macro>

<#macro instanceFormat instance academicYear department><#compress>
	<#local event = instance._1() />
	<#local week = instance._2() />
	<#if event.title?has_content>${event.title} </#if>${event.day.shortName} <@fmt.time event.startTime />, <@fmt.singleWeekFormat week academicYear department />
</#compress></#macro>

<#macro studentAttendanceRow student attendance notes instances group>
	<#local set = group.groupSet />
	<#local module = set.module />
	<#local department = module.adminDepartment />
	<#local academicYear = set.academicYear />

	<tr>
		<#list instances as instance>
			<#local state = mapGet(attendance, instance) />
			<#local title><@instanceFormat instance academicYear department /></#local>

			<#if state.name == 'Attended'>
				<#local class = "fa-check attended" />
				<#local title = "${student.fullName} attended: " + title />
			<#elseif state.name == 'MissedAuthorised'>
				<#local class = "fa-times-circle-o authorised" />
				<#local title = "${student.fullName} did not attend (authorised absence): " + title />
			<#elseif state.name == 'MissedUnauthorised'>
				<#local class = "fa-times unauthorised" />
				<#local title = "${student.fullName} did not attend (unauthorised): " + title />
			<#elseif state.name == 'Late'>
				<#local class = "fa-exclamation-triangle late" />
				<#local title = "No data: " + title />
			<#elseif state.name == 'NotExpected'>
				<#local class = "" />
				<#local title = "No longer in group" />
			<#else>
				<#local class = "fa-minus" />
			</#if>

			<#local titles = [title] />

			<#if mapGet(notes, instance)??>
				<#local studentNote = mapGet(notes, instance) />
				<#local note>
					${studentNote.absenceType.description}<br />
					${studentNote.truncatedNote}
					<#if (studentNote.truncatedNote?length > 0)>
						<br/>
					</#if>
					<a class='attendance-note-modal' href='<@routes.groups.viewNote studentNote.student studentNote.occurrence />'>View attendance note</a>
				</#local>
				<#local titles = titles + [note] />
			</#if>

			<#local renderedTitle>
				<#list titles as t>
					<#if (titles?size > 1)>
						<p>${t}</p>
					<#else>
					${t}
					</#if>
				</#list>
			</#local>

			<td>
				<span class="fa-stack fa-stack-original-size fa-stack-right fa-fw use-popover" data-content="${renderedTitle?replace('\"', '')}" data-html="true">
					<i class="fa fa-fw fa-stack-2x ${class}"></i>
					<#if mapGet(notes, instance)??><i class="fa fa-fw fa-stack-1x fa-envelope-o fa-filled-white"></i></#if>
				</span>
			</td>
		</#list>
	</tr>
</#macro>

<#macro singleGroupAttendance group instances studentAttendance attendanceNotes showRecordButtons=true>
	<#local set = group.groupSet />
	<#local module = set.module />
	<#local department = module.adminDepartment />
	<#local academicYear = set.academicYear />

	<div class="group-attendance" id="group-attendance-${group.id}">
		<div class="scrollable-table">
			<div class="scrollable-table-row">
				<div class="left">
					<table class="table table-striped table-condensed attendance-table">
						<thead>
							<tr>
								<th class="sortable student">Student</th>
								<th class="sortable missed"></th>
							</tr>
						</thead>
						<tbody>
							<#list studentAttendance?keys as student>
								<#local attendance = mapGet(studentAttendance, student) />
								<#local missedCount = 0 />
								<#list instances as instance>
									<#local state = mapGet(attendance, instance) />
									<#if state.name == 'MissedUnauthorised'>
										<#local missedCount = missedCount + 1 />
									</#if>
								</#list>
								<tr>
									<td data-sortBy="${student.lastName}, ${student.firstName}" title="${student.fullName}">
										<div class="ellipsis">${student.firstName!}</div>
										<div class="ellipsis">${student.lastName!}</div>
										<@pl.profile_link student.warwickId! />
									</td>
									<td>
										<span class="badge progress-bar-<#if (missedCount > 2)>danger<#elseif (missedCount > 0)>warning<#else>success</#if>">${missedCount}</span>
									</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</div>
				<div class="right">
					<table class="table table-striped table-condensed attendance-table sb-no-wrapper-table-popout">
						<thead>
						<tr>
							<#list instances as instance>
								<#local event = instance._1() />
								<#local week = instance._2() />

								<th class="instance-date-header">
									<div class="instance-date use-tooltip"
										 data-container="body"
										 title="Tutor<#if (event.tutors.size > 1)>s</#if>: <#if (event.tutors.size < 1)>[no tutor]</#if><#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>">
										<@instanceFormat instance academicYear department />
									</div>

									<#if showRecordButtons && features.smallGroupTeachingRecordAttendance && !event.unscheduled>
										<#if can.do("SmallGroupEvents.ViewRegister", event)>
											<div class="eventRegister">
												<a class="btn btn-xs btn-default use-tooltip <#if !can.do("SmallGroupEvents.Register", event)>disabled</#if>"
												   href="<@routes.groups.registerForWeek event week/>&returnTo=${(info.requestedUri!"")?url}"
												   title="<#if can.do("SmallGroupEvents.Register", event)>Record attendance for <@instanceFormat instance academicYear department /><#else>You don't have permission to record attendance for <@instanceFormat instance academicYear department /></#if>"
												   data-html="true"
												   data-container="body"
												>
													Record
												</a>
											</div>
										</#if>
									</#if>
								</th>
							</#list>
						</tr>
						</thead>
						<tbody>
							<#list studentAttendance?keys as student>
								<#local attendance = mapGet(studentAttendance, student) />
								<#local notes = mapGet(attendanceNotes, student) />
								<@studentAttendanceRow student=student attendance=attendance notes=notes instances=instances group=group />
							</#list>
						</tbody>
					</table>
				</div>
			</div>
		</div>

		<#if studentAttendance?keys?size gt 0>
			<script type="text/javascript">
				jQuery(window).on('load', function(){
					GlobalScripts.scrollableTableSetup();
				});
				jQuery(function($){
					var $leftTable = $('#group-attendance-${group.id} .left table');
					GlobalScripts.tableSortMatching([
						$leftTable,
						$('#group-attendance-${group.id} .right table')
					]);
					$leftTable.sortableTable({
						textExtraction: function(node) {
							var $el = $(node);
							if ($el.data('sortby')) {
								return $el.data('sortby');
							} else {
								return $el.text().trim();
							}
						}
					});
				});
			</script>
		</#if>
	</div>
</#macro>

<#macro single_groupset_attendance groupSet groups>
	<#local module = groupSet.module />

	<div class="striped-section <#if groups?keys?has_content>collapsible expanded</#if>"
		 data-name="${module_anchor(module)}">
		<div class="clearfix">
			<h4 class="section-title">${groupSet.name}</h4>

			<div class="striped-section-contents">
				<#list groups?keys as group>
					<div class="item-info clearfix">
						<h4 class="name">
							${group.name}
							<#if can.do("SmallGroups.ReadMembership", group)>
								<a href="<@routes.groups.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
									<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
								</a>
							<#else>
								<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
							</#if>
						</h4>

						<div id="group-attendance-container-${group.id}">
							<#local attendanceInfo = mapGet(groups, group) />

							<@singleGroupAttendance group attendanceInfo.instances attendanceInfo.attendance attendanceInfo.notes />
						</div>
					</div>
				</#list>
			</div>
		</div>
	</div> <!-- attendance-info striped-section-->
</#macro>

<#macro single_module_attendance_contents module sets>
	<#list sets?keys as set>
		<#local groups = mapGet(sets, set) />

		<div class="item-info row clearfix">
			<div class="col-md-2">
				<h3 class="name">
					<small>
					${set.name}
						<#if set.archived>
							(Archived)
						</#if>
					</small>
				</h3>

				<span class="format">
					${set.format.description}
				</span>
			</div>

			<div class="col-md-10">
				<#list groups?keys as group>
					<h4 class="name">
						${group.name}
						<#if can.do("SmallGroups.ReadMembership", group)>
							<a href="<@routes.groups.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
								<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
							</a>
						<#else>
							<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
						</#if>
					</h4>

					<div id="group-attendance-container-${group.id}">
						<#local attendanceInfo = mapGet(groups, group) />

						<@singleGroupAttendance group attendanceInfo.instances attendanceInfo.attendance attendanceInfo.notes />
		      </div>
		    </#list>
	    </div>
    </div>
  </#list>
</#macro>

<#macro department_attendance department modules academicYear>
	<div class="small-group-modules-list">
	<#list modules as module>
		<span id="${module_anchor(module)}-container">
			<#local has_groups=(module.groupSets!?size gt 0) />
			<#local has_archived_groups=false />
			<#list module.groupSets as set>
				<#if set.archived>
					<#local has_archived_groups=true />
				</#if>
			</#list>

			<a id="${module_anchor(module)}"></a>
			<div class="striped-section collapsible"
				 data-populate=".striped-section-contents"
				 data-href="<@routes.groups.moduleAttendanceInYear module academicYear/>"
				 data-name="${module_anchor(module)}">
				<div class="clearfix">
					<h4 class="section-title with-button"><@fmt.module_name module /></h4>

					<div class="striped-section-contents">
			    </div>
			  </div>
			</div> <!-- module-info striped-section-->
		</span>
	</#list>
	</div> <!-- small-group-modules-list-->
</#macro>

<#function sortClass field command>
	<#list command.sortOrder as order>
		<#if order.propertyName == field>
			<#if order.ascending>
				<#return "headerSortDown" />
			<#else>
				<#return "headerSortUp" />
			</#if>
		</#if>
	</#list>
	<#return "" />
</#function>

<#macro manageStudentTable
	membershipItems
	doSorting=false
	command=""
	checkboxName=""
	onlyShowCheckboxForStatic=false
	checkAll=false
	showRemoveButton=false
	showResetButton=false
>

	<#if (membershipItems?size > 0)>

		<table class="manage-student-table table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tablesorter sb-no-wrapper-table-popout">
			<thead>
			<tr>
				<th style="width: 50px;" <#if doSorting> class="${sortClass("source", command)} sortable" data-field="source"</#if>>Source</th>
				<th <#if doSorting> class="${sortClass("firstName", command)} sortable" data-field="firstName"</#if>>First name</th>
				<th <#if doSorting> class="${sortClass("lastName", command)} sortable" data-field="lastName"</#if>>Last name</th>
				<th <#if doSorting> class="${sortClass("universityId", command)} sortable" data-field="universityId"</#if>>ID</th>
				<th <#if doSorting> class="${sortClass("userId", command)} sortable" data-field="userId"</#if>>User</th>
				<#if checkboxName?has_content>
					<th style="width: 65px; padding-right: 5px;" <#if checkAll>class="for-check-all"</#if>>
						<#if showRemoveButton>
							<input class="btn btn-danger btn-sm use-tooltip"
							  <#if findCommandResult.membershipItems?size == 0>disabled</#if>
							  type="submit"
							  name="${ManageDepartmentSmallGroupsMappingParameters.manuallyExclude}"
							  value="Remove"
							  title="Remove selected students from these groups"
							  style="margin-left: 0.5em;"
							/>
						</#if>
						<#if (showResetButton && (editMembershipCommandResult.includedStudentIds?size > 0 || editMembershipCommandResult.excludedStudentIds?size > 0))>
							<input class="btn btn-danger btn-sm use-tooltip"
							   type="submit"
							   style="float: right; padding-left: 5px; padding-right: 5px; margin-left: 5px;"
							   name="${ManageDepartmentSmallGroupsMappingParameters.resetMembership}"
							   value="Reset"
							   data-container="body"
							   title="Restore the manually removed and remove the manually added students selected"
							/>
						</#if>
					</th>
				</#if>
			</tr>
			</thead>
			<tbody>
				<#list membershipItems as item>
					<tr class="${item.itemTypeString}">
						<td>
							<#if item.itemTypeString == "static">
								<span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="fa fa-list-alt"></i></span>
							<#elseif item.itemTypeString == "exclude">
								<span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="fa fa-ban"></i></span>
							<#else>
								<span class="use-tooltip" title="Added manually" data-placement="right"><i class="fa fa-hand-o-up"></i></span>
							</#if>
						</td>
						<td>${item.firstName}</td>
						<td>${item.lastName}</td>
						<td>${item.universityId}</td>
						<td>${item.userId}</td>
						<#if checkboxName?has_content>
							<td>
								<#if !onlyShowCheckboxForStatic || item.itemTypeString == "static">
									<input type="checkbox" name="${checkboxName}" value="${item.universityId}" />
								</#if>
							</td>
						</#if>
					</tr>
				</#list>
			</tbody>
		</table>
	</#if>
</#macro>

<#macro set_wizard is_new current_step set>
	<#local is_linked = set.linked />
	<#local has_groups = set.groups?size gt 0 />

	<p class="progress-arrows">
		<#if !(set.id?has_content)>
			<@wizard_link
				label="Properties"
				is_first=true
				is_active=(current_step == 'properties')
				is_available=true
				tooltip="Edit properties" />

			<@wizard_link
				label="Groups"
				is_first=false
				is_active=(current_step == 'groups')
				is_available=false
				tooltip="Edit groups" />

			<@wizard_link
				label="Students"
				is_first=false
				is_active=(current_step == 'students')
				is_available=false
				tooltip="Edit students" />

			<@wizard_link
				label="Events"
				is_first=false
				is_active=(current_step == 'events')
				is_available=false
				tooltip="Edit events" />

			<@wizard_link
				label="Allocation"
				is_first=false
				is_active=(current_step == 'allocate')
				is_available=false
				tooltip="Allocate students to groups" />
		<#else>
			<#local properties_url><#if is_new><@routes.groups.createeditproperties smallGroupSet /><#else><@routes.groups.editsetproperties smallGroupSet /></#if></#local>
			<@wizard_link
				label="Properties"
				is_first=true
				is_active=(current_step == 'properties')
				is_available=true
				tooltip="Edit properties"
				url=properties_url />

			<#local groups_url><#if is_new><@routes.groups.createsetgroups smallGroupSet /><#else><@routes.groups.editsetgroups smallGroupSet /></#if></#local>
			<@wizard_link
				label="Groups"
				is_first=false
				is_active=(current_step == 'groups')
				is_available=true
				tooltip="Edit groups"
				url=groups_url />

			<#local students_url><#if is_new><@routes.groups.createsetstudents smallGroupSet /><#else><@routes.groups.editsetstudents smallGroupSet /></#if></#local>
			<@wizard_link
				label="Students"
				is_first=false
				is_active=(current_step == 'students')
				is_available=true
				tooltip="Edit students"
				url=students_url />

			<#local events_url><#if is_new><@routes.groups.createsetevents smallGroupSet /><#else><@routes.groups.editsetevents smallGroupSet /></#if></#local>
			<@wizard_link
				label="Events"
				is_first=false
				is_active=(current_step == 'events')
				is_available=has_groups
				tooltip="Edit events"
				url=events_url />

			<#local allocate_url><#if is_new><@routes.groups.createsetallocate smallGroupSet /><#else><@routes.groups.editsetallocate smallGroupSet /></#if></#local>
			<@wizard_link
				label="Allocation"
				is_first=false
				is_active=(current_step == 'allocate')
				is_available=has_groups
				tooltip="Allocate students to groups"
				url=allocate_url />
		</#if>
	</p>
</#macro>

<#macro reusable_set_wizard is_new current_step set>
	<#local has_groups = set.groups?size gt 0 />

	<p class="progress-arrows">
		<#local properties_action><#if is_new>${ManageDepartmentSmallGroupsMappingParameters.createAndEditProperties}<#else>${ManageDepartmentSmallGroupsMappingParameters.editAndEditProperties}</#if></#local>
		<@wizard_button
			label="Properties"
			is_first=true
			is_active=(current_step == 'properties')
			is_available=true
			tooltip="Save and edit properties"
			action=properties_action />

		<#local groups_action><#if is_new>${ManageDepartmentSmallGroupsMappingParameters.createAndAddGroups}<#else>${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}</#if></#local>
		<@wizard_button
			label="Groups"
			is_first=false
			is_active=(current_step == 'groups')
			is_available=true
			tooltip="Save and edit groups"
			action=groups_action />

		<#local students_action><#if is_new>${ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents}<#else>${ManageDepartmentSmallGroupsMappingParameters.editAndAddStudents}</#if></#local>
		<@wizard_button
			label="Students"
			is_first=false
			is_active=(current_step == 'students')
			is_available=true
			tooltip="Save and edit students"
			action=students_action />

		<#local allocate_action><#if is_new>${ManageDepartmentSmallGroupsMappingParameters.createAndAllocate}<#else>${ManageDepartmentSmallGroupsMappingParameters.editAndAllocate}</#if></#local>
		<@wizard_button
			label="Allocation"
			is_first=false
			is_active=(current_step == 'allocate')
			is_available=has_groups
			tooltip="Save and allocate students to groups"
			action=allocate_action />
	</p>
</#macro>

<#macro wizard_button label is_first is_active is_available tooltip="" action="">
	<span class="arrow-right<#if !is_first> arrow-left</#if><#if is_active> active</#if><#if is_available && !is_active> use-tooltip</#if>" <#if is_available && !is_active>title="${tooltip}"</#if>><#compress>
		<#if is_available && !is_active>
			<button type="submit" class="btn btn-link" name="${action}">${label}</button>
		<#else>
			${label}
		</#if>
	</#compress></span>
</#macro>

<#macro wizard_link label is_first is_active is_available tooltip="" url="">
	<span class="arrow-right<#if !is_first> arrow-left</#if><#if is_active> active</#if><#if is_available && !is_active> use-tooltip</#if>" <#if is_available && !is_active>title="${tooltip}"</#if>><#compress>
		<#if is_available && !is_active>
			<a href="${url}">${label}</a>
		<#else>
			${label}
		</#if>
	</#compress></span>
</#macro>

<#macro week_selector path allTerms smallGroupSet>
	<#local label>
		Runs in these weeks
	</#local>
	<@bs3form.labelled_form_group path=path labelText="${label}">
		<table class="table table-striped week-selector">
			<thead>
				<tr>
					<td>
						<@bs3form.checkbox>
							<input type="checkbox" class="show-vacations" value="true">
							Show vacations
						</@bs3form.checkbox>
					</td>
				</tr>
			</thead>
			<tbody>
				<#local maxCells = 0 />
				<#list allTerms as namedTerm>
					<#local weeks = namedTerm.weekRange.maxWeek - namedTerm.weekRange.minWeek + 1 />
					<#if (weeks > maxCells)>
						<#local maxCells = weeks />
					</#if>
				</#list>
				<#list allTerms as namedTerm>
					<#local is_vacation = !(namedTerm.term.termType?has_content) />
					<tr<#if is_vacation> class="vacation"</#if>>
						<th>
							<div class="checkbox">
								<label>
									<input type="checkbox" class="collection-check-all" />
									${namedTerm.name}<#if !is_vacation> term</#if>
								</label>
							</div>
						</th>
						<#list namedTerm.weekRange.minWeek..namedTerm.weekRange.maxWeek as weekNumber>
							<td class="use-tooltip"
								title="<@fmt.singleWeekFormat weekNumber smallGroupSet.academicYear smallGroupSet.module.adminDepartment />"
								data-html="true"
								data-container="body"
							>
								<div class="checkbox">
									<label>
									<@f.checkbox path=path value="${weekNumber}" cssClass="collection-checkbox" />
									${weekNumber}
								</label>
								</div>
							</td>
						</#list>
						<#local numberOfWeeks = namedTerm.weekRange.maxWeek - namedTerm.weekRange.minWeek + 1 />
						<#if numberOfWeeks < maxCells>
							<#list numberOfWeeks..(maxCells - 1) as extraWeeks>
								<td class="empty"></td>
							</#list>
						</#if>
					</tr>
				</#list>
			</tbody>
		</table>
	</@bs3form.labelled_form_group>
</#macro>

<#macro eventShortDetails event>
	<#if event.title?has_content><span class="eventTitle">${event.title} - </span></#if>
	<#if event.startTime??><@fmt.time event.startTime /></#if> ${(event.day.name)!""}
</#macro>

<#macro eventDetails event><#compress>
	<#if event.title?has_content><div class="eventTitle">${event.title}</div></#if>
	<div class="day-time">
		${(event.day.name)!""}
		<#if event.startTime??><@fmt.time event.startTime /><#else>[no start time]</#if>
		-
		<#if event.endTime??><@fmt.time event.endTime /><#else>[no end time]</#if>
	</div>
	<#if event.tutors.size gt 0>
		Tutor<#if event.tutors.size gt 1>s</#if>:
		<#list event.tutors.users as tutor> <#compress> <#-- intentional space -->
			${tutor.fullName}<#if tutor_has_next>,</#if>
		</#compress></#list>
	</#if>
	<#if ((event.location.name)!"")?has_content>
		<div class="location">
			Room: <@fmt.location event.location />
		</div>
	</#if>
	<div class="running">
		Running: <#compress>
			<#if event.weekRanges?size gt 0 && event.day??>
				${weekRangesFormatter(event.weekRanges, event.day, event.group.groupSet.academicYear, event.group.groupSet.module.adminDepartment)}
			<#elseif event.weekRanges?size gt 0>
				[no day of week selected]
			<#else>
				[no dates selected]
			</#if>
		</#compress>
	</div>
</#compress></#macro>

<#macro timetableEventDetails event academicYear department><#compress>
	<#if (event.name)?has_content>
		<div class="title">
			${event.name}
		</div>
	</#if>
	<div class="day-time">
		${(event.day.name)!""}
		<#if event.startTime??><@fmt.time event.startTime /><#else>[no start time]</#if>
		-
		<#if event.endTime??><@fmt.time event.endTime /><#else>[no end time]</#if>
	</div>
	<#if event.staff?size gt 0>
		<div class="tutors">
			Tutor<#if event.staff?size gt 1>s</#if>:
			<#list event.staff as user>
				${user.fullName}<#if user_has_next>,</#if>
			</#list>
		</div>
	</#if>
	<#if ((event.location.name)!"")?has_content>
		<div class="location">
			Room: <@fmt.location event.location />
		</div>
	</#if>
	<div class="running">
		Running: <#compress>
			<#if event.weekRanges?size gt 0 && event.day??>
				${weekRangesFormatter(event.weekRanges, event.day, academicYear, department)}
			<#elseif event.weekRanges?size gt 0>
				[no day of week selected]
			<#else>
				[no dates selected]
			</#if>
		</#compress>
	</div>
</#compress></#macro>
