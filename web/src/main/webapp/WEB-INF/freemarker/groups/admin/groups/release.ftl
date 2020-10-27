<#escape x as x?html>
  <#import "*/modal_macros.ftlh" as modal />
  <#assign smallGroupSet = releaseGroupSetCommand.singleGroupToPublish/>
  <#assign submitAction><@routes.groups.releaseset smallGroupSet /></#assign>
  <#if smallGroupSet.releasedToStudents >
    <#assign studentReadonly="disabled"/>
  </#if>

  <@modal.wrapper>
    <@f.form method="post" action="${submitAction}" modelAttribute="releaseGroupSetCommand" cssClass="double-submit-protection">

      <@modal.header>
        <h3 class="modal-title">Publish</h3>
      </@modal.header>

      <@modal.body>
        <p>Publish allocations for ${smallGroupSet.name} and show them in Tabula to:</p>

        <@bs3form.checkbox "notifyStudents">
          <@f.checkbox path="notifyStudents" disabled=smallGroupSet.releasedToStudents />
          <span tabindex="0" <#if smallGroupSet.releasedToStudents>class='disabled use-tooltip' title='Already published'</#if>>Students</span>
        </@bs3form.checkbox>

        <@bs3form.checkbox "notifyTutors">
          <@f.checkbox path="notifyTutors" disabled=smallGroupSet.releasedToTutors />
          <span <#if smallGroupSet.releasedToTutors>class='disabled use-tooltip' title='Already published to tutors'</#if>>Tutors</span>
        </@bs3form.checkbox>

        <p class="help-block">
          By default, a lecture event is not displayed to students because it may duplicate a Syllabus+ whole cohort event. You can still select the <strong>Students</strong>
          checkbox and publish the lecture event to students' timetables e.g. when there is no Syllabus+ event.
        </p>

        <hr>

        <@bs3form.checkbox "sendEmail">
          <@f.checkbox path="sendEmail" />
          Send an email about this and any future changes to group allocation
        </@bs3form.checkbox>
      </@modal.body>

      <@modal.footer>
        <div class="submit-buttons">
          <input class="btn btn-primary spinnable spinner-auto" type="submit" value="Publish" data-loading-text="Loading&hellip;">
          <button class="btn btn-default" data-dismiss="modal">Cancel</button>
        </div>
      </@modal.footer>
    </@f.form>
  </@modal.wrapper>
</#escape>
