<#import "*/csrf_macros.ftl" as csrf_macros />

<#escape x as x?html>

  <#function route_function dept>
      <#local selectCourseCommand><@routes.exams.generateGrid dept academicYear /></#local>
      <#return selectCourseCommand />
  </#function>

  <@fmt.id7_deptheader title="${selectCourseActionTitle} for ${department.name}" route_function=route_function />

  <h2>Select courses</h2>

  <#assign cancelUrl><@routes.marks.adminhome department academicYear /></#assign>
  <form method="get" action="${selectCourseAction}" class="form-inline select-course">
    <@csrf_macros.csrfHiddenInputField />
    <#assign chooseYears=false />
    <#include "../../exams/grids/generate/_select_course_fields.ftl" />
    <@bs3form.errors path="selectCourseCommand" />

    <div class="submit-buttons fix-footer">
      <input name="courseSelected" type="submit" class="btn btn-primary" value="${selectCourseActionLabel}" />
      <a class="btn btn-default dirty-check-ignore" href="${cancelUrl}">Cancel</a>
    </div>
  </form>

  <script nonce="${nonce()}">
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
                    .html('<i class="fa fa-ban"></i> Clear selected items')
                    .on('click', function (e) {
                      $list.find('input:checked').each(function () {
                        var $checkbox = $(this);
                        $checkbox.prop('checked', false);
                        updateFilter($checkbox);
                      });
                      hideYearCheckboxesArea();
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

        updateButtons($el);
      };

      var updateButtons = function ($el) {
        var $filterList = $el.closest(".filters");

        if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
          $('.clear-all-filters').attr("disabled", "disabled");

        } else {
          $('.clear-all-filters').removeAttr("disabled");
        }

        var course = $('[name=courses]:checked');
        var yearOfStudy = $('[name=yearOfStudy]:checked');
        var studyLevel = $('[name=levelCode]:checked');

        if (course.length === 0 || (yearOfStudy.length === 0 && studyLevel.length === 0)) {
          $('.buttons button.btn-default').prop('disabled', true);
        } else {
          $('.buttons button.btn-default').prop('disabled', false);
        }
      };

      var yearCheckboxes = function () {
        var yearOfStudy = $("input[type='radio'][name='yearOfStudy']:checked, input[type='radio'][name='levelCode']:checked");
        var selectedYearOfStudy = 0;
        $('.year_info .year_info_hdr').toggleClass("hidden", yearOfStudy.length === 0);
        $('.year_info_ftr').toggleClass("hidden", yearOfStudy.length === 0);
        if (yearOfStudy.length > 0) {
          selectedYearOfStudy = isNaN(Number(yearOfStudy.val())) ? 1 : yearOfStudy.val();
          $('.year_info').find("input[type='hidden'][name='courseYearsToShow']").val("Year" + selectedYearOfStudy);
        } else {
          return;
        }
        $('.year_info .col-sm-2').each(function () {
          var $yearCheckboxDiv = $(this);
          var $yearCheckbox = $yearCheckboxDiv.find('input');
          var year = $yearCheckboxDiv.data('year');
          if (year == selectedYearOfStudy) {
            $yearCheckbox.prop("checked", true);
          } else if (year > selectedYearOfStudy) {
            $yearCheckbox.prop("checked", false);
          }
          $yearCheckbox.prop("disabled", (selectedYearOfStudy == year));
          $yearCheckboxDiv.toggleClass("hidden", selectedYearOfStudy < year);
        });
      };

      var hideYearCheckboxesArea = function () {
        $('.year_info .year_info_hdr').addClass("hidden");
        $('.year_info_ftr').addClass("hidden");
        $('.year_info .col-sm-2').each(function () {
          var $yearCheckboxDiv = $(this);
          var $yearCheckbox = $yearCheckboxDiv.find('input');
          $yearCheckboxDiv.addClass("hidden");
          $yearCheckbox.prop("checked", false);
        });
      };

      $('form.select-course .filters').on('change', function (e) {
        updateFilter($(e.target));
        yearCheckboxes();
      });
      $('form.select-course .filters .filter-list').find('input:first').trigger('change');

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
        hideYearCheckboxesArea();
      });

      <#if department.enableLevelGrids >
      $('.level,.block').hide();
      var $gridScopeRadio = $('input[name=gridScope]');
      $gridScopeRadio.on('change', function () {
        if (this.value === "level") {
          $('.block').hide().find('input').prop('disabled', true);
          $('.level').show().find('input').prop('disabled', false);
        } else if (this.value === "block") {
          $('.level').hide().find('input').prop('disabled', true);
          $('.block').show().find('input').prop('disabled', false);
        }
      });

      if ($('.block input:checked').length) {
        $('input[name=gridScope][value=block]').prop('checked', true).trigger('change');
      } else if ($('.level input:checked').length) {
        $('input[name=gridScope][value=level]').prop('checked', true).trigger('change');
      }
      </#if>

    })
  </script>
</#escape>
