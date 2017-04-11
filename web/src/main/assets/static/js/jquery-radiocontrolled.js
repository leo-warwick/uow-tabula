(function ($) { "use strict";

/*
	Modifies form elements in response to changes to a set of radio buttons or select dropdown values.
	This plugin should be applied either to a set of radio buttons, or a select dropdown.

	Options:
		selector:
			CSS selector for container of the target inputs.
			You can optionally specify this per-radio by putting it in a "data-selector"
			attribute (or a plain "selector" attribute for FTL bug compability)
			  e.g. .controls
		parentSelector:
			On change, we will look for a parent of the radio that matches this, and use that
			to then search for `selector` containing the target elements. Otherwise
			it will search for `selector` across whole document, which is not recommended
			unless your selectors are IDs (which is not recommended).
			  e.g. .control-group
		mode:
			What to do when a radio is not selected.
			'disabled' (default): disable the fields.
			'readonly': mark the fields as read only.
			'hidden': hide the whole container defined by `selector`.

	Events:
		disable.radiocontrolled:
			Invoked on each field that is disabled.
			In 'hidden' mode, this will be the container.
		enable.radiocontrolled
			Invoked on each field that is enabled.
			In 'hidden' mode, this will be the container.

	Notes:
		You can't set a select element readonly in HTML, so that currently isn't supported.
		We might add support for it by dynamically copying its value to a hidden field and disabling it,
		and doing the opposite on reenabling.
*/
jQuery.fn.radioControlled = function(options) {
	options = options || {};
	var mode = options.mode || 'disabled';
	var fireEvents = false;
	var $controllers = this;

	// for radios, the items are the radios.
	// for select, the items are its options.
	var $items = $controllers;
	if (this.is('select')) {
		$items = $(this[0].options);
	}

	// Set items "on" or "off", with varying behaviour depending on `mode`.
	function toggle($container, checked) {
		var eventName = (checked?'enable':'disable') + ".radiocontrolled";
		if (mode == 'disabled') {
			$container.find('label,input,select').toggleClass('disabled', !checked);
			$container.find('input,select').attr({disabled: !checked});
			if (fireEvents) {
				$container.find('input,select').trigger(eventName);
			}
		} else if (mode == 'readonly') {
			$container.find('input').attr({readonly: !checked});
			// select not supported yet
			if (fireEvents) {
				$container.find('input').trigger(eventName);
			}
		} else if (mode == 'hidden') {
			$container.toggle(checked);
			if (fireEvents) {
				$container.trigger(eventName);
			}
		}
	}

	$controllers.on('change', function() {
		$.each($items, function(i, item) {
			var $item = $(item);
			var selector = $item.data('selector') || $item.attr('selector') || options.selector;

			var parent = $(document.body);
			if (options.parentSelector) {
				parent = $item.closest(options.parentSelector);
			}

			if (selector) {
				var $container = jQuery(selector, parent);
				var itemSelected = item.checked || item.selected;
				toggle($container, !!itemSelected);
			}
		});
	});

	// Set initial state
	$controllers.trigger('change');

	// Reset dirty state
	$controllers.closest('form.dirty-check').trigger('reinitialize.areYouSure');

	fireEvents = true;
}


})(jQuery);