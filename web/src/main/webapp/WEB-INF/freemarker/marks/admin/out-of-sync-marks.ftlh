<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />

<#function route_function dept>
  <#local result><@routes.marks.out_of_sync_marks dept academicYear /></#local>
  <#return result />
</#function>

<@fmt.id7_deptheader "Pending mark changes from SITS" route_function "for" />

<p>
  This screen shows where marks recorded in Tabula differ from those recorded in SITS. In these situations, the marks in Tabula
  show as "Out of sync with SITS" and can't be modified further in Tabula until this is resolved. As SITS is the canonical repository
  of mark information, you can use the information from SITS to overwrite the current marks in Tabula and resolve the issue.
</p>

<#if !command.pendingComponentMarkChanges?has_content && !command.pendingComponentMarkChanges?has_content>
  <p>There are no pending mark changes from SITS.</p>
<#else>
  <#assign submitUrl><@routes.marks.out_of_sync_marks department academicYear /></#assign>
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
  <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
    <div class="fix-area">
      <#if command.pendingComponentMarkChanges?has_content>
        <h2>Pending component mark changes</h2>

        <table class="table table-striped table-checkable">
          <thead>
            <th class="for-check-all"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
            <th class="col-sm-2">University ID</th>
            <th class="col-sm-2">First name</th>
            <th class="col-sm-2">Last name</th>
            <th class="col-sm-2">State</th>
            <th class="col-sm-2">Mark</th>
            <th class="col-sm-2">Grade</th>
            <th></th>
          </thead>

          <#list command.pendingComponentMarkChanges as pendingChange>
            <#assign info = pendingChange._1() />

            <@spring.nestedPath path="componentMarks[${info.assessmentComponent.moduleCode}/${info.upstreamAssessmentGroup.occurrence}/${info.upstreamAssessmentGroup.assessmentGroup}/${info.assessmentComponent.sequence}]">
              <tbody>
                <tr>
                  <th colspan="8">${info.assessmentComponent.name} (${info.assessmentComponent.moduleCode}, ${info.assessmentComponent.sequence}, ${info.upstreamAssessmentGroup.occurrence})</th>
                </tr>
                <#list pendingChange._2() as student>
                  <#assign member = membersByUniversityId[student.universityId] />

                  <@spring.nestedPath path="students[${student.universityId}_${student.resitSequence!}]">
                    <tr>
                      <td><@f.checkbox cssClass="collection-checkbox" path="acceptChanges" /></td>
                      <td>
                        <@pl.profile_link member.universityId />
                        ${student.universityId}
                        <@components.componentMarkStatusIcon student false />
                      </td>
                      <td>${member.firstName}</td>
                      <td>${member.lastName}</td>
                      <td>
                        <#if student.recordedStudent.latestState?has_content>
                          <span class="label label-${student.recordedStudent.latestState.cssClass}">${student.recordedStudent.latestState.description}</span>
                        <#else>
                          -
                        </#if>

                        <#if student.upstreamAssessmentGroupMember.firstDefinedMark?has_content || student.upstreamAssessmentGroupMember.firstDefinedGrade?has_content>
                          <#assign upstreamMarkState = {'description': 'Agreed', 'cssClass': 'success'} />
                        <#elseif (student.markState.description)! == 'Confirmed actual'>
                          <#assign upstreamMarkState = student.markState />
                        <#else>
                          <#assign upstreamMarkState = {'description': 'Unconfirmed actual', 'cssClass': 'default'} />
                        </#if>

                        <#if (student.recordedStudent.latestState.description)! != upstreamMarkState.description!>
                          <i class="fal fa-arrow-right"></i>
                          <span class="label label-${upstreamMarkState.cssClass}">${upstreamMarkState.description}</span>
                        </#if>
                      </td>
                      <td>
                        ${student.recordedStudent.latestMark!'-'}
                        <#if "${student.recordedStudent.latestMark!}" != "${student.upstreamAssessmentGroupMember.firstDefinedMark!}">
                          <i class="fal fa-arrow-right"></i>
                          ${student.upstreamAssessmentGroupMember.firstDefinedMark!'-'}
                        </#if>
                      </td>
                      <td>
                        <@components.format_grade student.recordedStudent.latestGrade!'-' />
                        <#if student.recordedStudent.latestGrade! != student.upstreamAssessmentGroupMember.firstDefinedGrade!>
                          <i class="fal fa-arrow-right"></i>
                          <@components.format_grade student.upstreamAssessmentGroupMember.firstDefinedGrade!'-' />
                        </#if>
                      </td>
                      <td>
                        <@components.componentMarkHistory student info.assessmentComponent member />
                      </td>
                    </tr>
                  </@spring.nestedPath>
                </#list>
              </tbody>
            </@spring.nestedPath>
          </#list>
        </table>
      </#if>

      <#if command.pendingModuleMarkChanges?has_content>
        <h2>Pending module mark changes</h2>

        <table class="table table-striped table-checkable">
          <thead>
            <th class="for-check-all"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
            <th class="col-sm-2">SPR Code</th>
            <th class="col-sm-2">First name</th>
            <th class="col-sm-2">Last name</th>
            <th class="col-sm-2">State</th>
            <th class="col-sm-1">Mark</th>
            <th class="col-sm-1">Grade</th>
            <th class="col-sm-2">Result</th>
            <th></th>
          </thead>

          <#list command.pendingModuleMarkChanges as pendingChange>
            <#assign info = pendingChange._1() />

            <@spring.nestedPath path="moduleMarks[${info.moduleCode}/${info.occurrence}]">
              <tbody>
                <tr>
                  <th colspan="9">${info.moduleCode} ${info.module.name} (${info.occurrence})</th>
                </tr>
                <#list pendingChange._2() as student>
                  <#assign member = membersBySprCode[student.sprCode] />

                  <@spring.nestedPath path="students[${student.sprCode}]">
                    <tr>
                      <td><@f.checkbox cssClass="collection-checkbox" path="acceptChanges" /></td>
                      <td>
                        <@pl.profile_link member.universityId />
                        ${student.sprCode}
                        <@components.moduleMarkStatusIcon student false />
                      </td>
                      <td>${member.firstName}</td>
                      <td>${member.lastName}</td>
                      <td>
                        <#if student.recordedStudent.latestState?has_content>
                          <span class="label label-${student.recordedStudent.latestState.cssClass}">${student.recordedStudent.latestState.description}</span>
                        <#else>
                          -
                        </#if>

                        <#if student.moduleRegistration.firstDefinedMark?has_content || student.moduleRegistration.firstDefinedGrade?has_content>
                          <#assign upstreamMarkState = {'description': 'Agreed', 'cssClass': 'success'} />
                        <#elseif (student.markState.description)! == 'Confirmed actual'>
                          <#assign upstreamMarkState = student.markState />
                        <#else>
                          <#assign upstreamMarkState = {'description': 'Unconfirmed actual', 'cssClass': 'default'} />
                        </#if>

                        <#if (student.recordedStudent.latestState.description)! != upstreamMarkState.description!>
                          <i class="fal fa-arrow-right"></i>
                          <span class="label label-${upstreamMarkState.cssClass}">${upstreamMarkState.description}</span>
                        </#if>
                      </td>
                      <td>
                        ${student.recordedStudent.latestMark!'-'}
                        <#if "${student.recordedStudent.latestMark!}" != "${student.moduleRegistration.firstDefinedMark!}">
                          <i class="fal fa-arrow-right"></i>
                          ${student.moduleRegistration.firstDefinedMark!'-'}
                        </#if>
                      </td>
                      <td>
                        <@components.format_grade student.recordedStudent.latestGrade!'-' />
                        <#if student.recordedStudent.latestGrade! != student.moduleRegistration.firstDefinedGrade!>
                          <i class="fal fa-arrow-right"></i>
                          <@components.format_grade student.moduleRegistration.firstDefinedGrade!'-' />
                        </#if>
                      </td>
                      <td>
                        ${(student.recordedStudent.latestResult.description)!'-'}
                        <#if (student.recordedStudent.latestResult.description)! != (moduleRegistration.result.description)!>
                          <i class="fal fa-arrow-right"></i>
                          ${(moduleRegistration.result.description)!'-'}
                        </#if>
                      </td>
                      <td>
                        <@components.moduleMarkHistory student info.moduleCode member.fullName />
                      </td>
                    </tr>
                  </@spring.nestedPath>
                </#list>
              </tbody>
            </@spring.nestedPath>
          </#list>
        </table>
      </#if>

      <div class="submit-buttons fix-footer">
        <input type="submit" class="btn btn-primary" value="Accept selected changes" />
        <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome department academicYear />">Cancel</a>
      </div>
    </div>
  </@f.form>
</#if>
