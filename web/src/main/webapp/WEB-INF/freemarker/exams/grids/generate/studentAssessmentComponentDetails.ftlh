<#import "/WEB-INF/freemarker/mitcircs/mitcircs_components.ftlh" as mitcircs_components />
<#import "/WEB-INF/freemarker/marks/marks_components.ftlh" as marks_components />

<div class="exam-grid-preview">
  <div class="deptheader">
    <#if department.examGridOptions.nameToShow.toString != 'none'>
      <h1>${member.fullName!}</h1>
      <h4 class="with-related">${member.universityId}</h4>
    <#else>
      <h1>${member.universityId}</h1>
    </#if>
  </div>
  <div class="key clearfix">
    <table class="table table-condensed">
      <thead>
      <tr>
        <th colspan="2">Report</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <th>Department:</th>
        <td>${studentCourseDetails.department.name}</td>
      </tr>
      <tr>
        <th>Course:</th>
        <td>${studentCourseDetails.course.code?upper_case} ${studentCourseDetails.course.name}</td>
      </tr>
      <tr>
        <th>Course occurrence:</th>
        <td>${command.studentCourseYearDetails.blockOccurrence!""?upper_case}</td>
      </tr>
      <tr>
        <th>Route:</th>
        <td>${studentCourseDetails.currentRoute.code?upper_case} ${studentCourseDetails.currentRoute.name}</td>
      </tr>
      <tr>
        <th>Academic Year:</th>
        <td>${academicYear.startYear?c}</td>
      </tr>
      <tr>
        <th>Study Year:</th>
        <td> ${command.studentCourseYearDetails.yearOfStudy}</td>
      </tr>

      <tr>
        <th>Year weightings:</th>
        <td>
          <#list weightings as weighting>
            Year ${weighting.yearOfStudy} = ${weighting.weightingAsPercentage}%<#if weighting_has_next> | </#if>
          </#list>
        </td>
      </tr>
      <tr>
        <th>Normal CAT load:</th>
        <td>
          <#if normalLoadLookup.withoutDefault(studentCourseDetails.currentRoute, command.studentCourseYearDetails.academicYear)?has_content>
            ${normalLoadLookup.withoutDefault(studentCourseDetails.currentRoute, command.studentCourseYearDetails.academicYear)}
          <#else>
            <#assign defaultNormalLoad>${normalLoadLookup.apply(studentCourseDetails.currentRoute, command.studentCourseYearDetails.academicYear)}</#assign>
            ${defaultNormalLoad} <@fmt.help_popover id="normal-load" content="Could not find a Pathway Module Rule for the normal load so using the default value of ${defaultNormalLoad}" />
          </#if>
        </td>
      </tr>
      </tbody>
    </table>

    <table class="table table-condensed">
      <thead>
      <tr>
        <th colspan="2">Key</th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td><span class="exam-grid-unconfirmed">#</span></td>
        <td>Unconfirmed marks (subject to change)</td>
      </tr>
      <tr>
        <td><span class="exam-grid-fail">#</span></td>
        <td>Failed module or component</td>
      </tr>
      <tr>
        <td><span class="exam-grid-actual-mark">#</span></td>
        <td>Agreed mark missing, using actual</td>
      </tr>
      <tr>
        <td><span class="exam-grid-resit"># (#)</span></td>
        <td>Resit mark (original mark)</td>
      </tr>
      <tr>
        <td><span class="exam-grid-actual-mark">X</span></td>
        <td>Agreed and actual mark missing</td>
      </tr>
      <tr>
        <td></td>
        <td>Blank indicates module not taken by student</td>
      </tr>
      </tbody>
    </table>
  </div>

  <div class="assessment_details">
    <table class="table table-condensed grid no-wide-tables">
      <thead>
      <tr class="assessment_componentdetails_header">
        <th>Module</th>
        <th>Module Credit</th>
        <th>Type</th>
        <th>Seq</th>
        <th>Assessment</th>
        <th>Weighting</th>
        <th>Component Mark</th>
        <th>Component Grade</th>
        <th class="header_col">Module Mark</th>
        <th class="header_col">Module Grade</th>
        <th class="header_col">Module Result</th>
      </tr>
      </thead>
      <tbody>
      <#list assessmentComponents as info>
        <#assign mr = info.moduleRegistration />
        <tr>
          <td class="assessment_details_col assessment_details_col2">
            <#assign module = mr.module!"" />
            <div><#if module?has_content>${module.code?upper_case}<#else>${mr.sitsModuleCode}</#if></div>
            <div><#if module?has_content>${module.name}</#if></div>
          </td>
          <td class="assessment_details_col">${mr.cats!}</td>
          <td class="assessment_details_col"><#if mr.selectionStatus??>${(mr.selectionStatus.description)!}<#else>-</#if></td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>${component.upstreamGroup.group.sequence}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>${component.upstreamGroup.name}</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <tr>
                  <td>${(component.weighting!0)?string["0.#"]}%</td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <#assign unconfirmedClass><#if component.markState?? && component.markState.entryName == "UnconfirmedActual">exam-grid-unconfirmed</#if></#assign>
                <tr>
                  <td class="${unconfirmedClass}">
                    <#if component.member.firstDefinedMark??>
                      <#assign passMark = mapGet(passMarkMap, component.upstreamGroup.assessmentComponent.module) />
                      <#assign class><#compress>
                        <#if component.member.firstDefinedMark?number < passMark>exam-grid-fail </#if>
                        <#if !component.member.isAgreedMark()>exam-grid-actual-mark </#if>
                        <#if component.member.reassessment>exam-grid-resit </#if>
                      </#compress></#assign>
                      <span class="${class}"><#compress>
                            ${component.member.firstDefinedMark}
                            <#if component.member.reassessment && component.member.firstOriginalMark??>
                          (${component.member.firstOriginalMark})
                        </#if>
                          </#compress></span>
                    <#elseif (component.member.firstDefinedGrade!"") == 'FM'>
                      <#assign class><#compress>
                        <#if !component.member.isAgreedMark()>exam-grid-actual-mark </#if>
                        <#if component.member.reassessment>exam-grid-resit </#if>
                      </#compress></#assign>
                      <span class="${class}"><#compress>
                        -
                        <#if component.member.reassessment && component.member.firstOriginalMark??>
                          (${component.member.firstOriginalMark})
                        </#if>
                          </#compress></span>
                    <#else>
                      <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No marks set for Assessment component">X</span>
                    </#if>
                    <@marks_components.componentMarkHistory component.markRecord component.upstreamGroup.assessmentComponent member />
                  </td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <td>
            <table class="component_info">
              <tbody>
              <#list info.components as component>
                <#assign unconfirmedClass><#if component.markState?? && component.markState.entryName == "UnconfirmedActual">exam-grid-unconfirmed</#if></#assign>
                <tr>
                  <td class="${unconfirmedClass}">
                    <#if component.member.firstDefinedGrade??>
                      <span class="<#if !component.member.isAgreedGrade()>exam-grid-actual-mark</#if> "> ${component.member.firstDefinedGrade}</span>
                    <#else>
                      <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No grade set for Assessment component">X</span>
                    </#if>
                  </td>
                </tr>
              </#list>
              </tbody>
            </table>
          </td>
          <#assign unconfirmedClass><#if info.markState?? && info.markState.entryName == "UnconfirmedActual">exam-grid-unconfirmed</#if></#assign>
          <td class="assessment_details_col assessment_details_col1 ${unconfirmedClass}">
            <#if module?has_content>
              <#assign passMark = mapGet(passMarkMap, module) />
              <#if mr.agreedMark??>
                <#if mr.agreedMark?number lt passMark>
                  <span class="exam-grid-fail">${mr.agreedMark}</span>
                <#else>
                  ${mr.agreedMark}
                </#if>
              <#elseif mr.actualMark??>
                <#if mr.actualMark?number < passMark>
                  <span class="exam-grid-fail">${mr.actualMark}</span>
                <#else>
                  <span class="exam-grid-actual-mark">${mr.actualMark}</span>
                </#if>
              <#elseif ((mr.agreedGrade)!((mr.actualGrade)!'FM')) == 'FM'>
                <#if mr.agreedGrade??>
                  -
                <#else>
                  <span class="exam-grid-actual-mark">-</span>
                </#if>
              <#else>
                <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No marks set for Module registration">X</span>
              </#if>
              <@marks_components.moduleMarkHistory info.markRecord mr.sitsModuleCode member.fullName />
            </#if>
          </td>
          <td class="assessment_details_col assessment_details_col1 ${unconfirmedClass}">
            <#if mr.agreedGrade??>
              <#if mr.agreedGrade == "F">
                <span class="exam-grid-fail">${mr.agreedGrade}</span>
              <#else>
                ${mr.agreedGrade}
              </#if>
            <#elseif mr.actualGrade??>
              <#if mr.actualGrade == "F">
                <span class="exam-grid-fail">${mr.actualGrade}</span>
              <#else>
                <span class="exam-grid-actual-mark">${mr.actualGrade}</span>
              </#if>
            <#else>
              <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No grade set for Module registration">X</span>
            </#if>
          </td>
          <td class="assessment_details_col assessment_details_col1 ${unconfirmedClass}">
            <#if mr.moduleResult??>
              <#if mr.moduleResult.dbValue == "F">
                <span class="exam-grid-fail">${mr.moduleResult.description}</span>
              <#else>
                ${mr.moduleResult.description}
              </#if>
            <#else>
              <span tabindex="0" class="exam-grid-actual-mark tabula-tooltip" data-title="No result set for Module registration">X</span>
            </#if>
          </td>
        </tr>
      </#list>
      </tbody>
    </table>
  </div>

  <#if mitigatingCircumstances?has_content>
    <div class="mitcircs_details">
      <h2>Mitigating circumstances</h2>
        <#list mitigatingCircumstances as submission>
          <div class="well">
            <h3>MIT-${submission.key}<#if submission.acute> &mdash; Acute mitigation already applied</#if></h3>

            <@mitcircs_components.detail label="Grading">
                ${submission.gradingCode} <@fmt.help_popover id="grading-${submission.key}" content="${submission.outcomeGrading.helpText}" />
            </@mitcircs_components.detail>

            <@mitcircs_components.detail label="Grading approved">
              <#if submission.acute><@fmt.date date=submission.outcomesLastRecordedOn /><#else><@fmt.date date=submission.outcomesApprovedOn /></#if>
            </@mitcircs_components.detail>

            <#if submission.outcomeGrading.entryName != "Rejected">
              <#if submission.acute>
                <@mitcircs_components.detail label="Outcome" condensed=true>
                  <#if submission.acuteOutcome??>
                    ${submission.acuteOutcome.description}
                  <#else>
                    <span class="very-subtle">None</span>
                  </#if>
                </@mitcircs_components.detail>

                <#if submission.affectedAssessments?has_content>
                  <@mitcircs_components.detail "Affected assessments">
                  <ul class="list-unstyled">
                    <#list submission.assessmentsWithAcuteOutcome as assessment>
                      <li><@mitcircs_components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}</li>
                    </#list>
                  </ul>
                  </@mitcircs_components.detail>
                </#if>
              <#else>
                <@mitcircs_components.detail "Recommendations to board">
                  <ul class="list-unstyled">
                    <#list submission.boardRecommendations as recommendation><#if !(recommendation.assessmentSpecific!false)>
                      <li>
                        <#if recommendation.entryName == "Other">${submission.boardRecommendationOther}<#else>${recommendation.description}</#if>
                        (all assessments) <@fmt.help_popover id="${recommendation.entryName}-${submission.key}" content="${recommendation.helpText}" />
                      </li>
                    </#if></#list>

                    <#list submission.affectedAssessments as assessment>
                      <#if assessment.boardRecommendations?has_content>
                        <li>
                          <@mitcircs_components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}
                          <ul>
                            <#list assessment.boardRecommendations as recommendation>
                              <li>
                                <#if recommendation.entryName == "Other">${submission.boardRecommendationOther}<#else>${recommendation.description}</#if>
                                <@fmt.help_popover id="${recommendation.entryName}-${submission.key}" content="${recommendation.helpText}" />
                              </li>
                            </#list>
                          </ul>
                        </li>
                      </#if>
                    </#list>
                  </ul>
                </@mitcircs_components.detail>

                <@mitcircs_components.detail "Comments for board">
                  <#noautoesc>${submission.formattedBoardRecommendationComments}</#noautoesc>
                </@mitcircs_components.detail>
              </#if>
            </#if>

          </div>
        </#list>

    </div>
  </#if>
</div>
