/**
 * Scripts used only by the coursework admin section.
 */
(function ($) { "use strict";

var exports = {};

exports.zebraStripeAssignments = function($module) {
	$module.find('.assignment-info').filter(':visible:even').addClass('alt-row');
};

// take anything we've attached to "exports" and add it to the global "Courses"
// we use extend() to add to any existing variable rather than clobber it
window.Courses = jQuery.extend(window.Courses, exports);

$(function(){

	// hide stuff that makes no sense when open-ended
	$('input#openEnded').slideMoreOptions($('.has-close-date'), false);
	$('input#modal-open-ended').slideMoreOptions($('.has-close-date'), false);

	// check that the extension UI elements are present
	if($('input#allowExtensionRequests').length > 0){
		$('input#allowExtensionRequests').slideMoreOptions($('#request-extension-fields'), true);
	}

	// Zebra striping on lists of modules/assignments
	$('.module-info').each(function(i, module) {
		exports.zebraStripeAssignments($(module));
	});

	$('.module-info.empty').css('opacity',0.66)
		.find('.module-info-contents').hide().end()
		.click(function(){
			$(this).css('opacity',1)
				.find('.module-info-contents').show().end();
		})
		.hide();

	$('.dept-show').click(function(event){
		event.preventDefault();
		var hideButton = $(this).find("a");

		$('.module-info.empty').toggle('fast', function() {
			if($('.module-info.empty').is(":visible")) {
				hideButton.html('<i class="icon-eye-close"></i> Hide');
				hideButton.attr("data-original-title", hideButton.attr("data-title-hide"));

			} else {
				hideButton.html('<i class="icon-eye-open"></i> Show');
				hideButton.attr("data-original-title", hideButton.attr("data-title-show"));
			}
		});

	});


	// code for tabs
	$('.nav.nav-tabs a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

	// code for the marks web forms
	$('#marks-web-form').tableForm({
		addButtonClass: 'add-additional-marks',
		headerClass: 'mark-header',
		rowClass: 'mark-row',
		tableClass: 'marksUploadTable',
		listVariable: 'marks'
	});

	$('.show-archived-assignments').click(function(e){
		e.preventDefault();
		$(e.target).hide().closest('.module-info').find('.assignment-info.archived').show();
	});

	$('.hide-awaiting-submission').on('click', function(){
		$('.awaiting-submission, .hide-label, .show-label').toggle();
	});
	// hide this button if it would do nothing
	if ($('.awaiting-submission').length == 0) {
		$('.hide-awaiting-submission').hide();
	}

	// enable shift-click on multiple checkboxes in tables
	$('table').find('input[type="checkbox"]').shiftSelectable();

	var biglistOptions = {
		setup : function() {
			var $container = this, $outerContainer = $container.closest('div.form-post-container');
			// #delete-selected-button won't work for >1 set of checkboxes on a page.
			$('#download-selected-button, #delete-selected-button', $outerContainer).click(function(event){
				event.preventDefault();

				var $checkedBoxes = $(".collection-checkbox:checked", $container);
				if ($container.data('checked') != 'none') {
					var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
					$form.append($checkedBoxes.clone());
					$(document.body).append($form);
					$form.submit();
				}
				return false;
			});

			$('#mark-plagiarised-selected-button:not(.disabled)', $outerContainer).click(function(event){
				event.preventDefault();

				var $checkedBoxes = $(".collection-checkbox:checked", $container);

				if ($container.data('checked') != 'none') {

					var $form = $('<form></form>').attr({method:'POST',action:this.href}).hide();
					$form.append($checkedBoxes.clone());

					if ($container.data("all-plagiarised") === true) {
						$form.append("<input type='hidden' name='markPlagiarised' value='false'>");
					}

					$(document.body).append($form);
					$form.submit();
				}
				return false;
			});

			$('.form-post', $outerContainer).click(function(event){
				event.preventDefault();
				var $this = $(this);
				if(!$this.hasClass("disabled")) {
					var action = this.href;
					if ($this.data('href')) {
						action = $this.data('href')
					}

					var $form = $('<form></form>').attr({method: 'POST', action: action}).hide();
					var doFormSubmit = false;

					if ($container.data('checked') != 'none' || $this.closest('.must-have-selected').length === 0) {
						var $checkedBoxes = $(".collection-checkbox:checked", $container);
						$form.append($checkedBoxes.clone());

						var $extraInputs = $(".post-field", $container);
						$form.append($extraInputs.clone());

						doFormSubmit = true;
					}

					if ($this.hasClass('include-filter') && ($('.filter-form').length > 0)) {
						var $inputs = $(':input', '.filter-form:not("#floatedHeaderContainer *")');
						$form.append($inputs.clone());

						doFormSubmit = true;
					}

					if (doFormSubmit) {
						$(document.body).append($form);
						$form.submit();
					} else {
						return false;
					}
				}
			});

		},

		// rather than just toggling the class check the state of the checkbox to avoid silly errors
		onChange : function() {
			this.closest(".itemContainer").toggleClass("selected", this.is(":checked"));
			var $checkedBoxes = $(".collection-checkbox:checked");

			var allPlagiarised = false;

			if ($checkedBoxes.length > 0) {
				allPlagiarised = true;
				$checkedBoxes.each(function(index){
					var $checkBox = $(this);
					if ($checkBox.closest('tr').data('plagiarised') != true) {
						allPlagiarised = false;
					}
				});
			}
			$('.submission-feedback-list,.submission-list,.coursework-progress-table').data("all-plagiarised", allPlagiarised);
			if (allPlagiarised) {
				$('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign icon-fixed-width"></i> Unmark plagiarised');
			}
			else {
				$('#mark-plagiarised-selected-button').html('<i class="icon-exclamation-sign icon-fixed-width"></i> Mark plagiarised');
			}
		}
	};

	$('.submission-feedback-list, .submission-list, .feedback-list, .marker-feedback-list, .coursework-progress-table, .expanding-table').not(".marker-feedback-table").bigList(
		$.extend(biglistOptions, {
			onSomeChecked : function() {
				$('.must-have-selected').removeClass('disabled');
			},

			onNoneChecked : function() {
				$('.must-have-selected').addClass('disabled');
			}
		})
	);

	$('.marker-feedback-table').bigList(
		$.extend(biglistOptions, {
			onSomeChecked: function() {
				var $markingContainer = $(this).closest(".workflow-role");
				var $checkboxes = $markingContainer.find("input[type=checkbox][name=markerFeedback]:checked");
				var $sendBack = $markingContainer.find(".must-be-blank");
				var $sendForward = $markingContainer.find(".must-be-populated");
				var allPopulated = $checkboxes.closest("tr").filter(".in-progress").size() == $checkboxes.size();
				var allBlank = $checkboxes.closest("tr").filter(".in-progress,.marking-completed").size() == 0;
				if (allBlank) { $sendBack.removeClass("disabled");} else { $sendBack.addClass("disabled");}
				if (allPopulated) { $sendForward.removeClass("disabled");} else{ $sendForward.addClass("disabled");}
			},

			onNoneChecked: function($table) {
				var $markingContainer = $(this).closest(".workflow-role");
				var $sendBack = $markingContainer.find(".must-be-blank");
				var $sendForward = $markingContainer.find(".must-be-populated");
				$sendBack.addClass("disabled");
				$sendForward.addClass("disabled");
			}
		})
	);

});

// shift selectable checkboxes
$.fn.shiftSelectable = function() {
	var lastChecked,
		$boxes = this;

	$boxes.click(function(evt) {
		if(!lastChecked) {
			lastChecked = this;
			return;
		}

		if(evt.shiftKey) {
			var start = $boxes.index(this),
				end = $boxes.index(lastChecked);
			$boxes.slice(Math.min(start, end), Math.max(start, end) + 1)
				.attr('checked', lastChecked.checked)
				.trigger('change');
		}

		lastChecked = this;
	});
};

// code for markingWorkflow add/edit
$(function(){
	var markingMethod = $('#markingMethod');
	var secondMarkers = $('.second-markers-container');
	if($('option:selected', markingMethod).hasClass('uses-second-markers'))
		secondMarkers.show();
	$('#markingMethod').on('change', function(){
		var option = $('option:selected', $(this));


		if(option.hasClass('uses-second-markers'))
			secondMarkers.show();
		else
			secondMarkers.hide();
	});
});

// code for bulk archive/copy assignments
$(function(){

	$('.copy-assignments, .archive-assignments').bigList({

		setup: function(e){
			if(!$(".collection-checkbox").is(":checked")){
				$('.btn-primary').prop('disabled', 'disabled');
			}
		},

		onSomeChecked: function() {
			$('.btn-primary').removeProp('disabled');
		},

		onNoneChecked: function() {
			$('.btn-primary').prop('disabled', 'disabled');
		}
	});

	$('form.copy-assignments').confirmModal({
		message: "Are you sure that you want to create these assignments?"
	});

	$('form.archive-assignments').confirmModal({
		message: "Are you sure that you want to archive these assignments?"
	});

});



var frameLoad = function(frame){
	if(jQuery(frame).contents().find("form").length == 0){
		jQuery("#feedback-report-modal").modal('hide');
		document.location.reload(true);
	}
}

$(function(){




// Ajax specific modal start

// modals use ajax to retrieve their contents
	$('#feedback-report-button').on('click', 'a[data-toggle=modal]', function(e){
		e.preventDefault();
		var $this = $(this);
		var target = $this.attr('data-target');
		var url = $this.attr('href');
		$(target).load(url);
	});



// any date fields returned by ajax will have datetime pickers bound to them as required
	$('#feedback-report-modal').on('focus', 'input.date-time-picker', function(e){
		e.preventDefault();
		var isPickerHidden = (typeof $('.datetimepicker').filter(':visible')[0] === "undefined") ? true : false;

		if(isPickerHidden) {
			$(this).datetimepicker('remove').datetimepicker({
				format: "dd-M-yyyy hh:ii:ss",
				weekStart: 1,
				minView: 'day',
				autoclose: true
			}).on('show', function(ev){
				var d = new Date(ev.date.valueOf()),
					  minutes = d.getUTCMinutes(),
						seconds = d.getUTCSeconds(),
						millis = d.getUTCMilliseconds();

				if (minutes > 0 || seconds > 0 || millis > 0) {
					d.setUTCMinutes(0);
					d.setUTCSeconds(0);
					d.setUTCMilliseconds(0);

					var DPGlobal = $.fn.datetimepicker.DPGlobal;
					$(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat("dd-M-yyyy hh:ii:ss", "standard"), "en", "standard"));

					$(this).datetimepicker('update');
				}
			});
		}
	});

	// feedback report
	$("#feedback-report-modal").tabulaAjaxSubmit(function(data) {
		window.location = data.result;
	});

	// makes dropdown menus dropup rather than down if they're so
	// close to the end of the screen that they will drop off it
	var bodyHeight = $('body').height();
	$('.module-info:not(.empty) .btn-group').each( function(index) {
		if(($(this).find('.dropdown-menu').height() +  $(this).offset().top) > bodyHeight) {
			$(this).addClass("dropup");
		}
	});

});

/**
 * Scripts for expanding tables, including online marking
 */
$(function() {
	// this has to descend from #main-content, as the content container is appended there, outside the table
	$('#main-content').on('tabula.expandingTable.contentChanged', '.content-container', function() {
		var $container = $(this);
		var $form = $container.find('form');

		// re-apply the details/summary tag fix for Firefox & IE post ajax insert
		$form.find('details').details().on('DOMSubtreeModified', function() {
			$('.expanding-table').trigger('tabula.expandingTable.repositionContent');
		});

		var contentId = $container.attr('data-contentid');
		var $row = $('tr.item-container[data-contentid='+contentId+'], tr.itemContainer[data-contentid='+contentId+']');

		var $expiryDateField = $form.find('.date-time-picker');
		$expiryDateField.tabulaDateTimePicker();
		var expiryDatePicker = $expiryDateField.data('datetimepicker');
		var closeDate = new Date($form.find('[name=closeDate]').val());
		if (closeDate && expiryDatePicker) {
			expiryDatePicker.setStartDate(closeDate);
		}

        $form.tabulaSubmitOnce();

		// record the initial values of the fields
		$('input, textarea', $form).each(function() {
			$(this).data('initialvalue', $(this).val());
		});

		// only relevant for marker_moderation.ftl:
		// rejection fields shown when reject is selected
		$('input.reject', $container).each(function() {
			var $this = $(this);
			var $form = $this.closest('form');
			var $table = $('.expanding-table');
			var $rejectionFields = $form.find('.rejection-fields');
			$this.slideMoreOptions($rejectionFields, true);

			$rejectionFields.on('tabula.slideMoreOptions.shown', function() {
				$table.trigger('tabula.expandingTable.repositionContent');
			});

			$rejectionFields.on('tabula.slideMoreOptions.hidden', function() {
				$table.trigger('tabula.expandingTable.repositionContent');
			});
		});

		$('.discard-changes', $form).on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();
			if(hasChanges($container)) {
				var message = 'Discarding unsaved changes is irreversible. Are you sure?';
				var modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
									"<div class='modal-body'>" +
										"<h5>"+message+"</h5>" +
									"</div>" +
									"<div class='modal-footer'>" +
										"<a class='confirm btn'>Yes, discard</a>" +
										"<a data-dismiss='modal' class='btn btn-primary'>No, go back to editing</a>" +
									"</div>" +
								"</div>";
				var $modal = $(modalHtml);
				$modal.modal();
				$('a.confirm', $modal).on('click', function(){
					resetFormValues($form, $row);
					$modal.modal('hide');
				});
			} else {
				resetFormValues($form, $row);
			}
		});

		$('.empty-form', $form).on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();
			$('input[type=text], textarea', $form).each(function() {
				$(this).val('');
			});
		});

		$('.feedback-comments:not(.collapsible)').off('click').on('open.collapsible close.collapsible', function() {
			$('.expanding-table').trigger('tabula.expandingTable.repositionContent');
		}).collapsible();

		$(".copyFeedback").off("click").on("click", function(){

			var $button = $(this);
			var feedbackHeading = $button.closest(".well").find(".feedback-summary-heading h3").text();
			var $summaryFeeback = $button.closest(".well");
			var $feedbackForm = $button.closest(".content-container").find("form");
			var attachments = "";
			var $targetFormSection = $form.find(".attachments");

			var bigTextArea = $feedbackForm.find(".big-textarea");

			if (bigTextArea.val()){
				bigTextArea.val(bigTextArea.val() + '\n\n')
			}

			bigTextArea.val(bigTextArea.val() + $.trim($summaryFeeback.find(".feedback-comments").contents(':not(h5)').text()));

			// replace the button with a note saying that the feedback has been copied.
			$button.after($('<span class="muted">Feedback copied</span>'));
			$button.remove();

			var $copyAlert = $feedbackForm.find(".alert-success");
			$copyAlert.text("Feedback copied from "+feedbackHeading.charAt(0).toLowerCase() + feedbackHeading.substr(1)).show(0, function(){
				$('.expanding-table').trigger('tabula.expandingTable.repositionContent');
			});

			var $summaryAttachments = $summaryFeeback.find('input[type="hidden"]');
			if($summaryAttachments.length > 0) {
				$summaryAttachments.each(function(){
					var $this = $(this);
					attachments += 	'<li id="attachment-' + $this.val() +'" class="attachment"><i class="icon-file-alt"></i>' +
						' <span>' + $this.attr("name") +' </span>&nbsp;<i class="icon-remove-sign remove-attachment"></i>' +
						'<input id="attachedFiles" name="attachedFiles" value="'+  $this.val() +'" type="hidden"></li>'
				});
				$targetFormSection.append(attachments);
				$feedbackForm.find('.feedbackAttachments').slideDown();
			}
		});

		/**
		 * Helper for ajaxified forms in tabula expanding tables
		 *
		 * This glues together the ajaxForm and expandingTable plugins. It adds boilerplate for coping with
		 * signed-out users, and provides a callback for giving UI feedback after the form has been processed.
		 *
		 * callback() is passed the raw response text.
		 * It MUST return an empty string if the form has been successfully handled.
		 * Otherwise, whatever is returned will be rendered in the container.
 		 */
		var prepareAjaxForm = function($form, callback) {
			$form.ajaxForm({
				iframe: true,
				statusCode: {
					403: function(jqXHR) {
						$container.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
						$container.trigger('tabula.expandingTable.contentChanged');
					}
				},
				success: function(response) {
					var result;

					if (response.indexOf('id="dev"') >= 0) {
						// for debugging freemarker...
						result = $(response).find('#column-1-content');
					} else {
						result = callback(response);
					}

					if (!result || /^\s*$/.test(result)) {
						// reset if empty
						$row.trigger('tabula.expandingTable.toggle');
						$container.html("<p>No data is currently available.</p>");
						$row.find('.status-col dt .unsaved').remove();
						$container.removeData('loaded');
					} else {
						$container.html(result);
						$container.trigger('tabula.expandingTable.contentChanged');
					}
				},
				error: function(){alert("There has been an error. Please reload and try again.");}
			});
		};

		if ($form.parents('.online-feedback').length) prepareAjaxForm($form, function(resp) {
			var $resp = $(resp);

			// there should be an ajax-response class somewhere in the response text
			var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
			var success = $response.length && $response.data('status') == 'success';

			if (success) {
				var rejected = $response.data('data') == 'Rejected';
				var markingCompleted = $response.data('data') == 'MarkingCompleted';
				var $statusContainer = $row.find('.status-col dt');
				var $actionContainer = $row.find('.action-col');
				var nextMarkerAction = $row.data('nextmarkeraction');

				if(rejected) {
					$statusContainer.find('.label-warning').remove(); // remove existing label before adding another
					$statusContainer.append($('<div class="label label-important">Changes requested</div>'));
					$actionContainer.html('No action required.');
				}
				else if(markingCompleted) {
					$statusContainer.find('.label-warning').remove(); // remove existing label before adding another
					$statusContainer.append($('<div class="label label-success">Marking completed</div>'));
					$row.addClass("marking-completed");
					$actionContainer.html('No action required.');
				}
				else if(nextMarkerAction != undefined && !$statusContainer.find('.marked').length) {
					$statusContainer.find('.label-warning').remove(); // remove existing label before adding another
					if(!$row.hasClass("in-progress")) {
						$statusContainer.append($('<span class="label label-info">In progress</span>'));
						$row.addClass("in-progress");
					}
					$actionContainer.html(nextMarkerAction);
				}

				$row.find('input[type="checkbox"]').first().trigger('change');
				$row.next('tr').trigger('tabula.expandingTable.toggle');

				return "";
			} else {
				return resp;
			}
		});

		if ($form.parents('.feedback-adjustment').length) prepareAjaxForm($form, function(resp) {
			var $resp = $(resp);
			// there should be an ajax-response class somewhere in the response text
			var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
			var success = $response.length && $response.data('status') == 'success';

			if (success) {
				return "";
			} else {
				return resp;
			}
		});

		if ($form.parents('.extension-detail').length) prepareAjaxForm($form, function(resp) {
			var $resp = $(resp);

			// there should be an ajax-response class somewhere in the response text
			var $response = $resp.find('.ajax-response').andSelf().filter('.ajax-response');
			var success = $response.length && $response.data('status') == 'success';

			if (success) {
				var rejected = $response.data('data').status == 'Rejected';
				var approved = $response.data('data').status == 'Approved';
				var revoked = $response.data('data').status == 'Revoked';

				// update status
				var $statusContainer = $row.find('.status-col dt');
				$statusContainer.find(".label").remove();

				if(rejected) {
					$statusContainer.append($('<span class="label label-important">Rejected</span>'));
				}
				else if(approved) {
					$statusContainer.append($('<span class="label label-success">Approved</span>'));
				}
				else if(revoked) {
					$statusContainer.append($('<span class="label no-extension">No extension</span>'));
				}

				// update duration
				$statusContainer.data('duration', parseInt($response.data('data').extensionDuration));
				$statusContainer.data('requestedExtraDuration', parseInt($response.data('data').requestedExtraExtensionDuration));
				$statusContainer.data('awaitingReview', false);
				$statusContainer.data('approved', approved);
				$statusContainer.data('rejected', rejected);
				renderDurationProgressBar($row);

				return "";
			} else {
				return resp;
			}
		});

		// Initialise any popovers loaded in the new content
		$('.use-popover').tabulaPopover({
			trigger: 'click',
			container: '#container'
		});
	});

	var maxDaysToDisplayAsProgressBar = $('table.students').data('max-days');
	var totalDuration;
	var $progress;

	var barWidth = function(duration) {
		return 100 * duration / totalDuration;
	}
	var tooltip = function(verb, duration) {
		return verb + ' ' + duration + ' day' + (duration == 1 ? "" : "s");
	}
	var appendBar = function(verb, customClass, duration) {
		if (duration) {
			$progress.append($('<div class="bar ' + customClass + ' use-tooltip" title="' + tooltip(verb, duration) + '" style="width: ' + barWidth(duration) + '%" data-container="body"></div>'));
		}
	}
	var renderDurationProgressBar = function($row) {
		var $dt = $row.find('dt');
		var $durationCol = $row.find('.duration-col');

		// ignore rows without extension
		if ($row.find('.no-extension').length) {
			$durationCol.empty();
		} else {
			var duration = $dt.data('duration');
			var requestedExtraDuration = $dt.data('requestedExtraDuration');
			totalDuration = duration + requestedExtraDuration;

			var isOverTime = totalDuration > maxDaysToDisplayAsProgressBar;
			var progressClass = "progress";
			var progressWidth;
			if (isOverTime) {
				progressWidth = 100;
				progressClass += " overTime";
			} else {
				progressWidth = 90*totalDuration / maxDaysToDisplayAsProgressBar;
			}

			$progress = $('<div class="' + progressClass + '" style="width: ' + progressWidth + '%"></div>');

			if ($dt.data('rejected') && !$dt.data('awaitingReview')) {
				appendBar('Rejected request for', 'bar-danger', requestedExtraDuration);
			} else if ($dt.data('approved')) {
				appendBar('Approved', 'bar-success', duration);
			}
			if ($dt.data('awaitingReview')) {
				appendBar('Requested further', 'bar-warning', requestedExtraDuration);
			}

			$durationCol.empty().append($progress);
		}
	}

	$('table.students tbody tr').each(function() {
			renderDurationProgressBar($(this));
			$(this).find('.bar').tooltip();
	});

	$('#main-content').on('click', '.btn.revoke', function(e) {
		e.preventDefault();
		e.stopPropagation();
		var message = 'Revoking an extension is irreversible. Are you sure?';
		var modalHtml = "<div class='modal hide fade' id='confirmModal'>" +
			"<div class='modal-body'>" +
			"<h5>"+message+"</h5>" +
			"</div>" +
			"<div class='modal-footer'>" +
			"<a class='confirm btn'>Yes, revoke</a>" +
			"<a data-dismiss='modal' class='btn btn-primary'>No, go back</a>" +
			"</div>" +
			"</div>"
		var $modal = $(modalHtml);
		$modal.data('form', $(this).closest('form'));
		$modal.modal();
		$('a.confirm', $modal).on('click', function() {
			$modal.modal('hide');
			var $form = $modal.data('form');
			// Dislike literals, but can't see Routes from here
			$form.attr('action', $form.attr('action').replace('detail/', 'revoke/'));
			$form.submit();
		});
	});


	$('#main-content').on('tabula.expandingTable.parentRowCollapsed', '.content-container', function(e) {
		var $this = $(this);
		var contentId = $this.attr('data-contentid');
		var $row = $('tr.itemContainer[data-contentid='+contentId+']');
		var $statusContainer = $row.find('.status-col dt');
		if(hasChanges($this)) {
			if(!$statusContainer.find('.unsaved').length){
				$statusContainer.append($('<div class="label label-important unsaved">Unsaved changes</div>'));
			}
		} else {
			$statusContainer.find('.unsaved').remove();
		}
	});

	$('#main-content').on("click", ".setExpiryToRequested", function(e) {
		e.preventDefault();
		var $form = $(this).closest('form');
		var $expiryDateField = $form.find('.date-time-picker');
		if ($expiryDateField.length > 0) {
			var expiryDatePicker = $expiryDateField.data('datetimepicker');
			var requestedExpiryDate = new Date($form.find('[name=rawRequestedExpiryDate]').val());
			if (requestedExpiryDate) {
				expiryDatePicker.setDate(requestedExpiryDate);
				expiryDatePicker.setValue();
			}
		}
	});

	$('#main-content').on("click", ".remove-attachment", function(e) {
		var $this = $(this);
		var $form = $this.closest('form');
		var $li = $this.closest("li");
		$li.find('input, a').remove();
		$li.find('span').before('<i class="icon-remove"></i>&nbsp;').wrap('<del />');
		$li.find('i').css('display', 'none');
		var $ul = $li.closest('ul');

		if (!$ul.find('li').last().is('.alert')) {
			var alertMarkup = '<li class="alert pending-removal"><i class="icon-lightbulb"></i> Files marked for removal won\'t be deleted until you <samp>Save</samp>.</li>';
			$ul.append(alertMarkup);
		}

		if($('input[name=attachedFiles]').length == 0){
			var $blankInput = $('<input name="attachedFiles" type="hidden" />');
			$form.append($blankInput);
		}
		return false;
	});

	function resetFormValues($form, $row) {
		// reset all the data for this row
		$('input, textarea', $form).each(function() {
			$(this).val($(this).data('initialvalue'));
		});

		// remove unsaved badges
		$row.find('.unsaved').remove();

		// collapse the row
		$row.trigger('tabula.expandingTable.toggle');
	}

	function hasChanges($container) {
		return $container.data('loaded') &&
			($container.find('.pending-removal').length > 0 || inputsHaveChanges($container));
	}

	function inputsHaveChanges($container) {
		var modifiedField = false;
		var $inputs = $container.find('input,textarea');
		$inputs.each(function() {
			modifiedField = $(this).val() != $(this).data("initialvalue");
			return !modifiedField; // false breaks from loop
		});
		return modifiedField;
	}

	// just for generic feedback
	$(".edit-generic-feedback").on('click', function() {
		var $icon = $(this).find('i');
		$icon.toggleClass('icon-chevron-right')
			 .toggleClass('icon-chevron-down');
		var $container = $('.edit-generic-feedback-container');
		if($container.is(':visible')){
			$icon.addClass('icon-chevron-right')
			$icon.removeClass('icon-chevron-down');
			$container.hide();
		} else {
			if($container.find('form').length) {
				$icon.removeClass('icon-chevron-right')
				$icon.addClass('icon-chevron-down');
				$container.show();
			} else {
				$icon.removeClass('icon-chevron-right');
				$icon.addClass('icon-spinner icon-spin');
				$container.load('generic', function(){
					$icon.removeClass('icon-spinner icon-spin');
					$icon.addClass('icon-chevron-down');
					$(this).show();
                    $(".expanding-table").trigger('tabula.expandingTable.repositionContent');
				});
			}
		}
        $(".expanding-table").trigger('tabula.expandingTable.repositionContent');
	});

	$(".generic-feedback").on('click', 'input[type=submit]', function(e){
		e.preventDefault();
		$('.before-save').hide();
		$('.saving').show();
		var $form = $(e.target).closest('form');
		$.ajax({
			type: "POST",
			url: $form.attr('action'),
			data: $form.serialize(),
			success: function( response ) {
				$('.saving').hide();
				$('.saved').show();
			}
		});
	});
});

