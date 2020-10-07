<#ftl strip_text=true />

<#-- FIXME why is this necessary? -->
<#if JspTaglibs??>
  <#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
  <#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
</#if>

<#import "*/modal_macros.ftlh" as modal />

<#macro moduleMarkStatusIcon student showOutOfSync=true>
  <#if student.needsWritingToSits>
    <span tabindex="0" class="tabula-tooltip" data-title="Waiting to be written to SITS">
      <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
      <span class="sr-only">Waiting to be written to SITS</span>
    </span>
  <#elseif student.sitsWriteError??>
    <span tabindex="0" class="tabula-tooltip" data-title="Error writing mark to SITS: ${student.sitsWriteError.description}">
      <i class="fa-fw fas fa-exclamation-square text-danger" aria-hidden="true"></i>
      <span class="sr-only">Error writing mark to SITS: ${student.sitsWriteError.description}</span>
    </span>
  <#elseif student.outOfSync && showOutOfSync>
    <span tabindex="0" class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
      <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
      <span class="sr-only">Mark is out of sync with current mark in SITS</span>
    </span>
  <#elseif student.agreed>
    <span tabindex="0" class="tabula-tooltip" data-title="Agreed mark published to student">
      <span class="label label-success">Agreed</span>
      <span class="sr-only">Agreed mark published to student</span>
    </span>
  </#if>
</#macro>

<#macro componentMarkStatusIcon marks showOutOfSync=true>
  <#if marks.needsWritingToSits>
    <span tabindex="0" class="tabula-tooltip" data-title="Waiting to be written to SITS">
      <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
      <span class="sr-only">Waiting to be written to SITS</span>
    </span>
  <#elseif marks.sitsWriteError??>
    <span tabindex="0" class="tabula-tooltip" data-title="Error writing mark to SITS: ${marks.sitsWriteError.description}">
      <i class="fa-fw fas fa-exclamation-square text-danger" aria-hidden="true"></i>
      <span class="sr-only">Error writing mark to SITS: ${marks.sitsWriteError.description}</span>
    </span>
  <#elseif marks.outOfSync && showOutOfSync>
    <span tabindex="0" class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
      <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
      <span class="sr-only">Mark is out of sync with current mark in SITS</span>
    </span>
  <#elseif marks.agreed>
    <span tabindex="0" class="tabula-tooltip" data-title="Agreed mark published to student">
      <span class="label label-success">Agreed</span>
      <span class="sr-only">Agreed mark published to student</span>
    </span>
  </#if>

  <#if marks.reassessment && marks.furtherFirstSit>
    <span class="label label-info">Further first sit</span>
  <#elseif marks.reassessment>
    <span tabindex="0" class="tabula-tooltip" data-title="Resit mark will be capped at the pass mark when calculating the module mark">
      <span class="label label-warning">Resit</span>
      <span class="sr-only">Resit mark will be capped at the pass mark when calculating the module mark</span>
    </span>
  </#if>
</#macro>

<#macro componentMarkStudentStatus marks>
  <#if !(marks.currentMember!false)>
    <span tabindex="0" class="label label-warning tabula-tooltip" data-title="Permanently withdrawn">PWD</span>
  </#if>
  <#if (marks.reassessment!false) && (marks.furtherFirstSit!false)>
    <span class="label label-info">Further first sit</span>
  <#elseif marks.reassessment!false>
    <span tabindex="0" class="tabula-tooltip" data-title="Resit mark will be capped at the pass mark when calculating the module mark">
      <span class="label label-warning">Resit</span>
      <span class="sr-only">Resit mark will be capped at the pass mark when calculating the module mark</span>
    </span>
  </#if>
</#macro>

