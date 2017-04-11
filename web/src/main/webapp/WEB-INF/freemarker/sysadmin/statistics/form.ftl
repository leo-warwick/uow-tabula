<h1>Internal statistics</h1>

<div class="alert alert-danger">
	<h3>Slow operations</h3>
	<p>
	Some of these operations (such as Hibernate statistics collection) are bad for performance and shouldn't
	be enabled in production, or at least not for any length of time.
	</p>
</div>

<div class="well">
<h2>Hibernate SessionFactory statistics</h2>

<p>
	These operations are sent asynchronously to each WAR via the message queue,
	so you may not see any effect except in the server log.
</p>

<button id="enable-hib-stats" class="btn btn default">Enable</button>

<button id="disable-hib-stats" class="btn btn default">Disable</button>

<button id="log-hib-stats" class="btn btn default">Output to log</button>

<button id="clear-hib-stats" class="btn btn default">Clear stats</button>

<div class="alert alert-danger" id="hib-response"></div>

</div>

<div class="well">
	<h2>memcached statistics</h2>

	<p>Uses the default memcached client, so if you have any special config you're out-of-luck.</p>

	<table class="table table-condensed table-striped">
		<tbody>
			<#list memcachedStatistics as stat>
				<tr>
					<th>${stat._1()}</th>
					<td>${stat._2()}</td>
				</tr>
			</#list>
		</tbody>
	</table>

	<button id="clear-memcached" class="btn btn-default">Flush the cache (expires all items)</button>

	<div class="alert alert-danger" id="memcached-response"></div>
</div>

<script>
	jQuery(function($){

		var debugResponse = function(data) {
			$('#hib-response').html(data);
		};

		$('#enable-hib-stats').click(function(e){
			e.preventDefault();
			$.post('/sysadmin/statistics/hibernate/toggleAsync.json', { enabled: true }, debugResponse);
		});

		$('#disable-hib-stats').click(function(e){
			e.preventDefault();
			$.post('/sysadmin/statistics/hibernate/toggleAsync.json', { enabled: false }, debugResponse);
		});

		$('#log-hib-stats').click(function(e){
			e.preventDefault();
			$.post('/sysadmin/statistics/hibernate/log', debugResponse);
		});

		$('#clear-hib-stats').click(function(e){
			e.preventDefault();
			$.post('/sysadmin/statistics/hibernate/clearAsync.json', debugResponse);
		});

		var debugResponseMemcached = function(data) {
			$('#memcached-response').html(data);
		};

		$('#clear-memcached').click(function(e){
			e.preventDefault();
			$.post('/sysadmin/statistics/memcached/clearAsync.json', debugResponseMemcached);
		});

	})
</script>