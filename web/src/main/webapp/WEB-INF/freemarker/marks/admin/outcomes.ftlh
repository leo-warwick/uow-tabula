<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/generated_grades_macros.ftlh" as gg />
<#import "/WEB-INF/freemarker/exams/grids/generate/form_fields.ftl" as gf />

<#macro agreed>
  <#assign tooltip>Decision has been agreed. To make changes contact student records.</#assign>
  <span tabindex="0" class="tabula-tooltip" data-title="${tooltip}">
    <span class="label label-success">Agreed</span>
    <span class="sr-only">${tooltip}</span>
  </span>
</#macro>

<#assign courses><#list selectCourseCommand.courses as course >${course.code}<#if course_has_next>, </#if></#list></#assign>
<div class="deptheader">
  <h1>Record outcomes</h1>
  <h4 class="with-related">for ${courses} &ndash; <#if selectCourseCommand.levelCode??>Level ${selectCourseCommand.levelCode}<#else>Block ${selectCourseCommand.yearOfStudy}</#if></h4>
</div>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<div class="fix-area process-module-marks">
  <@f.form method="post" modelAttribute="examBoardOutcomesCommand">
    <table class="table table-striped table-sortable table-checkable">
      <thead>
      <tr>
        <th class="for-check-all" data-sorter="false"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
        <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
        <th class="col-sm-1 sortable" data-sorter="text">First name</th>
        <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
        <th class="col-sm-1 sortable" data-sorter="text">Period</th>
        <th class="col-sm-3 sortable" data-sorter="formfield">Decision</th>
        <th class="col-sm-5 sortable" data-sorter="formfield">Notes</th>
      </tr>
      </thead>
      <tbody>
        <#list entities?keys as sprCode>
          <#assign student = mapGet(entities, sprCode) />
<#--          <#if student.resitting && student.agreedDecision??>-->
<#--            &lt;#&ndash; Show the decision from the summer period &ndash;&gt;-->
<#--            <tr class="student">-->
<#--              <td>-->
<#--                <@agreed />-->
<#--              </td>-->
<#--              <td data-sortby="${sprCode}"> <@pl.profile_link student.universityId /> ${sprCode}</td>-->
<#--              <td>${student.firstName}</td>-->
<#--              <td>${student.lastName}</td>-->
<#--              <td><span class="label label-info">Summer exam period</span></td>-->
<#--              <td>-->
<#--                  ${student.agreedDecision.outcome.description}-->
<#--                  <#if student.agreedDecision.outcome.hasAward && student.studentAwards?has_content>-->
<#--                    <#list student.studentAwards as studentAward>-->
<#--                      <p>${studentAward.award.name}<#if studentAward.classification?has_content> &mdash; ${studentAward.classification.name}</#if></p>-->
<#--                    </#list>-->
<#--                  </#if>-->
<#--              </td>-->
<#--              <td>${student.agreedDecision.notes!""} ${student.agreedDecision.minutes!""}</td>-->
<#--            </tr>-->
<#--          </#if>-->
          <tr class="student">
            <td>
              <#if student.agreed>
                <@agreed />
              <#else>
                <@f.checkbox cssClass="collection-checkbox" path="students[${sprCode}].record" />
                <#if student.existingRecordedDecision?? && student.existingRecordedDecision.needsWritingToSits>
                  <#assign tooltip>Waiting to be written to SITS.</#assign>
                  <span class="tabula-tooltip" data-title="${tooltip}">
                    <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
                    <span class="sr-only">${tooltip}</span>
                  </span>
                </#if>
              </#if>
            </td>
            <td data-sortby="${sprCode}"><@pl.profile_link student.universityId /> ${sprCode}</td>
            <td>${student.firstName}</td>
            <td>${student.lastName}</td>
            <td>
              <#if student.resitting>
                <span class="label label-info">Summer vacation exam period</span>
              <#else>
                <span class="label label-info">Summer exam period</span>
              </#if>
            </td>
            <td>
              <#if student.agreed>
                ${student.progressionDecision.outcome.description}
                <#if student.progressionDecision.outcome.hasAward && student.studentAwards?has_content>
                  <#list student.studentAwards as studentAward>
                    <p>${studentAward.award.name}<#if studentAward.classification?has_content> &mdash; ${studentAward.classification.name}</#if></p>
                  </#list>
                </#if>
              <#else>
                <@bs3form.labelled_form_group path="students[${sprCode}].decision">
                  <@f.select id="students${sprCode?replace('/', '_')}.decision" path="students[${sprCode}].decision" cssClass="form-control">
                    <@f.option value="" />
                    <#list decisions as decision>
                      <#if student.finalist>
                        <#if decision.validForFinalists><@f.option value=decision.entryName label=decision.finalistDescription data\-mandatory\-notes="${decision.notesRequired?c}" data\-notes\-help="${decision.notesHelp!''}" /></#if>
                      <#else>
                        <@f.option value=decision.entryName label=decision.description data\-mandatory\-notes="${decision.notesRequired?c}" data\-notes\-help="${decision.notesHelp!''}" />
                      </#if>
                    </#list>
                  </@f.select>
                </@bs3form.labelled_form_group>
              </#if>
            </td>
            <td>
              <#if student.agreed>
                ${student.progressionDecision.notes!""} ${student.progressionDecision.minutes!""}
              <#else>
                <@bs3form.labelled_form_group path="students[${sprCode}].notes">
                  <@f.textarea id="students${sprCode?replace('/', '_')}.comments" path="students[${sprCode}].notes" cssClass="form-control" rows="1" />
                </@bs3form.labelled_form_group>
              </#if>
            </td>
          </tr>
        </#list>
      </tbody>
    </table>

    <div class="submit-buttons fix-footer">
      <input type="submit" class="btn btn-primary" value="Save" />
      <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome department academicYear />">Cancel</a>
    </div>
  </@f.form>
</div>
