(function ($) { "use strict";

/*
	Used on forms. Blocks the form from submitting and displays a confirmation message in a modal.
	Clicking 'confirm' button submits the form.

	Options:
		message - the message to display in the modal.
		wrapMessage - wraps the message in a h5 tag - default is true
		confirm - the text for the confirm button
		cancel - the text for the cancel button
		primary - use text 'cancel' to make cancel the (highlighted) primary option

	Use $(el).confirmModal(false) to unbind the event handlers.

	The plugin uses namespaced events for sandboxing, and can only be bound once to a given form.
*/

	jQuery.fn.confirmModal = function(options) {
		var $ = jQuery;
		this.each(function() {
			var $form = $(this);
			if($form.is('form')) {
				if (options == false) {
					$(document).off('.confirmModal');
				} else if ($form.data('confirmModal') == undefined) {
					var message = options.message || 'Are you sure?',
						confirm = options.confirm || 'Confirm',
						cancel = options.cancel || 'Cancel',
						wrapMessage = options.wrapMessage || true,
						confirmPrimary = (options.primary && !(options.primary != 'confirm')) || true,
						confirmClass = 'confirmModal confirm btn' + (confirmPrimary?' btn-primary':''),
						cancelClass = 'confirmModal cancel btn' + (confirmPrimary?'':' btn-primary'),
						fullMessage = wrapMessage ? "<h5>" + message + "</h5>" : message,
						modalHtml = "<div class='modal hide fade'>" +
										"<div class='modal-body'>" +
											fullMessage +
										"</div>" +
										"<div class='modal-footer'>" +
											"<button class='" + confirmClass + "'>" + confirm + "</button>" +
											"<button data-dismiss='modal' class='" + cancelClass + "'>" + cancel + "</button>" +
										"</div>" +
									"</div>",
						$modal = $(modalHtml);

					$(document).on('click.confirmModal', '.confirm.confirmModal', function() {
						$form.submit();
					});

					$form.on('submit.confirmModal', function() {
						if (! $modal.hasClass("in")) {
                            // don't block if already showing
                            //$('body').append($modal);
                            $modal.modal();
                            return false;
                        }
					});

					$form.data('confirmModal', 'bound');
				}
			}
		});
		return this;
	}

})(jQuery);