<#macro moduleMarkHistory student moduleCode fullName>
  <#if student.history?has_content>
    <#local modalId = "mark-history-modal-${student.sprCode?replace('/', '_')}-${moduleCode?replace('.','_')}" />
    <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="View history" data-toggle="modal" data-target="#${modalId}"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>

    <@modal.modal id=modalId>
      <@modal.wrapper cssClass="modal-xl">
        <@modal.header>
          <h3 class="modal-title">Mark history for ${fullName}, ${student.sprCode}</h3>
        </@modal.header>
        <@modal.body>
          <table class="table table-condensed table-striped">
            <thead>
              <tr>
                <th class="col-sm-2">Date</th>
                <th class="col-sm-2">User</th>
                <th class="col-sm-2">Operation</th>
                <th class="col-sm-1">Status</th>
                <th class="col-sm-1">Mark</th>
                <th class="col-sm-1">Grade</th>
                <th class="col-sm-1">Result</th>
                <th class="col-sm-3">Comments</th>
              </tr>
            </thead>
            <tbody>
              <#list student.history as history>
                <tr>
                  <td>
                    <@fmt.date date=history.updatedDate />
                  </td>
                  <td>${history.updatedBy.fullName!(history.updatedBy.userId)}</td>
                  <td>${history.source.description}</td>
                  <td><span class="label label-${history.markState.cssClass}">${history.markState.description}</span></td>
                  <td>${history.mark!'-'}</td>
                  <td><@format_grade history.grade!'-' /></td>
                  <td>${(history.result.description)!'-'}</td>
                  <td>${history.comments!}</td>
                </tr>
              </#list>
            </tbody>
          </table>
        </@modal.body>
        <@modal.footer>
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </@modal.footer>
      </@modal.wrapper>
    </@modal.modal>
  </#if>
</#macro>

<#macro componentMarkHistory marks ac member>
  <#if marks.history?has_content>

    <#local modalId = "mark-history-modal-${marks.universityId}-${ac.moduleCode?replace('.','_')}-${ac.sequence}" />
    <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="View history" data-toggle="modal" data-target="#${modalId}"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>

    <@modal.modal id=modalId>
      <@modal.wrapper cssClass="modal-xl">
        <@modal.header>
          <h3 class="modal-title">Mark history for ${member.fullName}, ${marks.universityId}, ${ac.name}</h3>
        </@modal.header>
        <@modal.body>
          <table class="table table-condensed table-striped">
            <thead>
              <tr>
                <th class="col-sm-2">Date</th>
                <th class="col-sm-2">User</th>
                <th class="col-sm-2">Operation</th>
                <th class="col-sm-1">Status</th>
                <th class="col-sm-1">Mark</th>
                <th class="col-sm-1">Grade</th>
                <th class="col-sm-3">Comments</th>
              </tr>
            </thead>
            <tbody>
              <#list marks.history as history>
                <tr>
                  <td>
                    <@fmt.date date=history.updatedDate />
                  </td>
                  <td>${history.updatedBy.fullName!(history.updatedBy.userId)}</td>
                  <td>${history.source.description}</td>
                  <td><span class="label label-${history.markState.cssClass}">${history.markState.description}</span></td>
                  <td>${history.mark!'-'}</td>
                  <td><@format_grade history.grade!'-' /></td>
                  <td>${history.comments!}</td>
                </tr>
              </#list>
            </tbody>
          </table>
        </@modal.body>
        <@modal.footer>
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </@modal.footer>
      </@modal.wrapper>
    </@modal.modal>
  </#if>
</#macro>

<#macro preview_clear_warning student>
  <#if ((willClearModuleMarksForUniversityId[student.universityId])!{})?has_content>
    <#local moduleMarkRecord = willClearModuleMarksForUniversityId[student.universityId] />

    <#local tooltip>
      The current module mark record will be cleared.
      <#if moduleMarkRecord.mark?has_content>
        Mark: ${moduleMarkRecord.mark!'-'}
      </#if>
      <#if moduleMarkRecord.grade?has_content>
        Grade: ${moduleMarkRecord.grade!'-'}
      </#if>
      <#if moduleMarkRecord.result?has_content>
        Result: ${(moduleMarkRecord.result.description)!'-'}
      </#if>
    </#local>

    <span class="tabula-tooltip text-danger" data-title="${tooltip}">
      <i class="fa-fw fas fa-exclamation-triangle" aria-hidden="true"></i>
      <span class="sr-only">${tooltip}</span>
    </span>
  </#if>
</#macro>

<#macro format_grade grade>
  <#if sitsIndicatorLookup?has_content && grade?has_content && sitsIndicatorLookup?keys?seq_contains(grade)>
    <#local indicator = sitsIndicatorLookup[grade] />
    <span tabindex="0" class="tabula-tooltip" data-title="${indicator.description}">${indicator.grade}</span>
  <#else>
    ${grade!'-'}
  </#if>
</#macro>
