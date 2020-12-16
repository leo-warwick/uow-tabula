/* eslint-env browser */
import $ from 'jquery';

/**
 * Scripts used when recording attendance in Attendance Monitoring or Small Group Teaching.
 */

var exports = {};

exports.createButtonGroup = function (id) {
  var $this = $(id), selectedValue = $this.find('option:selected').val(), disabledOptions = $this.find('option.disabled');

  var disabledOptionValues = $.map(disabledOptions, function (option) {
    return option.value
  });

  var clonedButtons = $('.recordCheckpointForm div.forCloning div.btn-group')
    .clone(true)
    .insertAfter($this);

  var activeButton = clonedButtons
    .find('button').filter(function () {
      return $(this).data('state') == selectedValue;
    }).addClass('active').removeClass('disabled');

  if ($this.attr('title') && $this.attr('title').length > 0) {
    activeButton.attr('title', '<p>' + activeButton.attr('title') + '</p>' + $this.attr('title'));
  }

  clonedButtons.find('button').each(function () {
    var index = $.inArray($(this).data('state'), disabledOptionValues);
    if (index > -1) {
      $(this).addClass('disabled').attr('title', disabledOptions[index].getAttribute('title'));
    }
  });

  $this.hide();
};

exports.wireButtons = function (id) {
  var $this = $(id);

  $this.find('.use-popover')
    .on('shown.bs.popover', function (e) {
      var $po = $(e.target).popover().data('bs.popover').tip();
      $po.find('[data-action="remove"]').on('click', function () {
        var id = $(this).attr('data-student');
        $('form#recordAttendance')
          .prepend($('<input />').attr({'type': 'hidden', 'name': 'removeAdditionalStudent', 'value': id}))
          .prepend($('<input />').attr({'type': 'hidden', 'name': 'action', 'value': 'refresh'}))
          .submit();
      });
    })
    .tabulaPopover({
      trigger: 'click',
      container: 'body'
    });
};

var updateAttendanceState = function (e, $this) {
  if ($this.is('.disabled')) {
    e.stopPropagation();
    e.preventDefault();
    return false;
  } else {
    var state = $this.data('state');
    $this.closest('div.pull-right').find('option').filter(function () {
      return $(this).val() == state;
    }).prop('selected', true);
    var noteButton = $this.closest('div.pull-right').find('a.attendance-note');
    noteButton.attr('href', GlobalScripts.setArgOnUrl(noteButton.attr('href'), 'state', $this.data('state')));
  }
};

var checkForCheckpoints = function () {
  var attendanceData = {}, $attendanceForm = $('form#recordAttendance');
  $attendanceForm.find('select').each(function () {
    attendanceData[$(this).data('universityid')] = $(this).val();
  });
  $attendanceForm.find('.checkpoints-message').hide();
  $.post('/attendance/check/smallgroup', {
    'occurrence': $attendanceForm.data('occurrence'),
    'attendances': attendanceData
  }, function (data) {
    var states = [], buildState = function (students, state) {
      if (students.length > 0) {
        states.push($('<span/>').append(
          $('<a/>').attr({
            'class': 'use-popover',
            'data-placement': 'top',
            'data-container': 'body',
            'data-html': 'true',
            'data-content': '<ul>' + $.map(students, function (student) {
              return '<li>' + student.name + '</li>'
            }).join('') + '</ul>'
          }).html(students.length + ((students.length === 1) ? ' student' : ' students')),
          ' as ' + state
        ));
        states.push(', ');
      }
    };

    buildState(data.attended, 'attended');
    buildState(data.missedUnauthorised, 'missed (unauthorised)');
    buildState(data.missedAuthorised, 'missed (authorised)');

    var $container = $attendanceForm.find('.checkpoints-message .inner').empty();
    $.each(states.slice(0, states.length - 1), function (i, state) {
      $container.append(state);
    });
    if (states.length > 0) {
      $container.parent().show();
    }
    $('.use-popover').tabulaPopover({
      trigger: 'click',
      container: 'body'
    });
  });
};

