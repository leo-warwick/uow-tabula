<#escape x as x?html>
<h1>Upload bulk adjustment for ${assignment.name}</h1>

<p>
	Upload marks in a spreadsheet, which must be saved as an .xlsx file (ie created in Microsoft Office 2007 or later).
	The spreadsheet should have at least three column headings in the following order: <b>${StudentIdHeader}, ${MarkHeader}, ${GradeHeader}</b>.
	You can use this <a href="<@routes.coursework.feedbackBulkAdjustmentTemplate assignment />" >generated spreadsheet</a> as a template.
	Note that you can upload just marks, or both marks and grades.
</p>
	<#assign formUrl><@routes.coursework.feedbackBulkAdjustment assignment /></#assign>
	<@f.form method="post" enctype="multipart/form-data" action="${formUrl}" commandName="command"class="form-horizontal">
		<input name="isfile" value="true" type="hidden"/>
		<table role="presentation" class="narrowed-form">
			<tr>
				<td id="multifile-column">
					<h3>Select file</h3>
					<@form.labelled_row "file.upload" "File">
						<input type="file" name="file.upload" />
					</@form.labelled_row>
					<@f.errors path="file" cssClass="error" />
				</td>
			</tr>
		</table>
		<div class="submit-buttons">
			<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
		</div>
	</@f.form>
</#escape>