<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

	<#if smallGroupSet.linked>
		<div class="alert alert-danger">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<h4>Allocation linked to ${smallGroupSet.linkedDepartmentSmallGroupSet.name}</h4>

			<p>Changing the allocation on this screen will change the group allocation for any other modules linked to these groups.</p>
		</div>
	</#if>

	<#assign mappingById=command.mappingById />
	<#assign membersById=command.membersById />

	<#macro student_item student bindpath="">
		<#local profile = membersById[student.warwickId]!{} />
		<li class="student well well-sm"
			data-f-gender="${(profile.gender.dbValue)!}"
			data-f-route="${(mapGet(command.memberAllocationData, profile).routeCode)!}"
			data-f-year="${(mapGet(command.memberAllocationData, profile).yearOfStudy)!}"
		>
			<div class="profile clearfix">
				<@fmt.member_photo profile "tinythumbnail" false />
				<div class="name">
					<h6>${profile.fullName!student.fullName}&nbsp;<@pl.profile_link student.warwickId! /></h6>
					${(mapGet(command.memberAllocationData, profile).routeName)!student.shortDepartment!""}
				</div>
			</div>
			<input type="hidden" name="${bindpath}" value="${student.userId}" />
		</li>
	</#macro>

	<div id="timetable-clash-modal" class="modal fade timetable-clash-students"></div>
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<#if (command.isStudentSignup())>
		<div class="alert alert-info">These groups are currently <strong>${smallGroupSet.openForSignups?string("open","closed")}</strong> for self sign-up</div>
	</#if>
	<noscript>
		<div class="alert alert-info">This page requires Javascript.</div>
	</noscript>

	<div class="tabbable">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#allocategroups-tab1" data-toggle="tab">Drag and drop</a>
			</li>
			<li >
				<a href="#allocategroups-tab2" data-toggle="tab">Upload spreadsheet</a>
			</li>
		</ul>

		<div class="tab-content">
			<div class="tab-pane active" id="allocategroups-tab1">

				<p>Drag students onto a group to allocate them to it. Select multiple students by dragging a box around them.
					You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key and drag to add to a selection.</p>

				<@spring.hasBindErrors name="command">
					<#if errors.hasErrors()>
						<div class="alert alert-danger">
							<h3>Some problems need fixing</h3>
							<#if errors.hasGlobalErrors()>
								<#list errors.globalErrors as e>
									<div><@spring.message message=e /></div>
								</#list>
							<#elseif errors.hasFieldErrors('file')>
								<#list errors.getFieldErrors('file') as e>
									<div><@spring.message message=e /></div>
								</#list>
							<#else>
								<div>See the errors below.</div>
							</#if>
						</div>
					</#if>
				</@spring.hasBindErrors>

				<div class="fix-area">
					<@f.form method="post" action="${submitUrl}" commandName="command" class="allocateStudentsToGroupsCommand dirty-check">
						<div class="tabula-dnd"
							 data-item-name="student"
							 data-text-selector=".name h6"
							 data-use-handle="false"
							 data-selectables=".students .drag-target"
							 data-scroll="true"
							 data-remove-tooltip="Remove this student from this group">
							<div class="fix-header pad-when-fixed">
								<p class="btn-toolbar">
									<a class="random btn btn-xs btn-default" data-toggle="randomise" data-disabled-on="empty-list" href="#">
										Randomly allocate
									</a>
									<a class="return-items btn btn-xs btn-default" data-toggle="return" data-disabled-on="no-allocation" href="#">
										Remove all
									</a>
								</p>
								<div class="row">
									<div class="col-md-5">
										<h3>Students</h3>
									</div>
									<div class="col-md-2"></div>
									<div class="col-md-5">
										<h3>Groups</h3>
									</div>
								</div>
							</div><!-- end persist header -->

							<div class="row fix-on-scroll-container">
								<div class="col-md-5">
									<div id="studentslist"
										 class="students tabula-filtered-list"
										 data-item-selector=".student-list li">
										<div class="well">
											<h4>Not allocated to a group</h4>
											<#if features.smallGroupAllocationFiltering>
												<div class="filter" id="filter-by-gender-controls">
													<select data-filter-attr="fGender" class="form-control">
														<option data-filter-value="*">Any Gender</option>
														<option data-filter-value="M">Male</option>
														<option data-filter-value="F">Female</option>
													</select>
												</div>
												<div class="filter" id="filter-by-year-controls">
													<select data-filter-attr="fYear" class="form-control">
														<option data-filter-value="*">Any Year of study</option>
														<#list command.allMembersYears as data>
															<option data-filter-value="${data.yearOfStudy}">Year ${data.yearOfStudy}</option>
														</#list>
													</select>
												</div>
												<div class="filter" id="filter-by-route-controls">
													<select data-filter-attr="fRoute" class="form-control">
														<option data-filter-value="*">Any Route</option>
														<#list command.allMembersRoutes as data>
															<option data-filter-value="${data.routeCode}"><@fmt.route_name route="" routeCode=data.routeCode routeName=data.routeName /></option>
														</#list>
													</select>
												</div>
											</#if>
											<div class="student-list drag-target">
												<ul class="drag-list return-list unstyled" data-bindpath="unallocated">
													<@spring.bind path="unallocated">
														<#list status.actualValue as student>
															<@student_item student "${status.expression}[${student_index}]" />
														</#list>
													</@spring.bind>
												</ul>
											</div>
										</div>
									</div>
									<#if command.unallocatedPermWithdrawnCount gt 0>
										<p>
											There <@fmt.p number=command.unallocatedPermWithdrawnCount singular="is" plural="are" shownumber=false />
											<@fmt.p command.unallocatedPermWithdrawnCount "unallocated student" /> who
											<@fmt.p number=command.unallocatedPermWithdrawnCount singular="is " plural="are " shownumber=false />
											permanently withdrawn and so cannot be allocated. You can
											<a target="_blank" href="<@routes.groups.editsetstudents command.set />">edit the students in the group set</a>
											to remove them.
										</p>
									</#if>
								</div>
								<div class="col-md-2">
								<#-- I, for one, welcome our new jumbo icon overlords -->
									<div class="direction-icon fix-on-scroll">
										<i class="fa fa-arrow-right"></i>
									</div>
								</div>
								<div class="col-md-5">
									<div id="groupslist" class="groups fix-on-scroll">
										<#list command.sortedGroups as group>
											<#assign existingStudents = mappingById[group.id]![] />
											<div class="drag-target well clearfix group-${group.id}">
												<div class="group-header">
													<#assign popoverHeader>Students in ${group.name}</#assign>
													<#assign groupDetails>
														<ul class="unstyled">
															<#list group.events as event>
																<li>
																	<@components.event_schedule_info event />
																</li>
															</#list>
														</ul>
													</#assign>

													<h4 class="name">
														${group.name}
													</h4>

													<div>
														<#assign count = existingStudents?size />
														<span class="drag-count">${count}</span> <span class="drag-counted" data-singular="student" data-plural="students">student<#if count != 1>s</#if></span>

														<a id="show-list-${group.id}" class="show-list" title="View students" data-container=".group-${group.id}" data-title="${popoverHeader}" data-prelude="${groupDetails}" data-placement="left"><i class="fa fa-pencil-square-o"></i></a>
													</div>
												</div>

												<ul class="drag-list hide" data-bindpath="mapping[${group.id}]" <#if group.maxGroupSize??>data-max-members="${group.maxGroupSize}"</#if>>
													<#list existingStudents as student>
														<@student_item student "mapping[${group.id}][${student_index}]" />
													</#list>
												</ul>
											</div>
										</#list>
										<div class="well clearfix">
											<div class="loading">
												<i class="fa fa-spinner fa-spin"></i><em> Checking possibility of any small group event conflicts for all students&hellip;</em>
											</div>
											<div class="clash-info hide">
												<div class="clash-singular-info"><span class="clash-count"></span> student has small group event conflicts with this allocation.
													<a href="#" data-href="<@routes.groups.timetableclashstudentslist command.set />" class="ajax-modal timetable-clash-link" data-target="#timetable-clash-modal">See student</a>
												</div>
												<div class="clash-plural-info"><span class="clash-count"></span> students have small group event conflicts with this allocation.
													<a href="#" data-href="<@routes.groups.timetableclashstudentslist command.set />" class="ajax-modal timetable-clash-link" data-target="#timetable-clash-modal">See students</a>
												</div>
											</div>
											<div class="no-clash-info hide">No students have small group event conflicts with the current allocation.</div>
											<div class="student-clash-info hide">
												<span class="possibleclash-group-info"></span>
												<span class="actualclash-userIds"></span>
											</div>
										</div>
									</div>
								</div>

							</div>
						</div>

						<div class="fix-footer">
							<input type="submit" class="btn btn-primary" value="Save">
							<a class="btn btn-default dirty-check-ignore" href="<@routes.groups.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
						</div>
					</@f.form>
				</div>
			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocategroups-tab2">

				<@f.form method="post" enctype="multipart/form-data" action="${submitUrl}" commandName="command" cssClass="dirty-check">

					<p>You can allocate students to groups using a spreadsheet.</p>

					<ol>
						<li><strong><a href="<@routes.groups.templatespreadsheet smallGroupSet />">Download a template spreadsheet</a></strong>. This will be prefilled with the names and University ID numbers of students you have selected to be in ${smallGroupSet.name}. In Excel you may need to <a href="http://office.microsoft.com/en-gb/excel-help/what-is-protected-view-RZ101665538.aspx?CTT=1&section=7">exit protected view</a> to edit the spreadsheet.
						</li>
						<li><strong>Allocate students</strong> to groups using the dropdown menu in the <strong>Group name</strong> column or by pasting in a list of group names. The group names must match the groups you have already created for ${smallGroupSet.name}. The <strong>group_id</strong> field will be updated with a unique ID number for that group.
							You can select additional students to be in ${smallGroupSet.name} by entering their University ID numbers in the <strong>student_id</strong> column. Any students with an empty group_id field will be added to the list of students who haven't been allocated to a group.</li>
						<li><strong>Save</strong> your updated spreadsheet.</li>
						<li>
							<@bs3form.labelled_form_group path="file.upload" labelText="Choose your updated spreadsheet">
								<input type="file" name="file.upload" />
							</@bs3form.labelled_form_group>
						</li>
					</ol>

					<input name="isfile" value="true" type="hidden"/>


					<div>
						<button class="btn btn-primary btn-lg">Upload</button>
					</div>
				</@f.form>

			</div><!-- end 2nd tab-->

		</div><!-- end tab-content -->

	</div> <!-- end tabbable -->

	<script type="text/javascript">
		(function($) {
			<!--TAB-1008 - fix scrolling bug when student list is shorter than the group list-->
			$('#studentslist').css('min-height', function() {
				return $('#groupslist').outerHeight();
			});

			$(window).scroll(function() {
				Groups.fixHeaderFooter.fixDirectionIcon();
				Groups.fixHeaderFooter.fixTargetList('#groupslist'); // eg. personal tutors column
			});

			$.getJSON('/groups/${smallGroupSet.id}/timetableclash', {ts: new Date().getTime()}, function(data) {
				jQuery.each(data.students, function(i, val) {
					var groupId = val[0];
					var userIds = val[1];
					$('.possibleclash-group-info').append( $('<span/>').prop("id",'clash-group-info-' + groupId).append(userIds.join(',')));
				});
				$('.loading').addClass("hide");
				clashInfo();
			});

			// When the return list has changed, make sure the filter is re-run
			$('.return-list').on('changed.tabula', function(e) {
				// Make sure it exists before doing it
				var filter = $('.tabula-filtered-list').data('tabula-filtered-list');
				if (filter) {
					filter.filter();
				}
				setTimeout(function() { clashInfo() }, 12);
			});

			var clashInfo =  function() {
				// check if we have got json result back, if not then no need to check at this stage
				if ($('.loading').hasClass('hide')) {
					var userIds = [];
					var students = $('input[name^="mapping"]').each(function() {
						var inputName = this.name;
						var inputValue = this.value;
						var grpId = inputName.substring(inputName.indexOf('[') + 1, inputName.indexOf(']'));
						var possibleClashGrpInfo = $('#clash-group-info-' + grpId).text();
						if (possibleClashGrpInfo.length > 0 && possibleClashGrpInfo.indexOf(inputValue) >= 0) {
							userIds.push(inputValue);
						}
					});
					var clashUserIds = userIds.join(',');
					$('.actualclash-userIds').append(clashUserIds);
					var totalClashes = userIds.length;
					$('.clash-count').html(totalClashes);
					if (totalClashes > 0) {
						var $timtableClashLink = $('a.timetable-clash-link');
						if (totalClashes > 1) {
							$('.clash-plural-info').removeClass("hide");
							$('.clash-singular-info').addClass("hide");
						} else {
							$('.clash-singular-info').removeClass("hide");
							$('.clash-plural-info').addClass("hide");
						}
						$('.no-clash-info').addClass("hide");
						$('.clash-info').removeClass("hide");
						$timtableClashLink.attr('href', $timtableClashLink.data('href') + clashUserIds);
					} else {
						$('.clash-info').addClass("hide");
						$('.no-clash-info').removeClass("hide");
					}
				}
			};
		})(jQuery);
	</script>
</#escape>