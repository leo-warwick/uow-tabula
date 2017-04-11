<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<h1>${agent.fullName}'s ${relationshipType.studentRole}s</h1>

<div id="profile-modal" class="modal fade profile-subset"></div>

<div class="studentResults">
	<#assign returnTo><@routes.attendance.viewAgent department academicYear relationshipType agent /></#assign>
	<#assign returnTo = returnTo?url />
	<#if (result.totalResults > 0)>

		<@attendance_macros.scrollablePointsTable
			command=command
			filterResult=result
			visiblePeriods=visiblePeriods
			monthNames=monthNames
			department=department
			doCommandSorting=false
		; result>
			<td class="unrecorded">
				<a href="<@routes.attendance.viewSingleStudent department academicYear result.student />" title="<@attendance_macros.checkpointTotalTitle result.checkpointTotal />" class="use-tooltip">
					<span class="<#if (result.checkpointTotal.unrecorded > 2)>badge progress-bar-danger<#elseif (result.checkpointTotal.unrecorded > 0)>badge progress-bar-warning</#if>">
						${result.checkpointTotal.unrecorded}
					</span>
				</a>
			</td>
			<td class="missed">
				<a href="<@routes.attendance.viewSingleStudent department academicYear result.student />" title="<@attendance_macros.checkpointTotalTitle result.checkpointTotal />" class="use-tooltip">
					<span class="<#if (result.checkpointTotal.unauthorised > 2)>badge progress-bar-danger<#elseif (result.checkpointTotal.unauthorised > 0)>badge progress-bar-warning</#if>">
						${result.checkpointTotal.unauthorised}
					</span>
				</a>
			</td>
			<td class="record">
				<#assign record_url><@routes.attendance.viewRecordStudent department academicYear result.student returnTo /></#assign>
				<@fmt.permission_button
					permission='MonitoringPoints.Record'
					scope=result.student
					action_descr='record monitoring points'
					classes='btn btn-primary btn-xs'
					href=record_url
				>
					Record
				</@fmt.permission_button>
			</td>
		</@attendance_macros.scrollablePointsTable>

		<div class="clearfix">
			<div class="pull-left">
				<@fmt.bulk_email_students students=result.students />
			</div>
		</div>

	<#else>
		<p>No students were found.</p>
	</#if>
</div>

<script>
	jQuery(window).on('load', function(){
		GlobalScripts.scrollableTableSetup();
	});
	jQuery(function($){
		var $leftTable = $('.scrollable-points-table .left table');
		GlobalScripts.tableSortMatching([
			$leftTable,
			$('.scrollable-points-table .right table')
		]);
		$leftTable.tablesorter({
			sortList: [[2,0], [1,0]],
			headers: {0:{sorter:false},6:{sorter:false}},
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});
	});
</script>

</#escape>