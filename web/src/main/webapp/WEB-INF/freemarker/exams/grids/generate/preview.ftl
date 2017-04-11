<#import 'form_fields.ftl' as form_fields />
<#escape x as x?html>

<#function route_function dept>
	<#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

<#macro showMarks entity markType>
	<#list perYearColumns?keys?sort as year>
		<#if gridOptionsCommand.showComponentMarks>
			<th><span class="use-tooltip" title="${markType.description}">${markType.label}</span></th>
		</#if>
		<#list mapGet(perYearColumns, year) as column>
			<td>
				<#assign hasValue = mapGet(perYearColumnValues, column)?? && mapGet(mapGet(perYearColumnValues, column), entity)?? && mapGet(mapGet(mapGet(perYearColumnValues, column), entity), year)?? />
				<#if hasValue>
					<#assign values = mapGet(mapGet(mapGet(mapGet(perYearColumnValues, column), entity), year), markType) />
					<#list values as value><#noescape>${value.toHTML}</#noescape><#if value_has_next>,</#if></#list>
				</#if>
			</td>
		</#list>
		<#if !mapGet(perYearColumns, year)?has_content><td class="spacer">&nbsp;</td></#if>
		<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
	</#list>
</#macro>

<div class="fix-area">

	<form action="<@routes.exams.generateGrid department academicYear />" class="dirty-check exam-grid-preview" method="post">

		<@form_fields.select_course_fields />
		<@form_fields.grid_options_fields />

		<h2>Preview and download</h2>

		<p class="progress-arrows">
			<span class="arrow-right"><button type="submit" class="btn btn-link">Select courses</button></span>
			<span class="arrow-right arrow-left"><button type="submit" class="btn btn-link" name="${GenerateExamGridMappingParameters.selectCourse}">Set grid options</button></span>
			<span class="arrow-right arrow-left active">Preview and download</span>
		</p>

		<div class="alert alert-info">
			<h3>Your grid</h3>
			<p>
				This grid has been generated from the data available in SITS at
				<@fmt.date date=oldestImport capitalise=false at=true relative=true />. If data changes in SITS after this
				time, you'll need to generate the grid again to see the most recent information.
			</p>
		</div>

		<#if !routeRules?has_content>
			<div class="alert alert-info">
				<h3>Over catted marks</h3>
				<p>There were no Pathway Module Rules defined in SITS for this route, year of study, and academic year.</p>
				<p>Therefore for students who have elected to overcat you will need to review the marks generated and choose the best mark that meets all of your course regulations.</p>
				<p>'Select Edit' to add the best mark. If you download the grid without adding these marks these marks will remain blank. <a href="#" class="show-more">Show More</a></p>
				<div class="more hidden">
					<p>Each course normally consists of modules each with a CATs score and to pass that course you must achieve the minimum number of CATs.</p>
					<p>
						The University allows students to study additional modules, if they so wish for their own education and to potentially achieve a higher mark.
						Any student who studies more than the normal load of CATs has been deemed to have over-catted.
					</p>
					<p>
						So that no student is ever disadvantaged by overcatting the calculation works out the highest scoring combination of modules from those the student has taken.
						If this is higher than the mean module mark, it will be the mark they are awarded, as long as the combination satisfies the course regulations.
					</p>
					<p>
						The regulations governing each course vary widely but always have a minimum number of CATS and the maximum number of CATS to be taken.
						Usually a course is made up of sets of modules from which the student must pick modules.
						Some courses may not allow certain combinations of modules to be taken in a year or over several years.
					</p>
					<p>
						Unless Pathway Module Rules are defined it is not possible for Tabula to be sure that the final over-catted mark it derives complies with the regulations,
						and we ask the exam board to choose the final over-catted mark to ensure that it meets with all the regulations. <a href="#" class="show-less">Less</a>
					</p>
				</div>
			</div>
		</#if>

		<div class="key clearfix">
			<table class="table table-condensed">
				<thead>
					<tr>
						<th colspan="2">Report</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<th>Department:</th>
						<td>${department.name}</td>
					</tr>
					<tr>
						<th>Course:</th>
						<td>${selectCourseCommand.course.code?upper_case} ${selectCourseCommand.course.name}</td>
					</tr>
					<tr>
						<#if !selectCourseCommand.routes?has_content>
							<th>Routes:</th>
							<td>All routes</td>
						<#elseif selectCourseCommand.routes?size == 1>
							<th>Route:</th>
							<td>${selectCourseCommand.routes?first.code?upper_case} ${selectCourseCommand.routes?first.name}</td>
						<#else>
							<th>Routes:</th>
							<#assign popover>
								<ul><#list selectCourseCommand.routes?sort_by('code') as route>
									<li>${route.code?upper_case} ${route.name}</li>
								</#list></ul>
							</#assign>
							<td><a class="use-popover" href="#" data-html="true" data-content="${popover}">${selectCourseCommand.routes?size} routes</a></td>
						</#if>
					</tr>
					<tr>
						<th>Year of study:</th>
						<td>${selectCourseCommand.yearOfStudy}</td>
					</tr>
					<tr>
						<th>Year weightings:</th>
						<td>
							<#list weightings as weighting>
								Year ${weighting.yearOfStudy} = ${weighting.weightingAsPercentage}%<#if weighting_has_next><br /></#if>
							</#list>
						</td>
					</tr>
					<tr>
						<th>Normal CAT load:</th>
						<td>
							<#if normalLoadLookup.routes?size == 1>
								<#if normalLoadLookup.withoutDefault(normalLoadLookup.routes?first)?has_content>
									${normalLoadLookup.withoutDefault(normalLoadLookup.routes?first)}
								<#else>
									<#assign defaultNormalLoad>${normalLoadLookup.apply(normalLoadLookup.routes?first)}</#assign>
									${defaultNormalLoad} <@fmt.help_popover id="normal-load" content="Could not find a Pathway Module Rule for the normal load so using the default value of ${defaultNormalLoad}" />
								</#if>
							<#else>
								<#assign popover>
									<ul><#list normalLoadLookup.routes?sort_by('code') as route>
										<li>${route.code?upper_case}:
											<#if normalLoadLookup.withoutDefault(route)?has_content>
												${normalLoadLookup.withoutDefault(route)}
											<#else>
												<#assign defaultNormalLoad>${normalLoadLookup.apply(route)}</#assign>
												${defaultNormalLoad} <@fmt.help_popover id="normal-load" content="Could not find a Pathway Module Rule for the normal load so using the default value of ${defaultNormalLoad}" />
											</#if>
										</li>
									</#list></ul>
								</#assign>
								<a href="#" class="use-popover" data-html="true" data-content="${popover}">${normalLoadLookup.routes?size} routes</a>
							</#if>
						</td>
					</tr>
					<tr>
						<th>Student Count:</th>
						<td>${entities?size}</td>
					</tr>
					<tr>
						<th>Grid Generated:</th>
						<td><@fmt.date date=generatedDate relative=false /></td>
					</tr>
				</tbody>
			</table>

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
						<td><span class="exam-grid-overcat">#</span></td>
						<td>Used in overcatting calculation</td>
					</tr>
					<tr>
						<td><span class="exam-grid-actual-mark">#</span></td>
						<td>Agreed mark missing, using actual</td>
					</tr>
					<tr>
						<td><span class="exam-grid-actual-mark">X</span></td>
						<td>Agreed and actual mark missing</td>
					</tr>
					<tr>
						<td><span class="exam-grid-actual-mark exam-grid-overcat">#</span></td>
						<td>Actual mark used in overcatting calculation</td>
					</tr>
					<tr>
						<td></td>
						<td>Blank indicates module not taken by student</td>
					</tr>
					<tr>
						<td><strong>AB</strong></td>
						<td>Bold module name indicates a duplicate table entry</td>
					</tr>
				</tbody>
			</table>
		</div>

		<table class="table table-condensed grid <#if !gridOptionsCommand.showComponentMarks>with-hover</#if>">
			<tbody>
				<#-- Year row -->
				<tr class="year">
					<#list studentInformationColumns as column><td class="borderless">&nbsp;</td></#list>
					<#if !gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
					<#list perYearColumns?keys?sort as year>
						<#if gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
						<th colspan="${mapGet(perYearColumns, year)?size}">Year ${year}</th>
						<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
					</#list>
					<#list summaryColumns as column><td class="borderless">&nbsp;</td></#list>
				</tr>
				<#-- Category row -->
				<tr class="category">
					<#list studentInformationColumns as column><td class="borderless">&nbsp;</td></#list>
					<#if !gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
					<#list perYearColumns?keys?sort as year>
						<#if gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
						<#assign currentCategory = '' />
						<#list mapGet(perYearColumns, year) as column>
							<#if column.category?has_content>
								<#if currentCategory != column.category>
									<#assign currentCategory = column.category />
									<th class="rotated" colspan="${mapGet(perYearColumnCategories, year)[column.category]?size}"><div class="rotate">${column.category}</div></th>
								</#if>
							<#else>
								<td>&nbsp;</td>
							</#if>
						</#list>
						<#if !mapGet(perYearColumns, year)?has_content><td class="spacer">&nbsp;</td></#if>
						<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
					</#list>
					<#assign currentCategory = '' />
					<#list summaryColumns as column>
						<#if column.category?has_content>
							<#if currentCategory != column.category>
								<#assign currentCategory = column.category />
								<th class="rotated" colspan="${chosenYearColumnCategories[column.category]?size}"><div class="rotate">${column.category}</div></th>
							</#if>
						<#else>
							<td>&nbsp;</td>
						</#if>
					</#list>
				</tr>
				<#-- Header row -->
				<tr class="header">
					<#list studentInformationColumns as column>
						<th <#if !column.secondaryValue?has_content>rowspan="2"</#if>>${column.title}</th>
					</#list>
					<#if !gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
					<#list perYearColumns?keys?sort as year>
						<#if gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
						<#list mapGet(perYearColumns, year) as column>
							<th class="rotated <#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>" <#if !column.secondaryValue?has_content>rowspan="2"</#if>><div class="rotate">${column.title}</div></th>
						</#list>
						<#if !mapGet(perYearColumns, year)?has_content><td class="spacer">&nbsp;</td></#if>
						<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
					</#list>
					<#list summaryColumns as column>
						<th class="rotated <#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>" <#if !column.secondaryValue?has_content>rowspan="2"</#if>><div class="rotate">${column.title}</div></th>
					</#list>
				</tr>
				<#-- Secondary value row -->
				<tr class="secondary">
					<#if !gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
					<#list perYearColumns?keys?sort as year>
						<#if gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>
						<#list mapGet(perYearColumns, year) as column>
							<#if column.secondaryValue?has_content><th class="<#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>">${column.secondaryValue}</th></#if>
						</#list>
						<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
					</#list>
				</tr>

				<#-- Entities -->
				<#list entities as entity>
					<tr class="student <#if entity_index%2 == 1>odd</#if>">
						<#list studentInformationColumns as column>
							<td <#if gridOptionsCommand.showComponentMarks>rowspan="3"</#if>>
								<#assign hasValue = mapGet(chosenYearColumnValues, column)?? && mapGet(mapGet(chosenYearColumnValues, column), entity)?? />
								<#if hasValue>
									<#noescape>${mapGet(mapGet(chosenYearColumnValues, column), entity).toHTML}</#noescape>
								</#if>
							</td>
						</#list>

						<#if !gridOptionsCommand.showComponentMarks><td class="spacer">&nbsp;</td></#if>

						<@showMarks entity ExamGridColumnValueType.Overall />

						<#list summaryColumns as column>
							<td <#if gridOptionsCommand.showComponentMarks>rowspan="3"</#if>>
								<#assign hasValue = mapGet(chosenYearColumnValues, column)?? && mapGet(mapGet(chosenYearColumnValues, column), entity)?? />
								<#if hasValue>
									<#noescape>${mapGet(mapGet(chosenYearColumnValues, column), entity).toHTML}</#noescape>
								</#if>
							</td>
						</#list>
					</tr>

					<#if gridOptionsCommand.showComponentMarks>
						<tr class="assignments <#if entity_index%2 == 1>odd</#if>">
							<@showMarks entity ExamGridColumnValueType.Assignment />
						</tr>
						<tr class="exams <#if entity_index%2 == 1>odd</#if>">
							<@showMarks entity ExamGridColumnValueType.Exam />
						</tr>
					</#if>
				</#list>
			</tbody>
		</table>

		<div class="fix-footer">
			<div class="btn-group dropup">
				<button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">Download&hellip; <span class="caret"></span></button>
				<ul class="dropdown-menu download-options">
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.excel}">Excel grid</button></li>
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.marksRecord}">Marks record</button></li>
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.marksRecordConfidential}">Confidential marks record</button></li>
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.passList}">Pass list</button></li>
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.passListConfidential}">Confidential pass list</button></li>
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.transcript}">Transcript</button></li>
					<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.transcriptConfidential}">Confidential transcript</button></li>
				</ul>
			</div>
		</div>
	</form>
	<div class='modal fade' id='confirmModal'>
		<div class='modal-dialog' role='document'><div class='modal-content'>
			<div class='modal-body'>
				<p>
					Exam grids contain restricted information. Under the University's
					<a target='_blank' href='http://www2.warwick.ac.uk/services/gov/informationsecurity/handling/classifications'>information classification scheme</a>,
					student names and University IDs are 'protected', exam marks are 'restricted' and provisional degree classifications are 'reserved'.
				</p>
				<p>
					When you download the data provided you are responsible for managing the security of the
					information within it. You agree to abide by the University's <a target='_blank' href='http://www2.warwick.ac.uk/services/legalservices/dataprotection/'>
						Data Protection Policy
					<a> and the mandatory working practices for <a target='_blank' href='http://www2.warwick.ac.uk/services/gov/informationsecurity/working_practices/assets_protection/'>
						electronic information asset protection.</a>
					</p>
			</div>
			<div class='modal-footer'>
				<a class='confirm btn btn-primary'>Accept</a>
				<a data-dismiss='modal' class='btn btn-default'>Cancel</a>
				</div>
			</div>
		</div>
	</div>
