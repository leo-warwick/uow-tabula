<#escape x as x?html>

<h1>Submit year marks</h1>

<ul id="marks-tabs" class="nav nav-tabs">
	<li class="active"><a href="#upload">Upload</a></li>
	<li class="webform-tab"><a href="#webform">Web Form</a></li>
</ul>

<#assign formUrl><@routes.exams.uploadYearMarks department academicYear /></#assign>

<div class="tab-content">

	<div class="tab-pane active" id="upload">
		<p>
			You can upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
		</p>
		<p>
			The spreadsheet should have at least two column headings. This first should be <b>Student ID</b> and the second should be <b>Mark</b>.
			The Student ID can be a University ID or an SCJ code.
		</p>
		<p>
			You can also add a column named <b>Academic year</b> to upload marks for the same student in multiple years.
			Any students without an academic year specified will default to <b>${academicYear.toString}</b>.
		</p>

		<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command">
			<input name="isfile" value="true" type="hidden"/>
			<h3>Select file</h3>
			<p id="multifile-column-description">
				<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
			</p>
			<@bs3form.labelled_form_group "file.upload" "Files">
				<input type="file" name="file.upload" multiple />
			</@bs3form.labelled_form_group>
			<div class="submit-buttons">
				<button class="btn btn-primary btn-lg">Upload</button>
			</div>
		</@f.form>
	</div>

	<div class="tab-pane " id="webform">
		<p>Click the add button below to enter marks for a student.</p>

		<div class="row-markup hide">
			<div class="mark-row row form-group">
				<div class="col-md-3">
					<input class="universityId form-control" name="studentId" type="text" />
				</div>
				<div class="col-md-2">
					<div class="input-group">
						<input name="mark" type="text" class="form-control"/>
						<span class="input-group-addon">%</span>
					</div>
				</div>
			</div>
		</div>

		<@f.form id="marks-web-form" method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command">
			<div class="fix-area">
				<input name="isfile" value="false" type="hidden"/>
				<div class="marksUpload">
					<div class="mark-header form-group clearfix">
						<div class="col-md-3">University ID or SCJ code</div>
						<div class="col-md-2">Marks</div>
					</div>
					<#if marksToDisplay??>
						<#list marksToDisplay as markItem>
							<div class="mark-row row form-group clearfix">
								<div class="col-md-3">
									<input class="universityId form-control" value="${markItem.universityId}" name="marks[${markItem_index}].studentId" type="text" readonly="readonly" />
								</div>
								<div class="col-md-2">
									<div class="input-group">
										<input class="form-control" name="marks[${markItem_index}].mark" value="<#if markItem.mark??>${markItem.mark}</#if>" type="text" />
										<span class="input-group-addon">%</span>
									</div>
								</div>
							</div>
						</#list>
					</#if>
				</div>
				<p><button class="add-additional-marks btn btn-default" type="button">Add</button></p>

				<div class="fix-footer">
					<input type="submit" class="btn btn-primary" value="Save">
					or <a href="<@routes.exams.gridsDepartmentHomeForYear department academicYear />" class="btn btn-default">Cancel</a>
				</div>
			</div>
		</@f.form>
	</div>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
		// Fire a resize to get the fixed button in the right place
		$('.webform-tab').on('shown.bs.tab', function(){
			$(window).trigger('resize');
		});

		$('#marks-web-form').tableForm({
			addButtonClass: 'add-additional-marks',
			headerClass: 'mark-header',
			rowClass: 'mark-row',
			tableClass: 'marksUpload',
			listVariable: 'marks'
		});
	});
</script>

</#escape>