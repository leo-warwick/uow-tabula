<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/generated_grades_macros.ftlh" as gg />
<#import "/WEB-INF/freemarker/exams/grids/generate/form_fields.ftl" as gf />

<#assign courses><#list selectCourseCommand.courses as course >${course.code}<#if course_has_next>, </#if></#list></#assign>
<div class="deptheader">
  <h1>Process marks</h1>
  <h4 class="with-related">for ${courses} &ndash; <#if selectCourseCommand.levelCode??>Level ${selectCourseCommand.levelCode}<#else>Block ${selectCourseCommand.yearOfStudy}</#if></h4>
</div>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<#if changes?size == 0>
  <p>There were no changes to marks, grades or results.</p>
  <p><a href="<@routes.marks.adminhome department academicYear />">Back to marks</a></p>
<#else>
    <#if notificationDepartments?has_content>
      <div class="alert alert-info">
        <#list notificationDepartments?keys as dept>
          <#assign stuIds = mapGet(notificationDepartments, dept) />
          <p>Departmental administrators in <strong>${dept.name}</strong> will receive a notification that the mark has changed for <@fmt.p number=stuIds?size singular="student" shownumber=false/> <#list stuIds as stuId>${stuId}<#if stuId_has_next>, </#if></#list> when you click confirm.</p>
        </#list>
      </div>
    </#if>

    <p>The following marks will become agreed, which you will not be able to change without the involvement of the Exams Office:</p>

    <div class="fix-area">
      <@f.form method="post" modelAttribute="processCohortMarksCommand" cssClass="marks-form">
        <@gf.select_course_fields />
        <@f.hidden name="confirm" value="true" />

        <table class="table table-striped table-sortable">
          <thead>
          <tr>
            <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
            <th class="col-sm-1 sortable" data-sorter="text">First name</th>
            <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
            <th class="col-sm-9"></th>
          </tr>
          </thead>
          <tbody>
          <#list changes?keys as sprCode>
            <#assign student = mapGet(entities, sprCode)._1() />
            <tr>
              <td data-sortby="${sprCode}">
                <@pl.profile_link student.universityId />
                ${sprCode}
              </td>
              <td>${student.firstName}</td>
              <td>${student.lastName}</td>
              <td>
                <table class="table table-striped table-sortable">
                  <thead>
                  <tr>
                    <th class="col-sm-1" data-sortInitialOrder="asc" data-sorter="text">Module</th>
                    <th class="col-sm-2" data-sorter="false">State</th>
                    <th class="col-sm-1" data-sorter="text">Mark</th>
                    <th class="col-sm-1" data-sorter="text">Grade</th>
                    <th class="col-sm-1" data-sorter="text">Result</th>
                    <th class="col-sm-3" data-sorter="false">Comment</th>
                  </tr>
                  </thead>
                  <tbody>
                    <#assign moduleChanges = mapGet(changes, sprCode)/>
                    <#list moduleChanges as mr>
                      <#assign moduleCode = mr.moduleRegistration.sitsModuleCode />
                      <@spring.bind path="students[${sprCode}][${moduleCode}]">
                        <#assign c = status.actualValue />
                      </@spring.bind>
                      <tr>
                        <td data-sortby="${moduleCode}">
                          ${moduleCode}
                          <@f.hidden path="students[${sprCode}][${moduleCode}].process" />
                          <@f.hidden path="students[${sprCode}][${moduleCode}].moduleCode" />
                          <@f.hidden path="students[${sprCode}][${moduleCode}].occurrence" />
                          <@f.hidden path="students[${sprCode}][${moduleCode}].academicYear" />
                          <@components.moduleMarkHistory mr moduleCode student.fullName />
                        </td>
                        <td>
                          <#if mr.markState?has_content>
                            <span class="label label-${mr.markState.cssClass}">${mr.markState.description}</span>
                          <#else>
                            -
                          </#if>
                          <#if (mr.markState.entryName)! != 'Agreed'>
                            <i class="fal fa-arrow-right"></i>
                            <span class="label label-success">Agreed</span>
                          </#if>
                        </td>
                        <td data-sortby="${c.mark!-1}">
                          <@f.hidden path="students[${sprCode}][${moduleCode}].mark" />
                          ${mr.mark!'-'}
                          <#if "${mr.mark!}" != c.mark!>
                            <i class="fal fa-arrow-right"></i>
                            ${c.mark!}
                          </#if>
                        </td>
                        <td data-sortby="g${c.grade!}">
                          <@f.hidden path="students[${sprCode}][${moduleCode}].grade" />
                          <@components.format_grade mr.grade!'-' />
                          <#if mr.grade! != c.grade!>
                            <i class="fal fa-arrow-right"></i>
                            <@components.format_grade c.grade! />
                          </#if>
                        </td>
                        <td data-sortby="${c.result!}">
                          <@f.hidden path="students[${sprCode}][${moduleCode}].result" />
                          ${(mr.result.description)!'-'}
                          <#if (mr.result.dbValue)! != c.result!>
                            <i class="fal fa-arrow-right"></i>
                            <#if c.result?has_content>
                              ${moduleResultsDescriptions[c.result]}
                            </#if>
                          </#if>
                        </td>
                        <td><@f.input path="students[${sprCode}][${moduleCode}].comments" cssClass="form-control" readonly=true tabindex="-1" /></td>
                      </tr>
                    </#list>
                  </tbody>
                </table>
              </td>
            </tr>
          </#list>
          </tbody>
        </table>

        <div class="submit-buttons fix-footer">
          <input type="submit" class="btn btn-primary" value="Process" />
          <a class="btn btn-default dirty-check-ignore" href="">Cancel</a>
        </div>
      </@f.form>
  </div>
</#if>
