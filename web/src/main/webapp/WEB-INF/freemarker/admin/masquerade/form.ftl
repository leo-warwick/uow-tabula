<h1>Masquerade as a different user</h1>

<div class="row">
	<div class="col-md-<#if (activeSpringProfiles!"") == "sandbox">6<#else>12</#if>">
		<#if !user.sysadmin && user.masquerader>
			<p>You are not a system admin but are in a group of people able to masquerade freely as another user.</p>
		</#if>

		<p>Masquerading allows you to see the site exactly as another user would see it. If you do any audited
			actions, both your masquerade identity and your true identity will be stored.</p>

		<#if actionMessage!'' = "removed">
			<p>You are no longer masquerading.</p>
		</#if>

		<@f.form method="post" action="${url('/admin/masquerade')}" commandName="masqueradeCommand" cssClass="form-inline">
			<input type="hidden" name="returnTo" value="${returnTo!""}" />
			<@f.errors cssClass="error form-errors" />
			<@bs3form.form_group>
				<@bs3form.flexipicker name="usercode" placeholder="Type a name or usercode">
					<span class="input-group-btn">
						<button class="btn btn-default" type="submit">Mask</button>
					</span>
				</@bs3form.flexipicker>
			</@bs3form.form_group>
			<@bs3form.errors path="usercode" />
		</@f.form>

		<#if user.masquerading>

			<p>Masquerading as ${user.apparentId} (${user.apparentUser.fullName}).</p>

			<@f.form method="post" action="${url('/admin/masquerade')}">
				<input type="hidden" name="returnTo" value="${returnTo!""}" />
				<input type="hidden" name="action" value="remove" />
				<button type="submit" class="btn btn-default">Unmask</button>
			</@f.form>

		</#if>

		<#if (returnTo!"")?length gt 0>
			<br />
			<p><a href="${returnTo}" class="btn btn-default">Return to previous page</a></p>
		</#if>
	</div>
	<#if (activeSpringProfiles!"") == "sandbox" && masqueradeDepartments?size gt 0>
		<div class="col-md-6">
			<div class="alert alert-block">
				<h3><i class="fa fa-sun-o"></i> Sandbox data</h3>

				<p>The following users are available for masquerading in the Sandbox system:</p>

				<#list masqueradeDepartments as department>
					<#if masqueradeDepartments?size gt 1><h4>${department.name}</h4></#if>

					<#if department.staff?size gt 0>
						<h5>Staff</h5>

						<ul>
							<#list department.staff as staff>
								<li data-userId="${staff.userId}">${staff.userId} (${staff.fullName})</li>
							</#list>
						</ul>
					</#if>

					<#if department.students?size gt 0>
						<h5>Students</h5>

						<#list department.students as route>
							<h6>${route._1().code?upper_case} ${route._1().name} (${route._1().degreeType.dbValue})</h6><ul>
								<#list route._2() as student>
									<li data-userId="${student.userId}">${student.userId} (${student.fullName})</li>
								</#list>
							</ul>
						</#list>
					</#if>
				</#list>
			</div>
		</div>

		<script type="text/javascript">
			jQuery(function($) {
				$('li[data-userId]').each(function() {
					var $li = $(this);
					var userId = $li.data('userid');

					var $button = $('<button class="btn btn-xs" type="button">Mask</button>');
					$button.on('click', function(e) {
						e.preventDefault();
						e.stopPropagation();

						var command = $('#masqueradeCommand');
						command.find('input[type="text"]').val(userId);
						command.submit();

						return false;
					});

					$li.prepend('&nbsp;').prepend($button);
				});
			});
		</script>
	</#if>
</div>