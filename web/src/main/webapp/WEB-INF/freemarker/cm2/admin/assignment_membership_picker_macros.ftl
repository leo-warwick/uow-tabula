<#include "*/prelude.ftl" />
<#escape x as x?html>

<#macro coursework_sits_groups command >
	<#if command.availableUpstreamGroups?has_content>
		<div class="assessment-component">
			<table id="sits-table" class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
				<thead>
				<tr>
					<th class="for-check-all"><input  type="checkbox" class="collection-check-all" title="Select all/none" /> </th>
					<th class="sortable">Name</th>
					<th class="sortable">Members</th>
					<th class="sortable">Assessment group</th>
					<th class="sortable">CATS</th>
					<th class="sortable">Occurrence</th>
					<th class="sortable">Sequence</th>
					<th class="sortable">Type</th>
				</tr>
				</thead>
				<tbody>
					<#list command.availableUpstreamGroups as available>
						<#local isLinked = available.isLinked(command.assessmentGroups) />
					<tr>
						<td>
							<input
									type="checkbox"
									id="chk-${available.id}"
									value="${available.id}"
							${isLinked?string(" checked","")}
									class="collection-checkbox"
							>
						</td>
						<td><label for="chk-${available.id}">${available.name}<span class="label label-primary linked <#if !isLinked>hidden</#if>">Linked</span></label></td>
						<td class="sortable">${available.group.members?size}</td>
						<td>${available.group.assessmentGroup}</td>
						<td>${available.cats!'-'}</td>
						<td>${available.occurrence}</td>
						<td>${available.sequence}</td>
						<td>${available.assessmentType!'A'}</td>
					</tr>
					</#list>
				</tbody>
			</table>
			<div class="sits-picker">
				<a class="btn btn-primary disabled sits-picker sits-picker-action spinnable spinner-auto link-sits" data-url="<@routes.enrolment command.assignment />">Link</a>
				<a class="btn btn-danger disabled sits-picker sits-picker-action spinnable spinner-auto unlink-sits" data-url="<@routes.enrolment command.assignment/>">Unlink</a>
			</div>
		</div>
	<#else>
			<p class="alert alert-danger">No SITS membership groups for ${command.module.code?upper_case} are available</p>
	</#if>
</#macro>

