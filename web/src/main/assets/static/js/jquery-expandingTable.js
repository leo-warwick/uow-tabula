(function ($) { "use strict";
/*
	Applied to tables. When a table row is clicked then some hidden content is made visible and placed beneath the
	row. The following rows in the table are moved down to make space for the content. Clicking on the row again hides
	the content.


	A table cell in each row is designated as the content cell. This cell should contain the follwing markup
	Note that the class names given here can be overriden in the options.

	<td class="content-cell">
		<dl>
			<dt>
				<!-- cell content goes here - this will appear in the table -->
			</dt>
			<dd class="table-content-container">
				<!-- This is a placeholder for the content that will be displayed when a row is expanded -->
			<dd>
		</dl>
	</td>


	Options:
		contentCellSelector:
			A class added to the cell in the table that will contain the content (see above). Defaults to
			'content-cell'
		tableContainerSelector:
			A class added to the element that will hold the content (see above). Defaults to 'table-content-container'
		iconSelector:
			A class added to the element that will contain the state icon for the row. Defaults to 'toggle-icon'
		toggleCellSelector:
			A class added to each cell in the table that should trigger the toggle behaviour for the parent row.
			Defaults to 'toggle-controller'
		allowTableSort:
			Apply the tablesorter JQuery plugin to this table. Defaults to true
		tableSorterOptions:
			An option map that will be passed to the tableSorter plugin
		contentUrlFuction:
			A function to return which URL we will use to load the content. Function's argument is the selected row.
		preventContentIdInUrl:
			An option to prevent appending the contentId to the dataUrl (TAB-2427)
*/
jQuery.fn.expandingTable = function(options) {

	options = options || {};

	var contentCellSelector = options.contentCellSelector || '.content-cell';
	var tableContainerSelector = options.tableContainerSelector || '.table-content-container';
	var iconSelector = options.iconSelector || '.toggle-icon';
	var toggleCellSelector = options.toggleCellSelector || '.toggle-cell';
	var allowTableSort = options.allowTableSort || true;
	var tableSorterOptions = options.tableSorterOptions || {};
	var preventContentIdInUrl = options.preventContentIdInUrl || false;

	var iconMarkup = '<i class="row-icon icon-chevron-right icon-fixed-width"></i>';

	//var $table = this.first();

	this.each(function(index, table){

		var $table = $(table);

		function repositionContentBoxes() {
			// These have to be positioned in the right order, so we loop through content-container rather than
			// directly on content
			$table.find(tableContainerSelector).hide().each(function() {
				var contentID = $(this).attr('data-contentid');
				var $content = $('#content-' + contentID);

				if ($content.length) {
					var isOpen = $content.data('open');

					if (isOpen) {
						$(this).show();
						var $contentCell = $(this).closest(contentCellSelector);

						// Add bottom padding equivalent to the height of the content to the content cell
						var cellPosition = $contentCell.position();

						$contentCell.css('padding-bottom', '');
						var cellHeight = $contentCell.outerHeight();
						var contentHeight = $content.outerHeight();
						$contentCell.css('padding-bottom', contentHeight + 10);

						// Position the content div in the correct location
						$content.css({
							top: (cellPosition.top + cellHeight - 2) + 'px',
							left: ($table.position().left + 1) + 'px'
						});
					}
				}
			});
		}

		$table.on('tabula.expandingTable.repositionContent', function() {
			repositionContentBoxes();
		});

		function hideContent($content, $row, $icon) {
			var $contentCell = $('.content-cell', $row);

			// Hide the content div and move it back to where it was before
			$content.hide();
			$contentCell.find('.table-content').append($content);

			// Remove the bottom padding on the content cell
			$contentCell.css('padding-bottom', '');

			// Remove any position data from the content div and hide
			$content.attr('style', 'display:none;');

			// Change the icon to closed
			$icon.removeClass('icon-chevron-down').addClass('icon-chevron-right');

			// Set the data
			$content.data('open', false);

			repositionContentBoxes();
			$content.trigger('tabula.expandingTable.parentRowCollapsed');
		}

		function showContent($content, $row, $icon) {
			// Move the workflow div to be at the end of the offset parent and display it
			$('#main-content').append($content);
			$content.show();

			if ($row.is(':nth-child(odd)')) {
				$content.css('background-color', '#ebebeb');
			} else {
				$content.css('background-color', '#f5f5f5');
			}

			$content.css({
				width: ($row.closest('table').width() - 21) + 'px'
			});

			// Change the icon to open
			$icon.removeClass('icon-chevron-right').addClass('icon-chevron-down');

			// Set the data
			$content.data('open', true);

			repositionContentBoxes();
			$content.trigger('tabula.expandingTable.parentRowExpanded');
		}

		function toggleRow($content, $row, $icon) {
			if ($content.length) {
				var isOpen = $content.data('open');
				if (isOpen) {
					hideContent($content, $row, $icon);
				} else {
					if(options.contentUrlFunction && !$content.data("loaded")) {
						var contentId = "";
						// data attributes can't contain / so we use __ instead and replace them here
						// __ is just as arbitrary as _ but is less likely to actually be used in the content ID
						if(!preventContentIdInUrl) { contentId = '/' + ($row.attr("data-contentid")).replace("__","/"); }
						var dataUrl = options.contentUrlFunction($row) + contentId;

						$icon.removeClass('icon-chevron-right').addClass('icon-spinner icon-spin');

						$content.load(dataUrl, 'dt=' + new Date().valueOf(), function(resp, status, req) {
							if ("403" == req.status) {
								$content.html("<p class='text-error'><i class='icon-warning-sign'></i> Sorry, you don't have permission to see that. Have you signed out of Tabula?</p><p class='text-error'>Refresh the page and try again. If it remains a problem, please let us know using the comments link on the edge of the page.</p>");
							}
							$icon.removeClass('icon-spinner icon-spin');
							showContent($content, $row, $icon);
							$content.data("loaded", "true");
							$content.trigger('tabula.expandingTable.contentChanged');
						});

					} else {
						showContent($content, $row, $icon);
					}
				}
			}
		}

		// init
		$('tbody tr', $table).each(function() {
			var $row = $(this);
			// get the contentId for this row
			var contentId = $row.attr("data-contentid");
			var $content = $('#content-' + contentId);

			// add the arrow icon to the row
			var $iconCell = $(iconSelector, $row).first();
			var $icon = $(iconMarkup).css('margin-top', '2px');
			$iconCell.prepend(' ').prepend($icon);
			// find the cells in the row that will trigger a toggle when clicked.
			var $toggleCells = $(toggleCellSelector, $row);
			// add some styles to them and register the click event
			$toggleCells.css('cursor', 'pointer');

			// add click handlers to the toggle cells
			$toggleCells.on('click', function(evt) {
				toggleRow($content, $row, $icon);
				evt.preventDefault();
				evt.stopPropagation();
			});

			// add handlers for a cutom event so external modules can collapse the row
			$row.on('tabula.expandingTable.toggle', function(evt) {
				toggleRow($content, $row, $icon);
				evt.preventDefault();
				evt.stopPropagation();
			});

			// global scope for this event. all instances of the plugin will need to run this
			$(document).on('tabula.expandingTable.contentChanged', function(evt) {
				repositionContentBoxes();
			});

		});

		if(allowTableSort) {
			$.tablesorter.addWidget({
				id: 'repositionContentBoxes',
				format: repositionContentBoxes
			});

			var widgetOptions = { widgets: ['repositionContentBoxes'] };
			var sortOptions = $.extend(widgetOptions, tableSorterOptions);
			$table.tablesorter(sortOptions);
		}

		// TAB-2075 open expanded row when fragment identifier present
		// expects to find a data attribute on the table called row-to-open
		// which contains the content id for that row
		var contentIdToOpen = $table.data('row-to-open');

		if(contentIdToOpen) {
			var $content = $('#content-' + contentIdToOpen);
			var $row = $("tr[data-contentid='" + contentIdToOpen + "']");
			var $icon = $(iconSelector, $row).first().find(".row-icon");
			toggleRow($content, $row, $icon);
			$content.on('tabula.expandingTable.contentChanged', function() {
				$content[0].scrollIntoView();
			});
		}

	});

}

})(jQuery);