/**
 * Special case for downloading submissions as PDF.
 * Does a check for non PDF files in submissions before fetching the download
 */
$(function(){
	$('a.download-pdf').on('click', function(e){
		var $this = $(this);
		e.preventDefault();
		if(!$(this).hasClass("disabled")) {
			var $modal = $('#download-pdf-modal'), action = this.href;
			if ($this.data('href')) {
				action = $this.data('href')
			}
			var postData = $('div.form-post-container').find('.collection-checkbox:checked').map(function(){
				var $input = $(this);
				return $input.prop('name') + '=' + $input.val();
			}).get().join('&');
			$.post(action, postData, function(data){
				if (data.submissionsWithNonPDFs.length === 0) {
					// Use native click instead of trigger because there's no click handler for the marker version
					$modal.find('.btn.btn-primary').get(0).click();
				} else {
					$modal.find('.count').html(data.submissionsWithNonPDFs.length);
					var $submissionsContainer = $modal.find('.submissions').empty();
					$.each(data.submissionsWithNonPDFs, function(i, submission){
						var fileList = $('<ul/>');
						$.each(submission.nonPDFFiles, function(i, file){
							fileList.append($('<li/>').html(file));
						});
						$submissionsContainer.append($('<li/>').append(
							(submission.name.length > 0) ? submission.name + ' (' + submission.universityId + ')' : submission.universityId
						).append(fileList))
					});
					$modal.modal('show');
				}
			});
		}
	})
});

}(jQuery));