exports.bindButtonGroupHandler = function () {

  var $recordForm = $('.recordCheckpointForm'), enableCheckForCheckpoints = $recordForm.data('check-checkpoints');
  $recordForm.on('click', 'tbody div.btn-group button, .striped-section-contents div.btn-group button', function (e) {
    var $this = $(this);
    updateAttendanceState(e, $this);
    $this.trigger('checkform.areYouSure');

    if (enableCheckForCheckpoints) {
      checkForCheckpoints();
    }

    var noteButton = $this.closest('div.pull-right').find('a.attendance-note');
    // if the row has no note and authorised was clicked then open the attendance note popup
    if ($this.data('state') === "authorised" && !noteButton.hasClass("edit")) {
      noteButton.click();
    }
  });

  if (enableCheckForCheckpoints) {
    $(function () {
      checkForCheckpoints();
    });
  }
};

// SCRIPTS FOR RECORDING MONITORING POINTS
exports.recordMonitoringPointsScripts = function () {

  var $recordForm = $('.recordCheckpointForm'), enableCheckForCheckpoints = $recordForm.data('check-checkpoints');
  $recordForm.on('click.saveButtonPrompt', 'div.btn-group button', function () {
    $recordForm.off('click.saveButtonPrompt').find('.submit-buttons .btn-primary')
      .data({
        'content': 'Remember to save any changes you make by clicking \'Save\'',
        'placement': 'top',
        'container': '.fix-footer'
      }).tabulaPopover({'trigger': 'manual'}).off('click.fixSaving').on('click.fixSaving', function () {
      // TAB-4275
      $(this).closest('form').trigger('submit');
    });
    window.setTimeout(function () {
      $recordForm.find('.submit-buttons .btn-primary').popover('show');
    }, 100);
  }).find('.fix-header')
    .find('div.pull-right').show()
    .end().each(function () {
    $(this).find('.btn-group button').each(function (i) {
      $(this).on('click', function (e) {
        $('.attendees tbody tr, .attendees .item-info').each(function () {
          $(this).find('button').eq(i).each(function () {
            $(this).closest('[data-toggle="radio-buttons"]').find('button.active').removeClass('active');
            $(this).addClass('active');
            updateAttendanceState(e, $(this));
          })
        });
        if (enableCheckForCheckpoints) {
          checkForCheckpoints();
        }
        $recordForm.find('form').trigger('checkform.areYouSure');
        // if the bulk authorised was clicked then open the bulk attendance note popup
        if (i === 2) {
          var $bulkNote = $('.bulk-attendance-note');
          if ($bulkNote.length) {
            $bulkNote.attr('href', GlobalScripts.setArgOnUrl($bulkNote.attr('href'), 'isAuto', 'true'));
            $bulkNote.click();
          }
        }
      });
    });
  }).end().find('a.meetings').on('click', function (e) {
    e.preventDefault();
    var $meetingsModal = $('#meetings-modal');
    $.get($(this).attr('href'), function (data) {
      var $modalBody = $meetingsModal.find('.modal-body');
      $modalBody.html(data);
      var $customHeader = $modalBody.find('h3.modal-header').remove();
      if ($customHeader.length > 0) {
        $meetingsModal.find('.modal-header h3').html($customHeader.html());
      }
      $meetingsModal.modal("show");
      $('.use-popover').tabulaPopover({
        trigger: 'click',
        container: 'body'
      });
    });
  }).end().find('a.small-groups').on('click', function (e) {
    e.preventDefault();
    $.get($(this).attr('href'), function (data) {
      $('#small-groups-modal').html(data).modal("show");
      $('.use-popover').tabulaPopover({
        trigger: 'click',
        container: 'body'
      });
    });
  });

  $('a.upload-attendance').on('click', function (e) {
    e.preventDefault();
    $.get($(this).attr('href'), function (data) {
      $('#upload-attendance-modal').html(data).modal("show");
    });
  });

  $('.agent-search').find('input').on('keyup', function () {
    var rows = $('table.agents tbody tr'), query = $(this).val().toLowerCase();
    if (query.length === 0) {
      rows.show();
      rows.find('p.student-list').hide();
      $('.agent-search span.very-subtle').hide();
    } else if (query.length < 3) {
      $('.agent-search span.very-subtle').show();
    } else {
      $('.agent-search span.very-subtle').hide();
      rows.each(function () {
        var $row = $(this)
          , $agentCell = $row.find('td.agent')
          , $agentName = $agentCell.find('strong')
          , $students = $agentCell.find('div.student-list')
          , showRow = false;
        if ($agentName.text().toLowerCase().indexOf(query) >= 0) {
          showRow = true;
        }
        if ($students.text().toLowerCase().indexOf(query) >= 0) {
          showRow = true;
          $students.find('span').show().filter('.name').filter(function () {
            return $(this).text().toLowerCase().indexOf(query) === -1
          }).hide();
          $students.show();
          $students.find('span.name:visible').last().find('span.comma').hide();
        } else {
          $students.hide();
        }
        if (showRow) {
          $row.show();
        } else {
          $row.hide();
        }
      });
    }
  }).end().show();

  function flattenMemberData(data) {
    var members = [];
    $.each(data, function (i, member) {
      var item = member.name + "|" + member.id + "|" + member.userId + "|" + member.description;
      members.push(item);
    });

    return members;
  }

  // Lookup for additional people to add
  $recordForm.find('.fix-header')
    .find('div.pull-left').show()
    .end().each(function () {
    $(this).find('.profile-search').each(function () {
      var $search = $(this);
      var searchUrl = $search.data('target');
      var formUrl = $search.data('form');
      var $modalElement = $($search.data('modal'));

      var $input = $search.find('input[type="text"]').first();
      var $target = $search.find('input[type="hidden"]').first();

      var handleReplacement = function (e) {
        e.preventDefault();
        e.stopPropagation();

        var $form = $('form#recordAttendance')
          , $eventInput = $modalElement.find('input[name="replacedEvent"]')
          , $weekInput = $modalElement.find('input[name="replacedWeek"]')
          , $replacementInput = $modalElement.find('select#replacementEventAndWeek')
          , $opt = $replacementInput.find(':selected');

        $eventInput.val($opt.data('event'));
        $weekInput.val($opt.data('week'));

        $form.prepend($target.clone()).prepend(
          $('<input />').attr({
            'type': 'hidden',
            'name': 'action',
            'value': 'refresh'
          })
        ).prepend($eventInput).prepend($weekInput);

        $form.submit();

        return false;
      };

      $('body').on('submit', '#addAdditional-modal form', handleReplacement)
        .on('click', '#addAdditional-modal form [type="submit"]', handleReplacement);

      $modalElement.on('hidden', function () {
        // There was no modal-body when we first started, so revert back to
        // this situation to allow the init code to work next time.
        $modalElement.html("");
        $modalElement.removeData('modal');
      });

      $search.on('tabula:selected', function (evt, name, universityId) {
        $modalElement.html('<div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h3>Loading&hellip;</h3></div></div></div>');
        $modalElement.load(formUrl + '&student=' + universityId);
        $modalElement.modal('show');
      });

      var xhr = null;
      $input.prop('autocomplete', 'off').each(function () {
        var $spinner = $('<div />').addClass('spinner-container').addClass('pull-right').attr('style', 'position: relative; top: 15px; left: 15px;');
        $search.before($spinner);

        $(this).typeahead({
          source: function (query, process) {
            if (xhr != null) {
              xhr.abort();
              xhr = null;
            }

            query = $.trim(query);
            if (query.length < 3) {
              process([]);
              return;
            }

            // At least one of the search terms must have more than 1 character
            var terms = query.split(/\s+/g);
            if ($.grep(terms, function (term) {
              return term.length > 1;
            }).length == 0) {
              process([]);
              return;
            }

            $spinner.spin('small');
            xhr = $.get(searchUrl, {query: query}, function (data) {
              $spinner.spin(false);

              var members = flattenMemberData(data);
              process(members);
            }).error(function (jqXHR, textStatus) {
              if (textStatus != "abort") $spinner.spin(false);
            });
          },

          matcher: function () {
            return true;
          },
          sorter: function (items) {
            return items;
          }, // use 'as-returned' sort
          highlighter: function (item) {
            var member = item.split("|");
            return '<div class="photo size-tinythumbnail pull-right"><img src="/profiles/view/photo/' + member[1] + '.jpg?size=tinythumbnail"></div><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
          },

          updater: function (memberString) {
            var member = memberString.split("|");
            $target.val(member[1]);
            $search.trigger('tabula:selected', member);
            return member[0];
          },
          minLength: 3
        });
      });
    });
  });

  // END SCRIPTS FOR RECORDING MONITORING POINTS
};

window.AttendanceRecording = jQuery.extend(window.AttendanceRecording, exports);

$(function () {
  AttendanceRecording.recordMonitoringPointsScripts();
});


