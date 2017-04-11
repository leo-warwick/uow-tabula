<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		@page {
			margin: 0.5cm;
			size: A4 landscape;
		}
		body { font-family: "Helvetica Neue", Helvetica, Arial, sans-serif; font-size: 12px; }
		h1 { font-size: 20px; color: #7030a0; font-weight: bold; }
		h2 { font-size: 18px; color: #7030a0; font-weight: bold; }
		h3 { font-size: 14px; color: #000000; font-weight: bold; }
		p { margin-top: 0; }
		table {
			border-collapse: collapse;
			width: 100%;
		}
		table th {
			text-align: center;
			border: 1px solid #666;
			padding: 2px;
		}
		table td {
			vertical-align: top;
			border: 1px solid #ddd;
			padding: 2px;
			font-size: 10px;
		}
		table td.event {
			background-color: #F7F7F7;
		}
		table td.dayRow {
			border-top: 1px solid #666;
		}
		table th.day {
			border: 1px solid #666;
		}
		table tr.last td {
			border-bottom: 1px solid #666;
		}
	</style>
</head>
<body>
	<h1>Timetable for ${title}</h1>

	<table>
		<thead>
			<tr>
				<th></th>
				<#list hours as hour>
					<th>${hour?time?string("HH:mm")}</th>
				</#list>
			</tr>
		</thead>
		<tbody>
			<#assign lastEventUid = "" />
			<#list days as day>
				<#if mapGet(eventGrid, day)??>
					<#assign dayRows = mapGet(eventGrid, day) />
					<#list dayRows as row>
						<tr <#if !day_has_next && !row_has_next>class="last"</#if>>
							<#if row_index == 0>
								<th rowspan="${dayRows?size}" class="day">${day.name[0..*3]}</th>
							</#if>
							<#list hours as hour>
								<#if mapGet(row, hour)??>
									<#assign event = mapGet(row, hour) />
									<#if event.uid != lastEventUid>
										<#assign eventLength = 0 />
										<#list hours as hour>
											<#if mapGet(row, hour)?? && mapGet(row, hour).uid == event.uid>
												<#assign eventLength = eventLength + 1 />
											</#if>
										</#list>
										<td colspan="${eventLength}" class="event <#if row_index == 0>dayRow</#if>">
											${event.parent.shortName!} ${event.eventType.displayName}<br />
											${event.parent.fullName!}<br />
											${(event.location.name)!}<br />
											<#list event.staff as staff>${staff.fullName}<#if staff_has_next>, </#if></#list><br />
											<#list event.weekRanges as weekRange>${weekRange.toString}<#if weekRange_has_next>, </#if></#list><br />
											${event.startTime?time?string("HH:mm")} - ${event.endTime?time?string("HH:mm")}<br />
										</td>
										<#assign lastEventUid = event.uid />
									</#if>
								<#else>
									<td <#if row_index == 0>class="dayRow"</#if>></td>
								</#if>
							</#list>
						</tr>
					</#list>
				<#else>
					<tr <#if !day_has_next>class="last"</#if>>
						<th class="day">${day.name[0..*3]}</th>
						<#list hours as hour>
							<td class="dayRow"></td>
						</#list>
					</tr>
				</#if>
			</#list>
		</tbody>
	</table>
</body>
</html>
</#escape>