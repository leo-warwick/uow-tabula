<#escape x as x?html>
<input type="hidden" name="action" value="submit" id="action-submit">

<div class="pull-right">
	<#if syllabusPlusEventCount gt 0>
		<#if is_edit>
			<#assign import_external_url><@routes.groups.editimportfromexternal smallGroupSet /></#assign>
		<#else>
			<#assign import_external_url><@routes.groups.createimportfromexternal smallGroupSet /></#assign>
		</#if>

		<a class="btn btn-default" href="${import_external_url}">
			Create events from Syllabus+
		</a>

		<#assign helpText>
			<p>Create small group events using data from the central timetabling system Syllabus+.</p>
		</#assign>
		<a href="#"
		   class="use-introductory<#if showIntro("sgt-import-events-splus", "anywhere")> auto</#if>"
		   data-title="Create events from Syllabus+"
		   data-trigger="click"
		   data-placement="bottom"
		   data-html="true"
		   data-hash="${introHash("sgt-import-events-splus", "anywhere")}"
		   data-content="${helpText}"><i class="fa fa-fw fa-question-circle"></i></a>
	<#else>
		<div class="use-tooltip" title="There are no scheduled small groups defined for <@fmt.module_name module false /> in Syllabus+, the central timetabling system">
			<a class="btn btn-default disabled">
				Import events from Syllabus+
			</a>
		</div>
	</#if>
</div>

<@bs3form.form_group>
	<p>There are <@fmt.p smallGroupSet.groups?size "group" /> in ${smallGroupSet.name}<#if smallGroupSet.linked> (from ${smallGroupSet.linkedDepartmentSmallGroupSet.name})</#if>.</p>

	<p>
		<#if is_edit>
			<#assign default_properties_url><@routes.groups.editseteventdefaults smallGroupSet /></#assign>
		<#else>
			<#assign default_properties_url><@routes.groups.createseteventdefaults smallGroupSet /></#assign>
		</#if>

		<a class="btn btn-default" href="${default_properties_url}">Default properties</a>

		<#assign helpText>
			<p>You can set default properties for events created for all groups, such as tutor, location or which weeks
			   the events are running, and then edit these for individual groups if necessary.</p>
		</#assign>
		<a href="#"
		   class="use-introductory<#if showIntro("sgt-default-event-properties", "anywhere")> auto</#if>"
		   data-title="Default properties for events"
		   data-trigger="click"
		   data-placement="bottom"
		   data-html="true"
		   data-hash="${introHash("sgt-default-event-properties", "anywhere")}"
		   data-content="${helpText}"
		><i class="fa fa-fw fa-question-circle"></i></a>
	</p>
</@bs3form.form_group>

<div class="striped-section no-title">
	<#if groups?size gt 0>
		<#include "_events.ftl" />
	</#if>
</div>

<script type="text/javascript">
	jQuery(function($) {
		<#-- controller detects action=refresh and does a bind without submit -->
		$('.modal.refresh-form').on('hide', function(e) {
			// Ignore events that are something ELSE hiding and being propagated up!
			if (!$(e.target).hasClass('modal')) return;

			$('#action-submit').val('refresh');
			$('#action-submit').closest('form').submit();
		});

		// Open the first modal with an error in it
		$('.modal .error').first().closest('.modal').modal('show');

		// repeat these hooks for modals when shown
		$('body').on('shown.bs.modal', function() {
			var $m = $(this);
			$m.find('input.lazy-time-picker').tabulaTimePicker();
		});
	});
</script>
</#escape>