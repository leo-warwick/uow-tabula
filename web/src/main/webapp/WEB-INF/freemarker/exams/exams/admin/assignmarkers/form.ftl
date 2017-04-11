<#macro student_item student bindpath="">
<li class="student well well-sm ui-draggable ui-selectee">
	<div class="name"><h6><i class="icon-white icon-user"></i> ${student.displayValue}</h6></div>
	<input type="hidden" name="${bindpath}" value="${student.userCode}" />
</li>
</#macro>

<#macro assignStudents studentList markerList class name markerMapName cancelUrl>
<div class="fix-area">
	<div class="tabula-dnd" data-scroll="true">
		<p>Drag students onto a marker to allocate them. Select multiple students by dragging a box around them.
			You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key and drag to add to a selection.</p>

		<div class="tabula-dnd"
			 data-item-name="student"
			 data-text-selector=".name h6"
			 data-selectables=".students .drag-target"
			 data-scroll="true"
			 data-remove-tooltip="Unassign this student from the marker">

			<div class="fix-header pad-when-fixed">
				<p class="btn-toolbar">
					<a class="random btn btn-default btn-xs" data-toggle="randomise" data-disabled-on="empty-list" href="#">Randomly allocate</a>
					<a class="return-items btn btn-default btn-xs" data-toggle="return" data-disabled-on="no-allocation" href="#">Remove all</a>
				</p>
				<div class="row">
					<div class="col-md-5">
						<h3>Students</h3>
					</div>
					<div class="col-md-2"></div>
					<div class="col-md-5">
						<h3>${name}</h3>
					</div>
				</div>
			</div><!-- end persist header -->

			<div class="row fix-on-scroll-container">
				<div class="col-md-5">
					<div id="studentslist"
						 class="students tabula-filtered-list fix-on-scroll"
						 data-item-selector=".student-list li">
						<div class="well ">
							<h4>Not allocated to a marker</h4>
							<div class="student-list drag-target">
								<ul class="drag-list return-list unstyled" data-nobind="true">
									<#list studentList as student>
										<@student_item student "" />
									</#list>
								</ul>
							</div>
						</div>
					</div>
				</div>
				<div class="col-md-2">
					<div class="direction-icon fix-on-scroll">
						<i class="fa fa-arrow-right"></i>
					</div>
				</div>
				<div class="col-md-5">
					<div id="groupslist" class="agentslist ${class} ">
						<#list markerList as marker>
							<#local existingStudents = marker.students />

							<div class="marker drag-target well clearfix agent-${markerMapName}-${marker.userCode}">
								<div class="group-header">
									<h4 class="name">
										${marker.fullName}
									</h4>
									<div>
										<#assign count = existingStudents?size />
										<span class="drag-count">${count}</span>
										<span class="drag-counted" data-singular="student" data-plural="students">
											student<#if count != 1>s</#if>
										</span>
										<a id="show-list-${markerMapName}-${marker.userCode}"
										   class="show-list"
										   title="View students"
										   data-container=".agent-${markerMapName}-${marker.userCode}"
										   data-title="Students assigned to ${marker.fullName}"
										   data-placement="left"
										>
											<i class="fa fa-pencil-square-o"></i>
										</a>
									</div>
								</div>
								<ul class="drag-list hide" data-bindpath="${markerMapName}[${marker.userCode}]">
									<#list existingStudents as student>
										<@student_item student "${markerMapName}[${marker.userCode}][${student_index}]" />
									</#list>
								</ul>
							</div>
						</#list>
					</div>
				</div>
			</div>

		</div>
	</div>
	<div class="submit-buttons fix-footer">
		<input type="submit" name="dragAndDrop" class="btn btn-primary" value="Save">
		<a href="${cancelUrl}" class="btn btn-default">Cancel</a>
	</div>
</div>
</#macro>


