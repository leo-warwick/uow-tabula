<div class="marking-and-feedback"<#if command.stage.populateWithPreviousFeedback && !command.currentMarkerFeedback.hasBeenModified> style="display:none;"</#if>>
  <h4>Marking and feedback</h4>
  <#assign actionUrl><@routes.cm2.markerOnlineFeedback command.assignment command.stage command.marker command.student /></#assign>
  <@f.form method="post" enctype="multipart/form-data" modelAttribute="command" studentid="${command.student.userId}" action=actionUrl cssClass="dirty-check double-submit-protection ajax-form">

    <@f.errors cssClass="error form-errors" />

    <#list command.assignment.feedbackFields as field>
      <#assign showHelpText = true>
      <#include "/WEB-INF/freemarker/cm2/submit/formfields/${field.template}.ftl">
    </#list>

    <#if assignment.collectMarks>
      <@marking_macros.markField assignment command />

      <#if isGradeValidation>
        <#assign generateUrl><@routes.cm2.generateGradesForMarks command.assignment /></#assign>
        <@marking_macros.autoGradeOnline "grade" "Grade" "mark" marking_macros.extractId(command.student) generateUrl />
      <#else>
        <@bs3form.labelled_form_group path="grade" labelText="Grade">
          <div class="input-group">
            <@f.input path="grade" cssClass="form-control" />
          </div>
        </@bs3form.labelled_form_group>
      </#if>
    </#if>

    <#if command.attachedFiles?has_content >
      <@bs3form.labelled_form_group path="attachedFiles" labelText="Attached files">
        <ul class="list-unstyled attachments">
          <#list command.attachedFiles as attachment>
            <li id="attachment-${attachment.id}" class="attachment">
              <span>${attachment.name}</span>&nbsp;<a href="#" class="remove-attachment">Remove</a>
              <@f.hidden path="attachedFiles" value="${attachment.id}" />
            </li>
          </#list>
        </ul>
      </@bs3form.labelled_form_group>
    <#else>
    <#-- Add invisible empty row for populating in case of copying files from a feedback further back in the workflow -->
      <@bs3form.labelled_form_group cssClass="hide" path="attachedFiles" labelText="Attached files">
        <ul class="list-unstyled attachments"></ul>
      </@bs3form.labelled_form_group>
    </#if>

    <@bs3form.labelled_form_group path="file.upload" labelText="Attachments">
      <input type="file" name="file.upload" multiple />
      <div id="multifile-column-description" class="help-block">
        <#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
      </div>
    </@bs3form.labelled_form_group>

    <div class="buttons form-group">
      <button type="submit" class="btn btn-primary">Save</button>
      <a class="btn btn-default reset" href="#">Reset</a>
      <a class="btn btn-default cancel" href="#">Cancel</a>
    </div>

  </@f.form>
</div>