<#import 'form_fields.ftl' as form_fields />
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<h2>Generating document</h2>

<div class="alert alert-info">
	<div class="progress">
		<div class="progress-bar progress-bar-striped active" style="width: ${jobProgress!0}%;"></div>
	</div>
	<p class="job-status">${jobStatus!"Waiting for job to start"}</p>
</div>

<div class="hidden" id="downloadLink">
	<p>
		<a href="<@routes.exams.downloadGridDocument department academicYear job />" class="btn btn-default">
			<i class="fa fa-fw fa-arrow-circle-o-down"></i>
			Download
		</a>
	</p>
</div>

<script>
	jQuery(function($){
		var updateProgress = function(){
			$.post({
				method: 'post',
				url: window.location.pathname,
				error: function(jqXHR, textstatus, message) {
					if(textstatus === "timeout"){ updateProgress(); }
					else {
						// not a timeout - some other JS error - advise the user to reload the page
						var $progressContainer = $('.alert').removeClass('alert-info').addClass('alert-warning');
						$progressContainer.find('.progress-bar').addClass("progress-bar-danger");
						var messageEnd = jqXHR.status === 403 ? ", it appears that you have signed out. Please refresh this page." : ". Please refresh this page.";
						$progressContainer.find('.job-status').html("Unable to check the progress of your document"+messageEnd);
					}
				},
				success: function(data){
					if (data.status) {
						$('.job-status').html(data.status);
					}

					if (data.progress) {
						$('.progress .progress-bar').css('width', data.progress + '%');
					}

					if (data.finished) {
						if (data.succeeded) {
							$('.progress .progress-bar').addClass('progress-bar-success').removeClass('active');
							$('#downloadLink').removeClass('hidden');

							window.location = '<@routes.exams.downloadGridDocument department academicYear job />';
						} else {
							$('.progress .progress-bar').addClass('progress-bar-danger').removeClass('active');
							$('.job-status').text(data.status);
						}
					} else {
						setTimeout(updateProgress, 2 * 1000);
					}
				},
				timeout: 2000
			});
		};
		updateProgress();
	});
</script>

</#escape>