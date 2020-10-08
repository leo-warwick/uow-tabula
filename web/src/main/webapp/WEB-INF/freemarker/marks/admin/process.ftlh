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

<p>
  Use this form to process and finalise module marks as <strong>agreed</strong> marks, for ratification by the Senate Sub-Group (SSG).
  Students can see their agreed mark, grade and result (but not comments) in their Tabula profile on the release day for their year and level of study.
  The agreed marks are also uploaded to SITS on the <a href="https://warwick.ac.uk/coronavirus/intranet/continuity/teaching/marksandexamboards/guidance/results/#coordinatedrelease" target="_blank">release day</a>,
  showing in Tabula as waiting to be written to SITS until then.
</p>

<p>
  By processing module marks, any component marks recorded will also be agreed. Check the box next to any student you
  wish to agree marks for.
</p>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<div class="fix-area process-module-marks">
  <@f.form method="post" modelAttribute="processCohortMarksCommand" cssClass="marks-form">
      <table class="table table-striped table-sortable table-checkable process-cohort">
        <thead>
        <tr>
          <th class="for-check-all" data-sorter="false"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
          <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
          <th class="col-sm-1 sortable" data-sorter="text">First name</th>
          <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
          <th>Module results</th>
        </tr>
        </thead>
        <tbody>
          <#list entities?keys as sprCode>
            <#assign student = mapGet(entities, sprCode)._1() />
            <#assign moduleRegistrations = mapGet(entities, sprCode)._2() />
            <@spring.bind path="students[${sprCode}]*"><#if status.error><#assign hasErrors=true /><#else><#assign hasErrors=false /></#if></@spring.bind>
            <tr class="student">
              <td>
                <input type="checkbox" class="collection-checkbox process-checkbox" data-student="students[${sprCode}]"/>
              </td>
              <td data-sortby="${sprCode}">
                <#if hasErrors>
                  <span class="tabula-tooltip text-danger" data-title="There is a problem with one or more module results for this student">
                    <i class="fa-fw fas fa-exclamation-triangle" aria-hidden="true"></i>
                    <span class="sr-only">There is a problem with one or more module results for this student</span>
                  </span>
                </#if>
                <@pl.profile_link student.universityId />
                ${sprCode}
              </td>
              <td>${student.firstName}</td>
              <td>${student.lastName}</td>
              <td>
                <table class="table table-condensed">
                  <thead><tr><#list moduleRegistrations as mr>
                    <#assign studentModuleMarkRecord = (mapGet(mapGet(mapGet(recordsByStudent, sprCode), mr.sitsModuleCode), mr.occurrence))?first />
                    <th>
                      ${mr.sitsModuleCode}
                      <@components.moduleMarkStatusIcon studentModuleMarkRecord />
                      <@components.moduleMarkHistory studentModuleMarkRecord mr.sitsModuleCode student.fullName />
                    </th>
                  </#list></tr></thead>
                  <tbody><tr>
                  <#list moduleRegistrations as mr>
                    <#assign moduleCode = mr.sitsModuleCode  />
                    <#assign studentModuleMarkRecord = (mapGet(mapGet(mapGet(recordsByStudent, sprCode), moduleCode), mr.occurrence))?first />
                    <@spring.bind path="students[${sprCode}][${moduleCode}]*"><#if status.error><#assign hasModuleErrors=true /><#else><#assign hasModuleErrors=false /></#if></@spring.bind>
                    <td>
                      <#if hasModuleErrors>
                        <span class="tabula-tooltip text-danger" data-title="There is a problem with the module result for ${moduleCode}. Please edit the result and correct any errors.">
                          <i class="fa-fw fas fa-exclamation-triangle" aria-hidden="true"></i>
                          <span class="sr-only">There is a problem with the module result for ${moduleCode}. Please edit the result and correct any errors.</span>
                        </span
                      </#if>
                      <@f.hidden path="students[${sprCode}][${moduleCode}].process" />
                      <@f.hidden path="students[${sprCode}][${moduleCode}].moduleCode" />
                      <@f.hidden path="students[${sprCode}][${moduleCode}].occurrence" />
                      <@f.hidden path="students[${sprCode}][${moduleCode}].academicYear" />
                      <#if studentModuleMarkRecord.mark??>
                        <span data-updated-by="students${sprCode?replace('/', '_')}_${moduleCode}.mark"><#compress>
                          <@spring.bind path="students[${sprCode}][${moduleCode}].mark">
                            <#if status.actualValue?? && status.actualValue?has_content>${status.actualValue}%<#else>-</#if>
                          </@spring.bind>
                        </#compress></span> &mdash;
                      </#if>
                        <span data-updated-by="students${sprCode?replace('/', '_')}_${moduleCode}.grade"><#compress>
                          <@spring.bind path="students[${sprCode}][${moduleCode}].grade">
                            <#if status.actualValue?? && status.actualValue?has_content>${status.actualValue}<#else>-</#if>
                          </@spring.bind>
                        </#compress></span>
                      <#if studentModuleMarkRecord.result??> &mdash;
                        <span data-updated-by="students${sprCode?replace('/', '_')}_${moduleCode}.result"><#compress>
                          <@spring.bind path="students[${sprCode}][${moduleCode}].result">
                            <#if status.actualValue?? && status.actualValue?has_content>${mapGet(moduleResultsDescriptions, status.actualValue)}<#else>-</#if>
                          </@spring.bind>
                        </#compress></span>
                      </#if>
                      <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="Edit marks" data-toggle="modal" data-target="#students${sprCode?replace('/', '_')}_${moduleCode}_markmodal">
                        <i class="fal fa-pencil" aria-hidden="true"></i><span class="sr-only">Edit marks</span>
                      </button>
                      <@modal.modal id="students${sprCode?replace('/', '_')}_${moduleCode}_markmodal" cssClass="module-mark-modal" backdrop=false>
                        <@modal.wrapper>
                          <div class="modal-header"><h3 class="modal-title">Edit the module mark for ${sprCode} &mdash; ${moduleCode}</h3></div>
                          <@modal.body>
                            <p class="help-block">
                              Changes you make here won't be saved until you process this student.
                            </p>
                              <@bs3form.labelled_form_group path="students[${sprCode}][${moduleCode}].mark" labelText="Mark">
                                <div class="input-group">
                                  <@f.input id="students${sprCode?replace('/', '_')}_${moduleCode}.mark" path="students[${sprCode}][${moduleCode}].mark" cssClass="form-control mark-box" autocomplete="off" type="number" min="0" max="100" data\-initial="${studentModuleMarkRecord.mark!''}" />
                                  <div class="input-group-addon">%</div>
                                </div>
                              </@bs3form.labelled_form_group>
                              <@bs3form.labelled_form_group path="students[${sprCode}][${moduleCode}].grade" labelText="Grade">
                                <@spring.bind path="students[${sprCode}][${moduleCode}]">
                                  <#assign markItem = status.actualValue />
                                </@spring.bind>
                                <#assign generateUrl><@routes.marks.module_generateGrades moduleCode academicYear mr.occurrence sprCode /></#assign>
                                <@f.input id="students${sprCode?replace('/', '_')}_${moduleCode}.grade-txt" path="students[${sprCode}][${moduleCode}].grade" cssClass="form-control ${markItem.validGrades?has_content?string('auto-grade-modal grades-already-available', 'auto-grade-modal')} grade-box" data\-mark="students[${sprCode}][${moduleCode}].mark" data\-generate\-url=generateUrl?markup_string />
                                <@f.select id="students${sprCode?replace('/', '_')}_${moduleCode}.grade" path="students[${sprCode}][${moduleCode}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true data\-initial="${studentModuleMarkRecord.grade!''}">
                                  <#if markItem.validGrades?has_content>
                                    <@gg.gradeOptions markItem.validGrades._1() markItem.validGrades._2() markItem.grade!'' />
                                  </#if>
                                </@f.select>
                              </@bs3form.labelled_form_group>
                              <@bs3form.labelled_form_group path="students[${sprCode}][${moduleCode}].result" labelText="Result">
                                <#assign resultInitial><#if studentModuleMarkRecord.result??>${studentModuleMarkRecord.result.entryName}</#if></#assign>
                                <@f.select id="students${sprCode?replace('/', '_')}_${moduleCode}.result" path="students[${sprCode}][${moduleCode}].result" cssClass="form-control" data\-initial="${resultInitial?markup_string}">
                                  <@f.option value="" />
                                  <#list moduleResults as result>
                                    <@f.option value=result.entryName label=result.description />
                                  </#list>
                                </@f.select>
                              </@bs3form.labelled_form_group>
                              <@bs3form.labelled_form_group path="students[${sprCode}][${moduleCode}].comments" labelText="Comments">
                                <@f.textarea path="students[${sprCode}][${moduleCode}].comments" cssClass="form-control" data\-initial="" />
                              </@bs3form.labelled_form_group>
                          </@modal.body>
                          <@modal.footer>
                            <button type="button" class="btn btn-primary dirty-check-ignore" data-dismiss="modal">Save</button>
                            <button type="button" class="btn btn-default module-mark-cancel">Cancel</button>
                          </@modal.footer>
                        </@modal.wrapper>
                      </@modal.modal>
                    </td>
                  </#list>
                </tr></tbody></table>
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

