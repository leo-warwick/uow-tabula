<#ftl strip_text=true />
<#escape x as x?html>
<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#macro admin_section module expand_by_default=true>
	<#local can_manage=can.do("Module.ManageAssignments", module) />
	<#local exams = (mapGet(examMap, module))![] />

	<div id="${module_anchor(module)}" class="module-info striped-section<#if exams?has_content> collapsible<#if expand_by_default> expanded</#if></#if><#if !expand_by_default && !exams?has_content> empty</#if>"
		<#if exams?has_content && !expand_by_default>
			data-populate=".striped-section-contents"
			data-href="<@routes.exams.moduleHomeWithYear module academicYear />"
			data-name="${module_anchor(module)}"
		</#if>
	>
		<div class="clearfix">
			<div class="btn-group module-manage-button section-manage-button">
			  <a class="btn btn-default dropdown-toggle" data-toggle="dropdown">Manage <span class="caret"></span></a>
			  <ul class="dropdown-menu pull-right">
                    <#if can_manage>
						<li><a href="<@routes.exams.modulePermissions module />">Edit module permissions</a></li>
					</#if>

					<li>
						<#local create_url><@routes.exams.createExam module academicYear /></#local>
						<@fmt.permission_button
							permission='Assignment.Create'
							scope=module
							action_descr='create a new exam'
							href=create_url>
							Create new exam
						</@fmt.permission_button>
					</li>
			  </ul>
			</div>

			<h2 class="section-title"><@fmt.module_name module /></h2>
		</div>


		<#if exams?has_content>
			<div class="module-info-contents striped-section-contents">
				<#if exams?has_content && expand_by_default>
					<@admin_exams module exams/>
				</#if>
			</div>
		</#if>

	</div>
</#macro>

<#macro admin_exams module exams>
	<#list exams as exam>
		<#if !exam.deleted>
			<div class="item-info">
				<div class="col-md-3">
					<h3 class="name">${exam.name}</h3>
				</div>

				<div class="col-md-8">
					<#local membershipInfo = exam.membershipInfo />
					<#if membershipInfo.totalCount == 0>
						<span class="label label-default">No enrolled students</span>
					<#else>
						<a href="<@routes.exams.viewExam exam />">
							<span class="use-tooltip" title="View all students and feedback">
								<@fmt.p membershipInfo.sitsCount "enrolled student"/> from SITS<#if (membershipInfo.usedIncludeCount > 0)>, with <@fmt.p membershipInfo.usedIncludeCount "additional student"/></#if>
							</span>
						</a>
					</#if>
					<#local requiresMarks = exam.requiresMarks />
					<#if (requiresMarks > 0)>
						<br/><span class="label label-primary"><@fmt.p number=requiresMarks singular="student requires" plural="students require"/> marks</span>
					</#if>
				</div>

				<div class="col-md-1">
					<div class="btn-group pull-right">
						<a class="btn btn-default dropdown-toggle" data-toggle="dropdown">Actions <span class="caret"></span></a>
						<ul class="dropdown-menu pull-right">
							<li>
								<#local edit_url><@routes.exams.editExam exam /></#local>
								<@fmt.permission_button
									permission='Assignment.Update'
									scope=exam
									action_descr='edit exam properties'
									href=edit_url
								>
									Edit properties
								</@fmt.permission_button>
							</li>

							<li class="divider"></li>

							<#if exam.feedbacks?size == 0>
								<li>
									<#local edit_url><@routes.exams.deleteExam exam /></#local>
									<@fmt.permission_button
										permission='Assignment.Delete'
										scope=exam
										action_descr='delete exam'
										href=edit_url
									>
										Delete Exam
									</@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled"><a class="use-tooltip" data-delay="500" data-container="body" title="Marks associated with this exam and can't be deleted">Delete Exam</a></li>
							</#if>

							<li class="divider"></li>

							<li>
								<#local marks_url><@routes.exams.addMarks exam /></#local>
								<@fmt.permission_button
									permission='ExamFeedback.Manage'
									scope=exam
									action_descr='add marks'
									href=marks_url
								>
									Add marks
								</@fmt.permission_button>
							</li>

							<li>
								<#local adjust_url><@routes.exams.feedbackAdjustment exam /></#local>
								<@fmt.permission_button
									permission='ExamFeedback.Manage'
									scope=exam
									action_descr='adjust marks'
									href=adjust_url
								>
									Adjustments
								</@fmt.permission_button>
							</li>

							<#if exam.hasWorkflow>
								<#if !exam.markingWorkflow.studentsChooseMarker>
									<li>
										<#assign markers_url><@routes.exams.assignMarkers exam /></#assign>
										<@fmt.permission_button
										permission='ExamFeedback.Manage'
										scope=exam
										action_descr='assign markers'
										href=markers_url>
											Assign markers
										</@fmt.permission_button>
									</li>
								<#else>
									<li class="disabled"><a class="use-tooltip" data-delay="500" data-container="body" title="Marking workflow requires students to choose marker">Assign markers</a></li>
								</#if>
							<#else>
								<li class="disabled">
									<a class="use-tooltip" data-delay="500" data-container="body" title="Marking workflow is not enabled for this exam">Assign markers</a>
								</li>
							</#if>

							<#if !exam.released>
								<li>
									<#assign releaseForMarking_url><@routes.exams.releaseForMarking exam /></#assign>
									<@fmt.permission_button
										permission='Submission.ReleaseForMarking'
										scope=exam
										action_descr='release for marking'
										href=releaseForMarking_url
									>
										Release for marking
									</@fmt.permission_button>
								</li>
							<#else>
								<li class="disabled">
									<a class="use-tooltip" data-delay="500" data-container="body" title="This exam has already been released for marking">Release for marking</a>
								</li>
							</#if>

							<li>
								<#local upload_url><@routes.exams.uploadToSits exam /></#local>
								<@fmt.permission_button
									permission='ExamFeedback.Manage'
									scope=exam
									action_descr='upload to SITS'
									href=upload_url
								>
									Upload to SITS
								</@fmt.permission_button>
							</li>
						</ul>
					</div>
				</div>
				<div class="clearfix"></div>
			</div>
		</#if>
	</#list>
</#macro>

</#escape>