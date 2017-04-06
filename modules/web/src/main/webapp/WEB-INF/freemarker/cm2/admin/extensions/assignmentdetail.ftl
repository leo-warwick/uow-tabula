<#import "../cm2_macros.ftl" as cm2_macros />
<#if detail.extension?has_content>

	<#if detail.extension.requestedOn?has_content>
		<div>
			<label>Request received:</label> <@fmt.date date=detail.extension.requestedOn />
		</div>
	<#else>
		<#if (modifyExtensionCommand.e?has_content) >
			<#if (modifyExtensionCommand.e._state.description != "Revoked")>
				<div>
				</div>
			</#if>
		<#else>
			<div>
				<label>Manually granted:</label> <@fmt.date date=detail.extension.reviewedOn />
			</div>
		</#if>
	</#if>

	<#if detail.extension.requestedExpiryDate?has_content>
		<div>
			<label>Requested extension length:</label>
			<@fmt.p detail.extension.requestedExtraDuration "day"/> past the deadline.
			<@fmt.date date=detail.extension.requestedExpiryDate />
		</div>
	</#if>
	<#if detail.extension.feedbackDueDate?has_content>
		<div>
			<label>Feedback due:</label> <@fmt.date date=detail.extension.feedbackDueDate />
		</div>
	</#if>
	<div>
		<label>Previous extension requests:</label>
		<#if detail.previousExtensions?has_content>
			<a href="" data-toggle="modal" data-target="#prev-extensions-${detail.extension.id}">
			${detail.previousExtensions?size}
			</a>
		<#else>
			0
		</#if>
	</div>
	<div>
		<label>Previous submissions:</label>
		<#if detail.previousSubmissions?has_content>
			<a href="" data-toggle="modal" data-target="#prev-submissions-${detail.extension.id}">
			${detail.previousSubmissions?size}
			</a>
		<#else>
			0
		</#if>
	</div>


	<details>
		<summary>About this student (${student.universityId})</summary>
		<dl class="unstyled">
			<#if (studentContext.course)?has_content>
				<#assign c = studentContext.course />
				<dt>Course details</dt>
				<dd>
					<@fmt.course_description c />
					<span class="muted">${(c.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay)!}</span>
				</dd>
			</#if>
			<#if (studentContext.relationships)?has_content>
				<dt>Relationships</dt>
				<dd>
					<#assign rels = studentContext.relationships />
					<ul class="unstyled">
						<#list rels?keys as key>
							<#list rels[key] as agent>
								<li>
								${agent.agentMember.fullName}, ${agent.agentMember.description} &ensp;<span class="muted">${key}</span>
								</li>
							</#list>
						</#list>
					</ul>
				</dd>
			</#if>

			<#if student?has_content>
				<#if student.mobileNumber??><dt>Mobile number</dt><dd>${student.mobileNumber}</dd></#if>
				<#if student.phoneNumber?? && student.phoneNumber != student.mobileNumber!""><dt>Telephone number</dt><dd>${student.phoneNumber}</dd></#if>
				<#if student.email??><dt>Email address</dt><dd>${student.email}</dd></#if>
			<#else>
				<div class="alert alert-info">
					Further details for this user are not available in Tabula.
				</div>
			</#if>

		</dl>
	</details><br />

	<#if detail.extension.reason?has_content>
		<details>
			<summary>Reason for request</summary>

			<textarea class="form-control" rows="3" disabled="disabled">${detail.extension.reason}</textarea>

			<#if detail.extension.attachments?has_content>
				<label>Supporting documents</label>
				<ul>
					<#list detail.extension.attachments as attachment>
						<li>
							<a href="<@routes.cm2.extensionAttachment detail.extension attachment.name />">
							${attachment.name}
							</a>
						</li>
					</#list>
				</ul>
			</#if>
		</details>
	</#if>

	<#assign formAction><@routes.cm2.extensionUpdate detail.extension /></#assign>

	<@f.form
		method="post"
		class="modify-extension double-submit-protection"
		action="${formAction}"
		commandName="editExtensionCommand"
		>
	</@f.form>

	<@cm2_macros.previousExtensions detail.extension.id detail.student.warwickId detail.student.fullName detail.numAcceptedExtensions detail.numRejectedExtensions detail.previousExtensions />
	<@cm2_macros.previousSubmissions detail.extension.id detail.student.warwickId detail.student.fullName detail.previousSubmissions />

	<#assign feedbackNotice>
		<#if detail.extension.approved>
			<#if detail.extension.feedbackDeadline?has_content>
				<br />Feedback for this student is currently due <@fmt.date date=detail.extension.feedbackDeadline capitalise=false at=true />.
			<#else>
				<br />Feedback for this student has no due date.
			</#if>
		</#if>
	</#assign>
</#if>

