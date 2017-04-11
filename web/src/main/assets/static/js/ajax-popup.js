/**
 * Enhances links with "ajax-popup" class by turning them magically
 * into AJAX-driven popup dialogs, without needing much special magic
 * on the link itself.
 *
 * "data-popup-target" attribute can be a CSS selector for a parent
 * element for the popup to point at.
 */
(function ($) { "use strict";

var exports = {};

/**
 * Variant of ajax-popup that uses a modal instead.
 *
 * If the modal HTML is missing a modal-body, it's assumed that the AJAX
 * response will contain the header, body and footer, and will load all that
 * in. Otherwise it will insert it into the modal-body as normal.
 *
 * HEY: Currently only handles static responses, doesn't catch forms
 * and redecorate their responses. But it should be made to do that
 * eventually.
 */
jQuery.fn.ajaxModalLink = function(options) {

	this.click(function(e){
		e.preventDefault();
		var options = options || {},
		    link = this,
		    $link = $(link),
		    $modalElement = $($link.data('target')) || options.target,
		    href = options.href || $link.attr('href'),
		    $modalBody = $modalElement.find('.modal-body'),
		    wholeContent = $modalBody.length === 0,
			$tabulaModalContainer = $('<div class="modal-dialog"><div class="modal-content"><div class="modal-header"><h3>Loading&hellip;</h3></div></div></div>');

		var onLoad = function() {
			// Allow recursive modal links, because that can only end well
			$modalElement.find('a.ajax-modal').ajaxModalLink();
		};

		if (wholeContent) {
			// HTML has no modal-body. bootstrap.js doesn't directly support this,
			// so let's manually shove it in.
			$modalElement.append($tabulaModalContainer);
			$modalElement.modal({ remote: null });
			$modalElement.load(href, onLoad);
		} else {
			$modalElement.find('.modal-body').html("<p>Loading&hellip;</p>");
			$modalElement.modal({ remote: null });
			$modalElement.find('.modal-body').load(href, onLoad);
		}

		// Forget about modal on hide, otherwise it'll only load once.
		$modalElement.on('hidden hidden.bs.modal', function() {
			if (wholeContent) {
				// There was no modal-body when we first started, so revert back to
				// this situation to allow the init code to work next time.
				$modalElement.find('.modal-dialog').remove();
			}
			$modalElement.removeData('modal');
		});

		return false;
	});

	// some ajax links may be hidden to prevent them being clicked before this script is run - show them
	this.show();
};

$(function() {
	$('a.ajax-modal').ajaxModalLink();
});

var ajaxPopup = null;
exports.getPopup = function() {
	return ajaxPopup;
};

exports.wireAjaxPopupLinks = function($container) {
	$container.find('a.ajax-popup').click(function(e){
	    var link = e.target,
	        $link = $(link),
	        popup = exports.getPopup(),
	        width = 300,
	        height = 300,
	        pointAt = link;

	    if ($link.data('popup-target') !== null) {
	        pointAt = $link.closest($link.data('popup-target'));
	    }

	    var decorate = function($root) {
	        if ($root.find('html').length > 0) {
	            // dragged in a whole document from somewhere, whoops. Replace it
	            // rather than break the page layout
	            popup.setContent("<p>Sorry, there was a problem loading this popup.</p>");
	        }
	        $root.find('.cancel-link').click(function(e){
	            e.preventDefault();
	            popup.hide();
	        });
	        $root.find('input[type=submit]').click(function(e){
	            e.preventDefault();
	            var $form = $(this).closest('form');

	            // added ZLJ Aug 10th to make a unique URL which IE8 will load
	            var $randomNumber = Math.floor(Math.random() * 10000000);

	            // this line doesn't work in IE8 (IE8 bug) - need to make the URL
	            // unique using generated random number
	            //jQuery.post($form.attr('action'), $form.serialize(), function(data){
	            jQuery.post($form.attr('action') + "?rand=" + $randomNumber, $form.serialize(), function(data){
	                popup.setContent(data);
	                decorate($(popup.contentElement));
	            });
	        });
	        // If page returns hint that we're done, close the popup.
	        if ($root.find('.ajax-response').data('status') == 'success') {
	            //popup.hide();
	            // For now, reload the page. Might extend it to allow selective reloading
	            // of just the thing that's changed.
	            window.location.reload();
	        }
	    };

	    e.preventDefault();
	    popup.setContent("Loading&hellip;");
	    popup.setSize(width, height);
	    $.get(link.href, function (data) {
	        popup.setContent(data);
	        var $contentElement = $(popup.contentElement);
	        decorate($contentElement);
	        popup.setSize(width, height);
	        popup.show();
	        popup.increaseHeightToFit();
	        popup.positionLeft(pointAt);
	    }, 'html');
	});
};

$(function() {
	ajaxPopup = new WPopupBox();

	exports.wireAjaxPopupLinks($('#container'));
});


// take anything we've attached to "exports" and add it to the global "AjaxPopup"
// we use extend() to add to any existing variable rather than clobber it
window.AjaxPopup = jQuery.extend(window.AjaxPopup, exports);

})(jQuery);
// end AJAX modal
