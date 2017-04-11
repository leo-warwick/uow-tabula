<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		@page {
			margin: 1cm;
		}
		body { font-family: "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: 12px; }
		h1 { font-size: 20px; color: #7030a0; font-weight: bold; }
		h2 { font-size: 18px; color: #7030a0; font-weight: bold; }
		h3 { font-size: 14px; color: #000000; font-weight: bold; }
		p { margin-top: 0; }
		table {
			border-collapse: collapse;
			width: 100%;
			table-layout: fixed;
		}
		table th {
			text-align: center;
			border: 1px solid #ddd;
			padding: 2px;
		}
		table td {
			vertical-align: top;
			border: 1px solid #ddd;
			padding: 2px;
			font-size: 10px;
		}
		th.hour {
			width: 30px;
		}
		td.hour {
			text-align: right;
		}
		td.today {
			background-color: #FCF8E3;
		}
		div.date {
			text-align: right;
		}
		div.date.other {
			color: #ddd;
		}
		div.event {
			margin-bottom: 2px;
			padding: 2px;
		}
	</style>
</head>
<body>
	<#macro renderEvent event>
		<div class="event" style="background-color: ${event.backgroundColor};">
			<strong>${event.formattedStartTime} - ${event.formattedEndTime}</strong><br />
			${event.title}
		</div>
	</#macro>

	<#macro agendaView>
		<#if calendarView == 'agendaWeek'>
			<h1><#noescape>${title}</#noescape></h1>
		<#else>
			<h1>${renderDate.toDate()?string('EEEE, MMMM dd, yyyy')}</h1>
		</#if>

		<table class="agenda">
			<thead>
				<tr>
					<th class="hour"></th>
					<#list allDays as day>
						<th>${day.toDate()?string('EEE d/M')}</th>
					</#list>
				</tr>
			</thead>
			<tbody>
				<#list 0..23 as hour>
					<tr>
						<td class="hour"><#if hour == 0>12am<#elseif hour < 12>${hour}am<#elseif hour == 12>12pm<#else>${hour - 12}pm</#if></td>
						<#list allDays as day>
							<td <#if day.toDate()?date?string == .now?date?string>class="today"</#if>>
								<#if mapGet(groupedEvents, day)?? && mapGet(mapGet(groupedEvents, day), hour)??>
									<#list mapGet(mapGet(groupedEvents, day), hour) as event>
										<@renderEvent event />
									</#list>
								</#if>
							</td>
						</#list>
					</tr>
				</#list>
			</tbody>
		</table>
	</#macro>

	<#macro monthView>
		<h1>${renderDate.toDate()?string('MMMM yyyy')}</h1>
		<table>
			<thead>
			<tr>
				<th>Mon</th>
				<th>Tue</th>
				<th>Wed</th>
				<th>Thu</th>
				<th>Fri</th>
				<th>Sat</th>
				<th>Sun</th>
			</tr>
			</thead>
			<tbody>
				<#list groupedAllDays?keys?sort as week>
				<tr>
					<#local thisWeek = mapGet(groupedAllDays, week) />
					<#if !mapGet(groupedEvents, week)??>
						<#list thisWeek as day>
							<td <#if day.toDate()?date?string == .now?date?string>class="today"</#if>>
								<div class="date <#if day.isBefore(firstDayOfMonth) || day.isAfter(lastDayOfMonth)>other</#if>">${day.getDayOfMonth()}</div>
							</td>
						</#list>
					<#else>
						<#local thisWeeksEvents = mapGet(groupedEvents, week) />
						<#list thisWeek as day>
							<td <#if day.toDate()?date?string == .now?date?string>class="today"</#if>>
								<div class="date <#if day.isBefore(firstDayOfMonth) || day.isAfter(lastDayOfMonth)>other</#if>">${day.getDayOfMonth()}</div>
								<#if mapGet(thisWeeksEvents, day)??>
									<#list mapGet(thisWeeksEvents, day) as event>
										<@renderEvent event />
									</#list>
								</#if>
							</td>
						</#list>
					</#if>
				</tr>
				</#list>
			</tbody>
		</table>
	</#macro>

	<#if calendarView == 'month'>
		<@monthView />
	<#else>
		<@agendaView />
	</#if>
</body>
</html>
</#escape>