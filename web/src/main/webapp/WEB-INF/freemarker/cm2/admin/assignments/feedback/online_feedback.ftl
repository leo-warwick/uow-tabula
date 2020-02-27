<#import "*/marking_macros.ftl" as marking />

<#escape x as x?html>
<div class="online-marking">
  <#if command.assignment.collectSubmissions>
    <div class="details">
      <ul class="nav nav-tabs" role="tablist">
        <li role="presentation" class="active">
          <a href="#${student.userId}submission" aria-controls="${student.userId}submission" role="tab" data-toggle="tab">Submission details</a>
        </li>
      </ul>
      <div class="tab-content">
        <div role="tabpanel" class="tab-pane active" id="${student.userId}submission">
          <#include "_submission.ftl" />
        </div>
      </div>
    </div>
  </#if>

  <div class="marking-and-feedback">
    <h4>Marking and feedback</h4>
    <#assign actionUrl><@routes.cm2.onlinefeedbackform command.assignment command.student /></#assign>
    <@f.form method="post" enctype="multipart/form-data" modelAttribute="command" action=actionUrl cssClass="dirty-check double-submit-protection ajax-form">

      <@f.errors cssClass="error form-errors" />

      <#list command.assignment.feedbackFields as field>
        <#assign showHelpText = true>
        <#include "/WEB-INF/freemarker/cm2/submit/formfields/${field.template}.ftl">
      </#list>

      <#if assignment.collectMarks>
        <@marking.markField assignment command />

        <#if isGradeValidation>
          <#assign generateUrl><@routes.cm2.generateGradesForMarks command.assignment /></#assign>
          <@marking.autoGradeOnline "grade" "Grade" "mark" marking.extractId(command.student) generateUrl />
        <#else>
          <@bs3form.labelled_form_group path="grade" labelText="Grade">
            <div class="input-group">
              <@f.input path="grade" cssClass="form-control" />
            </div>
          </@bs3form.labelled_form_group>
        </#if>
      </#if>

      <#if command.attachedFiles?has_content>
        <#function render_attachment attachment>
          <#local result><@routes.cm2.feedbackSingleFileDownload command.feedback attachment /></#local>
          <#return result />
        </#function>
        <@bs3form.attachmentsList
          path="attachedFiles"
          labelText="Attached files"
          attachedFiles=command.attachedFiles
          routeFunction=render_attachment
          confirmModal=false
        />
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
</div>
</#escape>
