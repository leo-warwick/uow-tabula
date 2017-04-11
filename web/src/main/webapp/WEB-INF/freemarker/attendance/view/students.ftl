<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#assign filterQuery = filterCommand.serializeFilter />

<#if features.attendanceMonitoringReport && can.do("MonitoringPoints.Report", department) >
	<div class="pull-right send-to-sits">
		<a href="<@routes.attendance.viewReport department academicYear filterQuery />" class="btn btn-primary">Upload to SITS:eVision</a>
	</div>
</#if>

<#function route_function dept>
	<#local result><@routes.attendance.viewStudents dept academicYear /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader title="View students" route_function=route_function preposition="in" />

<#if reports?? && monitoringPeriod??>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Missed points for <@fmt.p reports "student" /> in the ${monitoringPeriod} monitoring period have been uploaded to SITS:eVision.
	</div>
</#if>

<#assign submitUrl><@routes.attendance.viewStudents department academicYear /></#assign>
<#assign filterCommand = filterCommand />
<#assign filterCommandName = "filterCommand" />
<#assign filterResultsPath = "/WEB-INF/freemarker/attendance/view/_students_results.ftl" />

<#include "/WEB-INF/freemarker/filter_bar.ftl" />

<script type="text/javascript">
	jQuery(window).on('load', function(){
		GlobalScripts.scrollableTableSetup();
	});
	jQuery(function($) {
		$('#command').find('input').on('change', function(e) {
			$('.send-to-sits a').addClass('disabled');
		});

		$(document).on("tabula.filterResultsChanged", function() {
			var sitsUrl = $('div.studentResults').data('sits-url');
			$('.send-to-sits a').attr('href', sitsUrl).removeClass('disabled');
		});
	});
</script>
</#escape>