<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />
<#import "/WEB-INF/freemarker/exams/grids/generate/form_fields.ftl" as gf />

<#assign courses><#list selectCourseCommand.courses as course >${course.code}<#if course_has_next>, </#if></#list></#assign>
<div class="deptheader">
  <h1>Generate resits</h1>
  <h4 class="with-related">for ${courses} &ndash; <#if selectCourseCommand.levelCode??>Level ${selectCourseCommand.levelCode}<#else>Block ${selectCourseCommand.yearOfStudy}</#if></h4>
</div>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<#assign hasErrors = false />
<@spring.hasBindErrors name="resitsCommand">
    <#assign hasErrors = errors.hasGlobalErrors() />
    <#if errors.hasErrors()>
      <div class="alert alert-danger">
          <#if errors.hasGlobalErrors()>
              <#list errors.globalErrors as e>
                <div><@spring.message message=e /></div>
              </#list>
          <#else>
            <div>See the errors below.</div>
          </#if>
      </div>
    </#if>
</@spring.hasBindErrors>

<#assign cancelUrl><@routes.marks.adminhome department academicYear /></#assign>

<div class="fix-area">
  <@f.form method="post" modelAttribute="resitsCommand" cssClass="marks-form">
    <#if requiresResit?has_content>
      <table class="table table-striped table-sortable table-checkable">
        <thead>
        <tr>
          <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">SPR Code</th>
          <th class="col-sm-1 sortable" data-sorter="text">First name</th>
          <th class="col-sm-1 sortable" data-sorter="text">Last name</th>
          <th>Resits required</th>
        </tr>
        </thead>
        <tbody>
          <#list requiresResit?keys as sprCode>
            <#assign entity = mapGet(entitiesBySprCode, sprCode)._1() />
            <tr>
              <td>
                <@pl.profile_link entity.universityId />
                ${sprCode}
              </td>
              <td>${entity.firstName}</td>
              <td>${entity.lastName}</td>
              <td>
                <#assign modules = mapGet(requiresResit, sprCode) />
                <#list modules as module>
                  <section class="panel panel-default">
                      <div class="panel-heading">
                        <strong>${module.moduleCode}</strong>
                        <ul class="list-inline">
                          <li>Mark: ${module.markRecord.mark!"-"}</li>
                          <li>Grade: ${module.markRecord.grade!"-"}</li>
                          <li>Result: <#if module.markRecord.result??>${module.markRecord.result.description}<#else>-</#if></li>
                        </ul>
                      </div>
                      <div class="panel-body">
                        <table class="table table-striped table-sortable table-checkable">
                          <thead>
                          <tr>
                            <th class="" data-sorter="false"></th>
                            <th class="col-sm-1 sortable" data-sortInitialOrder="asc" data-sorter="text">Sequence</th>
                            <th class="col-sm-4 sortable" data-sorter="text">Name</th>
                            <th class="col-sm-1 sortable" data-sorter="text">Weighting</th>
                            <th class="col-sm-3">Assessment Type</th>
                            <th class="col-sm-3">Attempt</th>
                          </tr>
                          </thead>
                          <#list module.reassessmentComponents as reassessmentComponent>
                            <#assign sequence = reassessmentComponent.component.sequence />
                            <tr>
                              <td>
                                <#if reassessmentComponent.existingResit?has_content>
                                  <span class="label label-success tabula-tooltip" data-title="Resit - ${reassessmentComponent.existingResit.resitSequence!''}">
                                    Resit created
                                    <span class="sr-only">Resit - ${reassessmentComponent.existingResit.resitSequence!''}</span>
                                  </span>
                                  <#if reassessmentComponent.existingResit.needsWritingToSits>
                                    <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
                                      <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
                                        <span class="sr-only">Waiting to be written to SITS</span>
                                    </span>
                                  </#if>
                                </#if>
                                <@f.checkbox path="resits[${sprCode}][${module.moduleCode}][${sequence}].create" />
                              </td>
                              <td>${reassessmentComponent.component.sequence}</td>
                              <td>${reassessmentComponent.component.name}</td>
                              <td>${reassessmentComponent.component.scaledWeighting!""}</td>
                              <td>${reassessmentComponent.assessmentType.name}</td>
                              <td>
                                <@bs3form.form_group path="resits[${sprCode}][${module.moduleCode}][${sequence}].attempt">
                                  <@f.select path="resits[${sprCode}][${module.moduleCode}][${sequence}].attempt" cssClass="form-control">
                                    <@f.options items=attempts itemLabel="name" itemValue="value" />
                                  </@f.select>
                                  <@bs3form.errors path="resits[${sprCode}][${module.moduleCode}][${sequence}].attempt" />
                                </@bs3form.form_group>
                              </td>
                            </tr>
                          </#list>
                        </table>
                      </div>
                  </section>
                </#list>
              </td>
            </tr>
          </#list>
        </tbody>
      </table>
    <#else>
      <p>No students are expected to resit.</p>
    </#if>
    <div class="submit-buttons fix-footer">
      <#if requiresResit?has_content><input type="submit" class="btn btn-primary" value="Confirm" /></#if>
      <a class="btn btn-default dirty-check-ignore" href="${cancelUrl?markup_string}">Cancel</a>
    </div>
  </@f.form>
</div>