</div>

<div class="modal fade" id="edit-overcatting-modal"></div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();

		var $form = $('form.exam-grid-preview'), $confirmModal = $('#confirmModal');
		$('a.confirm', $confirmModal).on('click', function() {
			$form.submit();
			$confirmModal.modal('hide');
			$form.find('input.download-option').remove();
		});
		$('.download-options').on('click', 'button', function(e) {
			e.preventDefault();
			e.stopPropagation();
			var $this = $(this);
			$form.find('input.download-option').remove();
			$form.append($('<input/>').attr({
				'type' : 'hidden',
				'class' : 'download-option',
				'name' : $this.attr('name'),
				'value' : true
			}));
			$confirmModal.modal('show');
		});

		$('th.rotated, td.rotated').each(function() {
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

		$('button.edit-overcatting').each(function(){
			$(this).attr('href', '<@routes.exams.generateGrid department academicYear />/overcatting/' + $(this).data('student'))
				.data('target', '#edit-overcatting-modal');

		}).ajaxModalLink();

		$('a.show-more').on('click', function(e){
			e.preventDefault();
			$(this).parent().next('.more').removeClass('hidden').end().end()
				.hide();
		});
		$('a.show-less').on('click', function(e){
			e.preventDefault();
			$(this).closest('.more').addClass('hidden').parent().find('a.show-more').show();
		});
	});
</script>

</#escape>