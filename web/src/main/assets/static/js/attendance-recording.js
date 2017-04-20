/**
* Scripts used when recording attendance in Attendance Monitoring or Small Group Teaching.
*/
(function ($) { "use strict";

var exports = {};

exports.createButtonGroup = function(id){
    var $this = $(id), selectedValue = $this.find('option:selected').val(), disabledOptions = $this.find('option.disabled');

    var disabledOptionValues = $.map(disabledOptions, function(option) {return option.value});

    var clonedButtons = $('.recordCheckpointForm div.forCloning div.btn-group')
    	.clone(true)
    	.insertAfter($this);

    var activeButton = clonedButtons
        .find('button').filter(function(){
            return $(this).data('state') == selectedValue;
        }).addClass('active');

    if ($this.attr('title') && $this.attr('title').length > 0) {
        activeButton.attr('title', '<p>' + activeButton.attr('title') +'</p>' + $this.attr('title'));
    }

    clonedButtons.find('button').each(function(){
    	var index = $.inArray($(this).data('state'), disabledOptionValues);
    	if (index > -1) {
    		$(this).addClass('disabled').attr('title', disabledOptions[index].getAttribute('title'));
    	}
    });

    $this.hide();
};

exports.wireButtons = function(id) {
	var $this = $(id);

	$this.find('.use-popover')
		.on('shown', function(e) {
			var $po = $(e.target).popover().data('popover').tip();
			$po.find('[data-action="remove"]').on('click', function() {
				var id = $(this).attr('data-student');
				$('form#recordAttendance')
					.prepend($('<input />').attr({ 'type': 'hidden', 'name': 'removeAdditionalStudent', 'value': id }))
					.prepend($('<input />').attr({ 'type': 'hidden', 'name': 'action', 'value': 'refresh' }))
					.submit();
			});
		})
		.tabulaPopover({
			trigger: 'click',
			container: 'body'
		});
};

var setArgOnUrl = function(url, argName, argValue){
	if(url.indexOf('?') === -1) {
		return url + '?' + argName + '=' + argValue;
	} else {
		var args = url.substring(url.indexOf('?') + 1, url.length).split('&'),
			found = false,
			newArgs = $.map(args, function(pair){
				var arg = pair.split('=');
				if (arg[0] === argName) {
					found = true;
					return argName + '=' + argValue;
				} else {
					return pair;
				}
			});
		if (!found) {
			newArgs.push(argName + '=' + argValue);
		}
		return url.substring(0, url.indexOf('?')) + '?' + newArgs.join('&');
	}
};

exports.bindButtonGroupHandler = function(enableCheckForCheckpoints) {
	var checkForCheckpoints = function(){
		var attendanceData = {};
		$('form#recordAttendance').find('select').each(function(){
			attendanceData[$(this).data('universityid')] = $(this).val();
		});
		$.post('/attendance/check/smallgroup', {
			'occurrence' : $('form#recordAttendance').data('occurrence'),
			'attendances' : attendanceData
		}, function(data){
			$('form#recordAttendance .checkpoints-message .students').popover('destroy').removeClass('tabulaPopover-init');
			if (data.length === 0) {
				$('form#recordAttendance .checkpoints-message').hide();
			} else {
				var popoverContent = $('<ul/>');
				$.each(data, function(i, student){
					popoverContent.append($('<li/>').html(student.name));
				});
				$('form#recordAttendance .checkpoints-message').show()
					.find('.students').html(data.length + ' student' + ((data.length > 1)?'s':''))
					.data('content', popoverContent.wrap('<div></div>').parent().html())
					.tabulaPopover({
						trigger: 'click'
					});
			}
		});
	};

	$('#recordAttendance').on('click', 'div.btn-group button', function(e, isBulkAction){
		var $this = $(this);
		if ($this.is('.disabled')) {
			e.stopPropagation();
			e.preventDefault();
			return false;
		} else {
			var state = $this.data('state');
			$this.closest('div.pull-right').find('option').filter(function(){
				return $(this).val() == state;
			}).prop('selected', true);
			var noteButton = $this.closest('div.pull-right').find('a.attendance-note');
			noteButton.attr('href', setArgOnUrl(noteButton.attr('href'), 'state', $this.data('state')));

			// if the row has no note and authorised was clicked then open the attendance note popup
			if(!isBulkAction && state === "authorised" && !noteButton.hasClass("edit")) {
				noteButton.click();
			}
		}

		if (enableCheckForCheckpoints) {
			checkForCheckpoints();
		}
    });

	if (enableCheckForCheckpoints) {
		$(function(){
			checkForCheckpoints();
		});
	}
};

exports.enableCheckForCheckpoints = function() {
	$('#recordAttendance').on('click', 'div.btn-group button', function(e){
		$('#recordAttendance')
	});
};

$(function(){
	// SCRIPTS FOR RECORDING MONITORING POINTS

	$('.recordCheckpointForm').find('.fix-header')
		.find('div.pull-right').show()
        .end().each(function(){
		$(this).find('.btn-group button').each(function(i){
			$(this).on('click', function(){
				$('.attendees .item-info').each(function(){
					$(this).find('button').eq(i).trigger('click', ['bulkAction']);
				});
				// if the bulk authorised was clicked then open the bulk attendance note popup
				if (i === 2) {
					var $bulkNote = $('.bulk-attendance-note');
					$bulkNote.attr('href', setArgOnUrl($bulkNote.attr('href'), 'isAuto', 'true'));
					$bulkNote.click();
				}
			});
		});
	}).end().find('a.meetings').on('click', function(e){
        e.preventDefault();
        $.get($(this).attr('href'), function(data){
            $('#meetings-modal .modal-body').html(data);
			var $customHeader = $('#meetings-modal .modal-body').find('h3.modal-header').remove();
			if ($customHeader.length > 0) {
				$('#meetings-modal .modal-header h3').html($customHeader.html());
			}
            $('#meetings-modal').modal("show");
            $('.use-popover').tabulaPopover({
                trigger: 'click',
                container: 'body'
            });
        });
    }).end().find('a.small-groups').on('click', function(e){
		e.preventDefault();
		$.get($(this).attr('href'), function(data){
			$('#small-groups-modal').html(data).modal("show");
			$('.use-popover').tabulaPopover({
				trigger: 'click',
				container: 'body'
			});
		});
	});

	$('a.upload-attendance').on('click', function(e){
		e.preventDefault();
		$.get($(this).attr('href'), function(data){
			$('#upload-attendance-modal').html(data).modal("show");
		});
	});

    $('.agent-search').find('input').on('keyup', function(){
        var rows = $('table.agents tbody tr'), query = $(this).val().toLowerCase();
        if (query.length === 0) {
            rows.show();
            rows.find('p.student-list').hide();
            $('.agent-search span.muted').hide();
        } else if (query.length < 3) {
            $('.agent-search span.muted').show();
        } else {
            $('.agent-search span.muted').hide();
            rows.each(function(){
                var $row = $(this)
                    , $agentCell = $row.find('td.agent')
                    , $agentName = $agentCell.find('h6')
                    , $students = $agentCell.find('p.student-list')
                    , showRow = false;
                if ($agentName.text().toLowerCase().indexOf(query) >= 0) {
                    showRow = true;
                }
                if ($students.text().toLowerCase().indexOf(query) >= 0) {
                    showRow = true;
                    $students.find('span').show().filter('.name').filter(function(){
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
		$.each(data, function(i, member) {
			var item = member.name + "|" + member.id + "|" + member.userId + "|" + member.description;
			members.push(item);
		});

		return members;
	}

	// Lookup for additional people to add
	$('.recordCheckpointForm').find('.fix-header')
		.find('div.pull-left').show()
		.end().each(function() {
			$(this).find('.profile-search').each(function() {
				var $search = $(this);
				var searchUrl = $search.data('target');
				var formUrl = $search.data('form');
				var $modalElement = $($search.data('modal'));

				var $input = $search.find('input[type="text"]').first();
				var $target = $search.find('input[type="hidden"]').first();

				$search.on('tabula:selected', function(evt, name, universityId, userId, description) {
					$modalElement.html('<div class="modal-header"><h3>Loading&hellip;</h3></div>');
					$modalElement.modal({ remote: null });
					$modalElement.load(formUrl + '&student=' + universityId);

					$modalElement
						.on('shown', function() {
							var $eventInput = $modalElement.find('input[name="replacedEvent"]');
							var $weekInput = $modalElement.find('input[name="replacedWeek"]');
							var $replacementInput = $modalElement.find('select#replacementEventAndWeek');

							$replacementInput.on('change', function() {
								var $opt = $(this).find(':selected');
								$eventInput.val($opt.data('event'));
								$weekInput.val($opt.data('week'));
							});

							var onSubmit = function(e) {
								e.preventDefault();
								e.stopPropagation();

								var $form = $('form#recordAttendance');

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

							$modalElement.find('form').on('submit', onSubmit);
							$modalElement.find('[type="submit"]').on('click', onSubmit);
						})
						.on('hidden', function() {
							// There was no modal-body when we first started, so revert back to
							// this situation to allow the init code to work next time.
							$modalElement.html("");
							$modalElement.removeData('modal');
						});
				});

				var xhr = null;
				$input.prop('autocomplete','off').each(function() {
					var $spinner = $('<div />').addClass('spinner-container').addClass('pull-right').attr('style', 'position: relative; top: 15px; left: 15px;');
					$search.before($spinner);

					$(this).typeahead({
						source: function(query, process) {
							if (xhr != null) {
								xhr.abort();
								xhr = null;
							}

							query = $.trim(query);
							if (query.length < 3) { process([]); return; }

							// At least one of the search terms must have more than 1 character
							var terms = query.split(/\s+/g);
							if ($.grep(terms, function(term) { return term.length > 1; }).length == 0) {
								process([]); return;
							}

							$spinner.spin('small');
							xhr = $.get(searchUrl, { query : query }, function(data) {
								$spinner.spin(false);

								var members = flattenMemberData(data);
								process(members);
							}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != "abort") $spinner.spin(false); });
						},

						matcher: function(item) { return true; },
						sorter: function(items) { return items; }, // use 'as-returned' sort
						highlighter: function(item) {
							var member = item.split("|");
							return '<img src="/profiles/view/photo/' + member[1] + '.jpg?size=tinythumbnail" class="photo size-tinythumbnail pull-right"><h3 class="name">' + member[0] + '</h3><div class="description">' + member[3] + '</div>';
						},

						updater: function(memberString) {
							var member = memberString.split("|");
							$target.val(member[1]);
							$search.trigger('tabula:selected', member);
							return member[0];
						},
						minLength:3
					});
				});
			});
		});

	// END SCRIPTS FOR RECORDING MONITORING POINTS
});

window.AttendanceRecording = jQuery.extend(window.AttendanceRecording, exports);

}(jQuery));