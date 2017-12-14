<#import "*/modal_macros.ftl" as modal />
<div id="workflow-edit-warning" class="modal fade">
	<@modal.wrapper>
		<@modal.body>
			<p>You have made changes to this assignments workflow properties. Any existing marker allocations and marker feedback will be deleted.</p>
		</@modal.body>
		<@modal.footer>
				<a class='confirm btn btn-primary'>Confirm</a>
				<a class="btn btn-default" data-dismiss='modal'>Cancel</a>
		</@modal.footer>
	</@modal.wrapper>
</div>

<script type="text/javascript">
	(function ($) { "use strict";

		var $modal, $form;

		$('.workflow-modification').on('change', function() {
			if (!$modal) {
				$modal = $('#workflow-edit-warning');
				$form = $('form#command');

				$form.on('click', 'input[type=submit]', function(e){
					var $button = $(e.target);
					if(!$button.data("confirm")) {
						e.preventDefault();
						$modal.find('.confirm').unbind('click').on('click', function(){
							$button.data('confirm', true).trigger('click');
						});
						$modal.modal();
					}
				});
			}
		});

	})(jQuery);
</script>
