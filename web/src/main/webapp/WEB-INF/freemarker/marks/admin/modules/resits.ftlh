<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />

<div class="deptheader">
  <h1>Generate resits</h1>
  <h4 class="with-related">for ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<#macro detail label>
  <div class="row form-horizontal">
    <div class="col-sm-6">
      ${label}
    </div>
    <div class="col-sm-6">
      <#nested>
    </div>
  </div>
</#macro>

<#assign hasErrors = false />
<@spring.hasBindErrors name="command">
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

<#assign submitUrl><@routes.marks.module_marks_resits sitsModuleCode academicYear occurrence /></#assign>
<#assign cancelUrl><@routes.marks.adminhome module.adminDepartment academicYear /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
    <@f.hidden name="confirm" value="true" />

    <#if requiresResits?has_content>
        <p>
          <@fmt.p number=requiresResits?size singular="student has" plural="students have" /> received a grade which indicates that they have further assessment
          requirements.  Please check the box next to each component that will be re-assessed and indicate for each whether this is an "Examination" (i.e. an
          online test to be administered via the Alternative Exams Portal (AEP)) or a written "Essay" (administered locally by the department), by selecting
          the appropriate type from the drop-down.
        </p>

        <#list requiresResits as record>
          <#assign member = membersBySprCode[record.module.sprCode] />
          <#assign sprCode = record.module.sprCode />
          <section class="resit-student panel panel-default">
            <div class="panel-heading">
                <@pl.profile_link member.universityId /> ${sprCode} &mdash; ${member.firstName} ${member.lastName}
            </div>
            <div class="panel-body">
              <div class="row">
                <div class="col-sm-2">
                  <@detail "Module mark">${record.module.mark!'-'}</@detail>
                  <@detail "Module grade">${record.module.grade!'-'}</@detail>
                  <@detail "Module result">${(record.module.result.description)!'-'}</@detail>
                </div>
                <div class="col-sm-10">
                  <table class="table table-sortable">
                    <thead>
                      <tr>
                        <th class="col-sm-1"></th>
                        <th class="col-sm-2 sortable">Original assessment</th>
                        <th class="col-sm-1 sortable">Sequence</th>
                        <th class="col-sm-1 sortable">Mark</th>
                        <th class="col-sm-1 sortable">Grade</th>
                        <th class="col-sm-2 sortable">Assessment Type</th>
                        <th class="col-sm-2 sortable">Weighting</th>
                        <th class="col-sm-2">Attempt</th>
                      </tr>
                    </thead>
                    <tbody>
                      <#list record.components?keys as assessmentComponent>
                        <#assign componentRecord = mapGet(record.components, assessmentComponent) />
                        <#assign sitsResit = mapGet(record.sitsResits, assessmentComponent)!{} />
                        <tr>
                          <td>
                            <#if componentRecord.existingResit?has_content>
                              <span class="label label-success tabula-tooltip" data-title="Resit - ${componentRecord.existingResit.resitSequence!''}">
                                Resit created
                                <span class="sr-only">Resit - ${componentRecord.existingResit.resitSequence!''}</span>
                              </span>
                              <#if componentRecord.existingResit.needsWritingToSits>
                                <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
                                  <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
                                  <span class="sr-only">Waiting to be written to SITS</span>
                                </span>
                              </#if>
                              <#if canUpdateResits>
                                <@f.checkbox path="resits[${sprCode}][${assessmentComponent.sequence}].create" />
                              </#if>
                            <#elseif sitsResit?has_content>
                              <span class="label label-success tabula-tooltip" data-title="Resit - ${sitsResit.resitSequence!''}. The resit was created directly in SITS. Any changes to this resit must be made in SITS.">
                                Resit created in SITS
                                <span class="sr-only">Resit - ${sitsResit.resitSequence!''}. The resit was created directly in SITS. Any changes to this resit must be made in SITS.</span>
                              </span>
                            <#elseif record.requiresResit>
                              <@f.checkbox path="resits[${sprCode}][${assessmentComponent.sequence}].create" />
                            </#if>
                          </td>
                          <td>${assessmentComponent.name}</td>
                          <td>${assessmentComponent.sequence}</td>
                          <td>${componentRecord.mark!'-'}</td>
                          <td>${componentRecord.grade!'-'}</td>
                          <td>
                            <#if componentRecord.existingResit?has_content && !canUpdateResits>
                              ${componentRecord.existingResit.assessmentType.name}
                            <#elseif sitsResit?has_content && !componentRecord.existingResit?has_content>
                              ${sitsResit.resitAssessmentType.name}
                            <#elseif record.requiresResit>
                              <#assign assessmentTypes = mapGet(record.assessmentTypes, assessmentComponent) />
                              <@f.select path="resits[${sprCode}][${assessmentComponent.sequence}].assessmentType" cssClass="form-control assessment-type">
                                <@f.options items=assessmentTypes itemLabel="name" itemValue="astCode" />
                              </@f.select>
                            <#else>
                              ${assessmentComponent.assessmentType.name}
                            </#if>
                          </td>
                          <td>
                            <#if componentRecord.existingResit?has_content && !canUpdateResits>
                              ${componentRecord.existingResit.weighting}
                            <#elseif sitsResit?has_content && !componentRecord.existingResit?has_content>
                                ${sitsResit.resitAssessmentWeighting!assessmentComponent.scaledWeighting}
                            <#else>
                              <@bs3form.form_group path="resits[${sprCode}][${assessmentComponent.sequence}].weighting">
                                <@f.input type="number" path="resits[${sprCode}][${assessmentComponent.sequence}].weighting" class="form-control"/>
                                <@bs3form.errors path="resits[${sprCode}][${assessmentComponent.sequence}].weighting" />
                              </@bs3form.form_group>
                            </#if>
                          </td>
                          <td>
                            <#if componentRecord.existingResit?has_content && !canUpdateResits>
                              ${componentRecord.existingResit.currentResitAttempt.value}
                            <#elseif sitsResit?has_content && !componentRecord.existingResit?has_content>
                              ${sitsResit.currentResitAttempt!1}
                            <#else>
                              <@f.select path="resits[${sprCode}][${assessmentComponent.sequence}].attempt" cssClass="form-control">
                                <@f.option value="1"/>
                                <@f.option value="2"/>
                                <@f.option value="3"/>
                              </@f.select>
                            </#if>
                          </td>
                        </tr>
                      </#list>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </section>
        </#list>
    <#else>
      <p>No students taking this module are expected to resit.</p>
    </#if>

  <div class="submit-buttons fix-footer">
    <#if requiresResits?has_content && !hasErrors><input type="submit" class="btn btn-primary" value="Confirm" /></#if>
    <a class="btn btn-default dirty-check-ignore" href="${cancelUrl?markup_string}">Cancel</a>
  </div>
</@f.form>