<#macro header command>
	<#local membershipInfo = command.membershipInfo />
	<#local popoverText>
	<p>You can link to one or more assessment components in SITS and the list of students will be updated automatically from there.
		If you are not using SITS you can manually add students by ITS usercode or university number.</p>

	<p>It is also possible to tweak the list even when using SITS data, but this is only to be used
		when necessary and you still need to ensure that the upstream SITS data gets fixed.</p>
	</#local>
	<#local hasMembers = membershipInfo.totalCount gt 0 />
	<p>
		<span class="uneditable-value enrolledCount">
			<#if linkedUpstreamAssessmentGroups?has_content>
					${membershipInfo.totalCount} enrolled
					<#if membershipInfo.excludeCount gt 0 || membershipInfo.includeCount gt 0>
						<span class="muted very-subtle">(${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount} removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, plus ${membershipInfo.usedIncludeCount} added manually</#if>)</span>
					<#else>
						<span class="muted very-subtle">from SITS</span>
					</#if>
			<#elseif hasMembers>
				${membershipInfo.includeCount} manually enrolled
			<#else>
				No students enrolled
			</#if>
			<span><@fmt.help_popover id="assessmentComponentInfo-${assignment.id}"  content="${popoverText}" html=true /></span>
		</span>
	</p>
</#macro>

<#macro fieldset command enrolment_url>
	<fieldset id="assignmentEnrolmentFields">
	<div class="assignmentEnrolmentInner">
		<#list command.upstreamGroups as item>
			<@f.hidden path="upstreamGroups[${item_index}]" cssClass="upstreamGroups" />
		</#list>

		<@spring.bind path="members">
			<#local membersGroup=status.actualValue />
		</@spring.bind>

		<#local includeText><span class="use-tooltip" title="Added manually" data-placement="right">Added</#local>
		<#local pendingDeletionText><span class="use-tooltip" title="Deleted manual addition" data-placement="right">Pending deletion</#local>
		<#local excludeText><span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right">Removed</#local>
		<#local sitsText><span class="use-tooltip" title="Automatically linked from SITS" data-placement="right">SITS</#local>

		<#local membershipInfo = command.membershipInfo />
		<#local hasMembers=(membershipInfo.totalCount gt 0 || membershipInfo.includeCount gt 0 || membershipInfo.excludeCount gt 0) />

		<div class="remove-restore assignmentEnrolmentInfo">
			<#if hasMembers>
				<span class="use-tooltip"  <#if availableUpstreamGroups??>title="This will only remove enrolment for this assignment in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>>
					<a class="btn btn-primary disabled remove-users member-action">Remove</a>
				</span>

				<span class="use-tooltip" title="Re-enrol selected students">
					<a class="btn btn-primary restore-users disabled">Restore</a>
				</span>
			</#if>
		</div>
		<#if hasMembers>
			<div id="enrolment" class="enrolment-btn">
				<table id="enrolment-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
					<thead>
						<tr>
							<th class="for-check-all"><input  type="checkbox" class="collection-check-all" title="Select all/none" /></th>
							<th class="sortable">Source</th>
							<th class="sortable">First name</th>
							<th class="sortable">Last name</th>
							<th class="sortable">ID</th>
							<th class="sortable">Usercode</th>

						</tr>
					</thead>

					<tbody>
						<#list membershipInfo.items as item>
							<#local _u = item.user>

							<tr class="membership-item item-type-${item.itemTypeString}"> <#-- item-type-(sits|include|exclude) -->
								<td>

									<#-- TAB-1240: use the right key -->
									<#if command.members.universityIds && item.universityId?has_content>
										<@bs3form.selector_check_row "modifyEnrolment" item.universityId />
									<#elseif item.userId?has_content>
										<@bs3form.selector_check_row "modifyEnrolment" item.userId />
									<#else>
										<i class="icon-ban-circle fa fa-ban use-tooltip" title="We are missing this person's usercode, without which we cannot modify their enrolment."></i>
									</#if>
								</td>
								<td class="source">
									<#noescape>
										<#if item.itemTypeString='include'>
											${includeText}
										<#elseif item.itemTypeString='exclude'>
											${excludeText}
										<#else>
											${sitsText}
										</#if>
									</#noescape>
								</td>
								<td>
									<#if _u.foundUser>
										${_u.firstName}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
								<td>
									<#if _u.foundUser>
										${_u.lastName}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
								<td>
									<#if item.universityId??>
										${item.universityId}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
								<td>
									<#if item.userId??>
										${item.userId}
									<#else>
										<span class="muted">Unknown</span>
									</#if>
								</td>
							</tr>
						</#list>
					</tbody>

					<#list command.members.allIncludedIds as untypedId>
						<input type="hidden" name="includeUsers" value="${untypedId}">
					</#list>

					<#list command.members.allExcludedIds as untypedId>
						<input type="hidden" name="excludeUsers" value="${untypedId}">
					</#list>
				</table>
			</div>
		<#else>
			<#-- placeholder to allow new links to be appended via script -->
			<span id="enrolment-table"></span>
		</#if>
	</fieldset>

	<script type="text/javascript">
		jQuery(function($) {
			var $enrolment = $('.assignmentEnrolment');

			var initEnrolment = function() {
				$enrolment.tabulaPrepareSpinners();
				$('.use-popover').tabulaPopover({
					trigger: 'click',
					container: 'body'
				});
			};
			// ensure that the close handler for any popovers still work
			$('.assignment-student-details').on('click', '.close', function() { $enrolment.find('.use-popover').popover('hide') });
			//TODO - This needs to be replaced with bigLisrt. Leaving it as it is for time being as it is breaking some other things
			$enrolment.find('.table-checkable').bigList({
				onChange: function(){
					var $table = $(this).closest('table');
					enableActions($table);
				}
			});

			<#-- enable/disable action buttons -->
			var enableActions = function($table) {
				var context = $table.prop('id');
				if (context == 'sits-table') {
					$('.sits-picker-action').toggleClass('disabled', $table.find('input:checked').length==0);
				} else if (context == 'enrolment-table') {
					$('.remove-users').toggleClass('disabled', $table.find('tr.item-type-include input:checked, tr.item-type-sits input:checked').length==0);
					$('.restore-users').toggleClass('disabled', $table.find('tr.item-type-exclude input:checked').length==0);
				}
			};

			<#-- initialise the scripting for enrolment management -->
			initEnrolment();
			enableActions($('#sits-table'));

			<#-- make table rows clickable -->
			$enrolment.on('click', '.table-checkable tr', function(e) {
				if ($(e.target).is(':not(input:checkbox)')) {
					e.preventDefault();
					var $chk = $(this).find('input:checkbox');
					if ($chk.length) {
						$chk.prop('checked', !$chk.prop('checked'));
					}
				}
			});

			<#-- sits-picker click handler -->
			$enrolment.on('click', '.sits-picker .btn', function(e) {
				e.preventDefault();
				var $assessment = $('.assessment-component');
				var $linkUnlink = $(this);
				if ($linkUnlink.is(':not(.disabled)')) {
					$('.sits-picker .btn').addClass('disabled');
					<#-- get current list of values and remove and/or add changes -->
					var current = $('.upstreamGroups').map(function(i, input) { return input.value }).toArray();
					var changes = $assessment.find('td input:checked').map(function(i, input) { return input.value }).toArray();
					// always remove even when adding, to dedupe
					var data = $(current).not(changes).toArray();
					if ($linkUnlink.is('.link-sits')) {
						data = data.concat(changes);
					}
					var $newInputs = $(data).map(function(i, value) {
						return $('<input>', { 'class': 'upstreamGroups', type: 'hidden', name: 'upstreamGroups['+i+']', value:value })[0];
					});
					$('.upstreamGroups').remove();

					$('#enrolment-table').append($newInputs);
					$.ajax({
						type: 'POST',
						url: '${enrolment_url}',
						data: $('#assignmentEnrolmentFields').find('input, textarea, select').add('#academicYear').serialize(),
						success: function(data, status) {
							$enrolment.find('.assignmentEnrolmentInner').html($(data).find('.assignmentEnrolmentInner').contents());
							$enrolment.find('.enrolledCount').html($(data).find('.enrolledCount').contents());
							initEnrolment();
							$assessment.find('td input:checked').each( function() {
								var $tr = $(this).closest('tr');
								if ($linkUnlink.is('.link-sits')) {
									$tr.find('.linked').removeClass('hidden');
								} else {
									$tr.find('.linked').addClass('hidden');
								}
							});
							$('.sits-picker .btn').removeClass('disabled');
						}
					});
				}
			});

			<#-- adder click handler -->
			$enrolment.on('click', '.btn.add-students-manually', function(e) {
				e.preventDefault();
				var $addManualStudentBtn = $(this);
				$addManualStudentBtn.addClass('disabled');
					$.ajax({
						type: 'POST',
						url: '${enrolment_url}',
						data: $('#command').find('input, textarea, select').add('#academicYear').serialize(),
						success: function(data, status) {
							$enrolment.find('.assignmentEnrolmentInner').html($(data).find('.assignmentEnrolmentInner').contents());
							$enrolment.find('.enrolledCount').html($(data).find('.enrolledCount').contents());
							$addManualStudentBtn.removeClass('disabled');
							initEnrolment();
						}
					});
			});

			<#-- remove user from enrolment table -->
			$enrolment.on('click', '.remove-users', function(e) {
				e.preventDefault();
				$('#enrolment-table').find('tr.item-type-include input:checked, tr.item-type-sits input:checked').each(function() {
					var untypedId = $(this).val();
					var $tr = $(this).closest('tr');

					// update both hidden fields and table
					$('#enrolment-table').find('input:hidden[name=includeUsers][value='+ untypedId + ']').remove();

					$('#enrolment-table').append($('<input type="hidden" name="excludeUsers" />').val(untypedId));
					if ($tr.is('.item-type-sits')) {
						$tr.find('.source').html('<#noescape>${excludeText}</#noescape>');
					} else {
						$tr.find('.source').html('<#noescape>${pendingDeletionText}</#noescape>');
					}
					$tr.removeClass(function(i, css) {
						return (css.match(/\bitem-type-\S+/g) || []).join(' ');
					}).addClass('item-type-exclude');
					this.checked = '';
				});
			});

			<#-- restore excluded user -->
			$enrolment.on('click', '.restore-users', function(e) {
				e.preventDefault();
				$('#enrolment-table').find('tr.item-type-exclude input:checked').each(function() {
					var untypedId = $(this).val();
					var $tr = $(this).closest('tr');

					// update both hidden fields and table
					$('#enrolment-table').find('input:hidden[name=excludeUsers][value='+ untypedId + ']').remove();
					$('#enrolment-table').append($('<input type="hidden" name="includeUsers" />').val(untypedId));
					$tr.find('.source').html('<#noescape>${includeText}</#noescape>');
					$tr.removeClass(function(i, css) {
						return (css.match(/\bitem-type-\S+/g) || []).join(' ');
					}).addClass('item-type-include pending');
					this.checked = '';
				});
			});
		});
	</script>
</#macro>

</#escape>
