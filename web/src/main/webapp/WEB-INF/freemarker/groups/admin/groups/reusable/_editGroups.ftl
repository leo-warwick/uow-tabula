<#escape x as x?html>
	<#list command.groupNames as group>
		<@bs3form.form_group path="groupNames[${group_index}]">
			<div class="row">
				<div class="col-md-6">
					<div class="input-group">
						<span class="input-group-addon">
							${group_index + 1}.
						</span>
						<@f.input path="groupNames[${group_index}]" cssClass="form-control" />
						<@f.hidden path="groupIds[${group_index}]" />
						<span class="input-group-btn">
							<button type="button" class="btn btn-danger" data-toggle="remove"><i class="fa fa-times"></i></button>
						</span>
					</div>
				</div>
				<div class="col-md-6">
					<@bs3form.errors path="groupNames[${group_index}]" />
				</div>
			</div>
		</@bs3form.form_group>
	</#list>

	<#assign group_index = command.groupNames?size />

	<@bs3form.form_group path="groupNames[${group_index}]">
		<div class="row">
			<div class="col-md-6">
				<div class="input-group">
					<span class="input-group-addon">
						${group_index + 1}.
					</span>
					<@f.input path="groupNames[${group_index}]" cssClass="form-control" />
				</div>
			</div>
		</div>
	</@bs3form.form_group>

	<@bs3form.form_group>
		<button type="button" class="btn btn-default" data-toggle="add" title="Add another group" disabled="disabled">
			Add group
		</button>
	</@bs3form.form_group>

	<script type="text/javascript">
		jQuery(function($) {
			$('button[data-toggle="add"]').each(function() {
				var $button = $(this);
				var $group = $button.closest('.form-group').prev('.form-group');
				var $input = $group.find('input[type="text"]');

				$input.on('paste', function() {
					setTimeout(function() {
						$input.trigger('change');
					}, 50);
				});

				$input.on('change keyup', function() {
					if ($input.val().length > 0) {
						$button.removeAttr('disabled');
					} else {
						$button.attr('disabled', 'disabled');
					}
				});
			}).on('click', function() {
				var $button = $(this);
				var $group = $button.closest('.form-group').prev('.form-group');

				var $clone = $group.clone();

				var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
				$clone.find('.input-group-addon').text((index + 1) + ".");

				$clone.insertBefore($group);
				$clone.find('.input-group')
					.append('<span class="input-group-btn"><button type="button" class="btn btn-danger" data-toggle="remove"><i class="fa fa-times"></i></button></span>');

				var nextIndex = index + 1;
				var $name = $group.find('input[type="text"]');
				$name.attr('name', 'groupNames[' + nextIndex + ']');
				$name.attr('id', 'groupNames' + nextIndex);

				$group.find('.input-group-addon').text((nextIndex + 1) + '.');

				$name.val('').focus();
				$button.attr('disabled', 'disabled');
			});

			$(document.body).on('click', 'button[data-toggle="remove"]', function() {
				var $button = $(this);
				var $group = $button.closest('.form-group');

				$group.remove();

				// Re-order all of the groups
				var groups = $('input[name^="groupNames"]').closest('.form-group');
				groups.each(function(index) {
					var $group = $(this);

					var $name = $group.find('input[type="text"]');
					$name.attr('name', 'groupNames[' + index + ']');
					$name.attr('id', 'groupNames' + index);

					var $id = $group.find('input[type="hidden"]');
					$id.attr('name', 'groupIds[' + index + ']');
					$id.attr('id', 'groupIds' + index);

					$group.find('.input-group-addon').text((index + 1) + '.');
				});
			});

			// Stop 'Enter' from submitting the form
			$('input[name^="groupNames"]').closest('form')
				.off('keyup.inputSubmitProtection keypress.inputSubmitProtection')
				.on('keyup.inputSubmitProtection keypress.inputSubmitProtection', function(e){
					var code = e.keyCode || e.which;
					if (code  == 13) {
						e.preventDefault();
						return false;
					}
				});
		});
	</script>
</#escape>