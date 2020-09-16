<#function route_function dept>
  <#local result><@routes.marks.adminhome dept academicYear /></#local>
  <#return result />
</#function>

<@fmt.id7_deptheader "Marks" route_function "for" />

<#function module_occurrence_anchor module_code occurrence>
  <#return "${module_code}-${occurrence}"?lower_case />
</#function>

<#macro all_module_occurrences results expand_by_default has_multiple_occurrences>
  <#list results as item>
    <@module_occurrence item expand_by_default has_multiple_occurrences />

    <#if !expand_by_default>
      <#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
      <script type="text/javascript" nonce="${nonce()}">
        GlobalScripts.initCollapsible(jQuery('#${module_occurrence_anchor(item.moduleCode, item.occurrence)}'));
      </script>
    </#if>
  </#list>
</#macro>

<#-- Output a dropdown menu only if there is anything in it. -->
<#macro dropdown_menu text button_size="btn-md">
  <#-- Capture the content between the macro tags into a string -->
  <#local content><#nested /></#local>
  <#if content?markup_string?trim?has_content>
    <a class="btn btn-default ${button_size} dropdown-toggle" data-toggle="dropdown" href="#">${text} <span class="caret"></span></a>
    <ul class="dropdown-menu pull-right">
      ${content?no_esc}
    </ul>
  </#if>
</#macro>

<#-- Progress bar for a single submission  -->
<#macro stage_progress_bar stages>
  <div class="stage-progress-bar">
    <#list stages as progress>
      <#local stage = progress.stage />

      <#local state = 'default' />
      <#local icon = 'fa-circle-o' />
      <#if progress.completed>
        <#if progress.health.toString == 'Good'>
          <#local state = 'success' />
          <#local icon = 'fa-check-circle-o' />
        <#else>
          <#local state = 'danger' />
          <#local icon = 'fa-times-circle-o' />
        </#if>
      <#elseif progress.skipped>
        <#local state = 'primary' />
        <#local icon = 'fa-arrow-circle-o-right' />
      <#elseif progress.started>
        <#local state = 'warning' />
        <#local icon = 'fa-dot-circle-o' />
      </#if>

      <#local title><@spring.message code=progress.stage.actionCode /></#local>
      <#if progress_index gt 0>
        <div tabindex="0" class="bar bar-${state} use-tooltip" data-toggle="tooltip" data-placement="top" title="${fmt.strip_html(title?markup_string)}"></div>
      </#if>
      <#local title><@spring.message code=progress.messageCode /></#local>
      <span tabindex="0" class="fa-stack use-tooltip" data-toggle="tooltip" data-placement="top" title="${fmt.strip_html(title?markup_string)}">
        <i class="fa fa-stack-1x fa-circle fa-inverse"></i>
        <i class="fa fa-stack-1x ${icon} text-${state}"></i>
      </span>
    </#list>
  </div>
</#macro>

<#macro module_occurrence item expand_by_default has_multiple_occurrences>
  <#local anchor = module_occurrence_anchor(item.moduleCode, item.occurrence) />

  <div id="${anchor}-container">
    <div id="${anchor}" class="module-occurrence-info striped-section collapsible<#if expand_by_default> expanded</#if>"
         <#-- Populate this lazily? -->
         data-name="${anchor}">
      <div class="clearfix">
        <div class="section-title row">
          <div class="col-md-6 col-xl-5">
            <span class="h4">
              <a class="collapse-trigger icon-container" href="#">
                <#-- Can't use the standard fmt.module_name as we want full module code and occurrence -->
                <span class="mod-code">${item.moduleCode?upper_case}</span>
                <span class="mod-name">${item.module.name}<#if has_multiple_occurrences> (${item.occurrence})</#if></span>
              </a>
            </span>
          </div>

          <div class="col-md-4 col-xl-6">
            <div class="row">
              <div class="col-xl-6">
                <@stage_progress_bar item.stages?values />
              </div>
              <div class="col-xl-6 next-action">
                <#if item.nextStage??>
                  <#local nextStageUrl="" />

                  <#-- Set nextStage URL based on action code -->
                  <#if item.nextStage.actionCode == "workflow.marks.moduleOccurrence.CalculateModuleMarks.action">
                    <#local nextStageUrl><@routes.marks.module_marks item.moduleCode academicYear item.occurrence /></#local>
                  </#if>

                  <#if nextStageUrl?has_content>
                    <a href="${nextStageUrl}"><#compress><@spring.message code=item.nextStage.actionCode /></#compress></a>
                  <#else>
                    <@spring.message code=item.nextStage.actionCode />
                  </#if>
                <#elseif item.progress.percentage == 100>
                  Complete
                </#if>
              </div>
            </div>
          </div>

          <div class="col-md-2 col-xl-1">
            <div class="pull-right">
              <@dropdown_menu "Module marks" "btn-xs">
                <li>
                  <a href="<@routes.admin.moduleperms item.module />">
                    Manage module permissions
                  </a>
                </li>

                <li>
                  <a href="<@routes.marks.module_marks item.moduleCode academicYear item.occurrence />">Calculate module marks</a>
                </li>

                <li>
                  <a href="<@routes.marks.module_marks_confirm item.moduleCode academicYear item.occurrence />" >Confirm module marks</a>
                </li>

                <#if features.processAgreedMarks>
                  <li>
                    <a href="<@routes.marks.module_marks_process item.moduleCode academicYear item.occurrence />" >Process module marks</a>
                  </li>
                  <li>
                    <a href="<@routes.marks.module_marks_resits item.moduleCode academicYear item.occurrence />" >Generate resits</a>
                  </li>
                <#else>
                  <li class="disabled">
                    <a>Process module marks</a>
                  </li>
                  <li class="disabled">
                    <a>Generate resits</a>
                  </li>
                </#if>
              </@dropdown_menu>
            </div>
          </div>
        </div>

        <div class="striped-section-contents">
          <#-- Populate this lazily? -->
          <@module_occurrence_inner item />
        </div>
      </div>
    </div> <!-- module-info striped-section-->
  </div>
