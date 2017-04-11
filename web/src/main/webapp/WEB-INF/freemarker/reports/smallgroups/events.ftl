<#escape x as x?html>
<#import "../reports_macros.ftl" as reports_macros />

<h1>All events</h1>

<#assign reportUrl><@routes.reports.events department academicYear /></#assign>
<@reports_macros.reportLoader reportUrl=reportUrl hasDatePicker=false>
	<ul class="dropdown-menu">
		<li><a href="#" data-href="<@routes.reports.eventsDownloadCsv department academicYear />">CSV</a></li>
		<li><a href="#" data-href="<@routes.reports.eventsDownloadXlsx department academicYear />">Excel</a></li>
		<li><a href="#" data-href="<@routes.reports.eventsDownloadXml department academicYear />">XML</a></li>
	</ul>
</@reports_macros.reportLoader>


<script>
	jQuery(function($){
		if (window.ReportBuilder == undefined)
			return false;

		window.ReportBuilder.rowKey = 'events';

		window.ReportBuilder.buildHeader = function(){
			var container = $('<tr/>');
			container.append(
				$('<th/>').addClass('sortable').html('Department name')
			).append(
				$('<th/>').addClass('sortable').html('Event name')
			).append(
				$('<th/>').addClass('sortable').html('Module title')
			).append(
				$('<th/>').addClass('sortable').html('Day')
			).append(
				$('<th/>').addClass('sortable').html('Start')
			).append(
				$('<th/>').addClass('sortable').html('Finish')
			).append(
				$('<th/>').addClass('sortable').html('Location')
			).append(
				$('<th/>').addClass('sortable').html('Size')
			).append(
				$('<th/>').addClass('sortable').html('Weeks')
			).append(
				$('<th/>').addClass('sortable').html('Staff')
			);
			return container;
		};

		window.ReportBuilder.buildRow = function(data) {
			var container = $('<tr/>');
			var dayAsNumber = 0;
			switch(data.day) {
				case 'Monday': dayAsNumber = 1; break;
				case 'Tuesday': dayAsNumber = 2; break;
				case 'Wednesday': dayAsNumber = 3; break;
				case 'Thursday': dayAsNumber = 4; break;
				case 'Friday': dayAsNumber = 5; break;
				case 'Saturday': dayAsNumber = 6; break;
				case 'Sunday': dayAsNumber = 7; break;
			}
			container.append(
				$('<td/>').html(data.departmentName)
			).append(
				$('<td/>').html(data.eventName)
			).append(
				$('<td/>').html(data.moduleTitle)
			).append(
				$('<td/>').data('sortby', dayAsNumber).html(data.day)
			).append(
				$('<td/>').html(data.start)
			).append(
				$('<td/>').html(data.finish)
			).append(
				$('<td/>').html(data.location)
			).append(
				$('<td/>').html(data.size)
			).append(
				$('<td/>').html(data.weeks)
			).append(
				$('<td/>').html(data.staff)
			);
			return container;
		};

	});
</script>

</#escape>