<#escape x as x?html>

<h1>Job status</h1>

<p>This page will update itself automatically. You'll be sent an email when it completes so you don't have to keep this page open.</p>

<p>When the job is finished you'll be able to see the results on the <a href="<@routes.coursework.assignmentsubmissionsandfeedback assignment />">submissions page</a>.</p>


<div id="job-progress">
	<div class="progress progress-striped <#if !status.finished>active</#if>">
		<div class="bar" style="width: ${status.progress}%;"></div>
	</div>

	<div id="job-status-value" data-progress="${status.progress}" data-succeeded="${status.succeeded?string}" data-finished="${status.finished?string}">
		<p>${status.status}</p>
	</div>
</div>

<script>
	(function($){
		function buildStatus(field, type, string) {
			return field + " " + type + ((field != 1)? "s" : "") + string;
		}
		var updateStatus = function() {
			$.get('<@routes.coursework.submitToTurnitinStatus assignment />', function(data){
				var $progress = $('#job-progress').find('.bar').width(data.progress + '%').end();
				if (data.finished) {
					$progress.removeClass('active');
					if (!data.succeeded) {
						$progress.addClass('progress-warning');
					} else {
						$progress.addClass('progress-success');
					}
					$('#job-status-value').find('p').empty().html(data.status);
				} else {
					var statuses = [];
					if (data.reportReceived) {
						statuses.push(buildStatus(data.reportReceived, "report", " received"))
					}
					if (data.reportRequested) {
						statuses.push(buildStatus(data.reportRequested, "report", " requested"))
					}
					if (data.fileSubmitted) {
						statuses.push(buildStatus(data.fileSubmitted, "file", " submitted"))
					}
					if (data.awaitingSubmission) {
						statuses.push(buildStatus(data.awaitingSubmission, "file", " awaiting submission"))
					}
					$('#job-status-value').find('p').empty().html(data.status + statuses.join(", "));
					setTimeout(updateStatus, 5000);
				}
			});
		};
		setTimeout(updateStatus, 5000);
	})(jQuery);
</script>

</#escape>