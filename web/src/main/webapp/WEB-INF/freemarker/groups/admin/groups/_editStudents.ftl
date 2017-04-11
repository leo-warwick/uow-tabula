<#escape x as x?html>
	<#import "*/membership_picker_macros.ftl" as membership_picker />

	<input type="hidden" name="action" value="submit" id="action-submit">

	<#if smallGroupSet.linked>
		<div class="groupEnrolment">
			<@bs3form.form_group path="members">
				<p>There are <@fmt.p smallGroupSet.members.size "student" /> in ${smallGroupSet.name} (from ${smallGroupSet.linkedDepartmentSmallGroupSet.name}).</p>

				<div id="enrolment">
					<table id="enrolment-table" class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
						<thead>
							<tr>
								<th class="sortable">First name</th>
								<th class="sortable">Last name</th>
								<th class="sortable">ID</th>
								<th class="sortable">User</th>
							</tr>
						</thead>
						<tbody>
							<#list smallGroupSet.members.users as user>

								<tr class="membership-item item-type-sits">
									<td>
										<#if user.foundUser>
											${user.firstName}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
									<td>
										<#if user.foundUser>
											${user.lastName}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
									<td>
										<#if user.warwickId??>
											${user.warwickId}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
									<td>
										<#if user.userId??>
											${user.userId}
										<#else>
											<span class="muted">Unknown</span>
										</#if>
									</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</div>
			</@bs3form.form_group>
		</div>

		<script type="text/javascript">
			jQuery(function($) {
				$('#enrolment').find('.table-sortable').sortableTable();
			});
		</script>
	<#else>
		<div class="groupEnrolment">
			<@bs3form.form_group path="members">
				<div class="alert alert-info" style="display: none;" data-display="fragment">
					The membership list for these groups has been updated
				</div>

				<@spring.bind path="members">
					<#assign membersGroup=status.actualValue />
				</@spring.bind>
				<#assign hasMembers=(membersGroup?? && (membersGroup.allIncludedIds?size gt 0 || membersGroup.allExcludedIds?size gt 0)) />

				<#-- Members picker is pretty hefty so it is in a separate file -->
				<@membership_picker.header command />
				<#assign enrolment_url><@routes.groups.enrolment smallGroupSet /></#assign>
				<@membership_picker.fieldset command 'group' 'group set' enrolment_url/>

			</@bs3form.form_group>
		</div>
	</#if>
</#escape>