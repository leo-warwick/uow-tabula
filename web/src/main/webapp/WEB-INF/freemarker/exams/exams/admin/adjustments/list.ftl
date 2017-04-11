<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#function>

<#macro row info>
	<#assign u = info.student />
	<tr class="item-container" data-contentid="${markingId(u)}" data-markingurl="<@routes.exams.feedbackAdjustment exam />">
		<td class="student-col toggle-cell"><h6 class="toggle-icon">${u.firstName}</h6></td>
		<td class="student-col toggle-cell">
			<h6>${u.lastName}&nbsp;<#if u.warwickId??><@pl.profile_link u.warwickId /><#else><@pl.profile_link u.userId /></#if></h6>
		</td>
		<td class="toggle-cell content-cell">
			<dl style="margin: 0; border-bottom: 0;">
				<dt>
					<#if u.warwickId??>${u.warwickId}<#else>${u.userId!}</#if>
				</dt>
				<dd style="display: none;" class="table-content-container" data-contentid="${markingId(u)}">
					<div id="content-${markingId(u)}" class="content-container" data-contentid="${markingId(u)}">
						<p>No data is currently available. Please check that you are signed in.</p>
					</div>
				</dd>
			</dl>
		</td>
	</tr>
</#macro>

<#escape x as x?html>
	<#if studentInfo?size gt 0>
		<div class="pull-right">
			<a href="<@routes.exams.bulkAdjustment exam />" class="btn btn-default">Adjust in bulk</a>
		</div>
	</#if>

	<div class="deptheader">
		<h1 class="with-settings">Mark adjustment</h1>
		<h5 class="with-related">for ${exam.name} (${exam.module.code?upper_case})</h5>
	</div>

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<#if studentInfo?size gt 0>
		<table id="feedback-adjustment" class="students table table-striped tabula-greenLight sticky-table-headers expanding-table">
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

				$('.expanding-table').expandingTable({
					contentUrlFunction: function($row){ return $row.data('markingurl'); },
					useIframe: true,
					tableSorterOptions: tsOptions
				});
			})(jQuery);
		</script>

	<#else>
		<p>There are no marks that can be adjusted.</p>
	</#if>

	<#if noFeedbackStudentInfo?size gt 0>
		<p><@fmt.p noFeedbackStudentInfo?size "student does" "students do" /> not have marks you can adjust. You can only adjust marks once marking is completed.</p>

		<table class="students table table-striped tabula-greenLight">
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
						<td class="student-col"><h6>${info.student.firstName}</h6></td>
						<td class="student-col">
							<h6>${info.student.lastName}&nbsp;<#if info.student.warwickId??><@pl.profile_link info.student.warwickId /><#else><@pl.profile_link info.student.userId /></#if></h6>
						</td>
						<td>
							<#if info.student.warwickId??>${info.student.warwickId}<#else>${info.student.userId!}</#if>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>
	</#if>

	<p><a class="btn btn-default" href="<@routes.exams.viewExam exam />">Return to previous page</a></p>
</#escape>
