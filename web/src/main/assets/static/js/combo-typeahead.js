(function($) { 'use strict';

	/**
	 * Automatically convert a <select> element to a Bootstrap Typeahead by annotating it with
	 * data-provide="typeahead".
	 *
	 * @see http://getbootstrap.com/2.3.2/javascript.html#typeahead
	 */
	$.fn.comboTypeahead = function(options) {
		this.each(function() {
			var $this = $(this);

			// Defaults
			var allOptions = {
				items: 8,
				minLength: 1
			};

			$.extend(allOptions, options || {});

			var currentValue = $this.find(':selected').text().trim() || '';

			var items =
				$this.find('option')
				 	.filter(function() { return $(this).val(); })
					.map(function() {
						return $(this).text().trim();
					})
					.get();

			// Create the typeahead element
			var $input = $('<input />', {
				'type': 'text',
				'class': $this.attr('class'),
				'style': $this.attr('style'),
				'placeholder': $this.attr('placeholder'),
				'value': currentValue
			}).attr("autocomplete", "off").on('keyup', function(e) {
				if ($.inArray(e.keyCode, [40,38,16,17,18,9,13,27]) == -1) {
					$this.find(':selected').prop('selected', false);
					$this.trigger('change');
				}
			});
			var typeaheadOptions = {
				source: items,
				items: allOptions.items,
				minLength: allOptions.minLength,
				matcher: function(item) {
					// Fast match
					if (~item.toLowerCase().indexOf(this.query.toLowerCase())) {
						return true;
					}

					var query = this.query.toLowerCase().replace(/[^a-z0-9\s]/g, '');
					if (!query.length) { return false; }

					var searchTerms = query.split(/\s+/g);
					if (searchTerms.length == 0) { return false; }

					// Each word in search is a substring of item
					var itemStripped = item.toLowerCase().replace(/ & /g, ' and ').replace(/[^a-z0-9\s]/g, '');

					for (var i = 0; i < searchTerms.length; i++) {
						if (itemStripped.indexOf(searchTerms[i]) == -1) {
							return false;
						}
					}

					return true;
				},
				highlighter: function(item) {
					// If fast match, use fast highlighter
					if (~item.toLowerCase().indexOf(this.query.toLowerCase())) {
						var query = this.query.replace(/[\-\[\]{}()*+?.,\\\^$|#\s]/g, '\\$&');
						return item.replace(new RegExp('(' + query + ')', 'ig'), function ($1, match) {
							return '<strong>' + match + '</strong>'
						});
					}

					var searchTerms = this.query.toLowerCase().replace(/[^a-z0-9\s]/g, '').split(/\s+/g);

					var itemParts = item.split(/\s+/); // FIXME will merge multiple spaces into one
					for (var i = 0; i < itemParts.length; i++) {
						for (var j = 0; j < searchTerms.length; j++) {
							if (itemParts[i].toLowerCase().replace(/[^a-z0-9\s]/g, '').indexOf(searchTerms[j]) != -1) {
								itemParts[i] = itemParts[i].replace(new RegExp('(' + searchTerms[j] + ')', 'ig'), function($1, match) {
									return '<strong>' + match + '</strong>';
								});
								break;
							}
						}
					}

					return itemParts.join(' ');
				},
				updater: function(item) {
					$this.find(':selected').prop('selected', false);

					$this.find('option')
						.filter(function() {
							return $(this).val() && $(this).text().trim() == item;
						})
						.prop('selected', true);

					$this.trigger('change');

					return item;
				}
			};

			if ($.fn.bootstrap3Typeahead) {
				$input.bootstrap3Typeahead(typeaheadOptions);
			} else {
				$input.typeahead(typeaheadOptions);
			}

			$this.hide().after($input);
		});
		return this;
	};

	$(function() {
		$('select[data-provide="typeahead"]').comboTypeahead();
	});
})(jQuery);