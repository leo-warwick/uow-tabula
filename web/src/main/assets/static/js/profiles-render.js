import $ from 'jquery';
import _ from 'lodash-es';
import moment from 'moment-timezone';

/**
 * Scripts used only by student profiles.
 */
var exports = {};

// Timetable calendar exports

function toTimestamp(now, then) {
  var yesterday = now.clone().subtract(1, 'day');
  if (now.diff(then) < 60000) { // less than a minute ago
    return then.from(now);
  } else if (now.isSame(then, 'day')) {
    return then.format('LT [Today]');
  } else if (yesterday.isSame(then, 'day')) {
    return then.format('LT [Yesterday]');
  } else if (now.isSame(then, 'year')) {
    return then.format('ddd Do MMM LT');
  } else {
    return then.format('ddd Do MMM YYYY LT');
  }
}

function onViewUpdate(view, weeks, $calendar) {
  updateCalendarTitle(view, weeks);
  $('.popover').hide();
  $calendar.find('table').attr('role', 'presentation');
}

function updateCalendarTitle(view, weeks) {
  if (view.name === 'agendaWeek') {
    var start = view.start.getTime();
    var end = view.end.getTime();
    var week = $.grep(weeks, function (week) {
      return (week.start >= start) && (week.end <= end);
    });
    if (week.length > 0) {
      var decodedTitle = $("<div/>").html(week[0].desc).text();
      view.title = decodedTitle;
      view.calendar.updateTitle();
    } // We should have an entry for every week; in the event that one's missing
    // we'll just leave it blank. The day columns still have the date on them.
    return true;
  }
}

function updateDownloadButton(view, $calendar, data) {
  var formData = data();
  if ($calendar.data('calendar-download-button')) {
    var $calendarDownloadButton = $($calendar.data('calendar-download-button'))
      , originalHref = $calendarDownloadButton.data('href')
      , args = [
        'calendarView=' + view.name,
        'renderDate=' + view.start.getTime() / 1000
      ]
    ;
    if (formData.length > 0) {
      args.push(formData);
    }
    $calendarDownloadButton.prop('href', originalHref + '?' + args.join('&'));
  }
  if ($calendar.data('timetable-download-button')) {
    var $timetableDownloadButtons = $($calendar.data('timetable-download-button'));
    $timetableDownloadButtons.each(function () {
      var $this = $(this), originalHref = $this.data('href');
      if (formData.length > 0) {
        $this.prop('href', originalHref + '?' + formData);
      }
    });
  }
}

