<#escape x as x?html>

  <div class="well well-sm filters">
    <button type="button" class="clear-all-filters btn btn-link" aria-label="Clear all filters">
			<span class="fa-stack">
				<i class="fa fa-filter fa-stack-1x"></i>
				<i class="fa fa-ban fa-stack-2x"></i>
			</span>
    </button>

      <#macro filter path placeholder currentFilter allItems prefix="" customPicker="" cssClass="" emptyText="N/A for this department">
          <@spring.bind path=path>
            <div class="btn-group<#if currentFilter == placeholder> empty-filter</#if> ${cssClass}">
              <a class="btn btn-default btn-mini btn-xs dropdown-toggle" href="#" data-toggle="dropdown">
              <span class="filter-short-values" data-placeholder="${placeholder}"
                    data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
                <span class="caret"></span>
              </a>
              <div tabindex="-1" class="dropdown-menu filter-list">
                <button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
                <ul>
                    <#if customPicker?has_content>
                      <li>
                          ${customPicker}
                      </li>
                    </#if>
                    <#if allItems?has_content>
                        <#list allItems as item>
                          <li class="check-list-item" data-natural-sort="${item_index}">
                              <#nested item />
                            <label class="checkbox">

                            </label>
                          </li>
                        </#list>
                    <#else>
                      <li>
                        <div class="small muted" style="padding-left: 5px;">${emptyText}</div>
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
                  <#if status.actualValue?is_collection>
                      <#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
                  <#else>
                      <#nested status.actualValue />
                  </#if>
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

      <#assign placeholder = "Course" />
      <#assign currentfilter><#compress><@current_filter_value "selectCourseCommand.courses" placeholder; course>
          <#if course?is_sequence>
              <#list course as c>${c.code?upper_case}<#if c_has_next>, </#if></#list>
          <#else>
              ${course.code?upper_case}
          </#if>
      </@current_filter_value></#compress></#assign>

      <@filter "selectCourseCommand.courses" placeholder currentfilter selectCourseCommand.allCourses; course>
        <label class="checkbox">
          <input type="checkbox" name="${status.expression}" value="${course.code}" data-short-value="${course.code}"
                  ${contains_by_code(selectCourseCommand.courses, course)?string('checked','')}
          >
            ${course.code} ${course.name}
        </label>
      </@filter>

      <#assign placeholder = "All occurrences" />
      <#assign currentfilter><#compress><@current_filter_value "selectCourseCommand.courseOccurrences" placeholder; courseOccurrence>
          <#if courseOccurrence?is_sequence>
              <#list courseOccurrence as c>${c?upper_case}<#if c_has_next>, </#if></#list>
          <#else>
              ${courseOccurrence?upper_case}
          </#if>
      </@current_filter_value></#compress></#assign>

      <@filter "selectCourseCommand.courseOccurrences" placeholder currentfilter selectCourseCommand.allCourseOccurrences; courseOccurrence>
          <#if courseOccurrence??>
            <label class="checkbox">
              <input type="checkbox" name="${status.expression}" value="${courseOccurrence}" data-short-value="${courseOccurrence}"
                      ${selectCourseCommand.courseOccurrences?seq_contains(courseOccurrence)?string('checked','')}
              >
                ${courseOccurrence}
            </label>
          </#if>
      </@filter>

      <#assign placeholder = "All routes" />

      <#assign currentfilter><#compress><@current_filter_value "selectCourseCommand.routes" placeholder; route>
          <#if route?is_sequence>
              <#list route as r>${r.code?upper_case}<#if r_has_next>, </#if></#list>
          <#else>
              ${route.code?upper_case}
          </#if>
      </@current_filter_value></#compress></#assign>
      <@filter path="selectCourseCommand.routes" placeholder=placeholder currentFilter=currentfilter allItems=selectCourseCommand.allRoutes emptyText="No course routes found"; route>
        <label class="checkbox">
          <input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}"
                  ${contains_by_code(selectCourseCommand.routes, route)?string('checked','')}
          >
            ${route.code?upper_case} ${route.name}
        </label>
      </@filter>


      <#if department.enableLevelGrids>
        <div class="btn-group" style="margin: 0 10px;">
          Generate grid on:&nbsp;
          <label class="radio-inline">
            <input type="radio" name="gridScope" value="block"> Block
          </label>
          <label class="radio-inline">
            <input type="radio" name="gridScope" value="level"> Level
          </label>
        </div>
      </#if>

      <#assign placeholder = "Year of study" />
      <#assign currentfilter><@current_filter_value "selectCourseCommand.yearOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
      <@filter path="selectCourseCommand.yearOfStudy" placeholder=placeholder currentFilter=currentfilter allItems=selectCourseCommand.allYearsOfStudy prefix="Year " cssClass="block" ; yearOfStudy>
        <label class="radio">
          <input type="radio" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
                  ${(((selectCourseCommand.yearOfStudy)!0) == yearOfStudy)?string('checked','')}
          >
            ${yearOfStudy}
        </label>
      </@filter>

      <#if department.enableLevelGrids>
          <#assign placeholder = "Study level" />
          <#assign currentfilter><@current_filter_value "selectCourseCommand.levelCode" placeholder; levelCode>${levelCode}</@current_filter_value></#assign>
          <@filter path="selectCourseCommand.levelCode" placeholder=placeholder currentFilter=currentfilter allItems=selectCourseCommand.allLevels prefix="Level " cssClass="level"; level>
            <label class="radio">
              <input type="radio" name="${status.expression}" value="${level.code}" data-short-value="${level.code}"
                      ${(((selectCourseCommand.levelCode)!'') == level.code)?string('checked','')}
              >
                ${level.code} - ${level.name}
            </label>
          </@filter>
      </#if>

  </div>
  <#assign studyYear = (selectCourseCommand.studyYearByLevelOrBlock)!0 />

  <#if chooseYears!true>
    <div class="row year_info">
      <h3 class="year_info_hdr <#if studyYear == 0>hidden</#if>">Years to display on grid</h3>
        <#assign maxYear=selectCourseCommand.allYearsOfStudy?size>
        <#list 1..maxYear as counter>
          <div class="col-sm-2 year${counter} <#if counter gt studyYear>hidden</#if>" data-year="${counter}">
            <div class="checkbox">
                <#assign yearColumn="Year${counter}"/>
              <label>
                <input type="checkbox" name="courseYearsToShow" value="${yearColumn}"
                       <#if selectCourseCommand.courseYearsToShow?seq_contains("${yearColumn}")>checked</#if> <#if studyYear == counter>disabled</#if>
                /> Year ${counter}
              </label>
            </div>
          </div>
            <#if studyYear == counter><input type="hidden" name="courseYearsToShow" value="${yearColumn}"/></#if>
        </#list>
        <#if studyYear == 0><input type="hidden" name="courseYearsToShow" value="" /></#if>
    </div>
    <div class="year_info_ftr <#if studyYear == 0>hidden</#if>">
      <hr />
    </div>
  </#if>

  <h5>Types of student to include</h5>

  <div style="margin-bottom: 2rem;">
      <@bs3form.checkbox path="selectCourseCommand.includePermWithdrawn">
          <@f.checkbox path="selectCourseCommand.includePermWithdrawn" /> Permanently withdrawn
      </@bs3form.checkbox>
    <br>
      <@bs3form.checkbox path="selectCourseCommand.includeTempWithdrawn">
          <@f.checkbox path="selectCourseCommand.includeTempWithdrawn" /> Temporarily withdrawn
      </@bs3form.checkbox>
    <br>
      <@bs3form.checkbox path="selectCourseCommand.resitOnly">
          <@f.checkbox path="selectCourseCommand.resitOnly" /> Resit only
      </@bs3form.checkbox>
  </div>
</#escape>
