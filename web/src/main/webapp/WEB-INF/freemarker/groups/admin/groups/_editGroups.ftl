<#escape x as x?html>
	<style type="text/css">
		.existing-group.deleted label {
			text-decoration: line-through;
			color: @grayLight;
		}

		.existing-group.deleted .help-inline {
			text-decoration: none;
		}

		.existing-group.deleted input {
			cursor: not-allowed;
			background-color: #eee !important;
			text-decoration: line-through;
		}

		#container .control-group.flush {
			margin-bottom:6px;
		}

		.help-block.hidden { display: none !important; }
	</style>

	<#if smallGroupSet.linked>
		<@bs3form.form_group>
			<p>There are <@fmt.p smallGroupSet.groups?size "group" /> in ${smallGroupSet.name} (from ${smallGroupSet.linkedDepartmentSmallGroupSet.name}).</p>

			<#if smallGroupSet.groups?size gt 0>
				<ul>
					<#list smallGroupSet.groups as group>
						<li>${group.name}</li>
					</#list>
				</ul>
			</#if>
		</@bs3form.form_group>
	<#else>
		<@f.hidden path="defaultMaxGroupSizeEnabled" id="limitedDefaultGroupSize" value="true" />

		<@bs3form.form_group>
			<div class="row flush">
				<div class="col-md-4 col-md-offset-4">
				<#-- Well this sucks -->
					<span class="uneditable-input hidden">&nbsp;</span>
					&nbsp;
					Set group numbers
					<a class="use-popover" data-html="true" data-content="Leave the max group size field empty for no limit" data-original-title="" title="">
						<i class="icon-question-sign"></i>
					</a>
				</div>
			</div>
		</@bs3form.form_group>

		<@form.row cssClass="flush">
			<@form.field>

			</@form.field>
		</@form.row>

		<#macro fields index is_new=false no_remove_button=false>
			<#local cssClass><#compress>
				<#if is_new>
					new-group
				<#else>
					existing-group
					<@spring.bind path="delete">
						<#if status.actualValue>deleted</#if>
					</@spring.bind>
				</#if>
			</#compress></#local>

			<@bs3form.form_group>
				<div class="row ${cssClass}">

					<div class="col-md-4">
						<div class="input-group">
							<span class="input-group-addon">
								${index}.
							</span>
							<@f.input path="name" id="${index}-name" cssClass="form-control" />
						</div>
					</div>

					<div class="col-md-1 max-group-size-options">
						<@f.input id="${index}-maxGroupSize" path="maxGroupSize" placeholder="Unlimited" type="number" min="0" cssClass="text form-control" />
					</div>

					<#if !no_remove_button>
						<div class="col-md-2">
							<#if is_new>
								<button type="button" class="btn btn-danger" data-toggle="remove"><i class="fa fa-times"></i></button>
							<#else>
								<@f.hidden path="delete" />
								<button type="button" class="btn btn-danger" data-toggle="mark-deleted"><i class="fa fa-times"></i></button>
								<button type="button" class="btn btn-primary" data-toggle="undo-deleted"><i class="fa fa-undo"></i></button>
							</#if>
						</div>

						<#if !is_new>
							<p class="help-block col-md-12 hidden"><small>
								Click "Save and add students" or "Save and exit" to delete this group and any events associated with it
							</small></p>
						</#if>
					</#if>

					<@bs3form.errors "name" />
					<@bs3form.errors "maxGroupSize" />
					<#if !is_new>
						<@bs3form.errors "delete" />
					</#if>

				</div>
			</@bs3form.form_group>
		</#macro>

		<#list smallGroupSet.groups as group>
			<@spring.nestedPath path="existingGroups[${group.id}]">
				<@fields index=(group_index + 1) />
			</@spring.nestedPath>
		</#list>
		<#list command.newGroups as newGroup>
			<@spring.nestedPath path="newGroups[${newGroup_index}]">
				<@fields index=(smallGroupSet.groups?size + newGroup_index + 1) is_new=true />
			</@spring.nestedPath>
		</#list>

		<@spring.nestedPath path="newGroups[${command.newGroups?size}]">
			<@fields index=(smallGroupSet.groups?size + command.newGroups?size + 1) is_new=true no_remove_button=true />
		</@spring.nestedPath>

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

					var offset = $('.row.existing-group').length;

					var $clone = $group.clone();

					var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
					$clone.find('.input-group-addon').text((index + offset + 1) + ".");

					$clone.insertBefore($group);
					$clone.find('.row')
						.append('<div class="col-md-2"><button type="button" class="btn btn-danger" data-toggle="remove"><i class="fa fa-times"></i></button></div>');

					var nextIndex = index + 1;
					var $name = $group.find('input[type="text"]');
					$name.attr('name', 'newGroups[' + nextIndex + '].name');
					$name.attr('id', (nextIndex + offset + 1) + '-name');

					var $max = $group.find('input[type="number"]');
					$max.attr('name', 'newGroups[' + nextIndex + '].maxGroupSize');
					$max.attr('id', (nextIndex + offset + 1) + '-maxGroupSize');

					$group.find('.input-group-addon').text((nextIndex + offset + 1) + '.');

					$name.val('').focus();
					$button.attr('disabled', 'disabled');
					$button.closest('form.dirty-check').trigger('rescan.areYouSure');
				});

				$(document.body).on('click', 'button[data-toggle="remove"]', function() {
					var $button = $(this);
					var $group = $button.closest('.form-group');

					$group.remove();

					// Re-order all of the groups
					var groups = $('.row.new-group').closest('.form-group');
					var offset = $('.row.existing-group').length;
					groups.each(function(index) {
						var $group = $(this);

						var $name = $group.find('input[type="text"]');
						$name.attr('name', 'newGroups[' + index + '].name');
						$name.attr('id', (index + offset + 1) + '-name');

						var $max = $group.find('input[type="number"]');
						$max.attr('name', 'newGroups[' + index + '].maxGroupSize');
						$max.attr('id', (index + offset + 1) + '-maxGroupSize');

						$group.find('.input-group-addon').text((index + offset + 1) + '.');
					});
					$button.closest('form.dirty-check').trigger('rescan.areYouSure');
				});

				// Stop 'Enter' from submitting the form
				$('input[name$=".name"]').closest('form')
						.off('keyup.inputSubmitProtection keypress.inputSubmitProtection')
						.on('keyup.inputSubmitProtection keypress.inputSubmitProtection', function(e){
							var code = e.keyCode || e.which;
							if (code  == 13) {
								e.preventDefault();
								return false;
							}
						});

				$('.existing-group').each(function() {
					var $this = $(this);

					var $deleteButton = $this.find('.btn-danger');
					var $undoButton = $this.find('.btn-primary');
					var $helpBlock = $this.find('.help-block');
					var $hiddenField = $this.find('input[name$="delete"]');

					var handleButtonDisplay = function() {
						if ($this.is('.deleted')) {
							$deleteButton.hide();
							$undoButton.show();
							$helpBlock.removeClass('hidden');
						} else {
							$deleteButton.show();
							$undoButton.hide();
							$helpBlock.addClass('hidden');
						}
					};
					handleButtonDisplay();

					$deleteButton.on('click', function() {
						$this.addClass('deleted');
						$hiddenField.val('true');
						$this.closest('form.dirty-check').trigger('checkform.areYouSure');
						handleButtonDisplay();
					});

					$undoButton.on('click', function() {
						$this.removeClass('deleted');
						$hiddenField.val('false');
						$this.closest('form.dirty-check').trigger('checkform.areYouSure');
						handleButtonDisplay();
					});
				});
			});
		</script>
	</#if>
</#escape>