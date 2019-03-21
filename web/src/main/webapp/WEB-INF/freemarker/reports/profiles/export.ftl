<#escape x as x?html>

  <h1>Export profiles</h1>

  <#if jobId??>

    <h2>Report generating progress</h2>

    <div class="progress">
      <div class="progress-bar progress-bar-striped active" style="width: 0;"></div>
    </div>

    <p class="report-progress">Starting report</p>

    <div class="report-complete alert alert-info" style="display: none;">
      <h3>Report generation status</h3>
      <p><@fmt.p command.students?size "report" /> generated successfully</p>
      <p>
        Reports generated by Tabula contain only information held by Tabula.
        Information held on other systems, or existing as printed material only, will need to be added seperately to each student's files.
      </p>
      <p><a href="<@routes.reports.profileExportReportZip department academicYear jobId />" class="btn btn-default"><i class="fa fa-arrow-circle-o-down"></i>
          Download reports as a .zip file</a></p>
    </div>

    <script>
      jQuery(function ($) {
        var updateProgress = function () {
          $.get('<@routes.reports.profileExportReportProgress department academicYear jobId />', function (data) {
            if (data.succeeded) {
              $('.progress .progress-bar').width("100%").removeClass('active progress-striped');
              $('.report-progress').empty();
              $('.report-complete').show();
            } else {
              $('.progress .progress-bar').width(data.progress + "%");
              if (data.status) {
                $('.report-progress').html(data.status);
              }
              setTimeout(updateProgress, 5 * 1000);
            }
          });
        };
        updateProgress();
      });
    </script>

  <#else>
    <div class="alert alert-danger">
      <@f.errors path="command.students" cssClass="error" />
    </div>
  </#if>
</#escape>