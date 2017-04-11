<#ftl strip_text=true />
<#escape x as x?html>
<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#macro admin_section module expand_by_default=true>
	<#assign can_manage=can.do("Module.ManageAssignments", module) />
	<#assign has_assignments=module.hasLiveAssignments />
	<#assign has_archived_assignments=false />
	<#list module.assignments as assignment>
		<#if !assignment.alive>
			<#assign has_archived_assignments=true />
		</#if>
	</#list>

	<div id="${module_anchor(module)}" class="module-info striped-section<#if has_assignments || has_archived_assignments> collapsible<#if expand_by_default> expanded</#if></#if><#if !expand_by_default && !has_assignments> empty</#if>"
		<#if (has_assignments || has_archived_assignments) && !expand_by_default>
			data-populate=".striped-section-contents"
			data-href="<@routes.coursework.modulehome module />"
			data-name="${module_anchor(module)}"
		</#if>
	>
		<div class="clearfix">
			<div class="btn-group module-manage-button section-manage-button">
			  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
			  <ul class="dropdown-menu pull-right">
					<#if can_manage>
						<li><a href="<@routes.coursework.moduleperms module />">
							<i class="icon-user"></i> Edit module permissions
						</a></li>
					</#if>

					<li>
						<#assign create_url><@routes.coursework.createAssignment module /></#assign>
						<@fmt.permission_button
							permission='Assignment.Create'
							scope=module
							action_descr='create a new assignment'
							href=create_url>
							<i class="icon-plus"></i> Create new assignment
						</@fmt.permission_button>
					</li>
					<li>
						<#assign copy_url><@routes.coursework.copyModuleAssignments module /></#assign>
						<@fmt.permission_button
							permission='Assignment.Create'
							scope=module
							action_descr='copy existing assignments'
							href=copy_url>
							<i class="icon-share-alt"></i> Create assignments from previous
						</@fmt.permission_button>
					</li>
					<li>
						<#assign archive_url><@routes.coursework.archiveModuleAssignments module /></#assign>
						<@fmt.permission_button
							permission='Assignment.Archive'
							scope=module
							action_descr='archive existing assignments'
							href=archive_url>
							<i class="icon-folder-close"></i> Archive assignments
						</@fmt.permission_button>
					</li>

					<#if has_archived_assignments>
						<li><a class="show-archived-assignments" href="#">
							<i class="icon-eye-open"></i> Show archived assignments
							</a>
						</li>
					</#if>
			  </ul>
			</div>

			<h4 class="section-title with-button"><@fmt.module_name module /></h4>
		</div>


		<#if has_assignments || has_archived_assignments>
		<div class="module-info-contents striped-section-contents">
			<#if has_assignments && expand_by_default>
				<@admin_assignments module />
			</#if>
		</div>
		</#if>

	</div>
</#macro>

