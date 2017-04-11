<#import "*/modal_macros.ftl" as modal />

<#escape x as x?html>
	<#if success!false>
		<p>The meeting was successfully recorded.</p>
	<#else>
		<@modal.wrapper enabled=(isModal!false)>
			<#assign heading>
				<h2 <#if isModal!false>class="modal-title"</#if>>Record a meeting</h2>
				<h6 <#if isModal!false>class="modal-title"</#if>>
					<span class="very-subtle">between ${relationshipType.agentRole}</span> ${command.creator.fullName!""}
					<span class="very-subtle">and ${relationshipType.studentRole}(s)</span>
					<a class ="studentList" href="#" title="Bulk Students" data-content="Some content inside the popover">Show</a>
					<div class ="hide studentList"></div>
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
						<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
							Submit for approval
						</button>
						<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
					</form>
				</@modal.footer>
			<#else>
				<@f.form id="meeting-record-form" method="post" enctype="multipart/form-data" commandName="command" class="double-submit-protection">
					<@bs3form.labelled_form_group path="title" labelText="Title">
						<@f.input type="text" path="title" cssClass="form-control" maxlength="255" placeholder="Subject of meeting" />
					</@bs3form.labelled_form_group>

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
					</div>

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

					<#assign fileTypes=command.attachmentTypes />
					<@bs3form.filewidget basename="file" types=fileTypes />

					<@bs3form.labelled_form_group path="description" labelText="Description (optional)">
						<@f.textarea rows="6" path="description" cssClass="form-control" />
					</@bs3form.labelled_form_group>

					<#if isIframe!false>
						<input type="hidden" name="modal" value="true" />
					<#else>
						<#-- separate page, not modal -->
						<div class="form-actions">
							<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
								Submit for approval
							</button>
							<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Close</button>
						</div>
					</#if>
				</@f.form>
			</#if>

			<#if isIframe!false>
				</div> <#--container -->
			</#if>
		</@modal.wrapper>
	</#if>
<script>
	jQuery(function($){
		$(".remove-attachment").on("click", function(e){
			$(this).closest("li.attachment").remove();
			return false;
		});
		$("a.studentList").on("click", function() {
			var $studentList = $("div.studentList");
			var $selectedStudents = $("input.collection-checkbox:checked");
			if($studentList.hasClass("hide")) {
				$studentList.empty();
				$selectedStudents.each(function() {
					var $selectedStudent = $(this);
					$studentList.append('<div><span>' + $selectedStudent.data("fullname") + '</span><div>');
				});
				$studentList.removeClass("hide");
				$("a.studentList").text("Hide");
				$studentList.slideDown();
			} else {
				$studentList.addClass("hide");
				$("a.studentList").text("Show");
				$studentList.slideUp("fast");
			}
			return false;
		});

	});
</script>
</#escape>