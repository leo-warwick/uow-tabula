<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#import "*/cm2_macros.ftl" as cm2 />
	<#include "assign_marker_macros.ftl" />

	<@cm2.assignmentHeader "Assign markers" assignment "for" />

	<div class="fix-area">
		<@components.assignment_wizard 'markers' assignment.module false assignment />
		<p class="btn-toolbar">
			<a class="return-items btn btn-default" href="<@routes.cm2.assignmentmarkers assignment mode />" >
				Return to drag and drop
			</a>
		</p>
		<#if sets?has_content>
			<#assign actionUrl><@routes.cm2.assignmentmarkerssmallgroups assignment mode /></#assign>
			<@f.form method="post" action=actionUrl cssClass="dirty-check" commandName="assignMarkersCommand">

				<@bs3form.labelled_form_group path="" labelText="Small group set">
					<@f.select path="" cssClass="form-control set-selector">
						<option selected disabled></option>
						<#list sets as set>
							<option value="set-${set.id}">${set.name}</option>
						</#list>
					</@f.select>
				</@bs3form.labelled_form_group>

				<#list allocations as allocation>
					<div class="set" id="set-${allocation.set.id}" style="display:none">
						<#list allocationOrder as roleOrStage>
							<#assign groupAllocations = mapGet(allocation.allocations, roleOrStage)![]>
							<#assign stages = mapGet(stageNames, roleOrStage)![roleOrStage]>
							<div class="role" data-stages="${stages?join(",")}">
								<h2>${roleOrStage}</h2>
								<#list groupAllocations as group>
									<div class="group well">
										<h5>${group.name}</h5>
										<div class="row">
											<div class="col-md-5">
												<strong>Students</strong>
												<ul>
													<#list group.students as student>
														<li>
															${student.fullName}&nbsp;${student.warwickId!userId}
															<#list stages as stage>
																<#-- one input per stage -->
																<input
																	class="marker-input"
																	disabled="disabled"
																	type="hidden"
																	data-stage="${stage}"
																	name=""
																	value="${student.userId}"
																>
															</#list>
														</li>
													</#list>
												</ul>
											</div>
											<div class="col-md-7">
												<@bs3form.labelled_form_group path="" labelText="Marker">
													<@f.select path="" cssClass="form-control marker-select">
														<#-- list tutors from this group first -->
														<#list group.tutors as tutor>
															<option value="${tutor.userId}">${tutor.fullName}</option>
														</#list>
														<#-- list tutors from other groups last -->
														<#list group.otherTutors as tutor>
															<option value="${tutor.userId}">${tutor.fullName}</option>
														</#list>
													</@f.select>
												</@bs3form.labelled_form_group>
											</div>
										</div>
									</div>
								</#list>
							</div>
						</#list>
						<div class="fix-footer">
							<input
									type="submit"
									class="btn btn-primary"
									name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
									value="Save and continue"
							/>
							<input
									type="submit"
									class="btn btn-primary"
									name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
									value="Save and exit"
							/>
						</div>
					</div>
				</#list>

				<script type="text/javascript">
					(function($) {

						// change the marker ID for all allocations inputs in the parent group
						function selectMarker($select) {
							var $group = $select.closest('.group');
							var markerId = $select.val();
							var $markerInputs = $group.find('.marker-input');

							$markerInputs.each(function() {
								var $this = $(this);
								var stage = $this.data('stage');
								$this.prop('name', "allocations['"+stage+"']['"+markerId+"'][]");
							});
						}

						// recalculate indices - blegh
						function updateIndices($role){
							var stageData = $role.data('stages');
							var stages = stageData ? stageData.split(',') : [];
							var markers = [];
							// all selected markers
							$role.find('.marker-select').each(function(){
								var $this = $(this);
								var marker = $this.val();
								markers.indexOf(marker) === -1 ? markers.push(marker) : Function.prototype;
							});

							stages.forEach(function(s){
								markers.forEach(function(m){
									// horrible selector - all allocations for this marker and stage across all groups
									$("input[name^=allocations\\[\\'"+s+"\\'\\]\\[\\'"+m+"\\'\\]]").each(function(i){
										var $this = $(this);
										var n = $this.prop('name').replace(/\[\d*\]/, "["+i+"]");
										$this.prop('name', n);
									});
								});
							});
						}

						$('.marker-select').on('change', function(){
							var $this = $(this);
							selectMarker($this);
							updateIndices($this.closest('.role'));
						});

						// show / hide sets and enable their inputs
						$('.set-selector').on('change', function(){
							var set = $(this).val();
							$('.set').hide().find('.marker-input').prop('disabled', true);
							var $set = $('#'+set);
							$set.show().find('.marker-input').prop('disabled', false);
							$set.find('.marker-select').trigger('change'); // ensure name is pre-populated
						});

					})(jQuery);
				</script>
			</@f.form>
		<#else>
			<div>
				<p>
					The small group set allocation for <@fmt.module_name module=assignment.module withFormatting=false />
					in ${assignment.academicYear.toString} could not be imported.
				</p>
				The list of markers in the marking workflow for this assignment must include all tutors in the small group
				set allocation. Return to the <a href="<@routes.cm2.editassignmentdetails assignment />">
				Assignment details</a> section and ensure that you have added all the small group tutors as markers.
			</div>
		</#if>
	</div>
</#escape>