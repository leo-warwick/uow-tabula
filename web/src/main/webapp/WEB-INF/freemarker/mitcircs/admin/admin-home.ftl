<#import "../_filters.ftl" as filters />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

<#escape x as x?html>
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

  <div class="btn-toolbar dept-toolbar">
    <div class="btn-group">
      <a class="btn btn-default" href="<@routes.mitcircs.listPanels department academicYear />">
        Panels
      </a>
    </div>
  </div>

  <#function route_function dept>
    <#local result><@routes.mitcircs.adminhome dept academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Mitigating Circumstances Submissions" route_function "in" />

  <#-- Filtering -->
  <div class="fix-area">
    <div class="fix-header pad-when-fixed">
      <div class="filters btn-group-group well well-sm" data-lazy="true">
        <@f.form modelAttribute="command" action="${info.requestedUri.path}" method="GET" cssClass="form-inline">
          <@f.errors cssClass="error form-errors" />

          <div class="mitcircs-submission-filters">
            <#-- Student filters -->
            <span class="mitcircs-submission-filters__icon mitcircs-submission-filters__icon--student">
              <i class="fal fa-user-graduate fa-fw"></i>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--course-types">
              <#assign placeholder = "All course types" />
              <#assign currentfilter><@filters.current_filter_value "courseTypes" placeholder; courseType>${courseType.code}</@filters.current_filter_value></#assign>
              <@filters.filter "courseTypes" "command.courseTypes" placeholder currentfilter command.allCourseTypes; courseType>
                <input type="checkbox" name="${status.expression}" value="${courseType.code}"
                       data-short-value="${courseType.code}" ${filters.contains_by_code(command.courseTypes, courseType)?string('checked','')}>
                ${courseType.description}
              </@filters.filter>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--route">
              <#assign placeholder = "All routes" />
              <#assign currentfilter><@filters.current_filter_value "routes" placeholder; route>${route.code?upper_case}</@filters.current_filter_value></#assign>
              <#assign routesCustomPicker>
                <div class="route-search input-append input-group">
                  <input class="route-search-query route prevent-reload form-control" type="text" value="" placeholder="Search for a route" />
                  <span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
                </div>
              </#assign>
              <@filters.filter name="routes" path="command.routes" placeholder=placeholder currentFilter=currentfilter allItems=command.allRoutes validItems=command.visibleRoutes customPicker=routesCustomPicker; route, isValid>
                <input type="checkbox" name="${status.expression}" value="${route.code}"
                       data-short-value="${route.code?upper_case}" ${filters.contains_by_code(command.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
                <@fmt.route_name route false />
              </@filters.filter>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--mode-of-attendance">
              <#assign placeholder = "All attendance" />
              <#assign currentfilter><@filters.current_filter_value "modesOfAttendance" placeholder; moa>${moa.shortName?capitalize}</@filters.current_filter_value></#assign>
              <@filters.filter "modesOfAttendance" "command.modesOfAttendance" placeholder currentfilter command.allModesOfAttendance; moa>
                <input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}"
                        ${filters.contains_by_code(command.modesOfAttendance, moa)?string('checked','')}>
                ${moa.fullName}
              </@filters.filter>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--year-of-study">
              <#assign placeholder = "All years" />
              <#assign currentfilter><@filters.current_filter_value "yearsOfStudy" placeholder; year>${year}</@filters.current_filter_value></#assign>
              <@filters.filter "yearsOfStudy" "command.yearsOfStudy" placeholder currentfilter command.allYearsOfStudy command.allYearsOfStudy "Year "; yearOfStudy>
                <input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
                        ${command.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
                ${yearOfStudy}
              </@filters.filter>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--spr-status">
              <#assign placeholder = "All statuses" />
              <#assign currentfilter><@filters.current_filter_value "sprStatuses" placeholder; sprStatus>${sprStatus.shortName?capitalize}</@filters.current_filter_value></#assign>
              <@filters.filter "sprStatuses" "command.sprStatuses" placeholder currentfilter command.allSprStatuses; sprStatus>
                <input type="checkbox" name="${status.expression}" value="${sprStatus.code}"
                       data-short-value="${sprStatus.shortName?capitalize}" ${filters.contains_by_code(command.sprStatuses, sprStatus)?string('checked','')}>
                ${sprStatus.fullName}
              </@filters.filter>
            </span>

            <#-- Submission filters -->
            <span class="mitcircs-submission-filters__icon mitcircs-submission-filters__icon--submission">
              <i class="fal fa-file-alt fa-fw"></i>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--affected-assessment-module">
              <#assign placeholder = "All affected modules" />
              <#assign modulesCustomPicker>
                <div class="module-search input-append input-group">
                <input class="module-search-query module prevent-reload form-control" type="text" value="" placeholder="Search for a module" />
                <span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
              </div>
              </#assign>
              <#assign currentfilter><@filters.current_filter_value "affectedAssessmentModules" placeholder; module>${module.code?upper_case}</@filters.current_filter_value></#assign>
              <@filters.filter name="modules" path="command.affectedAssessmentModules" placeholder=placeholder currentFilter=currentfilter allItems=command.possibleModules customPicker=modulesCustomPicker; module>
                <input type="checkbox" name="${status.expression}"
                       value="${module.code}"
                       data-short-value="${module.code?upper_case}"
                      ${filters.contains_by_code(command.affectedAssessmentModules, module)?string('checked','')}>
                <@fmt.module_name module false />
              </@filters.filter>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--state">
              <#assign placeholder = "All states" />
              <#assign currentfilter><@filters.current_filter_value "state" placeholder; state>${state.description}</@filters.current_filter_value></#assign>
              <@filters.filter "state" "command.state" placeholder currentfilter allSubmissionStates; state>
                <input type="checkbox" name="${status.expression}" value="${state.entryName}" data-short-value="${state.description}"
                      ${filters.contains_by_enum(command.state, state)?string('checked','')}>
                ${state.description}
              </@filters.filter>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--coronavirus">
              <#assign placeholder = "All submissions" />
              <div id="coronavirusState-filter" class="btn-group filter empty">
                <a class="btn btn-filter btn-sm dropdown-toggle" href="#" data-toggle="dropdown" aria-expanded="true">
                  <span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="">${placeholder}</span>
                  <span class="caret"></span>
                </a>
                <div tabindex="-1" class="dropdown-menu filter-list">
                  <button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
                  <ul>
                    <li class="check-list-item" data-natural-sort="0">
                      <label class="radio">
                      <input type="radio" name="isCoronavirus" value="true" data-short-value="Coronavirus submissions"
                        <#if command.coronavirus?? && command.coronavirus>checked="checked"</#if>
                      >
                        Coronavirus submissions
                      </label>
                    </li>
                    <li class="check-list-item" data-natural-sort="1">
                      <label class="radio">
                      <input type="radio" name="isCoronavirus" value="false" data-short-value="All other submissions"
                        <#if command.coronavirus?? && !command.coronavirus>checked="checked"</#if>
                      >
                        All other submissions
                      </label>
                    </li>
                  </ul>
                </div>
              </div>
            </span>

            <span class="mitcircs-submission-filters__filter mitcircs-submission-filters__filter--unread">
              <div class="checkbox">
                <label>
                  <input type="checkbox" name="isUnread" value="true"> Unread changes
                </label>
              </div>
            </span>

            <label class="control-label mitcircs-submission-filters__label mitcircs-submission-filters__label--includes-start-date" for="includesStartDate">
              Affected dates from
            </label>
            <span class="input-group mitcircs-submission-filters__filter mitcircs-submission-filters__filter--includes-start-date">
              <@f.input type="text" path="includesStartDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </span>

            <label class="control-label mitcircs-submission-filters__label mitcircs-submission-filters__label--includes-end-date" for="includesEndDate">
              to
            </label>
            <span class="input-group mitcircs-submission-filters__filter mitcircs-submission-filters__filter--includes-end-date">
              <@f.input type="text" path="includesEndDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </span>

            <label class="control-label mitcircs-submission-filters__label mitcircs-submission-filters__label--approved-start-date" for="approvedStartDate">
              Submitted between
            </label>
            <span class="input-group mitcircs-submission-filters__filter mitcircs-submission-filters__filter--approved-start-date">
              <@f.input type="text" path="approvedStartDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </span>

            <label class="control-label mitcircs-submission-filters__label mitcircs-submission-filters__label--approved-end-date" for="approvedEndDate">
              and
            </label>
            <span class="input-group mitcircs-submission-filters__filter mitcircs-submission-filters__filter--approved-end-date">
              <@f.input type="text" path="approvedEndDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
              <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
            </span>

            <button type="button" class="mitcircs-submission-filters__button mitcircs-submission-filters__button--clear clear-all-filters btn btn-sm btn-filter">
              Clear filters
            </button>
          </div>
        </@f.form>
      </div>
    </div>

    <@f.form class="mitcircs-submission-actions" action="" method="POST">
      <#if can.do("MitigatingCircumstancesPanel.Modify", department)>
        <div class="mitcircs-submission-action__buttons">
          <div class="btn-group-group">
            <button type="submit" formaction="<@routes.mitcircs.createPanel department academicYear />" class="btn btn-default">Create panel</button>
            <#if panels?has_content>
              <button type="button" class="requires-selected btn btn-default" disabled="disabled" data-toggle="modal" data-target="#addToPanelModal">Add to panel</button>
            <#else>
              <button type="button"  class="btn btn-default" disabled="disabled" data-toggle="tooltip" title="There are no panels for ${academicYear.toString}">Add to panel</button>
            </#if>
          </div>
          <@modal.modal id="addToPanelModal" role="dialog">
            <@modal.wrapper>
              <@modal.header>
                <h3 class="modal-title">Select a panel</h3>
              </@modal.header>
              <@modal.body>
                <@bs3form.form_group path=panel>
                  <select name="panel" class="form-control">
                    <option></option>
                    <#list panels as panel>
                      <option value="${panel.id}" data-formaction="<@routes.mitcircs.editPanel panel />">${panel.name}</option>
                    </#list>
                  </select>
                </@bs3form.form_group>
              </@modal.body>
              <@modal.footer>
                <button type="submit" disabled="disabled" class="btn btn-primary mitcircs-submission-action__buttons__add-to-panel">Confirm</button>
                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
              </@modal.footer>
            </@modal.wrapper>
          </@modal.modal>
        </div>
      </#if>
      <div class="filter-results mitcircs-submission-list">
        <i class="id7-koan-spinner id7-koan-spinner--xs id7-koan-spinner--inline" aria-hidden="true"></i> Loading&hellip;
      </div>
    </@f.form>
  </div>
</#escape>
