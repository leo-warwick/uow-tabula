<#escape x as x?html>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#import "*/modal_macros.ftl" as modal />

<#macro autoGradeOnline gradePath gradeLabel markPath markingId generateUrl>
	<@form.label path="${gradePath}">${gradeLabel}</@form.label>
	<@form.field>
		<@f.input path="${gradePath}" cssClass="input-small auto-grade" id="auto-grade-${markingId}" />
		<select name="${gradePath}" class="input-small" disabled style="display:none;"></select>
		<@fmt.help_popover id="auto-grade-${markingId}-help" content="The grades available depends on the mark entered and the SITS mark scheme in use" />
		<@f.errors path="${gradePath}" cssClass="error" />
	</@form.field>
	<script>
		jQuery(function($){
			var $gradeInput = $('#auto-grade-${markingId}').hide()
				, $markInput = $gradeInput.closest('form').find('input[name=${markPath}]')
				, $select = $gradeInput.closest('div').find('select').on('click', function(){
					$(this).closest('.control-group').removeClass('info');
				})
				, currentRequest = null
				, data = {'studentMarks': {}}
				, doRequest = function(){
					if (currentRequest != null) {
						currentRequest.abort();
					}
					data['studentMarks']['${markingId}'] = $markInput.val();
					if ($select.is(':visible') || $gradeInput.val().length > 0) {
						data['selected'] = {};
						data['selected']['${markingId}'] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
					}
					currentRequest = $.ajax('${generateUrl}',{
						'type': 'POST',
						'data': data,
						success: function(data) {
							$select.html(data);
							if ($select.find('option').length > 1) {
								$gradeInput.hide().prop('disabled', true);
								$select.prop('disabled', false).show()
									.closest('.control-group').addClass('info');
								$('#auto-grade-${markingId}-help').show();
							} else {
								$gradeInput.show().prop('disabled', false);
								$select.prop('disabled', true).hide();
								$('#auto-grade-${markingId}-help').hide();
							}
						}, error: function(xhr, errorText){
							if (errorText != "abort") {
								$gradeInput.show().prop('disabled', false);
								$('#auto-grade-${markingId}-help').hide();
							}
						}
					});
				};
			$markInput.on('keyup', doRequest);
			doRequest();
		});
	</script>
</#macro>

