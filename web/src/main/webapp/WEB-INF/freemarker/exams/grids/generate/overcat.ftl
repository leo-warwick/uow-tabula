<#escape x as x?html>
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />
<@modal.wrapper cssClass="modal-xl">
	<@modal.body>
		<div class="exam-grid-preview">
			<div class="overcat-key">
				<table class="table table-condensed">
					<thead>
						<tr>
							<th colspan="2">Key</th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td><span class="exam-grid-fail">#</span></td>
							<td>Failed module</td>
						</tr>
						<tr>
							<td><span class="exam-grid-override">#</span></td>
							<td>Manually adjusted and not stored in SITS</td>
						</tr>
						<tr>
							<td><span class="exam-grid-actual-mark">#</span></td>
							<td>Agreed mark missing, using actual</td>
						</tr>
					</tbody>
				</table>
			</div>

			<h3>Select an overcatted mark for ${scyd.studentCourseDetails.student.fullName!} - ${scyd.studentCourseDetails.student.universityId}</h3>

			<p>You can change module marks but any changes made at this stage will not be saved back to SITS.</p>

			<p class="clearfix">&nbsp;</p>
			<form action="<@routes.exams.generateGridOvercatting department academicYear scyd/>" method="post">
				<table class="table table-condensed grid overcat">
					<tbody>
						<#-- Year row -->
						<tr class="year">
							<th colspan="3">Options</th>
							<td class="spacer">&nbsp;</td>
							<#list overcatView.perYearColumns?keys?sort?reverse as year>
								<th colspan="${mapGet(overcatView.perYearColumns, year)?size}">Year ${year}</th>
								<td class="spacer">&nbsp;</td>
							</#list>
						</tr>
						<#-- Category row -->
						<tr class="category">
							<#assign currentCategory = '' />
							<#list overcatView.optionsColumns as column>
								<#if column.category?has_content>
									<#if currentCategory != column.category>
										<#assign currentCategory = column.category />
										<th class="rotated" colspan="${overcatView.optionsColumnCategories[column.category]?size}"><div class="rotate">${column.category}</div></th>
									</#if>
								<#else>
									<td>&nbsp;</td>
								</#if>
							</#list>
							<td class="spacer">&nbsp;</td>
							<#list overcatView.perYearColumns?keys?sort?reverse as year>
								<#assign currentCategory = '' />
								<#list mapGet(overcatView.perYearColumns, year) as column>
									<#if column.category?has_content>
										<#if currentCategory != column.category>
											<#assign currentCategory = column.category />
											<th class="rotated" colspan="${mapGet(overcatView.perYearColumnCategories, year)[column.category]?size}"><div class="rotate">${column.category}</div></th>
										</#if>
									<#else>
										<td>&nbsp;</td>
									</#if>
								</#list>
								<td class="spacer">&nbsp;</td>
							</#list>
						</tr>
						<#-- Header row -->
						<tr class="header">
							<#list overcatView.optionsColumns as column>
								<th class="rotated" <#if !column.secondaryValue?has_content>rowspan="2"</#if>><div class="rotate">${column.title}</div></th>
							</#list>
							<td class="spacer">&nbsp;</td>
							<#list overcatView.perYearColumns?keys?sort?reverse as year>
								<#list mapGet(overcatView.perYearColumns, year) as column>
									<th class="rotated <#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>" <#if !column.secondaryValue?has_content>rowspan="2"</#if>><div class="rotate">${column.title}</div></th>
								</#list>
								<td class="spacer">&nbsp;</td>
							</#list>
						</tr>
						<#-- Secondary value row -->
						<tr class="secondary">
							<td class="spacer">&nbsp;</td>
							<#list overcatView.perYearColumns?keys?sort?reverse as year>
								<#list mapGet(overcatView.perYearColumns, year) as column>
									<#if column.secondaryValue?has_content><th class="<#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>">${column.secondaryValue}</th></#if>
								</#list>
								<td class="spacer">&nbsp;</td>
							</#list>
						</tr>

						<#-- Entities -->
						<#list overcatView.overcattedEntities as entity>
							<tr class="student clickable">
								<#list overcatView.optionsColumns as column>
									<td>
										<#assign hasValue = mapGet(overcatView.optionsColumnValues, column)?? && mapGet(mapGet(overcatView.optionsColumnValues, column), entity)?? />
										<#if hasValue>
											<#noescape>${mapGet(mapGet(overcatView.optionsColumnValues, column), entity).toHTML}</#noescape>
										</#if>
									</td>
								</#list>
								<td class="spacer">&nbsp;</td>
								<#list overcatView.perYearColumns?keys?sort?reverse as year>
									<#list mapGet(overcatView.perYearColumns, year) as column>
										<td>
											<#assign hasValue = mapGet(overcatView.perYearColumnValues, column)?? && mapGet(mapGet(overcatView.perYearColumnValues, column), entity)?? && mapGet(mapGet(mapGet(overcatView.perYearColumnValues, column), entity), year)?? />
											<#if hasValue>
												<#assign values = mapGet(mapGet(mapGet(mapGet(overcatView.perYearColumnValues, column), entity), year), ExamGridColumnValueType.Overall) />
												<#list values as value><#noescape>${value.toHTML}</#noescape><#if value_has_next>,</#if></#list>
											</#if>
										</td>
									</#list>
									<td class="spacer">&nbsp;</td>
								</#list>
							</tr>
						</#list>
						<#if overcatView.overcattedEntities?has_content>
							<#-- Manual marks -->
							<tr class="new-marks">
								<th colspan="${overcatView.optionsColumns?size + 1}">New Module Mark</th>
								<#list mapGet(overcatView.perYearColumns, overcatView.perYearColumns?keys?sort?reverse?first) as column>
									<td>
										<input type="text" class="form-control" name="newModuleMarks[${column.module.code}]" value="${mapGet(command.newModuleMarks, column.module)!}"/>
									</td>
								</#list>
							</tr>
							<tr>
								<td colspan="${overcatView.optionsColumns?size + 1}" class="borderless"></td>
								<#-- 1000 just so it spans the whole rest of the row -->
								<td colspan="1000" class="borderless">
									<button type="button" class="btn btn-default" name="recalculate">Recalculate</button>
									<button type="button" class="btn btn-default" name="reset">Reset</button>
								</td>
							</tr>
						</#if>
					</tbody>
				</table>
			</form>
		</div>
	</@modal.body>
	<@modal.footer>
		<div class="pull-left">
			<button type="button" class="btn btn-primary" name="continue">Continue</button>
			<button type="button" class="btn btn-default" name="${GenerateExamGridMappingParameters.excel}">Download for printing</button>
			<button type="button" class="btn btn-default" name="cancel">Cancel</button>
		</div>
	</@modal.footer>
