(function ($) { "use strict";

	var Config = {
		Defaults: {
			container: 'body',
			template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
			html: true,
			content: _.template(['<iframe width="300" height="400" frameborder="0" src="<%- mapUrl %>"></iframe>']),
			placement: 'right',
			expandClickTarget: false
		}
	};

	$.fn.mapPopups = function(o) {
		this.each(function() {
			var $this = $(this), options = $.extend({}, Config.Defaults, o);
			$this.find('.map-location[data-lid]').add($this.filter('[data-lid]')).each(function() {
				var $el = $(this);
				if ($el.data('map-wired')) return;

				var mapUrl = '//campus.warwick.ac.uk/?lite=1&search=' + encodeURIComponent($el.text()) + '&slid=' + encodeURIComponent($el.data('lid'));

				var $icon = $('<i />').addClass('fa').addClass('fa-map-marker');
				var $a = $('<a />').addClass('use-popover').attr('href', '#');

				if (options.expandClickTarget) {
					$a.insertAfter($el);
					$el.remove().appendTo($a).append('&nbsp;').append($icon);
				} else {
					$el.append('&nbsp;').append($a.append($icon));
				}

				$a.tabulaPopover({
					trigger: 'click',
					container: options.container,
					template: options.template,
					html: options.html,
					content: options.content({mapUrl:mapUrl}),
					placement: options.placement
				});

				$el.data('map-wired', true);
			});
		});
	};

	$(function() {
		$('.map-location[data-lid]').mapPopups();

		// Look for popovers being shown
		$(document.body).on('shown.bs.popover shown.bs.modal', function() {
			$(this).mapPopups();
		});
	});
})(jQuery);