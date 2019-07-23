<#import 'form_fields.ftl' as form_fields />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#escape x as x?html>

  <#function route_function dept>
    <#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
    <#return selectCourseCommand />
  </#function>

  <@fmt.id7_deptheader title="Create a new exam grid for ${department.name}" route_function=route_function />

  <form action="<@routes.exams.generateGridSkipImport department academicYear />" class="dirty-check" method="post">

    <@form_fields.select_course_fields />
    <@form_fields.grid_options_fields />

    <h2>Importing student data</h2>

    <p class="progress-arrows">
      <span class="arrow-right"><a class="btn btn-link" href="<@routes.exams.generateGrid department academicYear />?${uriParser(gridOptionsQueryString)}">Select courses</a></span>
      <span class="arrow-right arrow-left"><a class="btn btn-link"
                                              href="<@routes.exams.generateGridOptions department academicYear />?${uriParser(gridOptionsQueryString)}">Set grid options</a></span>
      <span class="arrow-right arrow-left active">Preview and download</span>
    </p>

    <div class="alert alert-info">
      <div class="progress">
        <div class="progress-bar progress-bar-striped active" style="width: ${jobProgress!0}%;"></div>
      </div>
      <p class="job-status">${jobStatus!"Waiting for job to start"}</p>
    </div>
    <p>
      Tabula is currently importing fresh data for the students you selected from SITS.
      You can <a href="#" data-toggle="modal" data-target="#student-import-dates">view the last import date for each student</a>.
      If you wish you can skip this import and proceed to generate the grid.
    </p>
    <#if oldestImport??>
      <p>
        If you skip the import, the grid will be generated from data available in SITS at
        <@fmt.date date=oldestImport capitalise=false at=true relative=true />.
      </p>
    </#if>
    <@modal.modal id="student-import-dates">
      <@modal.wrapper>
        <@modal.body>
          <table class="table table-condensed table-striped table-hover">
            <thead>
            <tr>
              <th>Name</th>
              <th>Last imported date</th>
            </tr>
            </thead>
            <tbody>
            <#list studentLastImportDates as studentDate>
              <tr>
                <td>${studentDate._1()}</td>
                <td><@fmt.date studentDate._2() /></td>
              </tr>
            </#list>
            </tbody>
          </table>
        </@modal.body>
      </@modal.wrapper>
    </@modal.modal>

    <button class="btn btn-primary" type="submit">Skip import and generate grid</button>
  </form>

  <script nonce="${nonce()}">
    jQuery(function ($) {
      var updateProgress = function () {
        $.ajax({
          type: "POST",
          url: '<@routes.exams.generateGridProgress department academicYear />',
          data: {'jobId': '${jobId}'},
          error: function (jqXHR, textstatus, message) {
            if (textstatus === "timeout") {
              updateProgress();
            } else {
              // not a timeout - some other JS error - advise the user to reload the page
              var $progressContainer = $('.alert').removeClass('alert-info').addClass('alert-warning');
              $progressContainer.find('.progress-bar').addClass("progress-bar-danger");
              var messageEnd = jqXHR.status === 403 ? ", it appears that you have signed out. Please refresh this page." : ". Please refresh this page.";
              $progressContainer.find('.job-status').html("Unable to check the progress of your import" + messageEnd);
            }
          },
          success: function (data) {
            if (data.finished) {
              window.location = '<#noescape><@routes.exams.generateGridPreview department academicYear />?${uriParser(gridOptionsQueryString)}</#noescape>';
            } else {
              if (data.progress) {
                $('.progress .progress-bar').css('width', data.progress + '%');
              }
              if (data.status) {
                $('.job-status').html(data.status);
              }
              setTimeout(updateProgress, 5 * 1000);
            }
          },
          timeout: 5000
        });
      };
      updateProgress();
    });
  </script>

</#escape>