<#macro admin_assignments module>
	<#list module.assignments as assignment>
		<#if !assignment.deleted>
			<#assign has_feedback = assignment.hasFullFeedback />

			<#-- Build feedback deadline rendering -->
			<#assign feedbackLabel = "" />
			<#assign feedbackDeadline = "" />
			<#if assignment.isClosed() && (features.submissions && assignment.collectSubmissions) && assignment.hasOutstandingFeedback && assignment.feedbackDeadlineWorkingDaysAway??>
				<#assign workingDaysAway = assignment.feedbackDeadlineWorkingDaysAway />
				<#if workingDaysAway lt 0>
					<#if assignment.hasUnreleasedFeedback>
						<#assign feedbackLabel><span class="label label-important use-tooltip" title="There is unreleased feedback, and the default deadline has passed. Drill down to see if there is good reason, such as late submissions.">Late feedback</span></#assign>
					<#elseif assignment.submissions?has_content && !assignment.hasReleasedFeedback>
						<#assign feedbackLabel><span class="label label-important use-tooltip" title="There have been submissions, but no feedback, and the default deadline has passed. Drill down to see if there is good reason, such as late submissions.">Feedback overdue</span></#assign>
					</#if>
				<#elseif workingDaysAway lte 5>
					<#assign feedbackLabel><span class="label use-tooltip" title="The default deadline for feedback is less than five working days away.">Feedback due soon</span></#assign>
				</#if>

				<#assign feedbackDeadline>
					<div class="use-tooltip" title="The deadline for returning feedback is calculated using working days at Warwick.">
						<#if workingDaysAway == 0>
							<b>Feedback due today</b>
						<#elseif workingDaysAway lt 0>
							Feedback deadline was <@fmt.p -workingDaysAway "working day" /> ago,
							<@fmt.date date=assignment.feedbackDeadline includeTime=false />
						<#else>
							Feedback due in <@fmt.p workingDaysAway "working day" />,
							<@fmt.date date=assignment.feedbackDeadline includeTime=false />
						</#if>
					</div>
				</#assign>
			</#if>

			<div class="assignment-info<#if !assignment.alive> archived</#if>">
				<div class="column1">
					<h3 class="name">
						<small>
							${assignment.name}
							<#if !assignment.alive>
								(Archived)
							</#if>
						</small>
					</h3>

					<#if assignment.hasReleasedFeedback || features.submissions>
						<p class="feedback-published">
							<#assign urlforstudents><@routes.coursework.assignment assignment /></#assign>
							<a href="${urlforstudents}" class="linkForStudents">Link for students</a>
							<a class="use-popover" id="popover-${assignment.id}" data-html="true"
								data-original-title="<span class='text-info'><strong>Link for students</strong></span>"
								data-content="This is the assignment page for students. You can give this web address or URL to students so that they can submit work and receive feedback and/or marks. Copy and paste it into an email or publish it on your module web page."><i class="icon-question-sign"></i></a>
						</p>
					</#if>
				</div>

				<div class="stats">
					<#assign membershipInfo = assignment.membershipInfo />
					<#assign memberCount>
						<#if membershipInfo.totalCount == 0>
							<span class="badge badge-mini<#if assignment.restrictSubmissions> badge-important</#if>" title="No enrolled students">0</span>
						<#else>
							<span class="badge badge-mini" title="<@fmt.p membershipInfo.totalCount "enrolled student"/> (${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount} removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, plus ${membershipInfo.usedIncludeCount} added manually</#if>)">${membershipInfo.totalCount}</span>
						</#if>
					</#assign>

					<#if assignment.openEnded>
						<div class="dates">
							<#noescape>${memberCount}</#noescape>
							<@fmt.interval assignment.openDate />, never closes
							(open-ended)
							<#if !assignment.opened>
								<span class="label label-warning">Not yet open</span>
							</#if>
						</div>
					<#else>
						<div class="dates">
							<#noescape>${memberCount}</#noescape>
							<@fmt.interval assignment.openDate assignment.closeDate />
							<#if assignment.closed>
								<span class="label label-warning">Closed</span>
							</#if>
							<#if !assignment.opened>
								<span class="label label-warning">Not yet open</span>
							</#if>

							<#noescape>${feedbackLabel!""}</#noescape>
						</div>
					</#if>

					<div class="submission-and-feedback-count">
						<i class="icon-file"></i>
						<a href="<@routes.coursework.assignmentsubmissionsandfeedback assignment=assignment />">
							<span class="use-tooltip" title="View all submissions and feedback">
							<@fmt.p assignment.submissions?size "submission" /><#--
							--><#if has_feedback> and ${assignment.countFullFeedback} item<#if assignment.countFullFeedback gt 1>s</#if> of feedback</#if><#--
						--></span></a>

						<#assign numUnapprovedExtensions = assignment.countUnapprovedExtensions />
						<#if numUnapprovedExtensions gt 0>
							<span class="has-unapproved-extensions">
								<i class="icon-info-sign"></i>
								<a href="<@routes.coursework.extensions assignment=assignment />" title="Manage extensions" class="use-tooltip">
									<#if numUnapprovedExtensions gt 1>
										${numUnapprovedExtensions} extensions need granting
									<#else>
										1 extension needs granting
									</#if>
								</a>
							</span>
						</#if>

						<#assign countUnreleasedFeedback = assignment.countUnreleasedFeedback />
						<#if countUnreleasedFeedback gt 0>
							<span class="has-unreleased-feedback">
								<i class="icon-info-sign"></i>
								${countUnreleasedFeedback}
								<#if countUnreleasedFeedback gt 1>
									items of feedback need publishing
								<#else>
									item of feedback needs publishing
								</#if>
							</span>
						</#if>

						<#noescape>${feedbackDeadline!""}</#noescape>
					</div>
				</div>

				<div class="assignment-buttons">
					<div class="btn-toolbar">
					<div class="btn-group">
					  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-cog"></i> Actions <span class="caret"></span></a>
					  <ul class="dropdown-menu pull-right">
						<li>
							<#assign edit_url><@routes.coursework.assignmentedit assignment /></#assign>
							<@fmt.permission_button
								permission='Assignment.Update'
								scope=assignment
								action_descr='edit assignment properties'
								href=edit_url>
								<i class="icon-wrench"></i> Edit properties
							</@fmt.permission_button>
						</li>

						<li>
							<#-- TAB-4255 datetime param added to cache-bust popup contents for IE-->
							<#assign archive_url><@routes.coursework.archiveAssignment assignment />?dt=${.now?string('iso')}</#assign>
							<#if !assignment.alive>
								<#assign archive_caption>Unarchive assignment</#assign>
							<#else>
								<#assign archive_caption>Archive assignment</#assign>
							</#if>
							<@fmt.permission_button permission='Assignment.Archive' scope=assignment action_descr='${archive_caption}'?lower_case classes='archive-assignment-link ajax-popup' href=archive_url
													data_attr='data-popup-target=.btn-group data-container=body'>
								<i class="icon-folder-close"></i> ${archive_caption}
							</@fmt.permission_button>
						</a></li>


						<li class="divider"></li>

						<#if !assignment.openEnded>
							<#if assignment.allowExtensions || assignment.hasExtensions>
								<li>
									<#if can.do('Extension.Update', assignment)>
										<#assign ext_caption='Manage extensions' />
									<#else>
										<#assign ext_caption='View extensions' />
									</#if>
									<#assign ext_url><@routes.coursework.extensions assignment /></#assign>
									<@fmt.permission_button
									permission='Extension.Read'
									scope=assignment
									action_descr=ext_caption?lower_case
									href=ext_url>
										<i class="icon-calendar"></i> ${ext_caption}
									</@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Extensions are not allowed on this assignment."><i class="icon-calendar"></i> Grant extensions </a></li>
							</#if>

							<li class="divider"></li>
						</#if>

						<#if assignment.hasWorkflow>
							<#if !assignment.markingWorkflow.studentsChooseMarker>
								<li>
									<#assign markers_url><@routes.coursework.assignMarkers assignment /></#assign>
									<@fmt.permission_button
									permission='Assignment.Update'
									scope=assignment
									action_descr='assign markers'
									href=markers_url>
										<i class="icon-user"></i> Assign markers
									</@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Marking workflow requires students to choose marker"><i class="icon-user"></i> Assign markers </a></li>
							</#if>
						<#else>
							<li class="disabled">
								<a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Marking workflow is not enabled for this assignment">
									<i class="icon-user"></i>
									Assign markers
								</a>
							</li>
							<#if assignment.collectMarks>
								<li>
									<#assign marks_url><@routes.coursework.addMarks assignment /></#assign>
									<@fmt.permission_button
									permission='AssignmentFeedback.Manage'
									scope=assignment
									action_descr='add marks'
									href=marks_url>
										<i class="icon-check"></i> Add marks
									</@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled"><a class="use-tooltip" data-delay="500" data-container=".assignment-buttons" title="Mark collection is not enabled for this assignment."><i class="icon-check"></i> Add marks</a></li>
							</#if>
							<li>
								<#assign feedback_url><@routes.coursework.addFeedback assignment /></#assign>
								<@fmt.permission_button
								permission='AssignmentFeedback.Manage'
								scope=assignment
								action_descr='upload feedback'
								classes='feedback-link'
								href=feedback_url>
									<i class="icon-upload"></i> Upload feedback
								</@fmt.permission_button>
							</li>
						</#if>

						<#if assignment.canPublishFeedback>
							<li>
								<#assign publishfeedbackurl><@url page="/coursework/admin/module/${module.code}/assignments/${assignment.id}/publish" /></#assign>
								<@fmt.permission_button permission='AssignmentFeedback.Publish' scope=module action_descr='release feedback to students' href=publishfeedbackurl data_attr='data-container=body'>
									<i class="icon-envelope-alt"></i> Publish feedback
								</@fmt.permission_button>
							</li>
						<#else>
							<li class="disabled"><a class="use-tooltip" data-delay="500" data-container="body" title="No current feedback to publish, or the assignment is not yet closed."><i class="icon-envelope-alt"></i> Publish feedback </a></li>
						</#if>
					  </ul>
					</div>
				</div>

				</div>
				<div class="end-assignment-info"></div>
			</div>
		</#if>
	</#list>
</#macro>

</#escape>