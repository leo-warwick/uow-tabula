// A lot like <details> (and is based on the jquery-details plugin) except has no <summary>;
// it is just a collapsibly container that shows the first line when collapsed.
;(function(document, $) {
	'use strict';

	var proto = $.fn,
		// :'(
		isOpera = Object.prototype.toString.call(window.opera) == '[object Opera]',
		collapsible,
		toggleOpen = function($element, toggle) {
			var isOpen = $element.prop('open'),
				close = isOpen && toggle || !isOpen && !toggle;
			if (close) {
				$element.removeClass('open').prop('open', false).triggerHandler('close.collapsible');
			} else {
				$element.addClass('open').prop('open', true).triggerHandler('open.collapsible');
			}
		};

	collapsible = proto.collapsible = function(op) {
		return this.each(function() {

			// Store a reference to the current `details` element in a variable
			var $element = $(this);

			$element.addClass('collapsible');

			if (op === 'open') {
				$element.prop('open', true);
				toggleOpen($element);
				return;
			} else if (op === 'close') {
				$element.prop('open', false);
				toggleOpen($element);
				return;
			}

			// Hide content unless there’s an `open` attribute
			$element.prop('open', typeof $element.attr('open') == 'string');
			toggleOpen($element);

			// Add `role=button` and set the `tabindex` of the `summary` element to `0` to make it keyboard accessible
			$element.on('click', function() {
					// Focus on the `summary` element
					//$element.focus();
					// Toggle the `open` and `aria-expanded` attributes and the `open` property of the `details` element and display the additional info
					toggleOpen($element, true);
				})
				.on('click', 'a', function(e) {
					e.stopPropagation(); // let clicks happen without the thing collapsing.
				})
				.keyup(function(event) {
					if (32 == event.keyCode || (13 == event.keyCode && !isOpera)) {
						// Space or Enter is pressed — trigger the `click` event on the `summary` element
						// Opera already seems to trigger the `click` event when Enter is pressed
						event.preventDefault();
						$element.click();
					}
				});

		});

	}

}(document, jQuery));