</@modal.wrapper>

<script>
	jQuery(function($){
		var $modalBody = $('.modal-body'), $modalFooter = $('.modal-footer'), $modalFooterButtons = $modalFooter.find('button');

		$('.modal-body th.first-in-category, .modal-body td.first-in-category').each(function(){
			$(this).prev().addClass('last-in-category');
		});

		$('.modal-body th.rotated, .modal-body td.rotated').each(function() {
			var width = $(this).find('.rotate').width();
			var height = $(this).find('.rotate').height();
			$(this).css('height', width + 15).css('min-width', height + 5);
			$(this).find('.rotate').css({
				'margin-left': height / 2
			}).not('.nomargin').css({
				'margin-top': -(height)
			}).end().filter('.middle').not('.nomargin').css({
				'margin-top': width / 4
			});
		});
		$modalBody.wideTables();

		var updateButtons = function(){
			if ($('.modal-body input:checked').length > 0) {
				$('.modal-footer button[name=continue]').prop('disabled', false);
			} else {
				$('.modal-footer button[name=continue]').prop('disabled', true);
			}
			if ($('.modal-body tr.new-marks input').filter(function(){ return $(this).val().length > 0 }).length > 0) {
				$('.modal-body button[name=recalculate]').prop('disabled', false);
				$('.modal-body button[name=reset]').prop('disabled', false);
			} else {
				$('.modal-body button[name=recalculate]').prop('disabled', true);
				$('.modal-body button[name=reset]').prop('disabled', true);
			}
		};
		updateButtons();
		$('.modal-body table.grid').on('click', 'tr.clickable', function(){
			$(this).find('input').prop('checked', true);
			updateButtons();
		}).on('keyup', updateButtons);

		$modalBody.on('click', 'button[name=recalculate]', function(){
			$('.modal button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'recalculate'
			}));
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				$('#edit-overcatting-modal').html(data);
			});
		}).on('click', 'button[name=reset]', function(){
			$('.modal button').prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'recalculate'
			})).find('tr.new-marks input').each(function(){ $(this).val(''); });
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				$('#edit-overcatting-modal').html(data);
			});
		});
		$modalFooter.on('click', 'button[name=continue]', function(){
			$('.modal button').prop('disabled', true);
		}).on('click', 'button[name=${GenerateExamGridMappingParameters.excel}]', function(){
			$modalFooterButtons.prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': '${GenerateExamGridMappingParameters.excel}'
			}));
			$form.submit();
			$form.find('input[name=${GenerateExamGridMappingParameters.excel}]').remove();
			$modalFooterButtons.prop('disabled', false);
		}).on('click', 'button[name=continue]', function(){
			$modalFooterButtons.prop('disabled', true);
			var $form = $('.modal-body form');
			$form.append($('<input/>').attr({
				'type': 'hidden',
				'name': 'continue'
			}));
			$.post('<@routes.exams.generateGridOvercatting department academicYear scyd/>', $form.serialize(), function(data){
				if (!data.errors) {
					$('form.exam-grid-preview').append($('<input/>').attr({
						'type': 'hidden',
						'name': '${GenerateExamGridMappingParameters.previewAndDownload}'
					})).submit();
				} else {
					updateButtons();
				}
			});
		}).on('click', 'button[name=cancel]', function(){
			$(this).closest('.modal').modal('hide');
		});

		$modalBody.closest('.modal').on('shown.bs.modal', function(){
			$('.modal-body th.rotated, .modal-body td.rotated').each(function() {
				var width = $(this).find('.rotate').width();
				var height = $(this).find('.rotate').height();
				$(this).css('height', width + 15).css('min-width', height + 5);
				$(this).find('.rotate').css({
					'margin-left': height / 2
				}).not('.nomargin').css({
					'margin-top': -(height)
				}).end().filter('.middle').not('.nomargin').css({
					'margin-top': width / 4
				});
			});
			$modalBody.wideTables();
		});

	});
</script>

</#escape>