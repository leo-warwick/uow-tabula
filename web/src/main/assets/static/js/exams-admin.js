(function ($) { "use strict";

/**
 * Scripts used only by the exams admin section.
 */
var examsExports = {};

examsExports.zebraStripeExams = function($module) {
	$module.find('.assignment-info').filter(':visible:even').addClass('alt-row');
};

examsExports.prepareAjaxForm = function($form, callback) {
	var $container = $form.closest('.content-container');
	var contentId = $container.attr('data-contentid');
	var $row = $('tr.item-container[data-contentid='+contentId+']');
	$form.ajaxForm({
		iframe: true,
		statusCode: {
			403: function(jqXHR) {
				$container.html("<p>Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p>Refresh the page and try again. If it remains a problem, please let us know using the 'Problems, questions?' button at the top of the page.</p>");
				$container.trigger('tabula.expandingTable.contentChanged');
			}
		},
		success: function(response) {
			var result = callback(response);

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

window.Exams = jQuery.extend(window.Exams, examsExports);

/**
* Scripts used only by the exam grids section.
*/

var examGridsExports = {};

function additionalYearsOfStudyToggle($editor) {
	$('input[name=showAdditionalYears]').on('change', function(){
		var $this = $(this);
		if ($this.is(':checked')) {
			$editor
				.find('.row > .col-md-5').removeClass('col-md-5').addClass('col-md-2').end()
				.find('.row > .col-md-offset-5').removeClass('col-md-offset-5').addClass('col-md-offset-2').end()
				.find('.row > .col-md-6').removeClass('col-md-6').addClass('col-md-9')
					.find('.col-md-2').removeClass('col-md-2').addClass('col-md-1').end()
					.each(function(){
						$(this).find('.col-md-1').show();
					});
		} else {
			$editor
				.find('.row > .col-md-2').removeClass('col-md-2').addClass('col-md-5').end()
				.find('.row > .col-md-offset-2').removeClass('col-md-offset-2').addClass('col-md-offset-5').end()
				.find('.row > .col-md-9').removeClass('col-md-9').addClass('col-md-6')
					.find('.col-md-1').removeClass('col-md-1').addClass('col-md-2').end()
					.each(function(){
						$(this).find('.col-md-2').filter(function(i){ return i > 5; }).hide();
					});

		}
	}).trigger('change');
}

function otherAcademicYearModalHandler($modal, inputName, collectionName) {
	var $loading = $modal.find('.loading')
		, $content = $modal.find('.content')
		, $academicYearSelect = $('select[name=academicYear]')
		, $copyButton = $modal.find('.modal-footer .btn-primary').on('click', function(){
			var data = $(this).data('otherAcademicYear');
			if (data) {
				var codes = _.keysIn(data), yearsOfStudy = _.keysIn(data[codes[0]]);
				_.each(codes, function(code){ _.each(yearsOfStudy, function(year){
					$('input[name="' + inputName + '[' + code + '][' + year + ']"]').val(data[code][year]);
				})});
				$content.empty();
				$academicYearSelect.val('');
				$modal.modal('hide');
			}
		})
		, complete = false;

	$modal.on('click', 'select', function(){
		complete = false;
		var academicYear = $academicYearSelect.find(':selected').val();
		if (academicYear.length > 0) {
			$content.empty();
			$copyButton.prop('disabled', true);
			setTimeout(function() {
				if (!complete) {
					$loading.show();
				}
			}, 300);
			$.get($academicYearSelect.data('href') + academicYear, function(data){
				var codes = _.keysIn(data).sort();
				if (codes.length === 0) {
					$content.append('Could not find any ' + collectionName + ' owned by this department');
				} else {
					var yearsOfStudy = _.sortBy(_.keysIn(data[codes[0]]), function(year){ return parseInt(year); });
					var $table = $('<table/>').addClass('table table-striped table-condensed').append(
						$('<thead/>').append(
							$('<tr/>').append(
								$('<th/>')
							).append(
								$('<th/>').attr('colspan', yearsOfStudy.length).html('Year of study')
							)
						).append(
							$('<tr/>').append(
								$('<th/>').html(_.capitalize(collectionName))
							)
						)
					).append(
						$('<tbody/>')
					);
					var $yearOfStudyRow = $table.find('thead tr:nth-of-type(2)'), $tbody = $table.find('tbody');
					_.each(yearsOfStudy, function(year){
						$yearOfStudyRow.append($('<th/>').html(year));
					});
					_.each(codes, function(code){
						var $row = $('<tr/>').append($('<th/>').html(code.toUpperCase()));
						_.each(yearsOfStudy, function(year){
							$row.append($('<td/>').html(data[code][year]));
						});
						$tbody.append($row);
					});
					$content.append($table).wideTables();
					$copyButton.data('otherAcademicYear', data).prop('disabled', false);
				}

				$loading.hide();
				complete = true;
			});
		}
	});
}

function stripeElements($elements) {
	$elements.each(function(i){
		if (i % 2 === 0) {
			$(this).removeClass('even odd').addClass('even');
		} else {
			$(this).removeClass('even odd').addClass('odd');
		}
	});
}
function editorFilterAndBulkEdit($editor, inputName) {
	var $rows = $editor.find('.row').not('.header .row')
		, $applyButton = $editor.find('button[name=bulkapply]')
		, $allValues = $editor.find('input[name^=' + inputName + ']');
	$editor.on('keyup', 'input[name=filter]', function(){
		var filterVal = $(this).val().toLowerCase();
		if (filterVal.length === 0) {
			stripeElements($rows.show());
		} else {
			var terms = filterVal.split(' ');
			stripeElements($rows.hide().filter(function(){
				var text = $(this).find('label').text().toLowerCase();
				return _.every(terms, function(val) { return text.indexOf(val) >= 0 });
			}).show());
		}
		$(window).trigger('resize.ScrollToFixed');
	}).on('keyup', 'input[name=bulk]', function(){
		if ($editor.find('input[name=bulk]').filter(function(){ return $(this).val().length > 0 }).length > 0) {
			$applyButton.prop('disabled', false);
		} else {
			$applyButton.prop('disabled', true);
		}
	});
	$applyButton.on('click', function(){
		$editor.find('input[name=bulk]').filter(function(){ return $(this).val().length > 0 }).each(function(){
			var $this = $(this), year = $this.data('year');
			$allValues.filter(':visible').filter('[name*="[' + year + ']"]').val($this.val());
		});
	});
}

examGridsExports.manageNormalLoads = function(){
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
		var $editor = $('.normal-load-editor');
		// Stop 'Enter' from submitting the form
		$editor.find('form')
			.off('keyup.inputSubmitProtection keypress.inputSubmitProtection')
			.on('keyup.inputSubmitProtection keypress.inputSubmitProtection', function(e){
				var code = e.keyCode || e.which;
				if (code  == 13) {
					e.preventDefault();
					return false;
				}
			});
		stripeElements($editor.find('.row').not('.header .row'));
		editorFilterAndBulkEdit($editor, 'normalLoads');
		additionalYearsOfStudyToggle($editor);
		otherAcademicYearModalHandler($('#copy-loads-modal'), 'normalLoads', 'routes');		
	});
};

examGridsExports.manageWeightings = function(){
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
		var $editor = $('.course-weightings-editor');
		// Stop 'Enter' from submitting the form
		$editor.find('form')
			.off('keyup.inputSubmitProtection keypress.inputSubmitProtection')
			.on('keyup.inputSubmitProtection keypress.inputSubmitProtection', function(e){
				var code = e.keyCode || e.which;
				if (code  == 13) {
					e.preventDefault();
					return false;
				}
			});
		stripeElements($editor.find('.row').not('.header .row'));
		editorFilterAndBulkEdit($editor, 'yearWeightings');
		additionalYearsOfStudyToggle($editor);
		otherAcademicYearModalHandler($('#copy-weightings-modal'), 'yearWeightings', 'courses');
	});
};

window.ExamGrids = jQuery.extend(window.ExamGrids, examGridsExports);

$(function(){

	// code for tabs
	$('.nav.nav-tabs a').click(function (e) {
		e.preventDefault();
		$(this).tab('show');
	});

	/*** Admin home page ***/
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

})

}(jQuery));