function renderCalendarEvent(event, element) {
  var content = "<table class='event-info'>";
  if (event.parentType && event.parentFullName && event.parentShortName && event.parentType === "Module") {
    content = content + "<tr><th>Module</th><td>" + event.parentShortName + " " + event.parentFullName + "</td></tr>";
  }

  if (event.fullTitle && event.fullTitle.length > 0) {
    content = content + "<tr><th>Title</th><td>" + event.fullTitle + "</td></tr>";
  }

  if (event.name && event.name.length > 0) {
    content = content + "<tr><th>What</th><td>" + event.name + "</td></tr>";
  }

  content = content + "<tr><th>When</th><td>" + event.formattedInterval + "</td></tr>";

  if (event.location && event.location.length > 0) {
    content = content + "<tr><th>Where</th><td>";

    if (event.locationId && event.locationId.length > 0) {
      content = content + "<span class='map-location' data-lid='" + event.locationId + "'>" + event.location + "</span>";
    } else {
      content = content + event.location;
    }

    content = content + "</td></tr>";
  }

  if (event.onlineDeliveryUrl && event.onlineDeliveryUrl.length > 0) {
    const a = document.createElement('a');
    a.target = '_blank';
    a.rel = 'noopener noreferrer';
    a.href = event.onlineDeliveryUrl;
    a.innerText = `Link to online session`;
    content = content + `<tr><th></th><td>${a.outerHTML}</td><td>`;
  }

  if (event.tutorNames.length > 0) {
    content = content + "<tr><th>Who</th><td> " + event.tutorNames + "</td></tr>";
  }

  if (event.comments && event.comments.length > 0) {
    content = content + "<tr><th>Comments</th><td>" + event.comments + "</td></tr>";
  }

  if (event.relatedUrl && event.relatedUrl.urlString && event.relatedUrl.urlString.length > 0) {
    var relatedUrlTitle = (event.relatedUrl.title && event.relatedUrl.title.length > 0) ? event.relatedUrl.title : "More details";

    var a = document.createElement('a');
    a.href = event.relatedUrl.urlString;
    a.innerText = relatedUrlTitle;

    content = content + "<tr><th></th><td>" + a.outerHTML + "</td></tr>";
  }

  if (event.attendance) {
    var attendanceCell1 = '', attendanceCell2 = '';

    switch (event.attendance) {
      case 'attended':
        attendanceCell1 = '<i class="fa fa-check attended"/> Attended';
        attendanceCell2 = '<span class="very-subtle">Attendance at this event has been recorded.</span>';
        break;
      case 'authorised':
        attendanceCell1 = '<i class="fa fa-times-circle-o authorised"/> Missed (authorised)';
        attendanceCell2 = '<span class="very-subtle">Absence from this event has been authorised.</span>';
        break;
      case 'unauthorised':
        attendanceCell1 = '<i class="fa fa-times unauthorised"/> Missed (unauthorised)';
        attendanceCell2 = '<span class="very-subtle">Marked absent from this event.</span>';
        break;
      case 'not-recorded':
        attendanceCell1 = '<i class="fa fa-minus"/> Not recorded';
        attendanceCell2 = '<span class="very-subtle">Attendance has not yet been recorded for this event.</span>';
        break;
    }

    content = content + '<tr><th>Attendance</th><td>' + attendanceCell1 + '</td></tr><tr><th></th><td>' + attendanceCell2 + '</td></tr>';
  }

  content = content + "</table>";
  $(element).tabulaPopover({html: true, container: "body", title: event.shorterTitle, content: content});
}


function handleCalendarError(jqXHR, $container) {
  try {
    var data = $.parseJSON(jqXHR.responseText);

    var errors = $.map(data.errors, function (error) {
      return error.message;
    });

    $container.find('> .alert-danger').remove();
    $container.prepend(
      $('<div />').addClass('alert').addClass('alert-danger').text(errors.join(', '))
    );
  } catch (e) {
  }
}

