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

  <h1>Create a set of reusable small groups: ${smallGroupSet.name}</h1>

  <form method="POST">
    <@csrf_macros.csrfHiddenInputField />
    <#assign filterQueryString = findCommand.serializeFilter />
    <input type="hidden" name="filterQueryString" value="${filterQueryString}" />
    <@listStudentIdInputs />

    <@components.reusable_set_wizard true 'students' smallGroupSet />

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

        <input
                type="submit"
                class="btn btn-primary use-tooltip"
                name="${ManageDepartmentSmallGroupsMappingParameters.createAndAllocate}"
                value="Save and allocate students to groups"
                title="Allocate students to this set of reusable groups"
                data-container="body"
        />
        <input
                type="submit"
                class="btn btn-primary use-tooltip spinnable spinner-auto"
                name="persist"
                value="Save and exit"
                title="Save your groups and add students and groups to it later"
                data-container="body"
        />
        <a class="btn btn-default" href="<@routes.groups.crossmodulegroups smallGroupSet.department smallGroupSet.academicYear />">Cancel</a>
      </div>
    </div>
  </form>

</#escape>
