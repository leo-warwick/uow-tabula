<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<@modal.wrapper>

	<#if existingAgent??>
		<#assign pageAction = "edit">
	<#else>
		<#assign pageAction = "add">
	</#if>

	<@modal.header>
		<h3 class="modal-title">${pageAction?capitalize} ${relationshipType.agentRole?cap_first}</h3>
	</@modal.header>


	<#assign student = studentCourseDetails.student />

	<@modal.body>
		<@f.form method="post" commandName="command" action="" cssClass="dirty-check-ignore">

			<h5 id="studentName">${relationshipType.studentRole?capitalize}: ${student.fullName}</h5>

			<input type="hidden" name="removeAgent" value="false" />

			<@bs3form.labelled_form_group path="" labelText="${relationshipType.agentRole?cap_first}">
				<div class="row">
					<div class="col-sm-<#if pageAction != "add">10<#else>12</#if>">
						<div class="input-group profile-search-results">
							<#if existingAgent??>
								<input type="text" name="query" value="${existingAgent.fullName}" id="query" class="form-control" />
								<input type="hidden" name="newAgent" value="${existingAgent.universityId}" />
								<input type="hidden" name="oldAgent" value="${existingAgent.universityId}" />
							<#else>
								<input type="text" name="query" value="" id="query" class="form-control" />
								<input type="hidden" name="newAgent" />
							</#if>
							<span class="input-group-btn">
								<button class="inline-search-button btn btn-default" type="button"><i class="fa fa-search"></i></button>
							</span>
						</div>
					</div>
					<#if pageAction != "add">
						<div class="col-sm-2">
							<button id="remove-agent" class="btn btn-danger" type="button">Remove</button>
						</div>
					</#if>
				</div>
			</@bs3form.labelled_form_group>

			<#if pageAction != "add">
				<div id="removeAgentMessage" style="display: none" class="alert alert-info clearfix">
					<p>Are you sure you want to remove <strong>${existingAgent.fullName}</strong> as ${student.firstName}'s ${relationshipType.agentRole}?</p>
					<div class="pull-right">
						<button id="confirm-remove-agent" class="btn btn-primary" type="button">Confirm</button>
						<button id="cancel-remove-agent" class="btn" data-dismiss="modal" type="button">Cancel</button>
					</div>
				</div>
			</#if>

			<div id="extra-options">
				<#if pageAction != "add">
					<div id="notify-remove-agent" class="alert alert-info hide"><strong>${existingAgent.fullName}</strong> will no longer be ${student.firstName}'s ${relationshipType.agentRole}.</div>
				</#if>

				<div class="scheduledDate">
					<@bs3form.labelled_form_group path="" labelText="Make this change">
						<@bs3form.radio>
							<@f.radiobutton path="specificScheduledDate" value="false" /> Immediately
						</@bs3form.radio>
						<@bs3form.radio>
							<@f.radiobutton path="specificScheduledDate" value="true" /> On date
							<span class="additional"><@f.input path="scheduledDate" cssClass="date-time-picker form-control" autocomplete="off" /></span>
						</@bs3form.radio>
					</@bs3form.labelled_form_group>
				</div>

				<@bs3form.labelled_form_group path="" labelText="Notify these people via email of this change">
					<p>Notifications will be sent immediately, regardless of when the change is scheduled.</p>
					<@bs3form.checkbox>
						<input type="checkbox" name="notifyStudent" checked />
						${relationshipType.studentRole?cap_first}
					</@bs3form.checkbox>
					<@bs3form.checkbox>
						<input type="checkbox" name="notifyOldAgent" <#if pageAction != "add">checked <#else> disabled </#if> />
						Old ${relationshipType.agentRole}
					</@bs3form.checkbox>
					<@bs3form.checkbox>
						<input type="checkbox" name="notifyNewAgent" checked />
						New ${relationshipType.agentRole}
					</@bs3form.checkbox>
				</@bs3form.labelled_form_group>
			</div>

			<@bs3form.errors path="*" />
		</@f.form>
	</@modal.body>

	<@modal.footer>
		<div class="pull-right">
			<button id="save-agent" class="btn btn-default disabled" type="button">Save</button>
			<button id="cancel-save-agent" class="btn btn-default" data-dismiss="modal" type="button">Cancel</button>
		</div>
	</@modal.footer>

	<script type="text/javascript">
		jQuery(function($) {
			$("input:radio[name='specificScheduledDate']").radioControlled({
				'selector' : '.additional',
				'parentSelector' : '.scheduledDate'
			});
			$('input.date-time-picker').tabulaDateTimePicker();

			var $form = $('#command')
				, $modal = $('#change-agent')
				, $modalBody = $modal.find('.modal-body')
				, $saveButton = $('#save-agent');

			$modalBody.on('submit', 'input', function(e){
				e.preventDefault();
			});

			$saveButton.click(function() {
				if ($(this).hasClass("disabled")) return;
				$form.submit();
			});

			$('#remove-agent').click(function() {
				if ($(this).hasClass("disabled")) return;
				if ($('#extra-options').is(':visible')) $('#extra-options').hide();
				$('#removeAgentMessage').show();
			});

			$('#confirm-remove-agent').click(function() {
				$('[name=notifyNewAgent]').prop("disabled", "disabled")
					.prop("checked", false)
					.closest("label").addClass("muted");
				$('#removeAgentMessage').hide();
				$('#extra-options').show();
				$("#save-agent").removeClass("disabled").addClass("btn-primary");
				$form.find('input[name="query"]').prop('disabled', true);
				$('#remove-agent').hide();
				$('.inline-search-button').addClass('disabled');
				$('#notify-remove-agent').removeClass('hide');
				$form.find("input[name='removeAgent']").val('true');
			});

			var profileSearch = function(searchContainer, target, highlighterFunction, updaterFunction) {
				var container = $(searchContainer);
				var xhr = null;
				container.find('input[name="query"]').prop('autocomplete','off').each(function() {
					var $spinner = $('<div class="spinner-container" />');
					$(this).before($spinner).bootstrap3Typeahead({
						source: function(query, process) {
							if (xhr != null) {
								xhr.abort();
								xhr = null;
							}

							query = $.trim(query);
							if (query.length < 3) { process([]); return; }

							// At least one of the search terms must have more than 1 character
							var terms = query.split(/\s+/g);
							if ($.grep(terms, function(term) { return term.length > 1; }).length == 0) {
								process([]); return;
							}

							$spinner.spin('small');
							xhr = $.get(target, { query : query }, function(data) {
								$spinner.spin(false);

								var members = [];
								$.each(data, function(i, member) {
									var item = member.name + '|' + member.id + '|' + member.userId + '|' + member.description;
									members.push(item);
								});

								process(members);
							}).error(function(jqXHR, textStatus, errorThrown) { if (textStatus != "abort") $spinner.spin(false); });
						},

						matcher: function(item) { return true; },
						sorter: function(items) { return items; }, // use 'as-returned' sort
						highlighter: highlighterFunction,

						updater: updaterFunction,
						minLength:3
					});
				});
			};

			profileSearch($modalBody.find('.input-group'), "<@routes.profiles.relationship_search_json />", function(item) {
				var member = item.split("|");
				return '<h3 class="name">' + member[0] + '</h3><span class="description">' + member[3] + '</span>';
			}, function(memberString) {
				var member = memberString.split("|");

				if($form.find('input[name=oldAgent]').val() != member[1]) {
					$('#remove-agent').addClass("disabled");
					$('#extra-options').show();
				} else {
					$('#remove-agent').removeClass("disabled");
				}

				$form.find('input[name=newAgent]').val(member[1]);
				$saveButton.removeClass("disabled").addClass("btn-primary");
				return member[0];
			});

			$('#extra-options').hide();
		});
	</script>

</@modal.wrapper>

</#escape>
