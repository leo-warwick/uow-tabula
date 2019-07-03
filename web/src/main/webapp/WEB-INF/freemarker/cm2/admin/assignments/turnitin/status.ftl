<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

  <@cm2.assignmentHeader "Turnitin status" assignment "for" />

  <p>This page will update itself automatically. You'll be sent an email when it completes so you don't have to keep this page open.</p>

  <p>When the job is finished you'll be able to see the results on the <a href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">submissions
      page</a>.</p>

  <div id="job-progress">
    <div class="progress">
      <div class="progress-bar progress-bar-striped <#if !status.finished>active</#if>" style="min-width: 5%; width: ${status.progress}%;"></div>
    </div>

    <div id="job-status-value" data-progress="${status.progress}" data-succeeded="${status.succeeded?string}" data-finished="${status.finished?string}">
      <p>${status.status}</p>
    </div>
  </div>

  <script nonce="${nonce()}">
    (function ($) {
      function buildStatus(field, type, string) {
        return field + " " + type + ((field != 1) ? "s" : "") + string;
      }

      var updateStatus = function () {
        var $progress = $('#job-progress').find('.progress-bar');
        $.get('<@routes.cm2.submitToTurnitinStatus assignment />', function (data) {
          $progress.width(data.progress + '%');
          if (data.finished) {
            $progress.removeClass('active');
            if (!data.succeeded) {
              $progress.addClass('progress-bar-warning');
            } else {
              $progress.addClass('progress-bar-success');
            }
            $('#job-status-value').find('p').empty().html(data.status);
          } else {
            var statuses = [];
            if (data.reportReceived) {
              statuses.push(buildStatus(data.reportReceived, "report", " received"))
            }
            if (data.reportRequested) {
              statuses.push(buildStatus(data.reportRequested, "report", " requested"))
            }
            if (data.fileSubmitted) {
              statuses.push(buildStatus(data.fileSubmitted, "file", " submitted"))
            }
            if (data.awaitingSubmission) {
              statuses.push(buildStatus(data.awaitingSubmission, "file", " awaiting submission"))
            }
            $('#job-status-value').find('p').empty().html(data.status + statuses.join(", "));
            setTimeout(updateStatus, 2000);
          }
        }).fail(function(){
          $progress.width('100%');
          $progress.addClass('progress-bar-warning');
          $('#job-status-value').find('p').empty().html("Unable to retrieve progress, please try again later.");
        });
      };
      updateStatus();
    })(jQuery);
  </script>

</#escape>