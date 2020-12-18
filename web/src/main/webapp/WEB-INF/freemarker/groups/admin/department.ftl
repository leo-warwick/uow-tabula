<#import "*/group_components.ftlh" as components />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#escape x as x?html>
  <div class="btn-toolbar dept-toolbar">
    <#if !modules?has_content && department.children?has_content>
      <a tabindex="0" role="button" class="btn btn-default dropdown-toggle disabled use-tooltip" title="This department doesn't directly contain any modules. Check subdepartments.">
        Manage
      </a>
    <#else>
      <div class="btn-group dept-settings">
        <a class="btn btn-default dropdown-toggle" data-toggle="dropdown" href="#">
          Manage
          <span class="caret"></span>
        </a>
        <ul class="dropdown-menu pull-right">
          <li>
            <#assign settings_url><@routes.groups.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
            <@fmt.permission_button
            permission='Department.ManageDisplaySettings'
            scope=department
            action_descr='manage department settings'
            href=settings_url
            >
              Department settings
            </@fmt.permission_button>
          </li>
          <li>
            <#assign settings_url><@routes.groups.notificationsettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
            <@fmt.permission_button
            permission='Department.ManageNotificationSettings'
            scope=department
            action_descr='manage department notification settings'
            href=settings_url
            >
              Notification settings
            </@fmt.permission_button>
          </li>

          <li class="divider"></li>

          <li<#if !modules?has_content> class="disabled"</#if>>
            <#assign import_url><@routes.groups.import_groups_for_year department adminCommand.academicYear /></#assign>
            <@fmt.permission_button
            permission='SmallGroups.ImportFromExternalSystem'
            scope=department
            action_descr='import small groups from Syllabus+'
            href=import_url
            >
              Create small groups from Syllabus+
            </@fmt.permission_button>
          </li>
          <li<#if !modules?has_content || !features.smallGroupTeachingSpreadsheetImport> class="disabled"</#if>>
            <#assign import_url><@routes.groups.import_spreadsheet department adminCommand.academicYear /></#assign>
            <@fmt.permission_button
            permission='SmallGroups.ImportFromExternalSystem'
            scope=department
            action_descr='import small groups from a spreadsheet'
            href=import_url
            >
              Import small groups from a spreadsheet
            </@fmt.permission_button>
          </li>
          <li<#if !modules?has_content> class="disabled"</#if>>
            <#assign copy_url><@routes.groups.copyDepartment department /></#assign>
            <@fmt.permission_button
            permission='SmallGroups.Create'
            scope=department
            action_descr='copy groups from previous years'
            href=copy_url
            >
              Copy small groups from previous years
            </@fmt.permission_button>
          </li>
          <#if features.smallGroupCrossModules>
            <li>
              <#assign cross_module_url><@routes.groups.crossmodulegroups department adminCommand.academicYear /></#assign>
              <@fmt.permission_button
              permission='SmallGroups.Create'
              scope=department
              action_descr='create reusable small group allocations'
              href=cross_module_url
              >
                Reusable small groups
              </@fmt.permission_button>
            </li>
          </#if>
          <li>
            <#assign open_url><@routes.groups.missing_map_locations department adminCommand.academicYear /></#assign>
            <@fmt.permission_button
            permission='SmallGroups.Update'
            scope=department
            action_descr='events with missing map location'
            href=open_url
            >
              Events without a map location
            </@fmt.permission_button>
          </li>
          <li>
              <#assign open_url><@routes.groups.missing_online_locations department adminCommand.academicYear /></#assign>
              <@fmt.permission_button
              permission='SmallGroups.Update'
              scope=department
              action_descr='events with missing URLs'
              href=open_url
              >
                Events without an online delivery URL
              </@fmt.permission_button>
          </li>


          <li class="divider"></li>

          <#if features.smallGroupTeachingStudentSignUp>
            <li <#if !hasOpenableGroupsets> class='disabled use-tooltip' title='There are no self-signup groups to open' </#if>>
              <#assign open_url><@routes.groups.batchopen department adminCommand.academicYear /></#assign>
              <@fmt.permission_button
              permission='SmallGroups.Update'
              scope=department
              action_descr='open small groups'
              href=open_url
              >
                Open
              </@fmt.permission_button>
            </li>
            <li <#if !hasCloseableGroupsets> class='disabled use-tooltip' title='There are no self-signup groups to close'</#if>>
              <#assign close_url><@routes.groups.batchclose department adminCommand.academicYear /></#assign>
              <@fmt.permission_button
              permission='SmallGroups.Update'
              scope=department
              action_descr='close small groups'
              href=close_url
              >
                Close
              </@fmt.permission_button>
            </li>
          </#if>

          <li <#if !hasUnreleasedGroupsets> class='disabled use-tooltip' title='All modules already published'</#if>>
            <#assign notify_url><@routes.groups.batchnotify department viewedAcademicYear /></#assign>
            <@fmt.permission_button
            permission='SmallGroups.Update'
            scope=department
            action_descr='publish groups to students and staff'
            href=notify_url
            >
              Publish
            </@fmt.permission_button>
          </li>

          <li class="divider"></li>

          <li>
            <a <#if !hasGroupAttendance> class="disabled"</#if> href="<@routes.groups.departmentAttendance department adminCommand.academicYear />">View attendance</a>
          </li>

          <#if can.do('SmallGroupEvents.Register', department)>
            <li>
              <a href="<@routes.groups.printRegisters department adminCommand.academicYear />">Print registers</a>
            </li>
          </#if>
        </ul>
      </div>
    </#if>
  </div>

  <#function route_function dept>
    <#local result><@routes.groups.departmenthome dept adminCommand.academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader title="${department.name}" route_function=route_function />

  <#if !hasGroups && !isFiltered>
    <p class="alert alert-info empty-hint">There are no small groups set up for ${adminCommand.academicYear.label} in ${department.name}.</p>
  </#if>

  <h2>Create groups</h2>

  <div class="form-inline creation-form" style="margin-bottom: 10px;">
    <label for="module-picker">For this module:</label>
    <select id="module-picker" class="form-control" style="width: 360px;" placeholder="Start typing a module code or name&hellip;">
      <option value=""></option>
      <#list modules as module>
        <option value="${module.code}"><@fmt.module_name module false /></option>
      </#list>
    </select>
    <button type="button" class="btn btn-default disabled">Create</button>
  </div>

  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      <#assign template_module={"code":"__MODULE_CODE__"} />
      var url = '<@routes.groups.createset template_module />?academicYear=${adminCommand.academicYear.startYear?c}';

      var $picker = $('.creation-form #module-picker');
      var $button = $('.creation-form button');

      $picker.comboTypeahead();

      var manageButtonState = function (value) {
        if (value) {
          $button.removeClass('disabled').addClass('btn-primary');
        } else {
          $button.addClass('disabled').removeClass('btn-primary');
        }
      };
      manageButtonState($picker.find(':selected').val());

      $picker.on('change', function () {
        var value = $(this).find(':selected').val();
        manageButtonState(value);
      });
      $button.on('click', function () {
        if ($button.is('.disabled')) return;

        var moduleCode = $picker.find(':selected').val();
        if (moduleCode) {
          window.location.href = url.replace('__MODULE_CODE__', moduleCode);
        }
      });
    })(jQuery);
  </script>

  <#if hasGroups || isFiltered>
  <#-- Filtering -->
    <div class="fix-area">
      <div class="fix-header pad-when-fixed">
        <@f.form modelAttribute="adminCommand" action="${info.requestedUri.path}" method="GET" cssClass="form-inline">
        <@f.errors cssClass="error form-errors" />

        <div class="small-groups-filter btn-group-group well well-sm">
          <button type="button" class="clear-all-filters btn btn-link">
							<span class="fa-stack">
								<i class="fa fa-filter fa-stack-1x"></i>
								<i class="fa fa-ban fa-stack-2x"></i>
							</span>
          </button>

          <#macro filter path placeholder currentFilter allItems validItems=allItems prefix="" customPicker="">
            <@spring.bind path=path>
              <div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
                <a class="btn btn-xs btn-default dropdown-toggle" href="#" data-toggle="dropdown">
                  <span class="filter-short-values" data-placeholder="${placeholder}"
                        data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
                  <span class="caret"></span>
                </a>
                <div tabindex="-1" class="dropdown-menu filter-list">
                  <button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
                  <ul>
                    <#if customPicker?has_content>
                      <li><#noescape>${customPicker}</#noescape></li>
                    </#if>
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
                        <li class="check-list-item" data-natural-sort="${item_index}">
                          <label class="checkbox <#if !isValid>disabled</#if>">
                            <#nested item isValid/>
                          </label>
                        </li>
                      </#list>
                    <#else>
                      <li>
                        <small class="muted" style="padding-left: 5px;">N/A for this department</small>
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

          <#function contains_by_filter_name collection item>
            <#list collection as c>
              <#if c.name == item.name>
                <#return true />
              </#if>
            </#list>
            <#return false />
          </#function>

          <#assign placeholder = "All modules" />
          <#assign modulesCustomPicker>
            <div class="module-search input-group">
              <input class="module-search-query module-picker module prevent-reload form-control" type="text" value="" placeholder="Search for a module"
                     data-department="${department.code}" data-name="moduleFilters" data-wrap="true" />
              <span class="input-group-addon"><i class="fa fa-search"></i></span>
            </div>
          </#assign>
          <#assign currentfilter><@current_filter_value "moduleFilters" placeholder; f>${f.module.code?upper_case}</@current_filter_value></#assign>
          <@filter path="moduleFilters" placeholder=placeholder currentFilter=currentfilter allItems=allModuleFilters customPicker=modulesCustomPicker; f>
            <input type="checkbox" name="${status.expression}"
                   value="${f.name}"
                   data-short-value="${f.description}"
                    ${contains_by_filter_name(adminCommand.moduleFilters, f)?string('checked','')}>
            ${f.description}
          </@filter>

          <#assign placeholder = "All types" />
          <#assign currentfilter><@current_filter_value "formatFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
          <@filter "formatFilters" placeholder currentfilter allFormatFilters; f>
            <input type="checkbox" name="${status.expression}"
                   value="${f.name}"
                   data-short-value="${f.description}"
                    ${contains_by_filter_name(adminCommand.formatFilters, f)?string('checked','')}>
            ${f.description}
          </@filter>

          <#assign placeholder = "All statuses" />
          <#assign currentfilter><@current_filter_value "statusFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
          <@filter "statusFilters" placeholder currentfilter allStatusFilters; f>
            <input type="checkbox" name="${status.expression}"
                   value="${f.name}"
                   data-short-value="${f.description}"
                    ${contains_by_filter_name(adminCommand.statusFilters, f)?string('checked','')}>
            ${f.description}
          </@filter>

          <#assign placeholder = "All allocation methods" />
          <#assign currentfilter><@current_filter_value "allocationMethodFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
          <@filter "allocationMethodFilters" placeholder currentfilter allAllocationFilters; f>
            <input type="checkbox" name="${status.expression}"
                   value="${f.name}"
                   data-short-value="${f.description}"
                    ${contains_by_filter_name(adminCommand.allocationMethodFilters, f)?string('checked','')}>
            ${f.description}
          </@filter>

          <#assign placeholder = "All terms" />
          <#assign currentfilter><@current_filter_value "termFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
          <@filter "termFilters" placeholder currentfilter allTermFilters; f>
            <input type="checkbox" name="${status.expression}"
                   value="${f.name}"
                   data-short-value="${f.description}"
                    ${contains_by_filter_name(adminCommand.termFilters, f)?string('checked','')}>
            ${f.description}
          </@filter>
          </@f.form>
        </div>
      </div>

      <div class="small-group-sets-list">
        <#-- List of students modal -->
        <@modal.modal id="students-list-modal"></@modal.modal>
        <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

        <#-- Immediately start waiting for collapsibles to load - don't wait to wire this handler in, because we initialise collapsibles before the DOM has loaded below -->
        <script type="text/javascript" nonce="${nonce()}">
          (function ($) {
            var processContainer = function ($container) {
              Groups.zebraStripeGroups($container);
              $container.mapPopups();
              $container.find('.use-tooltip').tooltip({ sanitize: false, });
              $container.find('.use-popover').tabulaPopover({
                trigger: 'click focus',
                container: 'body'
              });
            };

            $(document.body).on('loaded.collapsible', '.set-info', function () {
              var $set = $(this);
              processContainer($set);
            });
          })(jQuery);
        </script>

        <div id="filter-results">
          <i class="icon-spinner icon-spin"></i> Loading&hellip;
        </div>
      </div>
    </div>

    <@modal.modal id="modal-container"></@modal.modal>

    <script type="text/javascript" nonce="${nonce()}">
      jQuery(function ($) {

        var prependClearLink = function ($list) {
          if (!$list.find('input:checked').length) {
            $list.find('.clear-this-filter').remove();
          } else {
            if (!$list.find('.clear-this-filter').length) {
              $list.find('> ul').prepend(
                $('<li />').addClass('clear-this-filter')
                  .append(
                    $('<button />').attr('type', 'button')
                      .addClass('btn btn-link')
                      .html('<i class="icon-ban-circle"></i> Clear selected items')
                      .on('click', function (e) {
                        $list.find('input:checked').each(function () {
                          var $checkbox = $(this);
                          $checkbox.prop('checked', false);
                          updateFilter($checkbox);
                        });

                        doRequest($list.closest('form'));
                      })
                  )
                  .append($('<hr />'))
              );
            }
          }
        };

        var updateFilter = function ($el) {
          // Update the filter content
          var $list = $el.closest('ul');
          var shortValues = $list.find(':checked').map(function () {
            return $(this).data('short-value');
          }).get();
          var $fsv = $el.closest('.btn-group').find('.filter-short-values');
          if (shortValues.length) {
            $el.closest('.btn-group').removeClass('empty-filter');
            $fsv.html($fsv.data("prefix") + shortValues.join(', '));
          } else {
            $el.closest('.btn-group').addClass('empty-filter');
            $fsv.html($fsv.data('placeholder'));
          }

          updateClearAllButton($el);
        };

        var updateClearAllButton = function ($el) {
          var $filterList = $el.closest(".student-filter");

          if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
            $('.clear-all-filters').attr("disabled", "disabled");
          } else {
            $('.clear-all-filters').removeAttr("disabled");
          }
        };

        var doRequest = function ($form, preventPageReset) {
          if (typeof history.pushState !== 'undefined')
            history.pushState(null, null, $form.attr('action') + '?' + window.GlobalScripts.csrfForm.serializeWithoutCsrf($form));

          if ($form.data('request')) {
            $form.data('request').abort();
            $form.data('request', null);
          }

          if (!preventPageReset) {
            $form.find('input[name="page"]').val('1');
          }

          $('#filter-results').addClass('loading');
          $form.data('request', $.post($form.attr('action'), $form.serialize(), function (data) {
            $('#filter-results').html(data);

            $form.data('request', null);
            $('#filter-results').removeClass('loading');

            $('.use-wide-popover').tabulaPopover({
              trigger: 'click',
              container: 'body',
              template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
            });

            $('.use-tooltip').tooltip({ sanitize: false, });
            AjaxPopup.wireAjaxPopupLinks($('body'));

            // callback for hooking in local changes to results
            $(document).trigger("tabula.filterResultsChanged");
          }));
        };
        window.doRequest = doRequest;

        $('#adminCommand input').on('change', function (e) {
          // Load the new results
          var $checkbox = $(this);
          if ($checkbox.is('.prevent-reload')) return;

          var $form = $checkbox.closest('form');

          doRequest($form);
          updateFilter($checkbox);
        });

        // Re-order elements inside the dropdown when opened
        $('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function (e) {
          var $this = $(this);
          if (!$this.closest('.btn-group').hasClass('open')) {
            // Re-order before it's opened!
            var $list = $this.closest('.btn-group').find('.filter-list');
            var items = $list.find('li.check-list-item').get();

            items.sort(function (a, b) {
              var aChecked = $(a).find('input').is(':checked');
              var bChecked = $(b).find('input').is(':checked');

              if (aChecked && !bChecked) return -1;
              else if (!aChecked && bChecked) return 1;
              else return $(a).data('natural-sort') - $(b).data('natural-sort');
            });

            $.each(items, function (item, el) {
              $list.find('> ul').append(el);
            });

            prependClearLink($list);
          }
        });

        $('.clear-all-filters').on('click', function () {
          $('.filter-list').each(function () {
            var $list = $(this);

            $list.find('input:checked').each(function () {
              var $checkbox = $(this);
              $checkbox.prop('checked', false);
              updateFilter($checkbox);
            });

            prependClearLink($list);
          });

          doRequest($('#adminCommand'));
        });

        var updateFilterFromPicker = function ($picker, name, value, shortValue) {
          if (value === undefined || value.length === 0)
            return;

          shortValue = shortValue || value;

          var $ul = $picker.closest('ul');

          var $li = $ul.find('input[value="' + value + '"]').closest('li');
          if ($li.length) {
            $li.find('input').prop('checked', true);
            if ($ul.find('li.check-list-item:first').find('input').val() !== value) {
              $li.insertBefore($ul.find('li.check-list-item:first'));
            }
          } else {
            $li = $('<li/>').addClass('check-list-item').append(
              $('<label/>').addClass('checkbox').append(
                $('<input/>').attr({
                  'type': 'checkbox',
                  'name': name,
                  'value': value,
                  'checked': true
                }).data('short-value', shortValue)
              ).append(
                $picker.val()
              )
            ).insertBefore($ul.find('li.check-list-item:first'));
          }

          doRequest($('#adminCommand'));
          updateFilter($picker);
        };

        $('.module-search-query').on('change', function () {
          var $picker = $(this);
          if ($picker.data('modulecode') === undefined || $picker.data('modulecode').length === 0)
            return;

          updateFilterFromPicker($picker, 'moduleFilters', 'Module(' + $picker.data('modulecode') + ')', $picker.data('modulecode').toUpperCase());

          $picker.data('modulecode', '').val('');
        });

        doRequest($('#adminCommand'));
      });
    </script>
  </#if>
</#escape>
