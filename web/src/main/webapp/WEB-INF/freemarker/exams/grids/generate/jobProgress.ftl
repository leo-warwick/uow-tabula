<#import 'form_fields.ftl' as form_fields />
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check" method="post">

	<@form_fields.select_course_fields />
	<@form_fields.grid_options_fields />

	<h2>Importing student data</h2>

	<p class="progress-arrows">
		<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
		<span class="arrow-right arrow-left"><button type="submit" class="btn btn-link" name="${GenerateExamGridMappingParameters.selectCourse}">Set grid options</button></span>
		<span class="arrow-right arrow-left active">Preview and download</span>
	</p>

	<div class="alert alert-info">
		<div class="progress">
			<div class="progress-bar progress-bar-striped active" style="width: ${jobProgress!0}%;"></div>
		</div>
		<p class="job-status">${jobStatus!"Waiting for job to start"}</p>
	</div>

	<p>
		Tabula is currently importing fresh data for the students you selected from SITS.
		You can <a href="#" data-toggle="modal" data-target="#student-import-dates">view the last import date for each student</a>.
		If you wish you can skip this import and proceed to generate the grid.
	</p>

	<div class="modal fade" id="student-import-dates">
		<@modal.wrapper>
			<@modal.body>
				<table class="table table-condensed table-striped table-hover">
					<thead>
						<tr>
							<th>Name</th>
							<th>Last imported date</th>
						</tr>
					</thead>
					<tbody>
						<#list studentLastImportDates as studentDate>
							<tr>
								<td>${studentDate._1()}</td>
								<td><@fmt.date studentDate._2() /></td>
							</tr>
						</#list>
					</tbody>
				</table>
			</@modal.body>
		</@modal.wrapper>
	</div>

	<button class="btn btn-primary" type="submit" name="${GenerateExamGridMappingParameters.previewAndDownload}">Skip import and generate grid</button>
</form>

<script>
	jQuery(function($){
		var updateProgress = function(){
			$.post('<@routes.exams.generateGridProgress department academicYear />', {'jobId': '${jobId}'}, function(data){
				if (data.finished) {
					$('button[name="${GenerateExamGridMappingParameters.previewAndDownload}"]').trigger('click');
				} else {
					if (data.progress) {
						$('.progress .progress-bar').css('width', data.progress + '%');
					}
					if (data.status) {
						$('.job-status').html(data.status);
					}
					setTimeout(updateProgress, 5 * 1000);
				}
			});
		};
		updateProgress();
	});
</script>

</#escape>