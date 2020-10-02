<#escape x as x?html>
    <#import "*/group_components.ftlh" as components />

    <#macro pagination currentPage totalResults resultsPerPage>
        <#local totalPages = (totalResults / resultsPerPage)?ceiling />
      <nav class="pull-right">
        <ul class="pagination pagination-sm">
            <#if currentPage lte 1>
              <li class="disabled"><span>&laquo;</span></li>
            <#else>
              <li><a data-page="${currentPage - 1}">&laquo;</a></li>
            </#if>

            <#list 1..totalPages as page>
                <#if page == currentPage>
                  <li class="active"><span>${page}</span></li>
                <#else>
                  <li><a data-page="${page}">${page}</a></li>
                </#if>
            </#list>

            <#if currentPage gte totalPages>
              <li class="disabled"><span>&raquo;</span></li>
            <#else>
              <li><a data-page="${currentPage + 1}">&raquo;</a></li>
            </#if>
        </ul>
      </nav>
    </#macro>

  <div class="add-student-to-set">

      <@spring.bind path="persistenceCommand.staticStudentIds">
          <#if status.error>
            <div class="alert alert-danger"><@f.errors path="persistenceCommand.staticStudentIds" cssClass="error"/></div>
          </#if>
      </@spring.bind>

      <#if summaryString?has_content>
        <p><#noescape>${summaryString}</#noescape></p>
      </#if>

    <div class="find-students striped-section collapsible <#if expandFind>expanded</#if>"
         data-submitparam="${ManageSmallGroupsMappingParameters.findStudents}">
      <h3 class="section-title">
        <a class="collapse-trigger icon-container" href="#">
          Find students
        </a>
        <span class="very-subtle">Select students by route, year of study etc.</span>
      </h3>
      <div class="striped-section-contents">
        <div class="item-info">
          <p>Use the filters to add students to this set of small groups:</p>

            <@f.hidden path="findCommand.page" />
            <@f.hidden path="findCommand.sortOrder" />

          <div class="student-filter btn-group-group well well-sm">
            <button type="button" class="clear-all-filters btn btn-link" aria-label="Clear all filters">
						<span class="fa-stack">
							<i class="fa fa-filter fa-stack-1x"></i>
							<i class="fa fa-ban fa-stack-2x"></i>
						</span>
            </button>

              <#macro filter path placeholder currentFilter allItems validItems=allItems prefix="">
                  <@spring.bind path=path>
                    <div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
                      <a class="btn btn-default btn-xs dropdown-toggle" href="#" data-toggle="dropdown">
                    <span class="filter-short-values" data-placeholder="${placeholder}"
                          data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
                        <span class="caret"></span>
                      </a>
                      <div tabindex="-1" class="dropdown-menu filter-list">
                        <button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
                        <ul>
                            <#if allItems?has_content>
                                <#list allItems as item>
                                    <#local isValid = (allItems?size == validItems?size)!true />
                                    <#if !isValid>
                                        <#list validItems as validItem>
                                            <#if ((validItem.id)!0) == ((item.id)!0)>
                                                <#local isValid = true />
                                            </#if>
                                        </#list>
                                    </#if>
                                  <li class="checkbox check-list-item" data-natural-sort="${item_index}">
                                    <label class="checkbox <#if !isValid>disabled</#if>">
                                        <#nested item isValid/>
                                    </label>
                                  </li>
                                </#list>
                            <#else>
                              <li>
                                <small class="very-subtle" style="padding-left: 5px;">N/A for this department</small>
                              </li>
                            </#if>
                        </ul>
                      </div>
                    </div>
                  </@spring.bind>
              </#macro>

              <#macro current_filter_value path placeholder><#compress>
                  <@spring.bind path=path>
                      <#if status.actualValue?has_content>
                          <#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
                      <#else>
                          ${placeholder}
                      </#if>
                  </@spring.bind>
              </#compress></#macro>

              <#function contains_by_code collection item>
                  <#list collection as c>
                      <#if c.code == item.code>
                          <#return true />
                      </#if>
                  </#list>
                  <#return false />
              </#function>

              <#assign placeholder = "All course types" />
              <#assign currentfilter><@current_filter_value "findCommand.courseTypes" placeholder; courseType>${courseType.code}</@current_filter_value></#assign>
              <@filter "findCommand.courseTypes" placeholder currentfilter findCommand.allCourseTypes; courseType>
                <input type="checkbox" name="${status.expression}" value="${courseType.code}"
                       data-short-value="${courseType.code}" ${contains_by_code(findCommand.courseTypes, courseType)?string('checked','')}>
                  ${courseType.description}
              </@filter>

              <#assign placeholder = "All routes" />
              <#assign currentfilter><@current_filter_value "findCommand.routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
              <@filter "findCommand.routes" placeholder currentfilter findCommand.allRoutes findCommand.visibleRoutes; route, isValid>
                <input type="checkbox" name="${status.expression}" value="${route.code}"
                       data-short-value="${route.code?upper_case}" ${contains_by_code(findCommand.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
                  <@fmt.route_name route false />
              </@filter>

              <#assign placeholder = "All attendance" />
              <#assign currentfilter><@current_filter_value "findCommand.modesOfAttendance" placeholder; moa>${moa.shortName?capitalize}</@current_filter_value></#assign>
              <@filter "findCommand.modesOfAttendance" placeholder currentfilter findCommand.allModesOfAttendance; moa>
                <input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}"
                        ${contains_by_code(findCommand.modesOfAttendance, moa)?string('checked','')}>
                  ${moa.fullName}
              </@filter>

              <#assign placeholder = "All years" />
              <#assign currentfilter><@current_filter_value "findCommand.yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
              <@filter "findCommand.yearsOfStudy" placeholder currentfilter findCommand.allYearsOfStudy findCommand.allYearsOfStudy "Year "; yearOfStudy>
                <input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
                        ${findCommand.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
                  ${yearOfStudy}
              </@filter>

              <#assign placeholder = "All statuses" />
              <#assign currentfilter><#if !filterQueryString?has_content>Current Student<#else><@current_filter_value "findCommand.sprStatuses" placeholder; sprStatus>${sprStatus.shortName?capitalize}</@current_filter_value></#if></#assign>
              <@filter "findCommand.sprStatuses" placeholder currentfilter findCommand.allSprStatuses; sprStatus>
                <#-- If there is no filter query then the default should be all current students -->
                <#assign applyDefault=(!filterQueryString?has_content && sprStatus.code == "C") />
                <input type="checkbox" name="${status.expression}" value="${sprStatus.code}"
                       data-short-value="${sprStatus.shortName?capitalize}" ${(applyDefault || contains_by_code(findCommand.sprStatuses, sprStatus))?string('checked','')}>
                  ${sprStatus.fullName}
              </@filter>

              <#assign placeholder = "All modules" />
              <#assign currentfilter><#if !filterQueryString?has_content>${module.code?upper_case}<#else><@current_filter_value "findCommand.modules" placeholder; module>${module.code?upper_case}</@current_filter_value></#if></#assign>
              <@filter "findCommand.modules" placeholder currentfilter findCommand.allModules; module>
                  <#-- If there is no filter query then the default should be current module associated students -->
                <#assign applyModuleDefault=(!findCommand.filterQueryString?has_content && module.code?upper_case == "${findCommand.module.code?upper_case}") />
                <input type="checkbox" name="${status.expression}"
                       value="${module.code}"
                       data-short-value="${module.code?upper_case}" ${(applyModuleDefault || contains_by_code(findCommand.modules, module))?string('checked','')}>
                  <@fmt.module_name module false />
              </@filter>

            <div class="btn-group">
              <button disabled class="btn btn-xs btn-primary search" type="submit" name="${ManageSmallGroupsMappingParameters.findStudents}"
                      value="true">Find
              </button>
            </div>

          </div>

            <#if (findCommandResult.membershipItems?size > 0)>
              <div class="clearfix">
                  <@pagination
                  currentPage=findCommand.page
                  resultsPerPage=findCommand.studentsPerPage
                  totalResults=findCommand.totalResults
                  />

                <#assign startIndex = ((findCommand.page - 1) * findCommand.studentsPerPage) />
                <#assign endIndex = startIndex + findCommandResult.membershipItems?size />
                <p class="not-relative">
                  Results ${startIndex + 1} - ${endIndex} of ${findCommand.totalResults}
                </p>
              </div>

                <@components.manageStudentTable
                membershipItems=findCommandResult.membershipItems
                doSorting=true
                command=findCommand
                checkboxName="excludeIds"
                onlyShowCheckboxForStatic=true
                showRemoveButton=true
                />
            </#if>
        </div>
      </div>
    </div>


    <div class="manually-added striped-section collapsible <#if expandManual>expanded</#if>">
      <h3 class="section-title">
        <a class="collapse-trigger icon-container" href="#">
          Manually add students
        </a>
        <small class="very-subtle">Add a list of students by university ID or username</small>
      </h3>
      <div class="striped-section-contents">
        <div class="item-info">
          <p style="margin-bottom: 1em;">
            <input style="margin-right: 8px;" class="btn btn-default" type="submit" name="${ManageSmallGroupsMappingParameters.manuallyAddForm}"
                   value="Add students manually" />
              <@fmt.p editMembershipCommandResult.includedStudentIds?size "student" /> added manually and
              <@fmt.p editMembershipCommandResult.excludedStudentIds?size "student" /> removed manually
          </p>

            <#if (addUsersResult.missingUsers?size > 0)>
              <div class="alert alert-danger">
                The following students could not be added as they were not found:
                <ul>
                    <#list addUsersResult.missingUsers as user>
                      <li>${user}</li>
                    </#list>
                </ul>
              </div>
            </#if>

            <@components.manageStudentTable
            membershipItems=editMembershipCommandResult.membershipItems
            checkboxName="resetStudentIds"
            checkAll=true
            showResetButton=true
            />

        </div>
      </div>
    </div>

    <input type="hidden" name="returnTo" value="${returnTo}">
      <@f.hidden path="findCommand.doFind" />
  </div>

</#escape>
