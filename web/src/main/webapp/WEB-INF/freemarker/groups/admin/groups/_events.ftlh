<#import "*/group_components.ftlh" as components />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#assign academicYear=smallGroupSet.academicYear />

<#-- List of students modal -->
<@modal.modal id="students-list-modal"></@modal.modal>
<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<div class="striped-section-contents">
  <#list groups as group>
    <@spring.nestedPath path="groups[${group.id}]">
      <div class="item-info">
        <div class="row">
          <div class="col-md-10 groupDetail">
            <h3 class="name inline-block">
              ${group.name!""}
              <#if ((group.students.size)!0) gt 0>
                <a href="<@routes.groups.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
                  <small><@fmt.p (group.students.size)!0 "student" "students" /></small>
                </a>
              <#else>
                <small><@fmt.p (group.students.size)!0 "student" "students" /></small>
              </#if>
            </h3>
          </div>
          <div class="col-md-2">
            <#if is_edit>
              <#assign addEventUrl><@routes.groups.editseteventsnewevent group /></#assign>
            <#else>
              <#assign addEventUrl><@routes.groups.createseteventsnewevent group /></#assign>
            </#if>
            <a class="btn btn-default pull-right" href="${addEventUrl}">Add event</a>
          </div>
        </div>

        <div class="row">
          <div class="col-md-12">
            <ul class="events unstyled">
              <#list mapGet(command.groups, group).events as event>
                <@spring.nestedPath path="events[${event_index}]">
                  <li>
                    <@f.hidden path="delete" id="group${group_index}_event${event_index}_delete" />

                    <@components.eventShortDetails event.event />
                    <@form.errors path="delete" />

                    <#assign popoverContent><@components.eventDetails event.event /></#assign>
                    <@fmt.help_popover id="events[${event_index}]" content="${popoverContent?markup_string}" html=true />

                    <div class="buttons pull-right">
                      <#if is_edit>
                        <#assign editEventUrl><@routes.groups.editseteventseditevent event.event /></#assign>
                        <#assign copyEventUrl><@routes.groups.editseteventscopyevent event.event /></#assign>
                      <#else>
                        <#assign editEventUrl><@routes.groups.createseteventseditevent event.event /></#assign>
                        <#assign copyEventUrl><@routes.groups.createseteventscopyevent event.event /></#assign>
                      </#if>

                      <a class="btn btn-xs btn-primary" href="${editEventUrl}">Edit</a>
                      <a class="btn btn-xs btn-primary" href="${copyEventUrl}">Copy</a>

                      <#if event.hasRecordedAttendance>
                        <div tabindex="0" class="use-tooltip" data-container="body" style="display: inline-block;"
                             title="This event can't be deleted as there is attendance recorded against it">
                          <button type="button" class="btn btn-danger btn-xs disabled use-tooltip" aria-label="Close">
                            <i class="fa fa-times"></i>
                          </button>
                        </div>
                      <#else>
                        <button type="button" class="btn btn-danger btn-xs" data-toggle="delete" data-value="true"
                                data-target="#group${group_index}_event${event_index}_delete" aria-label="Delete">
                          <i class="fa fa-times"></i>
                        </button>
                      </#if>

                      <button type="button" class="btn btn-primary btn-xs" data-toggle="delete" data-value="false"
                              data-target="#group${group_index}_event${event_index}_delete" aria-label="Undo">
                        <i class="fa fa-undo"></i>
                      </button>

                    </div>
                  </li>
                </@spring.nestedPath>
              </#list>
            </ul>
          </div>
        </div>
      </div>
    </@spring.nestedPath>
  </#list>
</div>

<script type="text/javascript" nonce="${nonce()}">
  jQuery(function ($) {
    $('.events button[data-toggle="delete"]').each(function () {
      var $button = $(this);
      var $li = $button.closest('li');
      var $target = $($button.data('target'));
      var value = "" + $button.data('value');

      if ($target.val() === value) {
        $button.hide();

        if (value === "true") {
          $li.addClass('deleted');
        }
      }

      $button.on('click', function () {
        $target.val(value);

        if (value === "true") {
          $li.addClass('deleted');
        } else {
          $li.removeClass('deleted');
        }

        $button.hide();
        $li.find('button[data-toggle="delete"]').filter(function () {
          var $otherButton = $(this);
          var otherValue = "" + $otherButton.data('value');

          return otherValue != value && $otherButton.data('target') == $button.data('target');
        }).show();
      });
    });
  });
</script>

<style type="text/css" nonce="${nonce()}">
  .item-info .events li {
    line-height: 30px;
    padding: 0 3px;
  }

  .item-info .events li button {
    margin-top: 0;
  }

  .item-info .events li:hover {
    background: #dddddd;
  }
</style>
