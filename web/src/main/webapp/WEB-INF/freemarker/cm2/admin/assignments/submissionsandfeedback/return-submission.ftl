<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
  <#assign formAction><@routes.cm2.returnToMarker assignment /></#assign>

  <@f.form method="post" action="${formAction}" modelAttribute="command">

    <@cm2.assignmentHeader "Return students to markers" assignment "for" />

    <@bs3form.errors path="" />
    <input type="hidden" name="confirmScreen" value="true" />

    <#if command.published?has_content>
      <div class="alert alert-info">
        <a class="clickable" id="invalid-students"> <@fmt.p (command.published?size ) "student" /></a> could not be returned for marking.
      </div>

      <div id="invalid-students-content" class="hide">
        <p>Feedback published</p>
        <ul><#list command.published as student>
            <li>${student}</li></#list></ul>
      </div>
      <script type="text/javascript" nonce="${nonce()}">
        jQuery(function ($) {
          $("#invalid-students").popover({
            html: true,
            content: function () {
              return $('#invalid-students-content').html();
            },
            title: 'Could not return the following students for marking'
          });
        });
      </script>
    </#if>

    <#if command.students?size - command.published?size != 0>

      <@spring.bind path="students">
        <#assign students = status.actualValue />
        <p>
          Returning <strong><@fmt.p (students?size - command.published?size ) "student" /></strong> to markers.
        </p>
        <#list students as usercode><input type="hidden" name="students" value="${usercode}" /></#list>
      </@spring.bind>

      <@bs3form.labelled_form_group path="targetStages" labelText="Return to">
        <@f.select path="targetStages" cssClass="form-control" multiple="false">
          <option value="" disabled selected></option>
          <#list allStages as stage>
            <option value="${stage.name}">${stage.description}</option>
          </#list>
        </@f.select>
      </@bs3form.labelled_form_group>

      <@bs3form.form_group>
        <@bs3form.checkbox path="confirm">
          <@f.checkbox path="confirm" />
          <#if ((students?size - command.published?size) > 1)>
            I confirm that I want to return these students to markers.
          <#else>
            I confirm that I want to return this student to the marker.
          </#if>
        </@bs3form.checkbox>
      </@bs3form.form_group>

      <div class="buttons">
        <input class="btn btn-primary" type="submit" value="Confirm">
        <a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
      </div>

    <#else>
      <div class="alert alert-info">
        None of the students that you selected can be returned for marking. <a href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Go back to the
          assignment summary.</a>
      </div>
    </#if>

  </@f.form>
</#escape>