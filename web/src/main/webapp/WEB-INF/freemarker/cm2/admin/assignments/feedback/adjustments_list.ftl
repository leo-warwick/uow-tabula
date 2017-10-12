<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/coursework_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/marking_macros.ftl" as marking />


<#macro row info>
	<#assign u = info.student />
	<tr data-toggle="collapse" data-target="#row-${marking.extractId(u)}" class="clickable collapsed expandable-row">
		<td class="student-col"><h6 class="toggle-icon">&nbsp;${u.firstName}</h6></td>
		<td class="student-col">
			<h6>${u.lastName}&nbsp;<#if u.warwickId??><@pl.profile_link u.warwickId /><#else><@pl.profile_link u.userId /></#if></h6>
		</td>
		<td class="content-cell">
			<#if u.warwickId??>${u.warwickId}<#else>${u.userId!}</#if>
		</td>
	</tr>
	<tr id="row-${marking.extractId(u)}" data-detailurl="<@routes.cm2.feedbackAdjustmentForm assignment marking.extractId(u) />" class="collapse detail-row">
		<td colspan="3" class="detailrow-container"><p>No data is currently available. Please check that you are signed in.</p></td>
	</tr>

</#macro>

<#escape x as x?html>
	<@cm2.assignmentHeader "Feedback adjustment" assignment "for" />

	<#if studentInfo?size gt 0>
		<div class="pull-right">
			<a href="<@routes.cm2.feedbackBulkAdjustment assignment />" class="btn btn-default">Adjust in bulk</a>
		</div>
	</#if>

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<#if studentInfo?size gt 0>
		<table id="feedback-adjustment" class="students table table-striped tabula-greenLight sticky-table-headers">
			<thead>
				<tr>
					<th class="student-col">First name</th>
					<th class="student-col">Last name</th>
					<th class="student-col">University ID</th>
				</tr>
			</thead>
			<tbody>
				<#list studentInfo as info>
					<@row info />
				</#list>
			</tbody>
		</table>

		<script type="text/javascript">
			(function($) {
				var tsOptions = {
					sortList: [[2, 0], [1,0]],
					headers: { 0: { sorter: false} }
				};
				$("#action").val("");
				$("button.use-suggested-mark").click(function () {
						$("#action").val("suggestmark");
				});

				var typeForm = "feedback-adjustment";

				var beforeSubmit = function($form){
					var $select = $form.find('select[name=reason]');
					if ($select.val() === "Other") {
						$select.prop("disabled", true);
					}
					return true;
				};

				var callback = function($row){
					var $form = $row.find('form');
					var $select = $form.find('select[name=reason]');
					var $otherInput = $form.find('.other-input');
					if($otherInput.val() !== "") {
						$select.val("Other");
						$otherInput.removeProp(("disabled");
						$otherInput.removeClass("hide");
					}
					$row.trigger('tabula.formLoaded');
				};

				$('body').on('tabula.formLoaded',function() {
					var $row = $(this).find('.detail-row');
					$("#action").val("");
					$row.tabulaAjaxForm({
						beforeSubmit: beforeSubmit,
						errorCallback: callback,
						type: typeForm
					});
					$row.find('form').removeData('submitOnceSubmitted');
					$row.find("button.use-suggested-mark").click(function () {
						$("#action").val("suggestmark");
					});
				});
			})(jQuery);
		</script>

	<#else>
		<p>There are no items of feedback that can be adjusted.</p>
	</#if>

	<#if noFeedbackStudentInfo?size gt 0>
		<p><@fmt.p noFeedbackStudentInfo?size "student does" "students do" /> not have feedback you can adjust. You can only adjust feedback once marking is completed.</p>

		<table class="students table table-striped">
			<thead>
			<tr>
				<th class="student-col">First name</th>
				<th class="student-col">Last name</th>
				<th class="student-col">University ID</th>
			</tr>
			</thead>
			<tbody>
				<#list noFeedbackStudentInfo as info>
					<tr>
						<td class="student-col">${info.student.firstName}</td>
						<td class="student-col">
							${info.student.lastName}&nbsp;<#if info.student.warwickId??><@pl.profile_link info.student.warwickId /><#else><@pl.profile_link info.student.userId /></#if>
						</td>
						<td>
							<#if info.student.warwickId??>${info.student.warwickId}<#else>${info.student.userId!}</#if>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</#if>
</#escape>
