<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>
<#assign student = studentCourseDetails.student/>
<#assign agent_role = relationshipType.agentRole />
<#assign member_role = relationshipType.studentRole />

<#if success!false>

	<p>The meeting was successfully scheduled.</p>

<#else>
	<#assign chooseRelationship = (allRelationships?? && allRelationships?size > 1) />
	<@modal.wrapper enabled=(isModal!false)>

		<#assign heading>
			<h2 <#if isModal!false>class="modal-title"</#if>>Record a meeting</h2>
			<h6 <#if isModal!false>class="modal-title"</#if>>
				<span class="very-subtle">between ${agent_role}</span><#if !chooseRelationship> ${command.relationship.agentName!""}</#if>
				<span class="very-subtle">and ${member_role}</span> ${student.fullName}
			</h6>
		</#assign>

		<#if isModal!false>
			<@modal.header>
				<#noescape>${heading}</#noescape>
			</@modal.header>
		<#elseif isIframe!false>
			<div id="container">
		<#else>
			<#noescape>${heading}</#noescape>
		</#if>
		<#if isModal!false>
			<div class="modal-body"></div>
			<@modal.footer>
				<form class="double-submit-protection">
					<#assign title>Submit record for approval by <#if isStudent>${agent_role}<#else>${member_role}</#if></#assign>
					<button title="${title}" class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
						Submit for approval
					</button>
					<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
				</form>
			</@modal.footer>
		<#else>
			<!-- blank action = post to current path. needs to be left blank so we know if we should post to create or edit -->
			<@f.form id="meeting-record-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="double-submit-protection">

				<@bs3form.labelled_form_group path="title" labelText="Title">
					<@f.input type="text" path="title" cssClass="form-control" maxlength="255" placeholder="Subject of meeting" />
				</@bs3form.labelled_form_group>

				<#if chooseRelationship>
					<#assign isCreatorAgent = command.creator.id == command.relationship.agent />
					<@bs3form.labelled_form_group path="relationship" labelText=agent_role?cap_first>
						<@f.select path="relationship" cssClass="form-control">
							<@f.option disabled=true selected="true" label="Please select one..." />
							<@f.options items=allRelationships itemLabel="agentName" />
						</@f.select>
						<div class="help-block <#if !isCreatorAgent>alert alert-info</#if>">
							<#if isCreatorAgent>
								You have been selected as ${agent_role} by default. Please change this if you're recording a colleague's meeting.
							<#else>
								The first ${agent_role} has been selected by default. Please check it's the correct one. <i id="supervisor-ok" class="fa fa-check"></i>
							</#if>
						</div>
					</@bs3form.labelled_form_group>
				</#if>

				<script>
					jQuery(function($) {
						$('#supervisor-ok, #relationship').on("focus click keyup", function() {
							$('#supervisor-ok').closest('.alert-info').removeClass('alert-info').end()
								.remove();
						});
					});
				</script>
				<div class="row">
					<div class="col-xs-4">
						<@bs3form.labelled_form_group path="meetingDateStr" labelText="Date of meeting">
							<div class="input-group">
								<@f.input type="text" path="meetingDateStr" cssClass="form-control date-picker" placeholder="Pick the date" />
								<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
							</div>
						</@bs3form.labelled_form_group>
					</div>
					<div class="col-xs-4">
						<@bs3form.labelled_form_group path="meetingTimeStr" labelText="Time of meeting">
							<div class="input-group">
								<@f.input type="text" path="meetingTimeStr" cssClass="form-control time-picker" placeholder="Pick the time" />
								<span class="input-group-addon"><i class="fa fa-clock-o"></i></span>
							</div>
						</@bs3form.labelled_form_group>
					</div>
					<div class="col-xs-4">
						<@bs3form.labelled_form_group path="meetingEndTimeStr" labelText="End time of meeting">
							<div class="input-group">
								<@f.input type="text" path="meetingEndTimeStr" cssClass="form-control time-picker" placeholder="Pick the end time" />
								<span class="input-group-addon"><i class="fa fa-clock-o"></i></span>
							</div>
						</@bs3form.labelled_form_group>
					</div>

					<div class="col-xs-12">
						<div class="help-block alert alert-info hidden">
							This meeting takes place in the <span class="year"></span> academic year.
							You will be able to find this meeting under the <span class="year"></span> tab.
						</div>
					</div>
				</div>

				<script>
					jQuery(function($){
						var $xhr = null;
						$('#meetingDateStr').on('change', function(){
							if ($xhr) $xhr.abort();
							var $this = $(this), meetingDateStr = $this.val();
							if (meetingDateStr.length > 0) {
								$xhr = jQuery.get('/ajax/academicyearfromdate', {date: meetingDateStr}, function(data) {
									if (data.startYear != '${academicYear.startYear?c}') {
										$this.closest('.row').find('.help-block')
											.find('span.year').text(data.string).end()
											.removeClass('hidden');
									} else {
										$this.closest('.row').find('.help-block').addClass('hidden');
									}
								});
							} else {
								$this.closest('.row').find('.help-block').addClass('hidden');
							}
						});
					});
				</script>

				<@bs3form.labelled_form_group path="format" labelText="Format">
					<@f.select path="format" cssClass="form-control">
						<@f.option disabled=true selected="true" label="Please select one..." />
						<@f.options items=formats itemLabel="description" itemValue="code" />
					</@f.select>
				</@bs3form.labelled_form_group>

				<@bs3form.labelled_form_group path="meetingLocation" labelText="Location">
					<@f.hidden path="meetingLocationId" />
					<@f.input path="meetingLocation" cssClass="form-control" />
				</@bs3form.labelled_form_group>

				<#if !isStudent>
					<div id="willCheckpointBeCreatedMessage" class="alert alert-info" style="display: none;">
						Submitting this record will mark a monitoring point as attended
					</div>
					<script>
						jQuery(function($){
							$('#meetingDateStr, #format').on('change', function(){
								$.get('<@routes.profiles.meeting_will_create_checkpoint />', {
									'student' : '${studentCourseDetails.student.universityId}',
									'relationshipType' : '${relationshipType.urlPart}',
									'meetingFormat' : $('#format').val(),
									'meetingDate': $('#meetingDateStr').val() + ' ' + $('#meetingTimeStr').val()
								}, function(data){
									if(data.willCheckpointBeCreated) {
										$('#willCheckpointBeCreatedMessage').show();
									} else {
										$('#willCheckpointBeCreatedMessage').hide();
									}
								})
							})
						});
					</script>
				</#if>

				<#-- file upload (TAB-359) -->
				<#assign fileTypes=command.attachmentTypes />
				<@bs3form.filewidget basename="file" types=fileTypes />

				<#if command.attachedFiles?has_content >
					<@bs3form.labelled_form_group path="attachedFiles" labelText="Previously uploaded files">
						<ul class="unstyled">
							<#list command.attachedFiles as attachment>
								<li id="attachment-${attachment.id}" class="attachment">
									<span>${attachment.name}</span>&nbsp;
									<@f.hidden path="attachedFiles" value="${attachment.id}" />
									<a class="remove-attachment" href="">Remove</a>
								</li>
							</#list>
						</ul>
						<script>
							jQuery(function($){
								$(".remove-attachment").on("click", function(e){
									$(this).closest("li.attachment").remove();
									return false;
								});
							});
						</script>
						<p class="very-subtle help-block">
							This is a list of all supporting documents that have been attached to this meeting record.
							Click the remove link next to a document to delete it.
						</p>
					</@bs3form.labelled_form_group>
				</#if>

				<@bs3form.labelled_form_group path="description" labelText="Description (optional)">
					<@f.textarea rows="6" path="description" cssClass="form-control" />
				</@bs3form.labelled_form_group>

				<#if isIframe!false>
					<input type="hidden" name="modal" value="true" />
				<#else>
					<#-- separate page, not modal -->
					<div class="form-actions">
						<#assign title>Submit record for approval by <#if isStudent>${relationshipType.agentRole}<#else>${relationshipType.studentRole}</#if></#assign>
						<button title="${title}" class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
							Submit for approval
						</button>
						<a class="btn btn-default" href="<@routes.profiles.profile student />">Cancel</a>
					</div>
				</#if>
			</@f.form>
		</#if>

		<#if isIframe!false>
			</div> <#--container -->
		</#if>

	</@modal.wrapper>

</#if>
</#escape>