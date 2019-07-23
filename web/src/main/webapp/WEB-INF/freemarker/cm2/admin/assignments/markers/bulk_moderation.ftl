<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#escape x as x?html>
  <@modal.wrapper>
    <@modal.header>
      <h3 class="modal-title">Bulk moderation</h3>
    </@modal.header>
    <@f.form method="POST" modelAttribute="command" >
      <@modal.body>

        <@bs3form.labelled_form_group path="previousMarker" labelText="Moderate all submissions marked by">
          <@f.select path="previousMarker" id="previousMarker" cssClass="form-control">
            <@f.option value="" label=""/>
            <@f.options items=previousMarkers itemLabel="fullName" itemValue="userId" />
          </@f.select>
        </@bs3form.labelled_form_group>
        <@bs3form.labelled_form_group  labelText="Moderate submissions">
          <div class="form-inline">
            <@bs3form.form_group path="direction">
              <@f.select path="direction" id="direction" cssClass="form-control">
                <@f.options items=directions itemLabel="name" itemValue="name" />
              </@f.select>
            </@bs3form.form_group>
            <@bs3form.form_group path="adjustment">
              <@f.input path="adjustment" cssClass="form-control" />
            </@bs3form.form_group>
            <@bs3form.form_group path="adjustmentType">
              <@f.select path="adjustmentType" id="adjustmentType" cssClass="form-control">
                <@f.options items=adjustmentTypes itemLabel="description" itemValue="name" />
              </@f.select>
            </@bs3form.form_group>
          </div>
          <div class="help-block">
            <@bs3form.errors path="adjustment"/>
            Decimals are rounded up.
          </div>
        </@bs3form.labelled_form_group>

        <#list previousMarkers as marker>
          <#assign validForAdjustment = mapGet(command.validForAdjustment, marker)![]/>
          <div style="display:none;" class="marker-specific marker-${marker.userId}">
            <p>
              Marks for <@fmt.p validForAdjustment?size "student" /> will change. This includes any submissions that:
            </p>
            <ul>
              <li>are already moderated</li>
              <li>are not moderated</li>
              <li>are not selected for moderation</li>
            </ul>
            <p>
              Unmarked submissions allocated to ${marker.fullName} will not change.
            </p>
            <#assign skipReasons = mapGet(command.skipReasons, marker)/>
            <#if skipReasons?has_content>
              <p>The following students are not included in the bulk moderation:</p>
              <ul>
                <#list skipReasons?keys as student>
                  <#assign reasons = mapGet(skipReasons, student) />
                  <li>${student.warwickId} - ${reasons?join(", ")}</li>
                </#list>
              </ul>
            </#if>
          </div>
        </#list>
      </@modal.body>
      <@modal.footer>
        <button type="submit" class="btn btn-primary">Submit</button>
        <button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
      </@modal.footer>
    </@f.form>
  </@modal.wrapper>
  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      "use strict";
      $('#previousMarker').on('change', function () {
        $('.marker-specific').hide();
        var marker = $(this).val();
        $('.marker-' + marker).show()
      }).trigger('change');
    })(jQuery);
  </script>

</#escape>