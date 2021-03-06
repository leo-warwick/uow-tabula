<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#import "mitcirc_form_macros.ftl" as mitcirc />
<#import "*/mitcircs_components.ftlh" as components />

<h1>Record outcomes for MIT-${submission.key}</h1>

<div class="col-md-4 col-md-push-8">
  <section class="mitcircs-details">
    <@components.studentSummary submission true />
    <@components.submissionSummary submission true />
  </section>
</div>
<div class="col-md-8 col-md-pull-4">
  <section class="mitcircs-form">

    <article class="mitcircs-form__fields">
      <@f.form id="recordOutcomesForm" method="POST" modelAttribute="command" class="mitcircs-outcomes-form dirty-check double-submit-protection">

        <@mitcirc.question_section
          question = "Mitigation grade"
          hint = "This grading will be shared with exam boards."
        >
          <@mitcirc.radios outcomeGrading "outcomeGrading" />
          <div class="mitcircs-form__fields__section__nested-checkboxes collapse" data-target=":input[name=outcomeGrading]" data-target-value="Rejected">
            <@mitcirc.checkboxesWithOther rejectionReasons "rejectionReasons" "rejectionReasonsOther"/>
          </div>
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Outline why this mitigating circumstances submission has been awarded a particular grade "
          hint = "Robust minutes are required for any consequent appeals or OIA investigations."
        >
          <@bs3form.form_group "outcomeReasons">
            <@f.textarea path="outcomeReasons" cssClass="form-control" rows="5" />
            <@bs3form.errors path="outcomeReasons" />
          </@bs3form.form_group>
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Recommendations to the board of examiners"
          hint = "Take into account and reflect relevant factors such as the student’s mode of study, or mode of assessment. The decision will only be based on the evidence that has been presented in the original submission"
        >
          <#if command.affectedAssessments?has_content>
            <#list command.affectedAssessments as assessment>
              <@spring.nestedPath path="affectedAssessments[${assessment_index}]">
                <@f.hidden path="name" />
                <@f.hidden path="module" />
                <@f.hidden path="moduleCode" />
                <@f.hidden path="sequence" />
                <@f.hidden path="academicYear" />
                <@f.hidden path="assessmentType" />
                <@f.hidden path="deadline" />
                <@f.hidden path="acuteOutcomeApplies" />
                <@f.hidden path="extensionDeadline" />
              </@spring.nestedPath>
            </#list>
          </#if>
          <#list boardRecommendations as value>
            <div class="checkbox <#if value.entryName == "Other">mitcircs-form__fields__checkbox-with-other</#if>">
              <label>
                <@f.checkbox path="boardRecommendations" value="${value.entryName}" /> ${value.description}
              </label>
              <#if value.entryName == "Other">
                <@f.input path="boardRecommendationOther" cssClass="form-control other-input" />
              </#if>
              <#if value.helpText??><@fmt.help_popover id="${value.entryName}" content="${value.helpText}" placement="left"/></#if>
              <#if value.assessmentSpecific!false && command.affectedAssessments?has_content>
                <section class="mitcircs-form__fields__section__nested-checkboxes collapse" data-target=":input[name=boardRecommendations][value=${value.entryName}]" data-match-state="true">
                  <#list command.affectedAssessments as assessment>
                    <@spring.nestedPath path="affectedAssessments[${assessment_index}]">
                      <div class="checkbox nested">
                        <label>
                          <@f.checkbox path="boardRecommendations" value="${value.entryName}" />
                          <@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}
                        </label>
                      </div>
                    </@spring.nestedPath>
                  </#list>
                </section>
              </#if>
            </div>
            <#if value.entryName == "Other"><@bs3form.errors path="boardRecommendationOther" /></#if>
          </#list>
          <@bs3form.errors path="boardRecommendations" />
          <@bs3form.errors path="boardRecommendations" />
        </@mitcirc.question_section>

        <@mitcirc.question_section
          question = "Additional comments for the board of examiners"
          hint = "Please include any additonal information about the panel's recommendations. Do not include any specifics about the mitigation and only refer to the student by their University ID"
        >
          <@bs3form.form_group "boardRecommendationComments">
            <@f.textarea path="boardRecommendationComments" cssClass="form-control" rows="5" />
            <@bs3form.errors path="boardRecommendationComments" />
          </@bs3form.form_group>
        </@mitcirc.question_section>

        <div class="fix-footer">
          <#if submission.state.entryName != "Outcomes Recorded">
            <button type="submit" class="btn btn-primary" name="confirm" value="false">Save as draft</button>
          </#if>
            <#if isPanelChair>
              <@modal.modal id="approveModal" role="dialog"><@modal.wrapper>
                  <@modal.header>
                    <h3 class="modal-title">Approve outcomes for MIT-${submission.key}</h3>
                  </@modal.header>
                  <@modal.body>
                    <p>
                      Please check that the outcomes are correct and that the reasons given are appropriate.
                      In particular, please ensure that accurate minutes have been recorded that contain sufficient detail for any future appeals or OIA investigations.
                    </p>
                  </@modal.body>
                  <@modal.footer>
                    <button type="submit" class="btn btn-primary" name="confirm" value="true">Confirm</button>
                    <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                  </@modal.footer>
              </@modal.wrapper></@modal.modal>
              <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#approveModal">Submit and approve</button>
            <#else>
              <button type="submit" class="btn btn-primary" name="confirm" value="true">
                Submit
              </button>
            </#if>
          <a class="btn btn-default dirty-check-ignore" href="<#if fromPanel && submission.panel??><@routes.mitcircs.reviewSubmissionPanel submission /><#else><@routes.mitcircs.reviewSubmission submission /></#if>">Cancel</a>
        </div>
      </@f.form>
    </article>
  </section>
</div>
