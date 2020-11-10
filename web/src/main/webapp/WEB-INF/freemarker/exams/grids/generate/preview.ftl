<#import 'form_fields.ftl' as form_fields />
<#import "*/modal_macros.ftlh" as modal />
<#escape x as x?html>

    <#function route_function dept>
        <#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
        <#return selectCourseCommand />
    </#function>

    <@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

  <div class="fix-area">
    <div class="exam-grid-preview">
      <h2>Preview and download</h2>

      <p class="progress-arrows">
        <span class="arrow-right"><a class="btn btn-link" href="<@routes.exams.generateGrid department academicYear />?${uriParser(gridOptionsQueryString)}">Select courses</a></span>
        <span class="arrow-right arrow-left"><a class="btn btn-link"
                                                href="<@routes.exams.generateGridOptions department academicYear />?${uriParser(gridOptionsQueryString)}">Set grid options</a></span>
        <span class="arrow-right arrow-left active">Preview and download</span>
      </p>

      <div id="examGridSpinner">
        <div><i class="id7-koan-spinner id7-koan-spinner--2x id7-koan-spinner--fast" aria-hidden="true"></i></div>
        <h3>Generating exam grid preview&hellip;</h3>
        <p>Please wait &mdash; the exam grid is being generated.</p>
        <div class="progress">
          <div class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" aria-valuenow="0" aria-valuemin="0"
               aria-valuemax="${entities?size}" style="width: 0%"></div>
        </div>
      </div>

      <div id="examGridContainer" style="display: none;">
        <div class="row">
          <div class="alert alert-info <#if user?? && user.sysadmin>col-sm-8<#else>col-sm-12</#if>">
            <h3>Your <#if gridOptionsCommand.showFullLayout>full<#else>short</#if> grid</h3>

              <#if oldestImport??>
                <p>
                  This grid has been generated from the data available in SITS at
                    <@fmt.date date=oldestImport capitalise=false at=true relative=true />. If data changes in SITS after this
                  time, you'll need to generate the grid again to see the most recent information.
                </p>
              </#if>

              <#if !(info.maintenance!false)>
                <#assign refreshSITSandGenerateGridAction><@routes.exams.generateGrid department academicYear /></#assign>
                <@f.form action=refreshSITSandGenerateGridAction method="post">
                    <@form_fields.select_course_fields />
                    <@form_fields.grid_options_fields />

                  <p>
                    <button type="submit" class="btn btn-primary" name="${GenerateExamGridMappingParameters.usePreviousSettings}">
                      Refresh SITS data and regenerate grid
                    </button>
                  </p>
                </@f.form>
              <#else>
                <p>
                  <button class="btn btn-primary use-tooltip" disabled
                          title="Tabula has been placed in a read-only mode. Refreshing SITS data is not currently possible.">
                    Refresh SITS data and regenerate grid
                  </button>
                </p>
              </#if>

            <p>
              <strong>
                Please note that all Examination Boards are required to be conducted anonymously and student names should be
                removed from/hidden in electronic or hard-copy versions of the grid used at the Exam Board meeting.
                If you wish to query this requirement, please contact Education Policy and Quality (<a href="mailto:quality@warwick.ac.uk">quality@warwick.ac.uk</a>).
              </strong>
            </p>
          </div>

            <#if user?? && user.sysadmin>
              <div class="alert col-sm-4">
                <h3>Check for missing students</h3>
                <p>Enter the name or university ID of a student to see why they don't appear on this exam grid.</p>
                <#assign gridCheckStudentAction><@routes.exams.gridCheckStudent department academicYear /></#assign>
                <@f.form class="student-checker" action=gridCheckStudentAction method="post">
                    <@bs3form.form_group>
                        <@bs3form.flexipicker name="member" membersOnly="true" universityId="true" placeholder="Type a name or university ID">
                          <span class="input-group-btn">
										<button class="btn btn-default" type="submit">Check</button>
									</span>
                        </@bs3form.flexipicker>
                    </@bs3form.form_group>
                    <@form_fields.select_course_fields />
                </@f.form>
                <@modal.modal cssClass="student-checker-modal" role="dialog"><@modal.wrapper></@modal.wrapper></@modal.modal>
              </div>
            </#if>

        </div>

          <#if !routeRules?has_content>
            <div class="alert alert-info">
              <h3>Over catted marks</h3>
              <p>There were no Pathway Module Rules defined in SITS for this route, year of study, and academic year.</p>
              <p>Therefore for students who have elected to overcat you will need to review the marks generated and choose the best mark that meets all of your
                course regulations.</p>
              <p>'Select Edit' to add the best mark. If you download the grid without adding these marks these marks will remain blank. <a href="#"
                                                                                                                                           class="show-more">Show
                  More</a></p>
              <div class="more hidden">
                <p>Each course normally consists of modules each with a CATs score and to pass that course you must achieve the minimum number of CATs.</p>
                <p>
                  The University allows students to study additional modules, if they so wish for their own education and to potentially achieve a higher mark.
                  Any student who studies more than the normal load of CATs has been deemed to have over-catted.
                </p>
                <p>
                  So that no student is ever disadvantaged by overcatting the calculation works out the highest scoring combination of modules from those the
                  student has taken.
                  If this is higher than the mean module mark, it will be the mark they are awarded, as long as the combination satisfies the course
                  regulations.
                </p>
                <p>
                  The regulations governing each course vary widely but always have a minimum number of CATS and the maximum number of CATS to be taken.
                  Usually a course is made up of sets of modules from which the student must pick modules.
                  Some courses may not allow certain combinations of modules to be taken in a year or over several years.
                </p>
                <p>
                  Unless Pathway Module Rules are defined it is not possible for Tabula to be sure that the final over-catted mark it derives complies with the
                  regulations,
                  and we ask the exam board to choose the final over-catted mark to ensure that it meets with all the regulations. <a href="#"
                                                                                                                                      class="show-less">Less</a>
                </p>
              </div>
            </div>
          </#if>

        <div class="key clearfix">
          <table class="table table-condensed report-table">
            <thead>
            <tr>
              <th colspan="2">Report</th>
            </tr>
            </thead>
            <tbody>
            <tr>
              <th>Grid type:</th>
              <td><#if gridOptionsCommand.showFullLayout>Full<#else>Short</#if> Grid</td>
            </tr>
            <tr>
              <th>Department:</th>
              <td>${department.name}</td>
            </tr>
            <tr>
                <#if selectCourseCommand.courses?size == 1>
                  <th>Course:</th>
                  <td>${selectCourseCommand.courses?first.code?upper_case} ${selectCourseCommand.courses?first.name}</td>
                <#else>
                  <th>Courses:</th>
                    <#assign popover>
                      <ul><#list selectCourseCommand.courses?sort_by('code') as course>
                          <li>${course.code?upper_case} ${course.name}</li>
                          </#list></ul>
                    </#assign>
                  <td>
                    <a class="use-popover hidden-print" data-trigger="click focus" href="#" data-html="true" data-content="${popover}">${selectCourseCommand.courses?size} courses</a>
                    <div class="visible-print">
                        <#noescape>${popover}</#noescape>
                    </div>
                  </td>
                </#if>
            </tr>
            <tr>
                <#if !selectCourseCommand.courseOccurrences?has_content>
                  <th>Course occurrences:</th>
                  <td>All occurrences</td>
                <#elseif selectCourseCommand.courseOccurrences?size == 1>
                  <th>Course occurrence:</th>
                  <td>${selectCourseCommand.courseOccurrences?first}</td>
                <#else>
                  <th>Course occurrences:</th>
                    <#assign popover>
                      <ul><#list selectCourseCommand.courseOccurrences?sort as courseOccurrence>
                          <li>${courseOccurrence}</li>
                          </#list></ul>
                    </#assign>
                  <td>
                    <a class="use-popover hidden-print" data-trigger="click focus" href="#" data-html="true" data-content="${popover}">${selectCourseCommand.courseOccurrences?size} occurrences</a>
                    <div class="visible-print">
                        <#noescape>${popover}</#noescape>
                    </div>
                  </td>
                </#if>
            </tr>
            <tr>
                <#if !selectCourseCommand.routes?has_content>
                  <th>Routes:</th>
                  <td>All routes</td>
                <#elseif selectCourseCommand.routes?size == 1>
                  <th>Route:</th>
                  <td>${selectCourseCommand.routes?first.code?upper_case} ${selectCourseCommand.routes?first.name}</td>
                <#else>
                  <th>Routes:</th>
                    <#assign popover>
                      <ul><#list selectCourseCommand.routes?sort_by('code') as route>
                          <li>${route.code?upper_case} ${route.name}</li>
                          </#list></ul>
                    </#assign>
                  <td>
                    <a class="use-popover hidden-print" data-trigger="click focus" href="#" data-html="true" data-content="${popover}">${selectCourseCommand.routes?size} routes</a>
                    <div class="visible-print">
                        <#noescape>${popover}</#noescape>
                    </div>
                  </td>
                </#if>
            </tr>
            <tr>
                <#if selectCourseCommand.yearOfStudy??>
                  <th>Year of study:</th>
                  <td>${selectCourseCommand.yearOfStudy}</td>
                <#elseif selectCourseCommand.levelCode??>
                  <th>Study level:</th>
                  <td>${selectCourseCommand.levelCode}</td>
                </#if>
            </tr>
            <tr>
              <th>Normal CAT load:</th>
              <td>
                  <#if normalLoadLookup.routes?size == 1>
                      <#if normalLoadLookup.withoutDefault(normalLoadLookup.routes?first, academicYear)?has_content>
                          ${normalLoadLookup.withoutDefault(normalLoadLookup.routes?first, academicYear)}
                      <#else>
                          <#assign defaultNormalLoad>${normalLoadLookup.apply(normalLoadLookup.routes?first, academicYear)}</#assign>
                          ${defaultNormalLoad} <@fmt.help_popover id="normal-load" cssClass="hidden-print" content="Could not find a Pathway Module Rule for the normal load so using the default value of ${defaultNormalLoad}" />
                      </#if>
                  <#else>
                      <#assign popover>
                        <ul><#list normalLoadLookup.routes?sort_by('code') as route>
                            <li>${route.code?upper_case}:
                                <#if normalLoadLookup.withoutDefault(route, academicYear)?has_content>
                                    ${normalLoadLookup.withoutDefault(route, academicYear)}
                                <#else>
                                    <#assign defaultNormalLoad>${normalLoadLookup.apply(route, academicYear)}</#assign>
                                    ${defaultNormalLoad} <@fmt.help_popover id="normal-load" cssClass="hidden-print" content="Could not find a Pathway Module Rule for the normal load so using the default value of ${defaultNormalLoad}" />
                                </#if>
                            </li>
                            </#list></ul>
                      </#assign>
                    <a href="#" class="use-popover hidden-print" data-trigger="click focus" data-html="true" data-content="${popover}">${normalLoadLookup.routes?size} routes</a>
                    <div class="visible-print">
                        <#noescape>${popover}</#noescape>
                    </div>
                  </#if>
              </td>
            </tr>
            <tr>
              <th>Student Count:</th>
              <td>${allEntities?size}</td>
            </tr>
            <tr>
              <th>Grid Generated:</th>
              <td><@fmt.date date=generatedDate relative=false /></td>
            </tr>
            </tbody>
          </table>

          <table class="table table-condensed key-table">
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
              <td><span class="exam-grid-overcat">#</span></td>
              <td>Used in overcatting calculation</td>
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
              <td><span class="exam-grid-actual-mark exam-grid-overcat">#</span></td>
              <td>Actual mark used in overcatting calculation</td>
            </tr>
            <tr>
              <td></td>
              <td>Blank indicates module not taken by student</td>
            </tr>
            <tr>
              <td><strong>AB</strong></td>
              <td>Bold module name indicates a duplicate table entry</td>
            </tr>
            </tbody>
          </table>

          <#if gridOptionsCommand.predefinedColumnIdentifiers?seq_contains("mitigating")>
            <table class="table table-condensed mitcircs-key-table">
              <thead>
                <tr>
                  <th colspan="2">Mitigating circumstances key</th>
                </tr>
              </thead>
              <tbody>
                <#list mitigatingCircumstancesGradings as grading>
                  <tr>
                    <td><span class="exam-grid-mitcircs-grading">${grading.code}</span></td>
                    <td>${grading.description}</td>
                  </tr>
                </#list>
                <tr>
                  <td><span class="exam-grid-mitcircs-grading">+0</span></td>
                  <td>Acute mitigation - mitigation has already been taken into account</td>
                </tr>
              </tbody>
            </table>
          </#if>
        </div>

          <#if gridOptionsCommand.showFullLayout>
              <#include "_full_grid.ftl" />
          <#else>
              <#include "_short_form_grid.ftl" />
          </#if>
      </div>

        <#if totalPages gt 1>
          <#assign generateGridPreviewAction><@routes.exams.generateGridPreview department academicYear /></#assign>
          <@f.form action=generateGridPreviewAction method="get" id="gridPreviewPagination">
              <@form_fields.select_course_fields />
              <@form_fields.grid_options_fields />

            <input type="hidden" name="page" value="" />
            <span class="sr-only">There are ${totalPages} page<#if currentPage != 1>s</#if> of results, you are on page ${currentPage}</span>
            <ul class="pagination" style="margin-top: 0;">
                <#if currentPage lte 1>
                  <li class="disabled"><span class="sr-only">You cannot move backwards, this is the first page</span><span>&laquo;</span></li>
                <#else>
                  <li><a data-page="${currentPage - 1}" href="#"><span class="sr-only">Previous page</span>&laquo;</a></li>
                </#if>

                <#list 1..totalPages as page>
                    <#if currentPage == page>
                      <li class="active"><span>${page}</span> <span class="sr-only">(current page)</span></li>
                    <#else>
                      <li><a href="#" data-page="${page}">${page}</a></li>
                    </#if>
                </#list>

                <#if currentPage gte totalPages>
                  <li class="disabled"><span class="sr-only">You cannot move forwards, this is the last page</span><span>&raquo;</span></li>
                <#else>
                  <li><a data-page="${currentPage + 1}" href="#"><span class="sr-only">Next page</span>&raquo;</a></li>
                </#if>
            </ul>
          </@f.form>

          <script nonce="${nonce()}">
            jQuery(function ($) {
              var $form = $('#gridPreviewPagination');

              $form.find('a[data-page]').on('click', function (e) {
                e.preventDefault();

                $form.find('input[name=page]').val($(this).data('page'));
                $form.submit();
              });
            });
          </script>
        </#if>


      <div class="fix-footer hidden-print">
        <div class="btn-group" role="group">
          <#assign generateGridAction><@routes.exams.generateGrid department academicYear /></#assign>
          <@f.form action=generateGridAction id="examGridDocuments" cssClass="dirty-check" method="post" target="_blank">
            <@form_fields.select_course_fields />
            <@form_fields.grid_options_fields />
              <div class="btn-group dropup">
                <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">Download&hellip; <span class="caret"></span></button>
                <ul class="dropdown-menu download-options">
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.excel}">Excel grid</button>
                  </li>
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.excelNoMergedCells}">Excel grid without merged cells</button>
                  </li>
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.excelNoCellComments}">Excel grid without cell comments</button>
                  </li>
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.excelNoMergedCellsNoCellComments}">Excel grid without merged cells, without cell comments</button>
                  </li>
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.marksRecord}">Marks record</button>
                  </li>
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.marksRecordConfidential}">Confidential marks record
                    </button>
                  </li>
                    <#-- Removed for - TAB-6217 -->
                    <#--<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.passList}">Pass list</button></li>-->
                    <#--<li><button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.passListConfidential}">Confidential pass list</button></li>-->
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.transcript}">Transcript</button>
                  </li>
                  <li>
                    <button class="btn btn-link" type="submit" name="${GenerateExamGridMappingParameters.transcriptConfidential}">Confidential transcript</button>
                  </li>
                </ul>
              </div>
            </@f.form>
            <#if features.recordBoardOutcomes>
              <@f.form cssClass="dirty-check" method="get" target="_blank">
                <@form_fields.select_course_fields />
                <div class="btn-group dropup">
                  <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown">Record outcomes&hellip; <span class="caret"></span></button>
                  <ul class="dropdown-menu">
                    <li>
                      <#assign processUrl><@routes.marks.cohort_process department academicYear /></#assign>
                      <button type="submit" name="courseSelected" formaction="${processUrl}">Process marks for this cohort</button>
                    </li>
                    <li>
                      <#assign decisionUrl><@routes.marks.cohort_outcomes department academicYear /></#assign>
                      <button type="submit" name="courseSelected" formaction="${decisionUrl}">Record decisions for this cohort</button>
                    </li>
                  </ul>
                </div>
              </@f.form>
            </#if>
          </div>
      </div>



    </div>
    <@modal.modal id='confirmModal'>
      <div class='modal-dialog' role='document'>
        <div class='modal-content'>
          <div class='modal-body'>
            <p>
              Exam grids contain restricted information. Under the University's
              <a target='_blank' href='https://warwick.ac.uk/services/sim/policies/information-governance/ig05'>information classification
                scheme</a>,
              student names and University IDs are 'protected', exam marks and provisional degree classifications are 'restricted'.
            </p>
            <p>
              When you download the data provided you are responsible for managing the security of the
              information within it. You agree to abide by the University's <a target='_blank' href='http://warwick.ac.uk/dataprotection'>
                Data Protection Policy
              </a> and the mandatory working practices for <a target='_blank'
                                                              href='https://warwick.ac.uk/services/sim/guidance-and-assessment/informationsecurity/working_practices##assets_protection2'>
                electronic information asset protection.</a>
            </p>
          </div>
          <div class='modal-footer'>
            <a class='confirm btn btn-primary'>Accept</a>
            <a data-dismiss='modal' class='btn btn-default'>Cancel</a>
          </div>
        </div>
      </div>
    </@modal.modal>
  </div>

  <@modal.modal id="edit-overcatting-modal"></@modal.modal>

  <script nonce="${nonce()}">
    jQuery(function ($) {
      $('.fix-area').fixHeaderFooter();

      var $form = $('#examGridDocuments'), $confirmModal = $('#confirmModal');
      $('a.confirm', $confirmModal).on('click', function () {
        $form.submit();
        $confirmModal.modal('hide');
        $form.find('input.download-option').remove();
      });
      $('.download-options').on('click', 'button', function (e) {
        e.preventDefault();
        e.stopPropagation();
        var $this = $(this);
        $form.find('input.download-option').remove();
        $form.append($('<input/>').attr({
          'type': 'hidden',
          'class': 'download-option',
          'name': $this.attr('name'),
          'value': true
        }));
        $confirmModal.modal('show');
      });

      $('button.edit-overcatting').each(function () {
        $(this).attr('href', '<@routes.exams.generateGrid department academicYear />/overcatting/' + $(this).data('student') + '?basedOnLevel=' + $(this).data('basedonlevel'))
          .data('target', '#edit-overcatting-modal');

      }).ajaxModalLink();

      $('a.show-more').on('click', function (e) {
        e.preventDefault();
        $(this).parent().next('.more').removeClass('hidden').end().end()
          .hide();
      });
      $('a.show-less').on('click', function (e) {
        e.preventDefault();
        $(this).closest('.more').addClass('hidden').parent().find('a.show-more').show();
      });

      $('#examGridContainer').show();
      $('#examGridSpinner').hide();
      $('#examGridContainer table.grid').parent().doubleScroll();

      if (navigator.platform !== 'MacIntel') {
        // fix the grid scrollbar to the footer
        var $scrollWrapper = $('.doubleScroll-scroll-wrapper');
        var $grid = $('.grid');
        $scrollWrapper.prependTo('.fix-footer').css('margin-bottom', '10px');

        function reflowScroll() {
          setTimeout(function () {
            $scrollWrapper
              // Update the width of the scroll track to match the container
              .width($scrollWrapper.parent().width())
              // Update the scroll bar so it reflects the width of the grid
              .children().width($grid.width()).end()
              // Reset the scroll bar to the initial position
              .scrollLeft(0);
          }, 0);
        }

        $(window).on('id7:reflow', reflowScroll);
        reflowScroll();
      }

      setTimeout(function () {
        $('.key table').css('max-width', '');
      }, 1);

      $('.use-popover').on('shown.bs.popover', function (e) {
        var $target = $(e.target).popover().data('bs.popover').tip();
        $target.find('.use-popover').tabulaPopover({
          trigger: 'click focus',
          container: 'body'
        });
      });

      $('.student-checker').on('submit', function (e) {
        e.preventDefault();
        var $form = $(this);
        $.ajax({
          type: "POST",
          url: $form.attr('action'),
          data: $form.serialize(),
          success: function (response) {
            $('.student-checker-modal').find('.modal-content').html(response);
            $('.student-checker-modal').modal('show')
          }
        });
      })
    });
  </script>

</#escape>
