<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<@f.form method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/marker/${marker.warwickId}/feedback')}" commandName="addMarkerFeedbackCommand">
<input type="hidden" name="batch" value="true">
<h1>Submit feedback for ${assignment.name}</h1>
<p>
	The feedback filenames need to contain the student's University ID
	(e.g. <span class="example-filename">0123456 feedback.doc</span>) in order to match each
	file with the correct student. If you are using a Zip file, you could also put each file inside
	a folder whose name includes the University ID
	(e.g. <span class="example-filename">0123456/feedback.doc</span>).
</p>
<#if features.feedbackTemplates && assignment.hasFeedbackTemplate>
	<p>
		You can <a  href="<@url page='/coursework/admin/module/${assignment.module.code}/assignments/${assignment.id}/marker-templates.zip'/>">download a zip</a>
		containing copies of this assignments feedback template for each student with the university IDs added for you.
	</p>
</#if>
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
		</td>
		<td id="zip-column">
			<h3>&hellip;or use a Zip file <img src="/static/images/icons/package-x-generic.png" alt="" style="margin-bottom: -8px"></h3>
			<p>
				You can pack the files into a Zip file if you prefer, or if your
				browser doesn't support uploading multiple files at once.
			</p>
			<@form.labelled_row "archive" "Zip archive">
				<input type="file" name="archive" />
			</@form.labelled_row>
		</td>
	</tr>
</table>
<div class="submit-buttons">
	<button class="btn btn-primary btn-large"><i class="icon-upload icon-white"></i> Upload</button>
</div>
</@f.form>
</#escape>