<#escape x as x?html>
	<#assign assignMarkersSmallGroups><@routes.exams.assignMarkersSmallGroups assessment/></#assign>

	<div class="deptheader">
		<h1>Assign students to markers</h1>
		<h4 class="with-related">for ${assessment.name}</h4>
	</div>
	<div class="btn-toolbar">
		<div class="pull-right">
			<div class="btn-group mode-nav">
				<button data-selector="spreadsheet" class="btn btn-default mode">Upload spreadsheet</button>
				<button data-selector="small-groups" class="btn btn-default mode">Import small groups</button>
			</div>
			<div class="btn-group hide back-nav">
				<a href="${assignMarkersURL}" class="btn btn-default">Return to drag and drop</a>
			</div>
		</div>
	</div>
	<div class="clearfix"></div>
	<@f.form method="post" enctype="multipart/form-data" action="${url(assignMarkersURL)}" commandName="command">
		<div id="assign-markers" class="tabbable">
			<ul class="nav nav-tabs">
				<li class="active"><a href="#first-markers">${firstMarkerRoleName}s</a></li>
				<#if hasSecondMarker><li><a href="#second-markers">${secondMarkerRoleName}s</a></li></#if>
			</ul>
			<div class="tab-content">
				<div class="tab-pane active" id="first-markers">
					<@assignStudents
						firstMarkerUnassignedStudents
						firstMarkers
						"first-markers"
						firstMarkerRoleName
						"firstMarkerMapping"
						cancelUrl
					/>
				</div>
				<#if hasSecondMarker>
					<div class="tab-pane" id="second-markers">
						<@assignStudents
							secondMarkerUnassignedStudents
							secondMarkers
							"second-markers"
							secondMarkerRoleName
							"secondMarkerMapping"
							cancelUrl
						/>
					</div>
				</#if>
			</div>
		</div>
		<div id="spreadsheet" class="hide">
			<p>You can assign students to markers by uploading a spreadsheet.</p>
			<ol>
				<li>
					<p><a class="btn btn-default" href="<@routes.exams.assignMarkersTemplate exam />">
						Download a template spreadsheet
					</a></p>
					<div class="alert alert-info">
						Any markers that you have already assigned using the drag and drop interface will be present in the template.
					</div>
				</li>
				<li>
					<p>
						Allocate students to markers using the dropdown menu in the marker name column or by typing a personal tutor's University ID into the agent_id column. The agent_id field will be updated with the University ID for that personal tutor if you use the dropdown. Any students with an empty agent_id field will have their marker removed, if they have one.
					</p>
				</li>
				<li><p><strong>Save</strong> your updated spreadsheet.</p></li>
				<li>
					<@bs3form.labelled_form_group path="file.upload" labelText="Choose your updated spreadsheet">
						<input type="file" name="file.upload" />
					</@bs3form.labelled_form_group>
				</li>
			</ol>
			<div class="fix-footer submit-buttons">
				<input type="submit" name="uploadSpreadsheet" class="btn btn-primary" value="Upload">
				<a href="${cancelUrl}" class="btn btn-default">Cancel</a>
			</div>
		</div>
		<div id="small-groups" class="hide tabbable">

		</div>
	</@f.form>

<script type="text/javascript">
(function($) {
	$('.fix-area').fixHeaderFooter();

	// mode buttons
	$('.mode').on('click', function(){
		var selector = $(this).data('selector');
		$('#assign-markers').fadeOut(300);
		$('.mode-nav').fadeOut(300, function() {
			$(this).remove();
			$('.back-nav').removeClass('hide');
			if(selector === "small-groups") {
				$('#small-groups').load('${assignMarkersSmallGroups}')
			}
			$('#'+selector).hide().removeClass('hide').fadeIn(300);
		});
	});

	$("#small-groups").on('click', 'button[name=smallGroupImport]', function(e){
		var $setSelector = $('.set-selector');
		var set = $setSelector.val();
		if (set === "") {
			var $controls = $setSelector.closest(".controls");
			$controls.closest(".control-group").addClass("error");
			$controls.append('<span class="error help-inline">You must choose a small group set</span>');
			e.preventDefault();
		}
	}).on("change", ".set-selector", function(e) {
		var $target = $(e.target);
		var set = $target.val();
		if (set === "") {
			$(".nav-tabs,.tab-content").hide();
		} else {
			$(".nav-tabs,.tab-content").show();
		}

		var $allSets = $('.set-info');
		$allSets.hide();
		$allSets.find("input").prop('disabled', true);
		var $set = $("."+set);
		$set.show();
		$set.find("input").prop('disabled', false);

		$(".marker-selector").trigger('change');
	}).on("change", ".marker-selector", function(e) {
		var $target = $(e.target);
		var newMarker = $target.val();
		var $group = $target.closest(".group");
		var $inputs = $group.find("input.allocation");

		var $roleContainer = $target.closest(".role-container");
		var roleBinding = $roleContainer.data("rolebinding");
		// work out how many students are already assigned to this marker so we can increment the binding correctly
		var $markersExistingStudents = $("input[name^="+roleBinding+"\\["+newMarker+"\\]]", $roleContainer);
		$inputs.each(function(index) {
			var newIndex = $markersExistingStudents.size() + index;
			var $this = $(this);
			var name = $this.attr("name");
			// update the hidden input that will bind the chosen marker to the student
			$this.attr("name", name.replace(/(.*\[).*(\]\[.*\])/g, "$1"+newMarker+"]["+newIndex+"]"));
		});
	});

})(jQuery);
</script>

</#escape>
