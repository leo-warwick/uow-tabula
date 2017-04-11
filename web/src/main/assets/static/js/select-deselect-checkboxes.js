(function($) {

	// toggle checkboxes depending on value of "select all" type checkbox
	jQuery.fn.selectDeselectCheckboxes = function(selectAllCheckbox) {

		var selectOrDeselect = $(selectAllCheckbox).prop('checked');
		var selectAllCheckboxes =  $("." + $(selectAllCheckbox).attr('class'))  ;
		selectAllCheckboxes.prop('checked', selectOrDeselect);

		var allCheckboxes = $(this).find('[type=checkbox]');
		allCheckboxes.prop('checked', $(selectAllCheckbox).prop('checked'));
		$(selectAllCheckbox).trigger('tabula.selectDeselectCheckboxes.toggle');
	}

})(jQuery);