function getCalendarEvents($container, $loading, url, data, method) {
  return function (start, end, callback) {
    var complete = false;
    setTimeout(function () {
      if (!complete) {
        $loading.show();
        $container.fadeTo('fast', 0.3);
      }
    }, 300);
    var startToSend = new Date(start.getTime());
    startToSend.setDate(startToSend.getDate() - 1);
    var endToSend = new Date(end.getTime());
    endToSend.setDate(endToSend.getDate() + 1);
    $('#from').val(startToSend.getTime());
    $('#to').val(endToSend.getTime());
    $.ajax({
      url: url,
      type: method,
      // make the from/to params compatible with what FullCalendar sends if you just specify a URL
      // as an eventSource, rather than a function. i.e. use seconds-since-the-epoch.
      data: data(start, end),
      success: function (data) {
        if (data.lastUpdated) {
          // Update the last updated timestamp
          $container.find('> .fc-last-updated').remove();

          var now = moment();
          var time = moment(data.lastUpdated);

          $container.append(
            $('<div />').addClass('fc-last-updated').addClass('pull-right').html('Last updated: ' + toTimestamp(now, time))
          );
        }

        var events = data.events;
        // TAB-3008 - Change times to Europe/London
        $.each(events, function (i, event) {
          event.start = moment(moment.unix(event.start).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
          event.end = moment(moment.unix(event.end).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
        });
        if ((data.errors || []).length > 0) {
          var $errorDiv = $('<div />').addClass('alert').addClass('alert-danger');
          $container.find('> .alert-danger').remove();
          $container.prepend($errorDiv);
          $.each(data.errors, function (i, error) {
            $errorDiv.append(error).append('<br />');
          });
        } else {
          $container.find('> .alert-danger').remove();
        }
        callback(events);
      },
      error: function (jqXKR) {
        handleCalendarError(jqXKR, $container)
      },
      complete: function () {
        complete = true;
        $loading.hide();
        $container.fadeTo('fast', 1);
      }
    });
  };
}

exports.getCalendarEvents = getCalendarEvents;

function createCalendar(container, defaultViewName, weeks, eventsCallback, data, hasStartDate, year, month, date, defaultDate) {
  if (!data) {
    data = function () {
      return "";
    }
  }
  var showWeekends = (defaultViewName == "month"), $container = $(container);
  var options = {
    events: eventsCallback,
    defaultView: defaultViewName,
    allDaySlot: false,
    slotMinutes: 60,
    firstHour: 8,
    firstDay: 1, // monday
    timeFormat: {
      agendaWeek: '', // don't display time on event
      agendaDay: '', // don't display time on event
      // for all other views
      '': 'HH:mm'   //  5:00 - 6:30
    },
    titleFormat: {
      month: 'MMMM yyyy',
      week: "MMM d[ yyyy]{ '&#8212;'[ MMM] d yyyy}",
      day: 'dddd, MMM d, yyyy'
    },
    columnFormat: {
      month: 'ddd',
      week: 'ddd d/M',
      day: 'dddd d/M'
    },
    defaultEventMinutes: 30,
    weekends: showWeekends,
    viewRender: function (view) {
      onViewUpdate(view, weeks, $container);
    },
    header: {
      left: 'title',
      center: 'month,agendaWeek,agendaDay',
      right: 'today prev,next'
    },
    weekNumbers: true,
    weekNumberCalculation: function (moment) {
      var start = moment.getTime();
      var week = $.grep(weeks, function (week) {
        return (week.start >= start);
      });

      if (week.length > 0) {
        return week[0].shortDescription;
      }

      // We should have an entry for every week; in the event that one's missing
      // we'll just leave it blank. The day columns still have the date on them.
      return '';
    },
    eventAfterRender: function (event, element) {
      renderCalendarEvent(event, element);
    },
    eventAfterAllRender: function (view) {
      updateDownloadButton(view, $container, data);
    }
  };
  if (hasStartDate) {
    options.year = year;
    options.month = month;
    options.date = date;
    options.defaultDate = defaultDate
  }
  $container.fullCalendar(options);
}

exports.createCalendar = createCalendar;

function createSmallScreenCalender($container, $loading, url, data, method) {
  var startToSend = new Date();
  startToSend.setHours(0, 0, 0, 0);

  function checkScrollAndRefresh() {
    if ($(window).scrollTop() + $(window).height() > $(document).height() - 400) {
      startToSend.setDate(startToSend.getDate() + 7);
      fetchAndRender();
    } else {
      setTimeout(checkScrollAndRefresh, 500);
    }
  }

  function fetchAndRender() {
    var complete = false;
    setTimeout(function () {
      if (!complete) {
        $loading.show();
      }
    }, 300);
    var endToSend = new Date(startToSend.valueOf());
    endToSend.setDate(endToSend.getDate() + 7);
    $.ajax({
      url: url,
      // make the from/to params compatible with what FullCalendar sends if you just specify a URL
      // as an eventSource, rather than a function. i.e. use seconds-since-the-epoch.
      data: data(startToSend, endToSend),
      type: method,
      success: function (data) {
        var events = data.events;
        // TAB-3008 - Change times to Europe/London
        _.forEach(events, function (event) {
          event.start = moment(moment.unix(event.start).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
          event.end = moment(moment.unix(event.end).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
        });

        $container.find('> .alert-danger').remove();

        var eventsByDay = {}
          , todayAtMidnight = moment().hour(0).minute(0).second(0).millisecond(0);
        _.forEach(events, function (event) {
          var date = moment.unix(event.start).hour(0).minute(0).second(0).millisecond(0);
          if (date.diff(todayAtMidnight) >= 0) {
            if (!_.has(eventsByDay, date.valueOf())) {
              eventsByDay[date.valueOf()] = [event];
            } else {
              eventsByDay[date.valueOf()].push(event);
            }
          }
        });
        var dates = _.keys(eventsByDay).sort();

        if (dates.length > 0) {
          _.each(dates, function (dateValue) {
            var date = moment(dateValue, 'x')
              , $table = $('<table/>').addClass('table table-condensed').append($('<thead/>')).append('<tbody/>')
              , $thead = $table.find('thead')
              , $tbody = $table.find('tbody');
            $thead.append(
              $('<tr/>').append(
                $('<th/>').attr('colspan', 3).append(
                  $('<h6/>').html(date.calendar(null, {
                    sameDay: '[Today] D MMMM',
                    nextDay: '[Tomorrow] D MMMM',
                    nextWeek: 'dddd D MMMM',
                    lastDay: 'dddd D MMMM',
                    lastWeek: 'dddd D MMMM',
                    sameElse: 'dddd D MMMM'
                  }))
                )
              )
            );
            if (date.diff(todayAtMidnight) === 0) {
              $table.addClass('today');
            }
            _.each(_.sortBy(eventsByDay[dateValue], ['startDate']), function (event) {
              var location = '';
              if (event.location.length > 0) {
                location = '(' + event.location + ')';
              }
              if (event.locationId && event.locationId.length > 0) {
                location = '(<span class="map-location" data-lid="' + event.locationId + '">' + event.location + '</span>)';
              }
              var tutors = '';
              if (event.tutorNames.length > 0) {
                tutors = '<i class="fa fa-user"></i> ' + event.tutorNames;
              }
              $tbody.append(
                $('<tr/>').append(
                  $('<td/>').addClass('time').html(moment.unix(event.start).format('kk:mm') + ' -<br />' + moment.unix(event.end).format('kk:mm'))
                ).append(
                  $('<td/>').addClass('marker').append(
                    $('<i/>').addClass('fa fa-circle').css('color', event.backgroundColor)
                  )
                ).append(
                  $('<td/>').html([
                    event.title.replace('(' + event.location + ')', ''),
                    event.name + ' ' + location,
                    tutors
                  ].join('<br />'))
                )
              );
            });
            $container.append($table);
          });

          $('.map-location[data-lid]').mapPopups({
            placement: 'bottom',
            expandClickTarget: true
          });

          $loading.hide();
          complete = true;
        }
      },
      error: function (jqXKR) {
        handleCalendarError(jqXKR, $container);
        $loading.hide();
        complete = true;
      },
      complete: function () {
        if (startToSend.valueOf() - new Date().valueOf() > (1000 * 60 * 60 * 24 * 365)) {
          if ($('.calendar-smallscreen table').length === 0) {
            $loading.html("No events found in the next 12 months").show();
          } else {
            $loading.hide();
            complete = true;
          }
        } else {
          setTimeout(checkScrollAndRefresh, 500);
        }
      }
    });
  }

  fetchAndRender();

  $(window).on('tabula.smallScreenCalender.refresh', function () {
    var startToSend = new Date();
    startToSend.setHours(0, 0, 0, 0);
    $container.empty();
    fetchAndRender();
  });
}

exports.createSmallScreenCalender = createSmallScreenCalender;

// End Timetable calendar exports

$(function () {
  $('.profile-search').each(function () {
    var container = $(this);

    var target = container.find('form').prop('action') + '.json';

    var xhr = null;
    container.find('input[name="query"]').prop('autocomplete', 'off').each(function () {
      var $spinner = $('<div class="spinner-container" />'), $this = $(this);
      $this
        .before($spinner)
        .on('focus', function () {
          container.find('.use-tooltip').tooltip('show');
        })
        .on('blur', function () {
          container.find('.use-tooltip').tooltip('hide');
        })
        .typeahead({
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

            var searchAllDepts = $("input[type='radio'][name='searchAllDepts']:checked").val();
            var includePast = $("input[type='checkbox'][name='includePast']").is(':checked');

            xhr = $.ajax({
              url: target,
              data: {
                query: query,
                searchAllDepts: searchAllDepts,
                includePast: includePast,
              },
              success: function (data) {
                $spinner.spin(false);

                var members = [];

                $.each(data, function (i, member) {
                  var item = member.name + '|' + member.id + '|' + member.userId + '|' + member.description;
                  members.push(item);
                });

                process(members);
              },
              error: function (jqXHR, textStatus, errorThrown) {
                if (textStatus !== 'abort') {
                  $spinner.spin(false);
                }
              },
            });
          },

          matcher: function (item) {
            return true;
          },
          sorter: function (items) {
            return items;
          }, // use 'as-returned' sort
          highlighter: function (item) {
            var member = item.split('|');
            return '<img src="/profiles/view/photo/' + member[1] + '.jpg?size=tinythumbnail size-tinythumbnail" class="photo pull-right"><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
          },

          changeInputOnSelect: false,
          changeInputOnMove: false,
          followLinkOnSelect: true,
          selectOnBlur: false,
          showHintOnFocus: true,
          itemLink: (item) => {
            if (item) {
              const member = item.split('|');
              return `/profiles/view/${member[1]}`;
            }

            return undefined;
          },
          afterSelect: () => {
            $this.trigger('blur');
          },

          minLength: 3
        });
    });
  });

  $('table.expanding-row-pairs').each(function () {
    $(this).find('tbody tr').each(function (i) {
      if (i % 2 === 0) {
        var $selectRow = $(this), $expandRow = $selectRow.next('tr');
        $selectRow.data('expandRow', $expandRow.remove()).find('td:first').addClass('can-expand').prepend(
          $('<a href="#">').addClass('expand-trigger').append($('<i/>').addClass('fa fa-fw fa-caret-right')).append($('<span>').addClass('sr-only').text('Toggle row'))
        );
      }
    }).end().on('click', 'td.can-expand', function (e) {
      e.preventDefault();
      var $row = $(this).closest('tr');
      if ($row.is('.expanded')) {
        $row.removeClass('expanded').next('tr').remove().end()
          .find('td i.fa-caret-down').removeClass('fa-caret-down').addClass('fa-caret-right');
      } else {
        $row.addClass('expanded').after($row.data('expandRow'))
          .find('td i.fa-caret-right').removeClass('fa-caret-right').addClass('fa-caret-down');
        $row.next().each(function () {
          var $expandRow = $(this);
          $expandRow.find('input.date-time-picker').tabulaDateTimePicker();
          $expandRow.find('input.date-picker').tabulaDatePicker();
          $expandRow.find('input.time-picker').tabulaTimePicker();
          $expandRow.find('input.date-time-minute-picker').tabulaDateTimeMinutePicker();
          $expandRow.find('form.double-submit-protection').tabulaSubmitOnce();
          $expandRow.find('select.selectOffset').selectOffset();
          $expandRow.tabulaPrepareSpinners();
        });
      }
    }).find('tr.expand td.can-expand').trigger('click');
  });

  // MEMBER NOTE / EXTENUATING CIRCUMSTANCES STUFF

  $('section.member-notes, section.circumstances').on('click', 'a.create, a.edit', function (e) {
    // Bind click to load create-edit member note

    var $this = $(this);
    if ($this.hasClass('disabled')) {
      e.preventDefault();
      e.stopPropagation();
      return false;
    }

    var url = $this.attr('data-url'), $modal = $('#note-modal'), $modalBody = $modal.find('.modal-body');

    $modalBody.html('<iframe src="' + url + '" style="height:100%; width:100%;" frameBorder="0" scrolling="no"></iframe>')
      .find('iframe').on('load', function () {
      if ($(this).contents().find('form').length == 0) {
        //Handle empty response from iframe form submission
        $('#note-modal').modal('hide');
        document.location.reload(true);
      } else {
        //Bind iframe form submission to modal button
        $('#member-note-save').on('click', function (e) {
          e.preventDefault();
          $('#note-modal').find('.modal-body').find('iframe').contents().find('form').submit();
          $(this).off();  //remove click event to prevent bindings from building up
        });
      }
    });
    $modal.find('.modal-header h3 span').text($this.attr('title')).end()
      .modal('show');
    e.preventDefault();
    e.stopPropagation();

  }).on('click', 'ul.dropdown-menu a:not(.edit)', function (e) {
    // Bind click events for dropdown

    var $this = $(this);
    if ($this.hasClass('disabled')) {
      e.preventDefault();
      e.stopPropagation();
      return false;
    }

    var $row = $this.closest('tr'),
      $dropdownItems = $this.closest('ul').find('a'),
      $loading = $this.closest('td').find('i.fa-spinner'),
      url = $this.attr('href');

    $loading.toggleClass('invisible');

    $.post(url, function (data) {
      if (data.status == 'successful') {
        if ($this.hasClass('delete') || $this.hasClass('restore')) {
          $dropdownItems.toggleClass('disabled');
          $row.toggleClass('subtle deleted');
          $row.find('.deleted-files').toggleClass('hidden');
        } else if ($this.hasClass('purge')) {
          if ($row.hasClass('expanded')) {
            $row.find('td.can-expand').trigger('click');
          }
          $row.hide();
        }
      }
      $loading.toggleClass('invisible');
    }, 'json');

    e.preventDefault();
    e.stopPropagation();
  });

  // END OF MEMBER NOTE / EXTENUATING CIRCUMSTANCES STUFF

  // Add in People search data

  $('.peoplesearch-info').each(function () {
    var $this = $(this);
    $.getJSON($this.data('href'), function (data) {
      var items = [];
      if (data.extensionNumberWithExternal) {
        items.push('<strong>Phone:</strong> ' + data.extensionNumberWithExternal + '<br/>');
      }
      if (data.room) {
        items.push('<strong>Room:</strong> ' + data.room + '<br/>');
      }
      $this.html(items.join(''));
    });
  });

  // End Add in People search data

  // Meeting records

  var $meetingModal = $("#meeting-modal");
  $meetingModal.on('submit', 'form', function (e) {
    e.preventDefault();
    // reattach the load handler and submit the inner form in the iframe
    $meetingModal.find('iframe')
      .off('load').on('load', meetingRecordIframeHandler)
      .contents().find('form').submit();

    // hide the iframe, so we don't get a FOUC
    $meetingModal.find('.modal-body').slideUp();
  });

  function meetingRecordFrameLoad(frame) {
    var $f = $(frame).contents();

    // reset slow load spinner
    $meetingModal.tabulaPrepareSpinners();

    if ($f.find('#meeting-record-form').length === 1) {
      // unhide the iframe
      $meetingModal.find('.modal-body').slideDown();

      // reset datepicker & submit protection
      var $form = $meetingModal.find('form.double-submit-protection');
      $form.tabulaSubmitOnce();
      $form.find('.btn').removeClass('disabled');
      // wipe any existing state information for the submit protection
      $form.removeData('submitOnceSubmitted');

      // show-time
      $meetingModal.modal('show');
      $meetingModal.off('shown.bs.modal.meetingRecordFrameLoad').on('shown.bs.modal.meetingRecordFrameLoad', function () {
        $f.find('[name="title"]').focus();
      });
    } else if ($f.find('#maintenance-message').length === 1) {
      $meetingModal.find('.modal-body').slideDown()
        .empty()
        .append($f.find('#maintenance-message'));
      $meetingModal.find('.modal-footer .btn-primary').remove();

      $meetingModal.modal('show');
    } else {
      $meetingModal.modal('hide');
      document.location.reload(true);
    }
  }

  // named handler that can be unbound
  function meetingRecordIframeHandler() {
    meetingRecordFrameLoad(this);
    $(this).off('load', meetingRecordIframeHandler);
  }

  function prepareMeetingModal($this, targetUrl) {
    $.get(GlobalScripts.setArgOnUrl(targetUrl, 'modal', ''), function (data) {
      $meetingModal.html(data);
      var $mb = $meetingModal.find('.modal-body').empty();
      var iframeMarkup = "<iframe frameBorder='0' scrolling='no' style='height:100%;width:100%;' id='modal-content'></iframe>";
      $(iframeMarkup)
        .off('load').on('load', meetingRecordIframeHandler)
        .attr('src', GlobalScripts.setArgOnUrl(targetUrl, 'iframe', ''))
        .appendTo($mb);
    }).fail(function () {
      if (!$('#meeting-modal-failure').length) {
        var $error = $('<p id="meeting-modal-failure" class="alert alert-danger hide"><i class="icon-warning-sign"></i> Sorry, I\'m unable to edit meeting records for this student at the moment.</p>');
        $this.before($error);
        $error.slideDown();
      }
    });
  }

  $('section.meetings').on('click', '.new-meeting-record:not(.disabled), .schedule-meeting-record:not(.disabled), .edit-meeting-record:not(.disabled)', function (e) {
    var $this = $(this);
    prepareMeetingModal($this, $this.attr('href'));
    e.preventDefault();
  }).on('click', 'ul.dropdown-menu a:not(.edit-meeting-record)', function (e) {
    // Bind click events for dropdown

    var $this = $(this);
    if ($this.hasClass('disabled')) {
      e.preventDefault();
      e.stopPropagation();
      return false;
    }

    var $row = $this.closest('tr'),
      $dropdownItems = $this.closest('ul').find('a'),
      $loading = $this.closest('td').find('i.fa-spinner'),
      url = $this.attr('href');

    $loading.toggleClass('invisible');

    $.post(url, function (data) {
      if (data.status === 'successful') {
        if ($this.hasClass('delete-meeting-record') || $this.hasClass('restore-meeting-record')) {
          $dropdownItems.toggleClass('disabled');
          $row.toggleClass('subtle deleted');
          $row.find('.deleted-files').toggleClass('hidden');
        } else if ($this.hasClass('purge-meeting-record')) {
          if ($row.hasClass('expanded')) {
            $row.find('td.can-expand').trigger('click');
          }
          $row.hide();
        }
      }
      $loading.toggleClass('invisible');
    }, 'json');

    e.preventDefault();
    e.stopPropagation();
  }).on('submit', 'form.scheduled-action', function (e) {
    // Scheduled meetings
    e.preventDefault();
    var $this = $(this), checkedInput = $this.find('input:checked');
    $this.find('div.ajaxErrors').hide();
    switch (checkedInput.val()) {
      case 'confirm':
        prepareMeetingModal($this, checkedInput.data('formhref'));
        break;
      case 'reschedule':
        $this.closest('tr').prev('tr').find('.edit-meeting-record').trigger('click');
        break;
      case 'missed':
        $.post(checkedInput.data('formhref'), $this.serialize(), function (data) {
          if (data.status === 'successful') {
            document.location.reload(true);
          } else {
            $this.find('div.ajaxErrors').empty().html(data.errors.join('<br />')).show();
          }
        });
        break;
    }
  });

  $('section.meetings input.reject').each(function () {
    var $this = $(this);
    var $form = $this.closest('form');
    var $commentBox = $form.find('.rejection-comment');
    $this.slideMoreOptions($commentBox, true);
  });
  $('section.meetings .approval').parent().tabulaAjaxSubmit(function () {
    document.location.reload(true);
  });

  // End Meeting records

  // Seminars

  // enable/disable the "sign up" buttons
  $('#student-groups-view')
    .find('.sign-up-button')
    .addClass('disabled use-tooltip')
    .prop('disabled', true)
    .prop('title', 'Please select a group')
    .end()
    .find('input.group-selection-radio')
    .on('change', function () {
      $(this).closest('.item-info').find('.sign-up-button').removeClass('disabled use-tooltip').prop('disabled', false).prop('title', '');
    });

  // End Seminars
});


// take anything we've attached to 'exports' and add it to the global 'Profiles'
// we use extend() to add to any existing variable rather than clobber it
window.Profiles = jQuery.extend(window.Profiles, exports);