<#escape x as x?html>
	<div class="content extension-detail">
		<#assign actionUrl><@routes.cm2.extensiondetail assignment usercode /></#assign>
		<@f.form method="post" enctype="multipart/form-data" action=actionUrl id="editExtensionCommand" commandName="editExtensionCommand" cssClass="form-horizontal double-submit-protection modify-extension">
			<input type="hidden" name="closeDate" class="startDateTime" value="${assignment.closeDate}" />

			<#if detail.extension?has_content>
				<#if detail.extension.awaitingReview>
					<input type="hidden" name="rawRequestedExpiryDate" value="${detail.extension.requestedExpiryDate}" />
					<h5><i class="icon icon-question icon-large"></i> Requested <@fmt.date date=detail.extension.requestedExpiryDate capitalise=false at=true/>&ensp;
						<span class="muted">${durationFormatter(assignment.closeDate, detail.extension.requestedExpiryDate)} after the set deadline</span></h5>
					<#if detail.extension.approved>
						<p>
							<b>This is a revised request from the student.</b>
							There is already an extension approved until <@fmt.date date=detail.extension.expiryDate capitalise=false at=true/>
							(${durationFormatter(assignment.closeDate, detail.extension.expiryDate)} after the set deadline).
							<#noescape>${feedbackNotice}</#noescape>
						</p>
						<p class="alert alert-info"><i class="icon-lightbulb"></i> To retain the existing extension, choose <i>Update</i> below,
							leaving a comment for the student if you wish.<br><i>Reject</i> will remove the existing extension as well.</p>
					<#elseif detail.extension.rejected>
						<p><b>This is a revised request from the student.</b> An earlier request was rejected
							<@fmt.date date=detail.extension.reviewedOn capitalise=false includeTime=false />.</p>
					</#if>
				<#elseif detail.extension.initiatedByStudent>
					<#if detail.extension.approved && detail.extension.expiryDate?has_content >
						<h5><i class="icon icon-exclamation icon-large"></i> Approved <@fmt.date date=detail.extension.reviewedOn capitalise=false includeTime=false /> until <@fmt.date date=detail.extension.expiryDate capitalise=false at=true />&ensp;
							<span class="muted">${durationFormatter(assignment.closeDate, detail.extension.expiryDate)} after the set deadline</span></h5>
					<#elseif detail.extension.rejected>
						<h5><i class="icon icon-exclamation icon-large"></i> Rejected <@fmt.date date=detail.extension.reviewedOn capitalise=false includeTime=false /></h5>
					</#if>
					<p>
						<#if (detail.extension.expiryDate?has_content)&&(detail.extension.requestedExpiryDate?has_content)&&(detail.extension.expiryDate != detail.extension.requestedExpiryDate)>
							The extension was requested for <@fmt.date date=detail.extension.requestedExpiryDate capitalise=false at=true />
							(${durationFormatter(assignment.closeDate, detail.extension.requestedExpiryDate)} after the set deadline).
							<#noescape>${feedbackNotice}</#noescape>
						</#if>
					</p>
				<#elseif detail.extension.approved>
					<p><#noescape>${feedbackNotice}</#noescape></p>
				</#if>

				<#if features.disabilityRenderingInExtensions && detail.extension.disabilityAdjustment && student?? && student.disability.reportable && can.do("Profiles.Read.Disability", student)>
					<p><i class="icon icon-stethoscope icon-large"></i> ${student.firstName} has requested their ${(student.disability.definition)!"recorded disability"} be taken into consideration.</p>
				</#if>

				<#if detail.extension.attachments?has_content>
					<details>
						<summary>Supporting documents</summary>
						<ul>
							<#list detail.extension.attachments as attachment>
								<li>
									<a href="<@routes.cm2.extensionreviewattachment assignment=assignment userid=universityId filename=attachment.name />">
									${attachment.name}
									</a>
								</li>
							</#list>
						</ul>
					</details>
				</#if>
			</#if>
			<@bs3form.labelled_form_group "expiryDate" "Extended deadline">
				<div class="input-group">
					<@f.input path="expiryDate" cssClass="form-control date-time-picker" />
					<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
				</div>
			</@bs3form.labelled_form_group>

			<@bs3form.labelled_form_group "reviewerComments" "Comments">
				<@f.textarea path="reviewerComments" cssClass="form-control text big-textarea" maxlength=4000/>
				<div class="muted">Any comments will be saved and sent to the student</div>
			</@bs3form.labelled_form_group>

			<@f.errors path="*" cssClass="error form-errors" />

			<div class="submit-buttons">

				<#if detail.extension?has_content && detail.extension.approved && can.do("Extension.Update", assignment)>
					<input class="btn btn-primary" type="submit" value="${updateAction}" name="action">
				<#elseif can.do("Extension.Create", assignment)>
					<input class="btn btn-primary" type="submit" value="${approvalAction}" name="action">
				</#if>

				<#if detail.extension?has_content>
					<#if detail.extension.rejectable && can.do("Extension.Update", assignment)>
						<input class="btn btn-danger" type="submit" value="${rejectionAction}" name="action">
					<#elseif detail.extension.revocable && can.do("Extension.Delete", assignment)>
						<input class="btn btn-danger revoke" type="submit" value="${revocationAction}" name="action">
					</#if>
				</#if>
				<a class="btn discard-changes" href="">Discard changes</a>
			</div>
		</@f.form>
	</div>
</#escape>