<#escape x as x?html>
  <#import "*/group_components.ftlh" as components />
  <#import "*/csrf_macros.ftl" as csrf_macros />

  <#macro listStudentIdInputs>
    <#list findCommand.staticStudentIds as id>
      <input type="hidden" name="staticStudentIds" value="${id}" />
    </#list>
    <#list findCommand.includedStudentIds as id>
      <input type="hidden" name="includedStudentIds" value="${id}" />
    </#list>
    <#list findCommand.excludedStudentIds as id>
      <input type="hidden" name="excludedStudentIds" value="${id}" />
    </#list>
  </#macro>

  <div class="deptheader">
    <h1>Edit small groups</h1>
    <h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /> <#if smallGroupSet??>(${smallGroupSet.academicYear.toString})</#if></h4>
  </div>

  <#if saved!false>
    <div class="alert alert-info">
      <button type="button" class="close" data-dismiss="alert">&times;</button>
      Students saved for ${smallGroupSet.name}.
    </div>
  </#if>

  <form method="POST">
    <@csrf_macros.csrfHiddenInputField />
    <#assign filterQueryString = findCommand.serializeFilter />
    <input type="hidden" name="filterQueryString" value="${filterQueryString}" />
    <@listStudentIdInputs />

    <@components.set_wizard false 'students' smallGroupSet />

    <div class="fix-area">
      <#include "_selectStudents.ftl" />

      <div class="fix-footer">
        <p style="padding-left: 20px;" class="checkbox">
          <label><#compress>
              <#if SITSInFlux>
                <input type="hidden" name="_linkToSits" value="on" />
                <input type="checkbox" name="linkToSits" disabled />
                Link to SITS
                <#assign popoverContent><#noescape>
                  You can no longer link to SITS for the current academic year,
                  as changes for the forthcoming academic year are being made that will make the students in this small group set inaccurate.
                </#noescape></#assign>
                <a class="use-popover"
                   tabindex="0" role="button" data-trigger="click focus"
                   id="popover-linkToSits"
                   data-content="${popoverContent}"
                   data-html="true" aria-label="Help"
                >
                  <i class="fa fa-question-circle"></i>
                </a>
              <#else>
                <@f.checkbox path="findCommand.linkToSits" />
                Link to SITS
                <#assign popoverContent><#noescape>
                  Select this option to automatically update the filtered list of students from SITS. If you choose not to link to SITS, these students are imported to Tabula as a static list, which does not update when SITS data changes. Therefore, you need to maintain the list yourself &ndash; e.g. when a student withdraws from their course.
                </#noescape></#assign>
                <a class="use-popover"
                   tabindex="0" role="button" data-trigger="click focus"
                   id="popover-linkToSits"
                   data-content="${popoverContent}"
                   data-html="true" aria-label="Help"
                >
                  <i class="fa fa-question-circle"></i>
                </a>
              </#if>
            </#compress></label>
        </p>

        <p>
          <input
                  type="submit"
                  class="btn btn-primary update-only"
                  name="${ManageSmallGroupsMappingParameters.editAndAddEvents}"
                  value="Save"
          />
          <input
                  type="submit"
                  class="btn btn-primary"
                  name="persist"
                  value="Save and exit"
          />
          <a class="btn btn-default dirty-check-ignore" href="<@routes.groups.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
        </p>
      </div>
    </div>
  </form>

</#escape>
