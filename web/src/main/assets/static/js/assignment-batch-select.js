jQuery(function ($) {

  // editable name field
  $('.editable-name').each(function () {
    var $this = $(this);

    var $target = $('<a class="name-edit-link"><i class="fa fa-pencil"></i></a>');

    $this.editable({
      toggle: 'manual',
      mode: 'inline',
      validate: function (value) {
        if ($.trim(value) == '') {
          return "A name is required.";
        }
      }
    }).on('save', function (e, params) {
      // set the hidden field to the new value.
      var newVal = params.newValue;
      $(this).closest('.itemContainer').find('input.name-field').val(newVal);
    }).on('shown', function () {
      $target.hide();
    }).on('hidden', function () {
      $target.show();
    });

    $this.after($target.on('click', function (e) {
      e.stopPropagation();
      $this.editable('toggle');
    })).after('&nbsp;');
  });


  var optionGroupCount = $('#options-buttons .options-button').length;

  var $form = $('#batch-add-form');

  // reload page when academic field dropdown changes, as it changes the contents of the list.
  $('#academicYearSelect, #includeSubDepartmentsSelect').change(function () {
    $form.find('input[name=action]').val('change-year');
    $form.submit();
  });

  // When clicking Next, set the action parameter to the relevant value before submitting
  $form.find('button[data-action]').click(function (event) {
    var action = $(this).data('action');
    if (action) {
      $form.find('input[name=action]').val(action);
    }
  });

  // Disable submit buttons after form is submitted.
  $form.submit(function () {
    var $buttons = $('button[data-action], #batch-add-submit-button');
    $buttons.prop('disabled', true).addClass('disabled');
    $(window).on('pageshow', function () {
      $buttons.prop('disabled', false).removeClass('disabled');
    })
  });

  // Set up checkboxes for the big table

  $('#batch-add-table').bigList({
    setup: function () {
      var $container = this;

      $('#selected-deselect').click(function () {
        $container.find('.collection-checkbox, .collection-check-all').prop('checked', false);
        $container.find("tr.selected").removeClass('selected');
        $('#selected-count').text("0 selected");
        return false;
      });
    },

    onChange: function () {
      this.closest("tr").toggleClass("selected", this.is(":checked"));
      var x = $('#batch-add-table .collection-checkbox:checked').length;
      $('#selected-count').text(x + " selected");
    },

    onSomeChecked: function () {
      $('#set-options-button, #set-dates-button').removeClass('disabled');
    },

    onNoneChecked: function () {
      $('#set-options-button, #set-dates-button').addClass('disabled');
      $('#selected-count').text("0 selected");
    }
  });

  var $opts = $('#options-buttons');

  var $optsButton = $('#set-options-button');
  var $optsModal = $('#set-options-modal');
  var $optsModalBody = $optsModal.find('.modal-body');
  var optsUrl = $optsButton.attr('href');

  var decorateOptionsModal = function () {
    $optsModalBody.find('details').details();

    // do all sorts of clever stuff when we submit this form.
    $optsModalBody.find('form').on('submit', function (e) {
      e.preventDefault();
      $.post(optsUrl, $optsModalBody.find('form').serialize(), function (data) {
        $optsModalBody.html(data);
        decorateOptionsModal();
        if ($optsModalBody.find('.ajax-response').data('status') == 'success') { // passed validation
          // grab all the submittable fields and clone them to the main page form
          var fields = $optsModalBody.find('[name]').clone();

          // Generate group names alphabetically from A, continuing later with B, and then C, and so on until
          // Z. Nobody knows what happens after Z...
          var groupName = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.charAt(optionGroupCount);
          var $groupNameLabel = $('<span>').addClass('label').addClass('label-' + groupName).html(groupName);
          optionGroupCount = optionGroupCount + 1;

          var $group = $('<span>').addClass('options-button');
          var $hidden = $('<div>').addClass('options-group').data('group', groupName);
          var $button = $('<button class="btn btn-default"></button>').html('Re-use options ').append($groupNameLabel);
          $button.data('group', groupName);
          $group.append($button);
          $group.append($hidden);

          // button behaviour already wired by an on() call.

          // rename all the fields to sit under an optionsMap entry.
          fields.each(function (i, field) {
            var prefix = "optionsMap[" + groupName + "].";
            // HFC-306 if it starts with _, keep that at the start after renaming
            if (field.name.indexOf('_') == 0) {
              field.name = field.name.substring(1);
              prefix = "_" + prefix;
            }
            field.name = prefix + field.name;
            $hidden.append(field);
          });

          $opts.append($group).append(' ');
          $optsModal.modal('hide');

          applyGroupNameToSelected(groupName);
        }
      });
      return false;
    });
  };

  if ($optsButton.length !== 0 && optsUrl) {
    // eagerly pre-load the options form into the modal.
    $optsModalBody.load(optsUrl, function () {
      decorateOptionsModal();
    });
  }

  $optsButton.click(function (e) {
    if (!$(this).hasClass('disabled')) {
      $optsModal.modal();
    }
    return false;
  });

  // sets the options ID for all the checked assignments so that they will
  // use this set of options.
  var applyGroupNameToSelected = function (groupName) {
    var $label = $('<span>').addClass('label').addClass('label-' + groupName).html(groupName);
    $(".collection-checkbox:checked").closest('tr')
      .find('.options-id-input').val(groupName).end()
      .find('.options-id-label').html($label).end();
  };

  var $datesModal = $('#set-dates-modal');
  // open dates modal
  $('#set-dates-button').click(function () {
    if (!$(this).hasClass('disabled')) {
      $datesModal.modal();
    }
    return false;
  });
  // set dates
  $datesModal.find('.modal-footer .btn-primary').click(function (e) {
    var openDate = $('#modal-open-date').val();
    var openEnded = $('#modal-open-ended').is(':checked');
    var closeDate = $('#modal-close-date').val();
    var $selectedRows = $('#batch-add-table tr.selected');
    $selectedRows.find('.open-date-field').val(openDate);
    $selectedRows.find('.open-ended-field').val("" + openEnded);
    $selectedRows.find('.close-date-field').val(closeDate);
    $selectedRows.find('.dates-label').html(openDate + (openEnded ? ' (open ended)' : ' - ' + closeDate));
    $datesModal.modal('hide');
  });

  // Wire all "Re-use options" buttons, present and future
  $('#options-buttons').on('click', '.options-button > button', function () {
    applyGroupNameToSelected($(this).data('group'));
    return false;
  });

  // complicated handling for when we submit the options modal...
  // if response contains .ajax-response[data-status=success] then validation succeeded,
  // and we copy all the form fields out into the main page form to be submitted.
  $optsModal.find('.modal-footer .btn-primary').click(function () {
    $optsModal.find('form').trigger('submit');
  });

  $('input#modal-open-ended').change(function () {
    var $this = $(this);
    if ($this.is(':checked')) {
      $('#modal-close-date').prop('disabled', true);
    } else {
      $('#modal-close-date').prop('disabled', false);
    }
  });

  $('.slow-page-warning').hide('fast');
});