<#macro marksForm assignment templateUrl formUrl commandName cancelUrl generateUrl seatNumberMap="" showAddButton=true>
	<div id="batch-feedback-form">
		<h1>Submit marks for ${assignment.name}</h1>
		<ul id="marks-tabs" class="nav nav-tabs">
			<li class="active"><a href="#upload">Upload</a></li>
			<li class="webform-tab"><a href="#webform">Web Form</a></li>
		</ul>
		<div class="tab-content">
			<div class="tab-pane active" id="upload">
				<p>
					You can upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
					The spreadsheet should have at least two column headings: <b>University ID</b> and <b>Mark</b>.
					You can use this <a href="${templateUrl}" >generated spreadsheet</a> as a template.
					Note that you can upload just marks, or marks and grades.
				</p>
				<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="${commandName}">
					<input name="isfile" value="true" type="hidden"/>
					<table role="presentation" class="narrowed-form">
						<tr>
							<td id="multifile-column">
								<h3>Select file</h3>
								<p id="multifile-column-description">
									<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
								</p>
								<@form.labelled_row "file.upload" "Files">
									<input type="file" name="file.upload" multiple />
								</@form.labelled_row>
								<@f.errors path="file" cssClass="error" />
							</td>
						</tr>
					</table>
					<div class="submit-buttons">
						<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
					</div>
				</@f.form>
			</div>
			<div class="tab-pane " id="webform">
				<#if showAddButton>
					<p>
						Click the add button below to enter marks for a student.
					</p>

					<table class="hide">
						<tbody class="row-markup">
						<tr class="mark-row">
							<#if seatNumberMap?has_content>
								<td></td>
							</#if>
							<td>
								<div class="input-prepend input-append">
									<span class="add-on"><i class="icon-user"></i></span>
									<input class="universityId span2" name="universityId" type="text" />
								</div>
							</td>
							<td>
								<div class="input-append">
									<input name="actualMark" type="text" />
									<span class="add-on">%</span>
								</div>
							<td>
								<input class="grade input-small" name="actualGrade" type="text"/>
								<#if isGradeValidation>
									<select name="actualGrade" class="input-small" disabled style="display:none;"></select>
								</#if>
							</td>
						</tr>
						</tbody>
					</table>
				</#if>
				<@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="${formUrl}" commandName="${commandName}">
					<div class="fix-area">
						<input name="isfile" value="false" type="hidden"/>
						<table class="marksUploadTable">
							<tr class="mark-header">
								<#if seatNumberMap?has_content>
									<th>Seat order</th>
								</#if>
								<th>University ID</th>
								<#if studentMarkerMap?has_content>
									<th>Marker</th>
								</#if>
								<th>Marks</th>
								<th>Grade <#if isGradeValidation><@fmt.help_popover id="auto-grade-help" content="The grade is automatically calculated from the SITS mark scheme" /></#if></th>
							</tr>
							<#if marksToDisplay??>
								<#list marksToDisplay as markItem>
									<#if markItem.universityId??>
										<tr class="mark-row">
											<#if seatNumberMap?has_content>
												<#if mapGet(seatNumberMap, markItem.usercode)??>
													<td>${mapGet(seatNumberMap, markItem.usercode)}</td>
												<#else>
													<td></td>
												</#if>
											</#if>
											<td>
												<div class="input-prepend input-append">
													<span class="add-on"><i class="icon-user"></i></span>
													<input class="universityId span2" value="${markItem.universityId}" name="marks[${markItem_index}].universityId" type="text" readonly="readonly" />
												</div>
											</td>
											<#if studentMarkerMap?has_content>
												<#if mapGet(studentMarkerMap, markItem.usercode)??>
													<td>${mapGet(studentMarkerMap, markItem.usercode)}</td>
												<#else>
													<td></td>
												</#if>
											</#if>
											<td>
												<div class="input-append">
													<input name="marks[${markItem_index}].actualMark" class="mark" value="<#if markItem.actualMark??>${markItem.actualMark}</#if>" type="text" />
													<span class="add-on">%</span>
												</div>
											</td>
											<td>
												<input name="marks[${markItem_index}].actualGrade" class="grade input-small" value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" />
												<#if isGradeValidation>
													<select name="marks[${markItem_index}].actualGrade" class="input-small" disabled style="display:none;"></select>
												</#if>
											</td>
										</tr>
									</#if>
								</#list>
							</#if>
						</table>
						<#if showAddButton>
							<br /><button class="add-additional-marks btn"><i class="icon-plus"></i> Add</button>
						</#if>
						<div class="submit-buttons fix-footer">
							<input type="submit" class="btn btn-primary" value="Save">
							or <a href="${cancelUrl}" class="btn">Cancel</a>
						</div>
					</div>
				</@f.form>
			</div>
		</div>
	</div>

	<script>
		jQuery(function($){
			$('.fix-area').fixHeaderFooter();
			// Fire a resize to get the fixed button in the right place
			$('.webform-tab').on('shown', function(){
				$(window).trigger('resize');
			});

			if (${isGradeValidation?string('true','false')}) {
				var currentRequest = null, doIndividualRequest = function() {
					if (currentRequest != null) {
						currentRequest.abort();
					}
					var data = {'studentMarks': {}, 'selected': {}}
						, $this = $(this)
						, $markRow = $this.closest('tr')
						, $gradeInput = $markRow.find('input.grade')
						, $select = $markRow.find('select')
						, universityId = $markRow.find('input.universityId').val();
					data['studentMarks'][universityId] = $this.val();
					if ($select.is(':visible') || $gradeInput.val().length > 0) {
						data['selected'] = {};
						data['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
					}
					currentRequest = $.ajax('${generateUrl}',{
						'type': 'POST',
						'data': data,
						success: function(data) {
							$select.html(data);
							if ($select.find('option').length > 1 || $this.val() == "") {
								$gradeInput.hide().prop('disabled', true);
								$select.prop('disabled', false).show()
							} else {
								$gradeInput.show().prop('disabled', false);
								$select.prop('disabled', true).hide();
							}
						}, error: function(xhr, errorText){
							if (errorText != "abort") {
								$gradeInput.show().prop('disabled', false);
							}
						}
					});
				};

				$('.marksUploadTable').on('keyup', 'input[name*="actualMark"]', doIndividualRequest).on('tableFormNewRow', function(){
					// Make sure all the selects have the correct name
					$('.marksUploadTable .mark-row select').each(function(){
						$(this).prop('name', $(this).closest('td').find('input').prop('name'));
					});
				});

				var currentData = {'studentMarks': {}, 'selected': {}};
				var $markRows = $('.marksUploadTable .mark-row').each(function(){
					var $markRow = $(this)
						, universityId = $markRow.find('input.universityId').val()
						, $select = $markRow.find('select')
						, $gradeInput = $markRow.find('input.grade');
					currentData['studentMarks'][universityId] = $markRow.find('input[name*="actualMark"]').val();
					currentData['selected'][universityId] = ($select.is(':visible')) ? $select.val() : $gradeInput.val();
					$gradeInput.hide();
				});
				$.ajax('${generateUrl}/multiple',{
					'type': 'POST',
					'data': currentData,
					success: function(data) {
						var $selects = $(data);
						$markRows.each(function(){
							var $markRow = $(this)
								, universityId = $markRow.find('input.universityId').val()
								, $thisSelect = $selects.find('select').filter(function(){
									return $(this).data('universityid') == universityId;
								});
							if ($thisSelect.length > 0 && ($thisSelect.find('option').length > 1 || $markRow.find('input.mark').val() == "")) {
								$markRow.find('input.grade').hide().prop('disabled', true);
								$markRow.find('select').html($thisSelect.html()).prop('disabled', false).show();
							} else {
								$markRow.find('input.grade').show().prop('disabled', false);
								$markRow.find('select').prop('disabled', true).hide();
							}

						});
					}
				});
			}
		});
	</script>
</#macro>

<#macro feedbackGradeValidation isGradeValidation gradeValidation>
	<#local gradeValidationClass><#compress>
		<#if isGradeValidation>
			<#if gradeValidation.invalid?has_content || gradeValidation.zero?has_content>error<#elseif gradeValidation.populated?has_content>info</#if>
		<#else>
			<#if gradeValidation.populated?has_content || gradeValidation.invalid?has_content>error</#if>
		</#if>
	</#compress></#local>

	<#if gradeValidation.populated?has_content || gradeValidation.invalid?has_content || gradeValidation.zero?has_content>
		<#if isGradeValidation>
			<div class="grade-validation alert alert-${gradeValidationClass}" style="display:none;">
				<#if gradeValidation.invalid?has_content>
					<#local total = gradeValidation.invalid?keys?size />
					<p>
						<a href="#grade-validation-invalid-modal" data-toggle="modal"><@fmt.p total "student" /></a>
						<#if total==1>
							has feedback with a grade that is invalid. It will not be uploaded.
						<#else>
							have feedback with grades that are invalid. They will not be uploaded.
						</#if>
					</p>
				</#if>
				<#if gradeValidation.zero?has_content>
					<#local total = gradeValidation.zero?keys?size />
					<p>
						<a href="#grade-validation-zero-modal" data-toggle="modal"><@fmt.p total "student" /></a>
						<#if total==1>
							has feedback with a mark of zero and no grade. Zero marks are not populated with a default grade and it will not be uploaded.
						<#else>
							have feedback with marks of zero and no grades. Zero marks are not populated with a default grade and they will not be uploaded.
						</#if>
					</p>
				</#if>
				<#if gradeValidation.populated?has_content>
					<#local total = gradeValidation.populated?keys?size />
					<p>
						<a href="#grade-validation-populated-modal" data-toggle="modal"><@fmt.p total "student" /></a>
						<#if total==1>
							has feedback with a grade that is empty. It will be populated with a default grade.
						<#else>
							have feedback with grades that are empty. They will be populated with a default grade.
						</#if>
					</p>
				</#if>
			</div>
			<div id="grade-validation-invalid-modal" class="modal hide fade">
				<@modal.header>
					<h2>Students with invalid grades</h2>
				</@modal.header>
				<@modal.body>
					<table class="table table-condensed table-bordered table-striped table-hover">
						<thead><tr><th>University ID</th><th>Mark</th><th>Grade</th><th>Valid grades</th></tr></thead>
						<tbody>
							<#list gradeValidation.invalid?keys as feedback>
								<tr>
									<td>${feedback.studentIdentifier}</td>
									<td>${(feedback.latestMark)!}</td>
									<td>${(feedback.latestGrade)!}</td>
									<td>${mapGet(gradeValidation.invalid, feedback)}</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</@modal.body>
			</div>
			<div id="grade-validation-zero-modal" class="modal hide fade">
				<@modal.header>
					<h2>Students with zero marks and empty grades</h2>
				</@modal.header>
				<@modal.body>
					<table class="table table-condensed table-bordered table-striped table-hover">
						<thead><tr><th>University ID</th><th>Mark</th><th>Grade</th></tr></thead>
						<tbody>
							<#list gradeValidation.zero?keys as feedback>
							<tr>
								<td>${feedback.studentIdentifier}</td>
								<td>${(feedback.latestMark)!}</td>
								<td>${(feedback.latestGrade)!}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</@modal.body>
			</div>
			<div id="grade-validation-populated-modal" class="modal hide fade">
				<@modal.header>
					<h2>Students with empty grades</h2>
				</@modal.header>
				<@modal.body>
					<table class="table table-condensed table-bordered table-striped table-hover">
						<thead><tr><th>University ID</th><th>Mark</th><th>Populated grade</th></tr></thead>
						<tbody>
							<#list gradeValidation.populated?keys as feedback>
							<tr>
								<td>${feedback.studentIdentifier}</td>
								<td>${(feedback.latestMark)!}</td>
								<td>${mapGet(gradeValidation.populated, feedback)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</@modal.body>
			</div>
		<#else>
			<div class="grade-validation alert alert-${gradeValidationClass}" style="display:none;">
				<#local total = gradeValidation.populated?keys?size + gradeValidation.invalid?keys?size />
				<a href="#grade-validation-modal" data-toggle="modal"><@fmt.p total "student" /></a>
				<#if total==1>
					has feedback with a grade that is empty or invalid. It will not be uploaded.
				<#else>
					have feedback with grades that are empty or invalid. They will not be uploaded.
				</#if>
			</div>
			<div id="grade-validation-modal" class="modal hide fade">
				<@modal.header>
					<h2>Students with empty or invalid grades</h2>
				</@modal.header>
				<@modal.body>
					<table class="table table-condensed table-bordered table-striped table-hover">
						<thead><tr><th>University ID</th><th>Mark</th><th>Grade</th><th>Valid grades</th></tr></thead>
						<tbody>
							<#list gradeValidation.populated?keys as feedback>
								<tr>
									<td>${feedback.studentIdentifier}</td>
									<td>${(feedback.latestMark)!}</td>
									<td></td>
									<td></td>
								</tr>
							</#list>
							<#list gradeValidation.invalid?keys as feedback>
								<tr>
									<td>${feedback.studentIdentifier}</td>
									<td>${(feedback.latestMark)!}</td>
									<td>${(feedback.latestGrade)!}</td>
									<td>${mapGet(gradeValidation.invalid, feedback)}</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</@modal.body>
			</div>
		</#if>
	</#if>
	<script>
		jQuery(function($){
			$('#sendToSits').on('change', function(){
				var $validationDiv = $('.grade-validation');
				if ($(this).is(':checked') && ($validationDiv.hasClass('alert-info') || $validationDiv.hasClass('alert-error'))) {
					$validationDiv.show();
				} else {
					$validationDiv.hide();
				}
			});
		});
	</script>
</#macro>

<#macro uploadToSits withValidation assignment verb isGradeValidation=false gradeValidation="">
	<div class="alert alert-info">
		<label class="checkbox">
			<@f.checkbox path="sendToSits" id="sendToSits" />
			Queue these marks for upload to SITS
		</label>
		<#if assignment.module.adminDepartment.canUploadMarksToSitsForYear(assignment.academicYear, assignment.module)>
			<div>
				<p>${verb} this feedback will cause marks to be queued for upload to SITS.</p>
				<p>Marks and grades will automatically be uploaded and displayed in the SITS SAT screen as actual marks and grades.</p>
			</div>
		<#else>
			<div class="alert alert-warning">
				<p>${verb} this feedback will cause marks to be queued for upload to SITS.</p>
				<p>
					However mark upload is closed for ${assignment.module.adminDepartment.name} <#if assignment.module.degreeType??> (${assignment.module.degreeType.toString})</#if>
					for the academic year ${assignment.academicYear.toString}.
				</p>
				<p>
					If you still have marks to upload, please contact the Exams Office <a id="email-support-link" href="mailto:aoexams@warwick.ac.uk">aoexams@warwick.ac.uk</a>.
				</p>
				<p>
					As soon as mark upload is re-opened for this department,
					the marks and grades will automatically be uploaded and displayed in the SITS SAT screen as actual marks and grades
				</p>
			</div>
		</#if>
	</div>

	<#if withValidation>
		<@feedbackGradeValidation isGradeValidation gradeValidation />
	</#if>
</#macro>

</#escape>