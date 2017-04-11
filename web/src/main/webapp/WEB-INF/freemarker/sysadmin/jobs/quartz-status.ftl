<h1>Quartz status</h1>

<p>Trigger key ${key}</p>

<div id="job-status">
	<div class="progress">
		<div class="progress-bar progress-bar-striped" style="width: 0;"></div>
	</div>
	<p class="status"></p>
</div>

<script>
(function($){
	var updateFragment = function() {
		$.get('${url('/sysadmin/jobs/quartz-status')}', {key: '${key}'}, function(data) {
			var message;
			var state = 'info';
			var percentage = 10;

			switch (data.status) {
				case 'NONE':
					// The trigger doesn't exist or has completed
					percentage = 100;
					message = 'No longer exists';
					state = 'success';

					break;
				case 'NORMAL':
					// The trigger is scheduled for execution or is currently executing
					percentage = 33;
					message = 'In progress';

					break;
				case 'PAUSED':
					// The trigger has been paused
					percentage = 33;
					message = 'Paused';
					state = 'warning';

					break;
				case 'ACQUIRED':
				case 'COMPLETE':
					// The trigger has fired and the job is ready to run
					percentage = 50;
					message = 'Trigger fired, job ready to start';

					break;
				case 'EXECUTING':
					// The job is executing
					percentage = 66;
					message = 'Job executing';
					state = 'info active';

					break;
				case 'ERROR':
					// The trigger has errored
					percentage = 50;
					message = 'Error';
					state = 'danger';

					break;
				case 'BLOCKED':
					// The trigger's execution is blocked
					message = 'Blocked';
					state = 'warning';

					break;
			}

			$('.progress-bar')
				.width(percentage + '%')
				.attr('class', 'progress-bar progress-bar-striped progress-bar-' + state);
			$('#job-status .status').html(message);
		});
	};

	updateFragment();
	setInterval(updateFragment, 2000);
})(jQuery);
</script>