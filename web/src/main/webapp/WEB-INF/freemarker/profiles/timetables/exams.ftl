<#escape x as x?html>

<div class="pull-right">
	<a class="btn btn-default" href="https://exams.warwick.ac.uk/timetable/${member.universityId}.pdf">Download PDF</a>
</div>

<h1 class="with-settings">Student examination timetable</h1>

<#if timetable?has_content>

	<div class="exam-timetable">

		<section class="header">
			<#noescape>${timetable.header}</#noescape>
		</section>

		<#assign showExtraTime = false />
		<#list timetable.exams as exam>
			<#if exam.extraTimePerHour?has_content>
				<#assign showExtraTime = true />
			</#if>
		</#list>

		<section class="table">
			<table class="table table-condensed table-striped">
				<thead>
					<tr>
						<th>Module</th>
						<th>Examination paper code and title </th>
						<th>Sct</th>
						<th>Length</th>
						<th>RdTime</th>
						<th>OpBook</th>
						<th>Date</th>
						<th>Time</th>
						<#if showExtraTime><th>Extra time per hr</th></#if>
						<th>Room</th>
					</tr>
				</thead>
				<tbody>
					<#list timetable.exams as exam>
						<tr>
							<td>${exam.moduleCode}</td>
							<td>${exam.paper}</td>
							<td>${exam.section}</td>
							<td>${exam.lengthString}</td>
							<td><#if exam.readingTime?has_content>R<#else>n/a</#if></td>
							<td><#if exam.openBook>OB<#else>n/a</#if></td>
							<td><@fmt.date date=exam.startDateTime relative=false includeTime=false /></td>
							<td><@fmt.time exam.startDateTime.toLocalDateTime() /></td>
							<#if showExtraTime><td>${exam.extraTimePerHour!}</td></#if>
							<td>${exam.room}</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</section>

		<section class="instructions">
			<#noescape>${timetable.instructions}</#noescape>
		</section>

	</div>

<#else>

	<div class="alert alert-danger">
		Unfortunately we could not show your exam timetable at this time<#if error?has_content>: ${error}</#if>
	</div>

</#if>

</#escape>