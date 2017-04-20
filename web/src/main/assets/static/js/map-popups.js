(function ($) { "use strict";

	$.fn.mapPopups = function() {
		this.each(function() {
			var $this = $(this);
			$this.find('.map-location[data-lid]').add($this.filter('[data-lid]')).each(function() {
				var $el = $(this);
				if ($el.data('map-wired')) return;

				var mapUrl = '//campus.warwick.ac.uk/?lite=1&search=' + encodeURIComponent($el.text()) + '&slid=' + encodeURIComponent($el.data('lid'));

				var $icon =
					$('<i />').addClass('icon-map-marker');

				var $a = $('<a />').attr('href', '#');

				$el.append('&nbsp;').append($a.append($icon));

				$a.tabulaPopover({
					trigger: 'click',
					container: 'body',
					template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
					html: true,
					content: '<iframe width="300" height="400" frameborder="0" src="' + mapUrl + '"></iframe>'
				});

				$el.data('map-wired', true);
			});
		});
	};

	$(function() {
		$('.map-location[data-lid]').mapPopups();

		// Look for popovers being shown
		$(document.body).on('shown', function() {
			$(this).mapPopups();
		});
	});
})(jQuery);