<#assign spring = JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f = JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign extension = command.extension />

<#assign feedbackNotice>
	<#if extension.approved>
		<#if extension.feedbackDeadline??>
			<br>Feedback for this student is currently due <@fmt.date date=extension.feedbackDeadline capitalise=false at=true />.
		<#else>
			<br>Feedback for this student has no due date.
		</#if>
	</#if>
</#assign>

<#escape x as x?html>
	<div class="content extension-detail">
		<#assign actionUrl><@routes.coursework.extensiondetail assignment usercode /></#assign>
		<@f.form method="post" enctype="multipart/form-data" action=actionUrl commandName="modifyExtensionCommand" cssClass="form-horizontal double-submit-protection">

			<input type="hidden" name="closeDate" class="startDateTime" value="${assignment.closeDate}" />

			<#if extension.awaitingReview>
				<input type="hidden" name="rawRequestedExpiryDate" value="${extension.requestedExpiryDate}" />
				<h5><i class="icon icon-question icon-large"></i> Requested <@fmt.date date=extension.requestedExpiryDate capitalise=false at=true/>&ensp;
				<span class="muted">${durationFormatter(assignment.closeDate, extension.requestedExpiryDate)} after the set deadline</span></h5>
				<#if extension.approved>
					<p>
						<b>This is a revised request from the student.</b>
						There is already an extension approved until <@fmt.date date=extension.expiryDate capitalise=false at=true/>
						(${durationFormatter(assignment.closeDate, extension.expiryDate)} after the set deadline).
						<#noescape>${feedbackNotice}</#noescape>
					</p>
					<p class="alert alert-info"><i class="icon-lightbulb"></i> To retain the existing extension, choose <i>Update</i> below,
					leaving a comment for the student if you wish.<br><i>Reject</i> will remove the existing extension as well.</p>
				<#elseif extension.rejected>
					<p><b>This is a revised request from the student.</b> An earlier request was rejected
					<@fmt.date date=extension.reviewedOn capitalise=false includeTime=false />.</p>
				</#if>
			<#elseif extension.initiatedByStudent>
				<#if extension.approved>
					<h5><i class="icon icon-exclamation icon-large"></i> Approved <@fmt.date date=extension.reviewedOn capitalise=false includeTime=false /> until <@fmt.date date=extension.expiryDate capitalise=false at=true />&ensp;
					<span class="muted">${durationFormatter(assignment.closeDate, extension.expiryDate)} after the set deadline</span></h5>
				<#elseif extension.rejected>
					<h5><i class="icon icon-exclamation icon-large"></i> Rejected <@fmt.date date=extension.reviewedOn capitalise=false includeTime=false /></h5>
				</#if>
				<p>
					The extension was requested for <@fmt.date date=extension.requestedExpiryDate capitalise=false at=true />
					(${durationFormatter(assignment.closeDate, extension.requestedExpiryDate)} after the set deadline).
					<#noescape>${feedbackNotice}</#noescape>
				</p>
			<#elseif extension.approved>
				<p><#noescape>${feedbackNotice}</#noescape></p>
			</#if>

			<#if features.disabilityRenderingInExtensions && extension.disabilityAdjustment && student?? && student.disability.reportable && can.do("Profiles.Read.Disability", student)>
				<p><i class="icon icon-stethoscope icon-large"></i> ${student.firstName} has requested their ${(student.disability.definition)!"recorded disability"} be taken into consideration.</p>
			</#if>

			<#if extension.reason?has_content>
				<details>
					<summary>Reason for request</summary>
					<p class="well">${extension.reason}</p>
				</details>
			</#if>

			<#if extension.attachments?has_content>
				<details>
					<summary>Supporting documents</summary>
					<ul>
						<#list extension.attachments as attachment>
							<li>
								<a href="<@routes.coursework.extensionreviewattachment assignment=assignment userid=usercode filename=attachment.name />">
								${attachment.name}
								</a>
							</li>
						</#list>
					</ul>
				</details>
			</#if>

			<details>
				<summary>About this student (${studentIdentifier})</summary>
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

					<#if student??>
						<#if student.mobileNumber??><dt>Mobile number</dt><dd>${student.mobileNumber}</dd></#if>
						<#if student.phoneNumber?? && student.phoneNumber != student.mobileNumber!""><dt>Telephone number</dt><dd>${student.phoneNumber}</dd></#if>
						<#if student.email??><dt>Email address</dt><dd>${student.email}</dd></#if>
					<#else>
						<div class="alert alert-info">
							Further details for this user are not available in Tabula.
						</div>
					</#if>

				</dl>
			</details>

			<hr />

			<div class="control-group">
				<@form.label path="expiryDate">Extended deadline</@form.label>
				<div class="controls">
					<@f.input id="picker0" path="expiryDate" cssClass="date-time-picker" />
					<#if !extension.manual && extension.awaitingReview>
						<button class="btn setExpiryToRequested">Use requested date</button>
					</#if>
				</div>
			</div>
			<div class="control-group">
				<@form.label path="reviewerComments">Comments</@form.label>
				<div class="controls">
					<@f.textarea path="reviewerComments" cssClass="span7" rows="6" />
					<div class="muted">Any comments will be saved and sent to the student</div>
				</div>
			</div>

			<@f.errors path="*" cssClass="error form-errors" />

			<div class="submit-buttons">

				<#if extension.approved && can.do("Extension.Update", assignment)>
					<input class="btn btn-primary" type="submit" value="${updateAction}" name="action">
				<#elseif can.do("Extension.Create", assignment)>
					<input class="btn btn-primary" type="submit" value="${approvalAction}" name="action">
				</#if>

				<#if extension.rejectable && can.do("Extension.Update", assignment)>
					<input class="btn btn-danger" type="submit" value="${rejectionAction}" name="action">
				<#elseif extension.revocable && can.do("Extension.Delete", assignment)>
					<input class="btn btn-danger revoke" type="submit" value="${revocationAction}" name="action">
				</#if>

				<a class="btn discard-changes" href="">Discard changes</a>
			</div>
		</@f.form>
	</div>
</#escape>
