/* global jQuery, doRequest */

/**
 * Scripts used only by the small group teaching admin section.
 */
(($) => {
  const exports = {};

  exports.zebraStripeGroups = ($module) => {
    $module.find('.group-info').filter(':visible:even').addClass('alt-row');
  };

  // take anything we've attached to "exports" and add it to the global "Groups"
  // we use extend() to add to any existing variable rather than clobber it
  window.Groups = jQuery.extend(window.Groups, exports);

  $(() => {
    exports.fixHeaderFooter = $('.fix-area').fixHeaderFooter();
    window.Groups = jQuery.extend(window.Groups, exports);
    $('#action-submit').closest('form').on('click', '.update-only', function onClick() {
      $('#action-submit').val('update');
      $('#action-submit').closest('form').find('[type=submit]').attr('disabled', true);
      $(this).attr('disabled', false);
    });

    // Zebra striping on lists of modules/groups
    $('.module-info').each((i, module) => {
      exports.zebraStripeGroups($(module));
    });

    $('.module-info.empty').css('opacity', 0.66)
      .find('.module-info-contents').hide()
      .end()
      .click(function onClick() {
        $(this).css('opacity', 1)
          .find('.module-info-contents').show()
          .end();
      })
      .hide();

    $('.dept-show').click(function onClick(event) {
      event.preventDefault();
      const hideButton = $(this).find('a');

      $('.striped-section.empty').toggle('fast', () => {
        if ($('.module-info.empty').is(':visible')) {
          hideButton.html('Hide');
          hideButton.attr('data-original-title', hideButton.attr('data-title-hide'));
        } else {
          hideButton.html('Show');
          hideButton.attr('data-original-title', hideButton.attr('data-title-show'));
        }
      });
    });

    $('.show-archived-small-groups').click((e) => {
      e.preventDefault();
      $(e.target).hide().closest('.striped-section').find('.item-info.archived')
        .show();
    });

    // enable/disable the "sign up" buttons on the student groups homepage
    $('#student-groups-view .sign-up-button').addClass('disabled use-tooltip').prop('disabled', true).attr('title', 'Please select a group');
    $('#student-groups-view input.group-selection-radio').change(function onChange() {
      $(this).closest('.item-info').find('.sign-up-button').removeClass('disabled use-tooltip')
        .prop('disabled', false)
        .attr('title', '');
    });
  });


  // modals use ajax to retrieve their contents
  $(() => {
    $('body').on('click', 'a[data-toggle=modal]', function onClick(e) {
      e.preventDefault();
      const $this = $(this);
      const $target = $($this.attr('data-target'));
      const url = $this.attr('href');
      if (url !== undefined) {
        $target.load(url, () => {
          $target.find('form.double-submit-protection').tabulaSubmitOnce();
        });
      }
    });
    $('body').on('click', 'li.disabled a', (e) => {
      e.preventDefault();
      e.stopImmediatePropagation();
    });

    $('#modal-container').on('click', "input[type='submit']", function onClick(e) {
      e.preventDefault();
      const $this = $(this);
      const $form = $this.closest('form').trigger('tabula.ajaxSubmit');
      $form.removeClass('dirty');
      const updateTargetId = $this.data('update-target');

      const randomNumber = Math.floor(Math.random() * 10000000);

      $.post(`${$form.attr('action')}?rand=${randomNumber}`, $form.serialize(), (data) => {
        $('#modal-container ').modal('hide');
        if (updateTargetId) {
          $(updateTargetId).html(data);
        } else {
          window.location.reload();
        }
      });
    });
  });

  // Week selector and location picker
  $(() => {
    $('table.week-selector').each(function eachTable() {
      const $table = $(this);

      const updateCell = ($cell, value) => {
        const groupRunningText = 'Group running on ';
        const $label = $cell.find('[data-original-title]');
        if (value) {
          $label.attr('data-original-title', groupRunningText + $label.data('original-title'));
        } else {
          const originalTitle = $label.data('original-title');
          if (originalTitle) {
            $label.attr('data-original-title', originalTitle.replace(groupRunningText, ''));
          }
        }
      };

      $table.find('tbody tr').each(function eachRow() {
        $(this).bigList({
          onChange() {
            updateCell($(this).closest('td'), $(this).is(':checked'));
          },
        });
      });

      $table.find('.show-vacations').each(function eachCheckbox() {
        const $checkbox = $(this);

        if ($table.find('tr.vacation td').find(':checked').length) {
          $checkbox.prop('checked', true);
        }

        function updateDisplay() {
          if ($checkbox.is(':checked')) {
            $table.find('tr.vacation').show();
          } else {
            $table.find('tr.vacation').hide();
          }
        }

        updateDisplay();

        $checkbox.on('change', updateDisplay);
      });
    });

    const $location = $('#location, #defaultLocation');
    const $locationId = $('#locationId, #defaultLocationId');
    const $namedLocationAlert = $('#namedLocationAlert');
    const locationValue = $location.val();
    const locationIdValue = $locationId.val();
    if ($('#useNamedLocation.errors').length > 0 || (locationValue !== undefined && locationValue.length > 0 && locationIdValue !== undefined && locationIdValue.length === 0)) {
      $namedLocationAlert.show();
    }

    $('input#location, input#defaultLocation')
      .on('change', function onChange() {
        const $this = $(this);

        if ($this.val() !== '' && $this.data('location-name') === $this.val()) {
          // Value hasn't changed since we last set the location ID
          return;
        }

        if ($this.data('lid') === undefined || $this.data('lid').length === 0) {
          $this.data('location-name', '');
          $this.closest('.form-group').find('input[type="hidden"]').val('');
          $namedLocationAlert.toggle($this.val().length > 0);
          return;
        }

        $namedLocationAlert.hide();

        $this.closest('.form-group').find('input[type="hidden"]').val($this.data('lid'));
        $this.data('lid', '').data('location-name', $this.val());
      })
      .locationPicker();

    const $locationAliasFormGroup = $('.location-alias-form-group');
    const $showLocationAlias = $('#showLocationAlias');
    const $removeLocationAlias = $('#removeLocationAlias');
    const inputValue = $locationAliasFormGroup.find('input').val();
    if (inputValue !== undefined && inputValue.length === 0) {
      $locationAliasFormGroup.hide();
    } else {
      $showLocationAlias.parent().hide();
    }

    $showLocationAlias.on('click', (e) => {
      e.preventDefault();
      $showLocationAlias.parent().hide();
      $locationAliasFormGroup.show().find('input').focus();
    });

    $removeLocationAlias.on('click', (e) => {
      e.preventDefault();
      $showLocationAlias.parent().show();
      $locationAliasFormGroup.hide();
      $locationAliasFormGroup.find('input').val('');
    });

    const $deliveryMethod = $('#deliveryMethod,#defaultDeliveryMethod');
    $deliveryMethod.on('change', (e) => {
      const value = $(e.target).val();
      const isOnline = value === 'OnlineOnly' || value === 'Hybrid';
      const isFaceToFace = value === 'FaceToFaceOnly' || value === 'Hybrid';
      const toggleFields = (selector, condition) => {
        $(selector)
          .toggle(condition)
          .find(':input').each((i, field) => {
            if (condition) {
              $(field).removeAttr('disabled');
            } else {
              $(field).attr('disabled', 'disabled');
            }
          });
      };
      toggleFields('.online-event-fields', isOnline);
      toggleFields('.f2f-event-fields', isFaceToFace);
    }).trigger('change');
  });

  function updateClearAllButton($el) {
    const $filterList = $el.closest('.student-filter, .small-groups-filter');

    if ($filterList.find('.empty-filter').length === $filterList.find('.btn-group').length) {
      $('.clear-all-filters').attr('disabled', 'disabled');
    } else {
      $('.clear-all-filters').removeAttr('disabled');
    }
  }

  function updateSearchButton($el) {
    const $filter = $el.closest('.student-filter, .small-groups-filter');
    if ($filter.find('input:checked').length > 0) {
      $filter.find('button.search').attr('disabled', false);
    } else {
      $filter.find('button.search').attr('disabled', true);
    }
  }

  function updateFilter($el) {
    // Add in route search
    // Update the filter content
    const $list = $el.closest('ul');
    const shortValues = $list.find(':checked').map(function map() {
      return $(this).data('short-value');
    }).get();
    const $fsv = $el.closest('.btn-group').find('.filter-short-values');
    if (shortValues.length) {
      $el.closest('.btn-group').removeClass('empty-filter');
      $fsv.html($fsv.data('prefix') + shortValues.join(', '));
    } else {
      $el.closest('.btn-group').addClass('empty-filter');
      $fsv.html($fsv.data('placeholder'));
    }

    updateSearchButton($el);
    updateClearAllButton($el);
  }

  function prependClearLink($list) {
    if (!$list.find('input:checked').length) {
      $list.find('.clear-this-filter').remove();
    } else if (!$list.find('.clear-this-filter').length) {
      $list.find('> ul').prepend(
        $('<li />').addClass('clear-this-filter')
          .append(
            $('<button />').attr('type', 'button')
              .addClass('btn btn-link')
              .html('Clear selected items')
              .on('click', () => {
                $list.find('input:checked').each(function eachChecked() {
                  const $checkbox = $(this);
                  $checkbox.prop('checked', false);
                  updateFilter($checkbox);
                });

                doRequest($list.closest('form'));
              }),
          )
          .append($('<hr />')),
      );
    }
  }

  // Re-usable small groups
  $(() => {
    if ($('.add-student-to-set').length > 0) {
      $('.tablesorter').find('th.sortable').addClass('header').on('click', function onClick() {
        const $th = $(this);

        function sortDescending() {
          $('#sortOrder').val(`desc(${$th.data('field')})`);
          $th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
          $th.addClass('headerSortUp');
        }

        function sortAscending() {
          $('#sortOrder').val(`asc(${$th.data('field')})`);
          $th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
          $th.addClass('headerSortDown');
        }

        const $form = $th.closest('form');

        const $section = $th.closest('.striped-section');

        if ($th.hasClass('headerSortUp')) {
          sortAscending();
        } else if ($th.hasClass('headerSortDown')) {
          sortDescending();
        } else if ($th.hasClass('unrecorded-col') || $th.hasClass('missed-col')) {
          sortDescending();
        } else {
          sortAscending();
        }

        if ($section.data('submitparam').length > 0) {
          $form.append($('<input/>').attr({
            type: 'hidden',
            name: $section.data('submitparam'),
            value: true,
          }));
        }
        $form.submit();
      });

      $('.pagination').on('click', 'a', function onClick() {
        const $this = $(this);
        const $form = $this.closest('form');
        const $section = $this.closest('.striped-section');
        if ($this.data('page').toString.length > 0) {
          $form.find('input[name="page"]').remove().end()
            .append($('<input/>').attr({
              type: 'hidden',
              name: 'page',
              value: $this.data('page'),
            }));
        }
        if ($section.data('submitparam').length > 0) {
          $form.find(`input[name="${$section.data('submitparam')}"]`).remove().end()
            .append($('<input/>').attr({
              type: 'hidden',
              name: $section.data('submitparam'),
              value: true,
            }));
        }
        $form.submit();
      });

      $('.student-filter input, .small-groups-filter input').on('change', function onChange() {
        // Load the new results
        const $checkbox = $(this);
        updateFilter($checkbox);
      });

      // Re-order elements inside the dropdown when opened
      $('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function onClick() {
        const $this = $(this);
        if (!$this.closest('.btn-group').hasClass('open')) {
          // Re-order before it's opened!
          const $list = $this.closest('.btn-group').find('.filter-list');
          const items = $list.find('li.check-list-item').get();

          items.sort((a, b) => {
            const aChecked = $(a).find('input').is(':checked');
            const bChecked = $(b).find('input').is(':checked');

            if (aChecked && !bChecked) return -1;
            if (!aChecked && bChecked) return 1;
            return $(a).data('natural-sort') - $(b).data('natural-sort');
          });

          $.each(items, (item, el) => {
            $list.find('> ul').append(el);
          });

          prependClearLink($list);
        }
      });

      $('.clear-all-filters').on('click', () => {
        $('.filter-list').each(function eachList() {
          const $list = $(this);

          $list.find('input:checked').each(function eachChecked() {
            const $checkbox = $(this);
            $checkbox.prop('checked', false);
            updateFilter($checkbox);
          });

          prependClearLink($list);
        });
      });
    }
  });

  $(() => {
    $('.manually-added')
      .find('.for-check-all').append(
        $('<input/>').addClass('check-all use-tooltip').attr({
          type: 'checkbox',
          title: 'Select all/none',
        }).addClass('collection-check-all'),
      ).end();

    $('.table-biglist').bigList();
  });
})(jQuery);