</#macro>

<#macro module_occurrence_inner item>
  <div class="row">
    <div class="col-md-3">Component</div>
    <div class="col-md-3 col-xl-2">
      <div class="row">
        <div class="col-xs-6">Deadline</div>
        <div class="col-xs-6">Students</div>
      </div>
    </div>
    <div class="col-md-6 col-xl-7">Progress / Actions</div>
  </div>

  <#-- Fix nth-of-type zebra striping -->
  <div></div>

  <#list item.assessmentComponents as components_by_group>
    <#local assessment_group = components_by_group._1() />
    <#local components = components_by_group._2() />

    <#list components as info>
      <div class="item-info row component-${info.assessmentComponent.id}">
        <div class="col-md-3">
          <h4 class="component-name">
            ${info.assessmentComponent.name}
          </h4>

          (${info.assessmentComponent.sequence}, ${info.assessmentComponent.assessmentType.name}, ${(info.assessmentComponent.scaledWeighting!0)?string["0.#"]}%)
        </div>
        <div class="col-md-3 col-xl-2">
          <div class="row">
            <div class="col-xs-6">
              <#if info.upstreamAssessmentGroup.deadline??>
                <@fmt.date date=info.upstreamAssessmentGroup.deadline includeTime=false relative=false shortMonth=true excludeCurrentYear=true />
              <#else>
                <span class="very-subtle">Unknown</span>
              </#if>
            </div>
            <div class="col-xs-6">
              <@fmt.p info.students?size "student" "students" />
            </div>
          </div>
        </div>

        <div class="col-md-4 col-xl-6">
          <div class="row">
            <div class="col-xl-6">
              <@stage_progress_bar info.stages?values />
            </div>
            <div class="col-xl-6 next-action">
              <#if info.nextStage??>
                <#local nextStageUrl="" />

                <#-- Set nextStage URL based on action code -->

                <#if nextStageUrl?has_content>
                  <a href="${nextStageUrl}"><#compress><@spring.message code=info.nextStage.actionCode /></#compress></a>
                <#else>
                  <@spring.message code=info.nextStage.actionCode />
                </#if>
              <#elseif info.progress.percentage == 100>
                Complete
              </#if>
            </div>
          </div>
        </div>

        <div class="col-md-2 col-xl-1">
          <div class="pull-right">
            <@dropdown_menu "Component marks" "btn-xs">
              <li>
                <#-- Don't need a permission_button here as we need the same permission to see this page -->
                <a href="<@routes.marks.assessmentcomponent_marks info.assessmentComponent info.upstreamAssessmentGroup />">
                  Record component marks
                </a>
              </li>

              <li>
                <a href="<@routes.marks.assessmentcomponent_missingMarks info.assessmentComponent info.upstreamAssessmentGroup />">
                  Missing marks adjustment
                </a>
              </li>

              <li>
                <a href="<@routes.marks.assessmentcomponent_scaling info.assessmentComponent info.upstreamAssessmentGroup />">
                  Scaling
                </a>
              </li>
            </@dropdown_menu>
          </div>
        </div>
      </div>
    </#list>
  </#list>
</#macro>

<#if pendingComponentMarkChanges?has_content || pendingModuleMarkChanges?has_content>
  <div class="alert alert-info">
    <h3>Pending mark changes</h3>

    <p>
      There
      <#if pendingComponentMarkChanges?has_content || pendingModuleMarkChanges?has_content>
        <@fmt.p number=pendingModuleMarkChanges?size singular="is" plural="are" shownumber=false />
        <@fmt.p pendingModuleMarkChanges?size "pending module mark change" /> and <@fmt.p pendingComponentMarkChanges?size "pending component mark change" />
      <#elseif pendingComponentMarkChanges?has_content>
        <@fmt.p number=pendingComponentMarkChanges?size singular="is" plural="are" shownumber=false />
        <@fmt.p pendingComponentMarkChanges?size "pending component mark change" />
      <#else>
        <@fmt.p number=pendingModuleMarkChanges?size singular="is" plural="are" shownumber=false />
        <@fmt.p pendingModuleMarkChanges?size "pending module mark change" />
      </#if>
      from SITS.
    </p>

    <p>
      <a href="<@routes.marks.out_of_sync_marks department academicYear />">View pending changes</a>
    </p>
  </div>
</#if>

<#if results?has_content>
  <@all_module_occurrences results false hasMultipleOccurrences />
<#else>
  <p>No modules were found for marks.</p>
</#if>
