<#escape x as x?html>

<h1>Scheduled ${relationshipType.agentRole} changes</h1>

<#macro relationships relationships>
	<div class="striped-section-contents">
		<div class="item-info">
			<form action="<@routes.profiles.relationship_scheduled_update department relationshipType />" method="post">
				<input type="hidden" name="notifyStudent" value="false" />
				<input type="hidden" name="notifyOldAgent" value="false" />
				<input type="hidden" name="notifyNewAgent" value="false" />
				<table class="related_students table table-striped table-condensed">
					<thead>
					<tr>
						<th><@bs3form.selector_check_all /></th>
						<th>First name</th>
						<th>Last name</th>
						<th>University ID</th>
						<th>New Tutor</th>
						<th>Current Tutor</th>
					</tr>
					</thead>

					<tbody>
						<#list relationships as relationship>
							<#assign studentCourseDetails = relationship.studentCourseDetails />
							<#assign readOnly=(studentCourseDetails.department.code!=department.code) />
							<tr>
								<td>
									<#if readOnly>
										<#assign studentDepartment=studentCourseDetails.department />
										<div class="use-tooltip" data-html="true" data-container="body" data-title="This change can be managed from the student's profile page or from within the ${studentDepartment.name} department.">
									</#if>
									<@bs3form.selector_check_row
										name="relationships"
										value="${relationship.id}"
										readOnly=readOnly
									/>
									<#if readOnly></div></#if>
								</td>
								<td>${studentCourseDetails.student.firstName}</td>
								<td>${studentCourseDetails.student.lastName}</td>
								<td><a class="profile-link" href="/profiles/view/course/${studentCourseDetails.urlSafeId}">${studentCourseDetails.student.universityId}</a></td>
								<#if (relationship.replacesRelationships?has_content)>
									<td>${relationship.agentName}</td>
									<td><#list relationship.replacesRelationships as replaced>${replaced.agentName}<#if replaced_has_next>, </#if></#list></td>
								<#elseif (relationship.startDate?? && relationship.startDate.afterNow)>
									<td>${relationship.agentName}</td>
									<td>-</td>
								<#elseif (relationship.endDate?? && relationship.endDate.afterNow)>
									<td>-</td>
									<td>${relationship.agentName}</td>
								</#if>

							</tr>
						</#list>
					</tbody>
				</table>

				<#if can.do_with_selector('Profiles.StudentRelationship.Manage', department, relationshipType)>
					<#assign apply_popover>
						<@bs3form.labelled_form_group path="" labelText="Notify these people via email of this change">
							<@bs3form.checkbox>
								<input type="checkbox" name="notifyStudent" />
								${relationshipType.studentRole?cap_first}
							</@bs3form.checkbox>
							<@bs3form.checkbox>
								<input type="checkbox" name="notifyOldAgent" />
								Old ${relationshipType.agentRole?cap_first}s
							</@bs3form.checkbox>
							<@bs3form.checkbox>
								<input type="checkbox" name="notifyNewAgent" />
								New ${relationshipType.agentRole?cap_first}s
							</@bs3form.checkbox>
						</@bs3form.labelled_form_group>
						<button type="button" class="btn btn-primary apply-scheduled-change" name="action" value="${UpdateScheduledStudentRelationshipChangesControllerActions.Apply}">Apply selected changes</button>
					</#assign>
					<#assign cancel_popover>
						<@bs3form.labelled_form_group path="" labelText="Notify these people via email of this cancellation">
							<@bs3form.checkbox>
								<input type="checkbox" name="notifyStudent" />
							${relationshipType.studentRole?cap_first}
							</@bs3form.checkbox>
							<@bs3form.checkbox>
								<input type="checkbox" name="notifyOldAgent" />
							${relationshipType.agentRole?cap_first}s no longer removed
							</@bs3form.checkbox>
							<@bs3form.checkbox>
								<input type="checkbox" name="notifyNewAgent" />
							${relationshipType.agentRole?cap_first}s no longer assigned
							</@bs3form.checkbox>
						</@bs3form.labelled_form_group>
						<button type="button" class="btn btn-danger cancel-scheduled-change" name="action" value="${UpdateScheduledStudentRelationshipChangesControllerActions.Cancel}">Cancel selected changes</button>
					</#assign>
					<p>
						<button
							type="button"
							class="btn btn-primary use-popover"
							data-html="true"
							data-title="Apply selected changes now"
							data-content="${apply_popover}"
						>
							Apply selected changes now
						</button>
						<button
							name="cancel"
							value="true"
							type="button"
							class="btn btn-default use-popover"
							data-html="true"
							data-title="Cancel selected changes"
							data-content="${cancel_popover}"
						>
							Cancel selected changes
						</button>
					</p>
				</#if>
			</form>
		</div>
	</div>
</#macro>

<#if !changesByDate?has_content>
	<p>No scheduled ${relationshipType.agentRole} changes</p>
<#else>
	<#list changesByDate as datePair>
		<#if datePair._2()?size == 1>
			<div class="striped-section collapsible">
				<h4 class="section-title" title="Expand">
					<span class="very-subtle pull-right"><@fmt.p datePair._2()?first._2()?size "change" /></span>
					<@fmt.date date=datePair._1() relative=false includeTime=false />
				</h4>
				<@relationships datePair._2()?first._2() />
			</div>
		<#else>
			<#list datePair._2() as dateTimePair>
				<div class="striped-section collapsible">
					<h4 class="section-title" title="Expand">
						<span class="very-subtle pull-right"><@fmt.p dateTimePair._2()?size "change" /></span>
						<@fmt.date date=dateTimePair._1() relative=false includeTime=true />
					</h4>
					<@relationships dateTimePair._2() />
				</div>
			</#list>
		</#if>
	</#list>
</#if>

<p><a href="<@routes.profiles.relationship_agents department relationshipType />" class="btn btn-default">Cancel</a></p>

<script>
	jQuery(function($){
		$('.striped-section-contents table').bigList({
			'onSomeChecked' : function(){
				$(this).closest('.item-info').find('p button').prop('disabled', false);
			},
			'onNoneChecked' : function(){
				$(this).closest('.item-info').find('p button').prop('disabled', true);
			}
		});
		$('body').on('click', '.apply-scheduled-change, .cancel-scheduled-change', function(){
			var $this = $(this), $form = $this.closest('.popover').data('creator').closest('form');
			$this.closest('div').find('input:checked').each(function(){
				$form.find('input[name=' + $(this).prop('name') + ']').val('true');
			});
			$form.append($('<input/>').prop({
				'type' : 'hidden',
				'name' : 'action',
				'value' : $this.prop('value')
			}));
			$form.submit();
		});
	});
</script>

</#escape>