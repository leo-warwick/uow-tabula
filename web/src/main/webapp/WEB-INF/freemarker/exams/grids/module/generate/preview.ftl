<#import 'form_fields.ftl' as form_fields />
<#escape x as x?html>

	<#function route_function dept>
		<#local selectModuleCommand><@routes.exams.generateModuleGrid dept academicYear /></#local>
		<#return selectModuleCommand />
	</#function>


<@fmt.id7_deptheader title="Create a new module exam grid for ${department.name}" route_function=route_function />

<div class="fix-area">

	<form action="<@routes.exams.generateModuleGrid department academicYear />" class="dirty-check exam-grid-preview" method="post">

		<@form_fields.select_module_fields />

		<h2>Preview and download</h2>

		<p class="progress-arrows">
			<span class="arrow-right"><button type="submit" class="btn btn-link">Select modules</button></span>
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
						<th>Module:</th>
						<td>${selectModuleExamCommand.module.code?upper_case} ${selectModuleExamCommand.module.name}</td>
					</tr>
					<tr>
						<th>Student Count:</th>
						<td>${studentCount}</td>
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
						<td>Failed</td>
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
						<td></td>
						<td>Blank indicates module not taken by student</td>
					</tr>
				</tbody>
			</table>
		</div>


		<table class="table table-condensed grid">
			<thead>
				<tr>
					<th>Name</th>
					<th>ID</th>
					<th>SCJ Code</th>
					<th>Course</th>
					<th>Route</th>
					<th>Start Year</th>
					<th>Credit</th>

					<#list componentInfo as component>
						<#assign groupAndSequence = component._1() />
						<#assign cName = component._2() />
						<th colspan="2"><span class="use-tooltip" title="" data-container="body" data-original-title="${cName}">${groupAndSequence}</span></th>
					</#list>
					<th>Module Mark</th>
					<th>Module Grade</th>
				</tr>
			</thead>
			<tbody>

				<#-- Entities -->
				<#list entities as entity>
					<#assign assessmentComponentMap = entity.componentInfo />
					<#assign mr = entity.moduleRegistration />
					<tr class="student <#if entity_index%2 == 1>odd</#if>">
						<td>${entity.name}</td>
						<td>${entity.universityId} </td>
						<td> ${mr.studentCourseDetails.scjCode}</td>
						<td>${mr.studentCourseDetails.course.code}</td>
						<td>${mr.studentCourseDetails.currentRoute.code}</td>
						<td>${mr.academicYear.startYear?c}</td>
						<td>${mr.cats}</td>

						<#list componentInfo as component>
							<#assign groupAndSequence = component._1() />
								<#if mapGet(assessmentComponentMap, groupAndSequence)??>
									<#assign componentDetails = mapGet(assessmentComponentMap, groupAndSequence) />
												<td>
													<#if componentDetails.mark??>
														<#if componentDetails.mark?number < passMark>
															<span class="exam-grid-fail">${componentDetails.mark}</span>
														<#elseif componentDetails.actual>
															<span class="exam-grid-actual-mark">${componentDetails.mark}</span>
														<#else>
															${componentDetails.mark}
														</#if>
													<#else>
														<span class="exam-grid-actual-mark use-tooltip" title="" data-container="body" data-original-title="No mark set">X</span>
													</#if>
												</td>
												<td>
													<#if componentDetails.grade??>
														<#if componentDetails.actual>
																<span class="exam-grid-actual-mark">${componentDetails.grade}</span>
														<#else>
															${componentDetails.grade}
														</#if>
													<#else>
														<span class="exam-grid-actual-mark use-tooltip" title="" data-container="body" data-original-title="No grade set">X</span>
													</#if>
												</td>
								<#else>
									<td></td><td></td>
								</#if>
						</#list>
						<td>
							<#if mr.agreedMark??>
									<#if mr.agreedMark?number < passMark>
										<span class=exam-grid-fail">${mr.agreedMark}</span>
									<#else>
										${mr.agreedMark}
									</#if>
							<#elseif mr.actualMark??>
								<span class="<#if mr.actualMark?number < passMark>exam-grid-fail<#else>exam-grid-actual-mark</#if>">
									${mr.actualMark}
								</span>
							<#else>
									<span class="exam-grid-actual-mark use-tooltip" title="" data-container="body" data-original-title="No marks set">X</span>
							</#if>
						</td>
						<td>
							<#if mr.agreedGrade??>
								${mr.agreedGrade}
							<#elseif mr.actualGrade??>
									<span class="exam-grid-actual-mark">${mr.actualGrade}</span>
							<#else>
									<span class="exam-grid-actual-mark use-tooltip" title="" data-container="body" data-original-title="No grade set">X</span>
							</#if>
						</td>
					</tr>
				</#list>
			</tbody>
		</table>

		<div class="fix-footer">
			<div class="btn-group dropup">
				<button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">Download&hellip; <span class="caret"></span></button>
				<ul class="dropdown-menu download-options">
					<li><button class="btn btn-link" type="submit" name="${GenerateModuleExamGridMappingParameters.excel}">Excel grid</button></li>
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


		$('a.show-more').on('click', function(e){
			e.preventDefault();
			$(this).parent().next('.more').removeClass('hidden').end().end()
				.hide();
		});
		$('a.show-less').on('click', function(e){
			e.preventDefault();
			$(this).closest('.more').addClass('hidden').parent().find('a.show-more').show();
		});

		// fix the grid scrollbar to the footer
		var $scrollWrapper = $('.doubleScroll-scroll-wrapper');
		var $grid = $('.grid');

		$scrollWrapper.prependTo('.fix-footer').css('margin-bottom', '10px');

		function reflowScroll() {
			setTimeout(function () {
				$scrollWrapper
					// Update the width of the scroll track to match the container
					.width($scrollWrapper.parent().width())
					// Update the scroll bar so it reflects the width of the grid
					.children().width($grid.width()).end()
					// Reset the scroll bar to the initial position
					.scrollLeft(0);
			}, 0);
		}
		$(window).on('id7:reflow', reflowScroll);
		reflowScroll();

		$('.table-responsive').css('overflow-x', 'hidden');

	});
</script>

</#escape>