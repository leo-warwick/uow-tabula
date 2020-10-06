/* eslint-env browser */
import $ from 'jquery';

$(() => {
  $('.fix-area').fixHeaderFooter();

  function autoGradeSelect(i, el) {
    const $input = $(el);
    const generateUrl = $input.data('generate-url');
    const $markInput = $input.closest('form').find(`[name="${$input.data('mark')}"]`);
    const $select = $input.next('select');

    function initialiseSelect() {
      if ($select.find('option').length > 1) {
        $input.hide().prop('disabled', true);
        $select.prop('disabled', false).show();
      } else {
        $input.show().prop('disabled', false);
        $select.prop('disabled', true).hide();
      }
    }

    if ($input.length && $markInput.length && $select.length) {
      let currentRequest;

      const doRequest = () => {
        if (currentRequest !== undefined) {
          currentRequest.abort();
        }

        const data = {
          mark: $markInput.val(),
          resitAttempt: $input.data('resit-attempt') || undefined,
        };

        if ($select.is(':visible')) {
          data.existing = $select.val();
        } else if ($input.val().length > 0) {
          data.existing = $input.val();
        }

        currentRequest = $.ajax(generateUrl, {
          type: 'POST',
          data,
          success: (html) => {
            $select.html(html);
            initialiseSelect();
          },
          error: (xhr, errorText) => {
            if (errorText !== 'abort') {
              $input.show().prop('disabled', false);
            }
          },
        });
      };

      $markInput.on('change input', doRequest);
      if (!$input.hasClass('grades-already-available')) {
        doRequest();
      } else {
        initialiseSelect();
      }
    }

    $input.data('grades-initialised', true);
  }

  // Auto grade generator
  $('.auto-grade[data-mark][data-generate-url]').each(autoGradeSelect);

  // Treat enter as tab in .marks-form
  $('form.marks-form').each((i, form) => {
    const $form = $(form);

    // All visible inputs that:
    // - Aren't buttons (including submit inputs)
    // - Aren't textareas (so we can still add newlines)
    $form.on('keydown', ':input:visible:not(:button):not(input[type="submit"]):not(textarea)', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();

        // Tabbable inputs are all form inputs, buttons and links that are visible and aren't
        // excluded from tabindex
        const $tabbable = $form.find(':input, :button, a').filter(':visible:not([tabindex="-1"])');
        const currentIndex = $tabbable.index(document.activeElement);

        $tabbable.eq(currentIndex + 1).focus();
      }
    });

    $form.on('click', '[data-toggle="copy-values"][data-target]', (e) => {
      const $button = $(e.target);
      const $target = $($button.data('target'), $form);

      $target.find('.mma-suggestion-field').each((j, data) => {
        const $data = $(data);
        const $dataTarget = $(document.getElementById($data.data('target')));
        const value = $data.val();

        if ($dataTarget.is('select')) {
          const $option = $dataTarget.find('option').filter((k, option) => $(option).val() === value);
          if ($option.length === 0) {
            $dataTarget.append($('<option />').attr('value', value).text(value));
          }

          $dataTarget.val(value);
        } else if ($dataTarget.is('input[type=checkbox]')) {
          $dataTarget.prop('checked', value);
        } else {
          $dataTarget.val(value);
        }
      });
    });
  });

  $('.module-mark-modal').on('show.bs.modal', (e) => {
    const $modal = $(e.target);
    $modal.find('.auto-grade-modal[data-mark][data-generate-url]').not('[data-grades-initialised]').each(autoGradeSelect);
  });

  // module mark modals on the cohort processing page update the overview when changed
  $('.module-mark-modal').on('hidden.bs.modal', (e) => {
    const $modal = $(e.target);
    const $fields = $modal.find('input,select,textarea');
    let hasChanged = false;
    $fields.each((i, field) => {
      const $field = $(field);
      const $view = $(`[data-updated-by="${$field.attr('id')}"]`);
      const value = ($field.is('select')) ? $field.find('option:selected').text() : $field.val();
      // eslint-disable-next-line eqeqeq
      if ($field.data('initial') && $field.data('initial') != $field.val()) hasChanged = true;
      $view.text(value);
    });
    const $container = $modal.closest('td');
    const $pendingChangesIcon = $container.find('.pending-changes');
    if (hasChanged && $pendingChangesIcon.length === 0) {
      $container.prepend($(`
        <span tabindex="0" class="tabula-tooltip pending-changes" data-title="Pending changes - these won't be saved unless you process this students marks">
          <i class="fa-fw fad fa-save" aria-hidden="true"></i>
          <span class="sr-only">Pending changes - these won't be saved unless you process this students marks</span>
        </span>
      `));
    } else if (!hasChanged) {
      $pendingChangesIcon.remove();
    }
  });

  // cancel button on process module mark model resets the form
  $('.module-mark-modal .module-mark-cancel').on('click', (e) => {
    const $modal = $(e.target).closest('.module-mark-modal');
    const $fields = $modal.find('input,select,textarea');
    $fields.filter('*[data-initial]').each((i, field) => {
      const $field = $(field);
      $field.val($field.data('initial'));
    });
    $modal.modal('hide');
  });

  // per-student process checkbox drives hidden module process inputs
  const updateProcess = (checkbox) => {
    const $checkbox = $(checkbox);
    const $container = $checkbox.closest('tr');
    $container.find('input[type=hidden][name$="process"]').val($checkbox.is(':checked'));
  };

  // $('.process-checkbox').on('change', (e) => { updateProcess(e.target); });

  // if any modules require processing set the parent checkbox to true
  $('input[type=hidden][name$="process"][value=true]').closest('tr.student').find('.process-checkbox')
    .prop('checked', true)
    .each((i, checkbox) => { updateProcess(checkbox); });

  // This is intentionally at the end, after we've messed around with things
  $('.table-sortable').sortableTable({
    sortLocaleCompare: true,
    textAttribute: 'data-sortby',
  }).on('tablesorter-ready', (e) => {
    const $table = $(e.target);

    if ($table.hasClass('table-checkable')) {
      const options = $table.hasClass('process-cohort') ? {
        onChange: function onChange() {
          updateProcess(this);
        },
      } : {};

      $table.bigList(options);
    }

    /*
     * Beware: performance is garbage if you use data-dynamic-sort="true" because it will re-init
     * the whole thing every time there's a change. Probably don't use it until we've found some
     * way to optimise the amount of time it takes tablesorter to init.
     */
    if ($table.data('dynamic-sort') && !$table.data('dynamic-sort-initialised')) {
      $table.data('dynamic-sort-initialised', true);

      $table.on('change', () => $table.trigger('update'));
    }
  });

  $('.table-checkable:not(.table-sortable)').bigList({});
});
