<#include "prelude.ftl" />
<#import "_profile_link.ftl" as pl />
<#import "*/modal_macros.ftl" as modal />

<#--

	Membership editor macros.


-->
<#escape x as x?html>

<#macro _what_is_this>
	<#local popoverText>
		<p>You can link to one or more assessment components in SITS and the list of students will be updated automatically from there.
			If you are not using SITS you can manually add students by ITS usercode or university number.</p>

		<p>It is also possible to tweak the list even when using SITS data, but this is only to be used
			when necessary and you still need to ensure that the upstream SITS data gets fixed.</p>
	</#local>
	<@fmt.help_popover id="what_is_this" title="Students" content="${popoverText}" html=true />
</#macro>

<#--


-->
<#macro header command>

	<#local membershipInfo = command.membershipInfo />
	<#local hasMembers = membershipInfo.totalCount gt 0 />

	<#-- enumerate current state -->
	<p>
	<#if linkedUpstreamAssessmentGroups?has_content>
		<span class="uneditable-value enrolledCount">
			${membershipInfo.totalCount} enrolled
			<#if membershipInfo.excludeCount gt 0 || membershipInfo.includeCount gt 0>
				<span class="muted very-subtle">(${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount} removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, plus ${membershipInfo.usedIncludeCount} added manually</#if>)</span>
			<#else>
				<span class="muted very-subtle">from SITS</span>
			</#if>
		<@_what_is_this />
		</span>
	<#elseif hasMembers>
		<span class="uneditable-value enrolledCount">${membershipInfo.includeCount} manually enrolled
		<@_what_is_this />
		</span>
	<#else>
		<span class="uneditable-value enrolledCount">No students enrolled
		<@_what_is_this />
		</span>
	</#if>
	</p>

</#macro><#-- end of "header" -->


<#--
Generates the bulk of the picker HTML, inside a fieldset element

	Params:
	command: The command object that extends UpdatesStudentMembership
	classifier: String to be used in IDs and classes - e.g. 'assignment' or 'group'
	name: Name in english to describe the entity with members - e.g. 'assignment' or 'group set'
	enrolment_url: URL to submit to
-->
<#macro fieldset command classifier name enrolment_url>

<fieldset id="${classifier}EnrolmentFields"><!-- new and improved -->
<div class="${classifier}EnrolmentInner">

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

	<#-- FIXME: alerts fired post SITS change go here, if controller returns something to say -->
	<#-- <p class="alert alert-success"><i class="icon-ok fa fa-check"></i> This ${name} is (now linked|no longer linked) to ${r"${name}"} and ${r"${name}"}</p> -->

	<div style="margin-bottom: 8px;">
		<#if linkedUpstreamAssessmentGroups?has_content>
			<a class="btn btn-default use-tooltip disabled show-sits-picker" title="Change the linked SITS ${name} used for enrolment data">Change link to SITS</a>
		<#elseif availableUpstreamGroups?has_content>
			<a class="btn btn-default use-tooltip disabled show-sits-picker" title="Use enrolment data from one or more ${name}s recorded in SITS">Add link to SITS</a>
		<#else>
			<a class="btn btn-default use-tooltip disabled" title="No ${name}s are recorded for this module in SITS. Add them there if you want to create a parallel link in Tabula.">No SITS link available</a>
		</#if>

		<a class="btn btn-default use-tooltip disabled show-adder"
		   <#if availableUpstreamGroups??>title="This will only enrol a student for this ${name} in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>
		>
			Add students manually
		</a>

		<#if hasMembers>
			<#if component.name == 'groups'>
				<a
					data-href="<@routes.withdrawnStudents command.set />"
					class="btn btn-default select-withdrawn member-action use-tooltip"
					title="Select permanently withdrawn students" data-loading-text="Loading&hellip;"
				>
					Select PWD students
				</a>
			</#if>
			<div class="use-tooltip" style="display: inline-block;" <#if availableUpstreamGroups??>title="This will only remove enrolment for this ${name} in Tabula. If SITS data appears to be wrong then it's best to have it fixed there."</#if>>
				<a class="btn btn-primary disabled remove-users member-action">Remove</a>
			</div>

			<div class="use-tooltip" style="display: inline-block;" title="Re-enrol selected students">
				<a class="btn btn-primary restore-users disabled">Restore</a>
			</div>

			<#if showIntro("membership-buttons-disabled", "anywhere")>
				<#assign introText>
					<p>Checkbox entries must be selected to activate options.</p>
				</#assign>
				<a href="#"
				   id="membership-buttons-disabled-intro"
				   class="use-introductory<#if showIntro("tier4-filtering", "anywhere")> auto</#if>"
				   data-hash="${introHash("tier4-filtering", "anywhere")}"
				   data-placement="bottom"
				   data-html="true"
				   data-container="#${classifier}EnrolmentFields"
				   data-content="${introText}"><i class="icon-question-sign fa fa-question-circle"></i></a>
			</#if>

		</#if>

		<span class="help-inline" id="js-hint"><small><i class="icon-lightbulb fa fa-lightbulb-o"></i> Javascript is required for editing</small></span>
	</div>

	<#if hasMembers>
		<div id="enrolment">
			<div id="profile-modal" class="modal fade profile-subset"></div>
			<table id="enrolment-table" class="table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers <#if component.name == 'courses'>tabula-orangeLight</#if>">
				<thead>
					<tr>
						<th class="for-check-all" style="width: 20px; padding-right: 0;"></th>
						<th class="sortable" style="width: 1px;">Source</th>
						<th class="sortable">First name</th>
						<th class="sortable">Last name</th>
						<th class="sortable">ID</th>
						<th class="sortable">User</th>
					</tr>
				</thead>

				<tbody>
					<#list membershipInfo.items as item>
						<#local _u = item.user>

						<tr class="membership-item item-type-${item.itemTypeString}"> <#-- item-type-(sits|include|exclude) -->
							<td>
								<#-- TAB-1240: use the right key -->
								<#if command.members.universityIds && item.universityId?has_content>
									<#if component.name == 'groups'>
										<@bs3form.selector_check_row "modifyEnrolment" item.universityId />
									<#else>
										<@form.selector_check_row "modifyEnrolment" item.universityId />
									</#if>
								<#elseif item.userId?has_content>
									<#if component.name == 'groups'>
										<@bs3form.selector_check_row "modifyEnrolment" item.userId />
									<#else>
										<@form.selector_check_row "modifyEnrolment" item.userId />
									</#if>
								<#else>
									<i class="icon-ban-circle fa fa-ban use-tooltip" title="We are missing this person's usercode, without which we cannot modify their enrolment."></i>
								</#if>
							</td>
							<td class="source" style="white-space: nowrap;">
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
									<@pl.profile_link item.universityId />
								<#elseif item.userID??>
									${item.userID}
									<@pl.profile_link item.userID />
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

</div>

<#-- Modal to add students manually -->
<div class="${classifier}Modals">
	<div class="modal fade <#if component.name == 'courses'>hide</#if> adder">
		<@modal.wrapper>
			<@modal.header>
				<h6 class="modal-title">Add students manually</h6>
			</@modal.header>

			<@modal.body>
				<p>
					Type or paste in a list of usercodes or University numbers here, separated by white space, then click <code>Add</code>.
				</p>
				<p class="alert">
					<i class="icon-lightbulb fa fa-lightbulb-o icon-large fa fa-lg"></i> <strong>Is your module in SITS?</strong> It may be better to fix the data there,
					as other University systems won't know about any changes you make here.
				</p>
				<textarea rows="6" class="input-block-level form-control" name="massAddUsers"></textarea>
			</@modal.body>

			<@modal.footer>
				<a class="btn btn-primary disabled spinnable spinner-auto add-students">Add</a>
			</@modal.footer>
		</@modal.wrapper>
	</div><#-- manual student modal -->


	<#-- Modal picker to select an upstream assessment group (${name}+occurrence) -->
	<div class="modal fade <#if component.name == 'courses'>hide</#if> sits-picker">
		<@modal.wrapper cssClass="modal-lg">
			<@modal.header>
				<h6>SITS link</h6>
			</@modal.header>

			<#if command.availableUpstreamGroups?has_content>
				<@modal.body>
					<p>Add students by linking this ${name} to one or more of the following assessment components in SITS for
					${command.module.code?upper_case} in ${command.academicYear.label}.</p>

					<table id="sits-table" class="table <#if component.name == 'courses'>table-bordered tabula-orangeLight</#if> table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
						<thead>
							<tr>
								<th class="for-check-all" style="width: 20px; padding-right: 0;"></th>
								<th class="sortable">Name</th>
								<th class="sortable">Members</th>
								<th class="sortable">Assessment group</th>
								<th class="sortable">CATS</th>
								<th class="sortable">Occurrence</th>
								<th class="sortable">Sequence</th>
								<th class="sortable">Type</th>
							</tr>
						</thead>
						<tbody><#list command.availableUpstreamGroups as available>
							<#local isLinked = available.isLinked(command.assessmentGroups) />
							<tr>
								<td><input type="checkbox" id="chk-${available.id}" name="" value="${available.id}"></td>
								<td><label for="chk-${available.id}">${available.name}<#if isLinked> <span class="label label-primary">Linked</span></#if></label></td>
								<td>${available.group.members?size}</td><#-- FIXME: a.popover (overflow-y: scroll) with member list -->
								<td>${available.group.assessmentGroup}</td>
								<td>${available.cats!'-'}</td>
								<td>${available.occurrence}</td>
								<td>${available.sequence}</td>
								<td>${available.assessmentType!'A'}</td> <#-- TAB-1174 can remove default when non-null -->
							</tr>
						</#list></tbody>
					</table>
				</@modal.body>

				<@modal.footer>
					<a class="btn btn-primary disabled sits-picker-action spinnable spinner-auto link-sits">Link</a>
					<a class="btn btn-danger disabled sits-picker-action spinnable spinner-auto unlink-sits">Unlink</a>
				</@modal.footer>
			<#else>
				<@modal.body>
					<p class="alert alert-danger">No SITS membership groups for ${command.module.code?upper_case} are available</p>
				</@modal.body>
			</#if>
		</@modal.wrapper>
	</div><#-- link picker modal -->
</div>


</fieldset>

	<script type="text/javascript">
	jQuery(function($) {
		var $enrolment = $('.${classifier}Enrolment');

		var initEnrolment = function() {
			<#-- well, if we're here, JS must be available :) -->
			$('#js-hint').remove();
			$('.show-sits-picker, .show-adder').removeClass('disabled');

			<#-- sortable tables -->
			$enrolment.find('.table-sortable').sortableTable();
			$enrolment.tabulaPrepareSpinners();
			$enrolment.find('summary:not([role="button"])').closest('details').details();

			// TODO this is cribbed out of scripts.js - re-use would be better
			$enrolment.find('.use-popover').each(function() {
				if ($(this).attr('data-title')) {
					$(this).attr('data-original-title', $(this).attr('data-title'));
				}
			});

			$enrolment.find('.use-popover').popover({
				trigger: 'click',
				container: '#container',
				template: '<div class="popover"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
			}).click(function(){ return false; });

			// ensure that the close handler for any popovers still work
			$('#container').on('click', '.close', function() { $enrolment.find('.use-popover').popover('hide') });

			<#-- FIXME: temporary pop-out hiding. Do this properly at source in SBTWO idscripts -->
			setTimeout(function() { $('.sb-table-wrapper-popout').remove() }, 500);

			<#-- dynamically attach check-all checkbox -->
			$('.for-check-all').append($('<input />', { type: 'checkbox', 'class': 'check-all use-tooltip', title: 'Select all/none' }));
			$('.check-all').tooltip({ delay: 1500 });
			$enrolment.on('click', '.table-checkable th .check-all', function(e) {
				var $table = $(this).closest('table');
				var checkStatus = this.checked;
				$table.find('td input:checkbox').prop('checked', checkStatus);

				updateCheckboxes($table);
				enableActions($table);
			});

			<#-- preset to open -->
			if ($enrolment.data('open')) {
				$('.${classifier}Enrolment details').prop('open', 'open');
				window.id6nav=window.id6nav||{};
				$("html, body").delay(200).animate({
					scrollTop: $enrolment.offset().top - window.id6nav.navigationHeight
				}, 300);
			}
		};

		<#-- initialise the scripting for enrolment management -->
		<#if RequestParameters.open?? || openDetails!false>
			$enrolment.data('open', true);
		</#if>
		initEnrolment();

		var $pendingAlert = $('<p class="alert alert-warning hide"><i class="icon-warning-sign fa fa-exclamation-triangle"></i> Your changes will not be recorded until you save this ${name}.	<input type="submit" value="Save" class="btn btn-primary btn-mini update-only"></p>');

		<#-- manage check-all state -->
		var updateCheckboxes = function($table) {
			var checked = $table.find('td input:checked').length;
			if (checked == $table.find('td input').length) $table.find('.check-all').prop('checked', true);
			if (checked == 0) $table.find('.check-all').prop('checked', false);
		}

		<#-- en/disable action buttons -->
		var enableActions = function($table) {
			var context = $table.prop('id');

			if (context == 'sits-table') {
				$('.sits-picker-action').toggleClass('disabled', $table.find('input:checked').length==0);
			} else if (context == 'enrolment-table') {
				$('.remove-users').toggleClass('disabled', $table.find('tr.item-type-include input:checked, tr.item-type-sits input:checked').length==0);
				$('.restore-users').toggleClass('disabled', $table.find('tr.item-type-exclude input:checked').length==0);
			}
		}

		var alertPending = function() {
			if (window.location.pathname.indexOf('/${classifier}s/new') == -1) {
				$('#enrolment').before($pendingAlert);
				$pendingAlert.delay(750).slideDown();
			}
		}

		<#-- make table rows clickable -->
		$enrolment.on('click', '.table-checkable tr', function(e) {
			if ($(e.target).is(':not(input:checkbox)')) {
				e.preventDefault();
				var $chk = $(this).find('input:checkbox');
				if ($chk.length) {
					$chk.prop('checked', !$chk.prop('checked'));
				}
			}

			var $table = $(this).closest('table');
			updateCheckboxes($table);
			enableActions($table);
		});

		<#-- sits-picker click handler -->
		$enrolment.on('click', '.sits-picker .btn', function(e) {
			e.preventDefault();
			var $m = $(this).closest('.modal');
			if ($(this).is(':not(.disabled)')) {
				$('.sits-picker .btn').addClass('disabled').prop('disabled', 'disabled');

				<#-- get current list of values and remove and/or add changes -->
				var current = $('.upstreamGroups').map(function(i, input) { return input.value }).toArray();
				var changes = $m.find('td input:checked').map(function(i, input) { return input.value }).toArray();
				// always remove even when adding, to dedupe
				var data = $(current).not(changes).toArray();
				if ($(this).is('.link-sits')) {
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
					data: $('#${classifier}EnrolmentFields').find('input, textarea, select').add('#academicYear').serialize(),
					error: function() {
						$m.modal('hide');
					},
					success: function(data, status) {
						$m.modal('hide');
						$enrolment.find('.${classifier}EnrolmentInner').html($(data).find('.${classifier}EnrolmentInner').contents());
						//TAB-3975 - to fix footer window positioning, trigger resize
						$(window).trigger('resize');
						$enrolment.find('.enrolledCount').html($(data).find('.enrolledCount').contents());
						$enrolment.find('.${classifier}Modals').html($(data).find('.${classifier}Modals').contents());
						$enrolment.data('open', true);
						initEnrolment();
						alertPending();
					}
				});
			}
		});

		<#-- adder click handler -->
		$enrolment.on('click', '.adder .btn', function(e) {
			e.preventDefault();
			var $m = $(this).closest('.modal');
			if ($(this).is(':not(.disabled)')) {
				$(this).addClass('disabled').prop('disabled', 'disabled');
				$.ajax({
					type: 'POST',
					url: '${enrolment_url}',
					data: $('#${classifier}EnrolmentFields').find('input, textarea, select').add('#academicYear').serialize(),
					error: function() {
						$m.modal('hide');
					},
					success: function(data, status) {
						$m.modal('hide');
						$enrolment.find('.${classifier}EnrolmentInner').html($(data).find('.${classifier}EnrolmentInner').contents());
						$enrolment.find('.enrolledCount').html($(data).find('.enrolledCount').contents());
						$enrolment.find('.${classifier}Modals').html($(data).find('.${classifier}Modals').contents());
						$enrolment.data('open', true);
						initEnrolment();
						alertPending();
					}
				});
			}
		});

		<#-- adder dis/enabled -->
		$enrolment.on('input propertychange keyup', '.adder textarea', function(e) {
			e.preventDefault();
			var empty = ($.trim($(this).val()) == "");
			$('.add-students').toggleClass('disabled', empty);
		});



		<#-- show modals -->
		$enrolment.on('click', '.show-sits-picker', function(e) {
			e.preventDefault();

			$('.sits-picker').modal('show');
		});
		$enrolment.on('click', '.show-adder', function(e) {
			e.preventDefault();

			$('.adder').on('shown', function() {
				$(this).find('textarea').focus();
			}).modal('show');
		});

		<#-- reset on modal close -->
		$enrolment.on('hidden', '.modal', function(e) {
			if (this == e.target) { // ignore 'hidden' events from within the modal
				$(this).find('input:checked').removeAttr('checked');
				$(this).find('.spinnable').spin(false);
			}
		});

		<#-- select users from enrolment table who are PWD -->
		$enrolment.on('click', '.select-withdrawn', function(e) {
			e.preventDefault();
			var $this = $(this);
			$.get($this.data('href'), function(data) {
				$.each(data.students, function(i, universityId) {
					$('#enrolment-table').find('input[name=modifyEnrolment][value='+ universityId + ']').trigger('click');
				});
				$this.button('reset');
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
				alertPending();
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
				alertPending();
			});
		});
	});
	</script>

</#macro>

</#escape>

