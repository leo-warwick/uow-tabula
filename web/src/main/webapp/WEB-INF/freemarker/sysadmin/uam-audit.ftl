<h1>User Access Manager audit</h1>

<@f.form method="post" action="${url('/sysadmin/uam-audit')}" class="double-submit-protection">
	<h2>Notifications</h2>

	<#if success!false>
		<div class="alert alert-success">
			A notification has been sent to each User Access Manager.
		</div>
	</#if>

	<#if error!false>
		<div class="alert alert-danger">
			Please check selection
		</div>
	</#if>

	<div class="form-group">
		<div class="radio">
			<label>
				<input type="radio" name="notification" value="first">
				Send the first notification
			</label>
		</div>
		<div class="radio">
			<label>
				<input type="radio" name="notification" value="second">
				Send the second notification
			</label>
		</div>
	</div>

	<div class="form-group">
		<button class="btn btn-danger" onclick="return confirm('A notification will be sent to each User Access Manager. Continue?')">Send</button>
		<a class="btn btn-default" href="${url('/sysadmin')}">Cancel</a>
	</div>
</@f.form>

