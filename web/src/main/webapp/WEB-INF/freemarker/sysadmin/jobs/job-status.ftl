<h1>Job status</h1>

<p>Job ID ${jobId}</p>

<div id="job-status">
	<div class="progress progress-striped active">
		<div class="bar" style="width: 0;"></div>
	</div>
	<p class="status"></p>
</div>

<script>
(function($){
	var updateFragment = function() {
		$.get('${url('/sysadmin/jobs/job-status')}', {id: '${jobId}'}, function(data){
			if (data.succeeded) {
				$('.progress .bar').width("100%");
				$('.progress').removeClass('active progress-striped');
				if (data.status) {
					$('#job-status').find('.status').html(data.status);
				}
			} else {
				$('.progress .bar').width(data.progress + "%");
				if (data.status) {
					$('#job-status').find('.status').html(data.status);
				}
				setTimeout(updateFragment, 2000);
			}
		});
	};
	setTimeout(updateFragment, 2000);

})(jQuery);
</script>