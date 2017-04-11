<#--

This section contains the form fields that can apply to a group of
assignments, as well as to an individual one.

If you add a field it should also be added to _common_fields_hidden.ftl
so that they can be passed around between requests.

-->

<#if features.submissions>
	<fieldset id="submission-options">
		<details <#if ((assignment.collectSubmissions)?? && assignment.collectSubmissions) || collectSubmissions?? && collectSubmissions > open </#if> class="submissions">
			<summary class="collapsible large-chevron">
				<span class="legend collapsible" >
					Submissions <small>Set submission options for this assignment</small>
				</span>
				<@form.row>
					<@form.field>
						<@form.label checkbox=true>
							<@f.checkbox path="collectSubmissions" id="collectSubmissions"/>
							Collect submissions
						</@form.label>
					</@form.field>
				</@form.row>
			</summary>
			<div>
				<@form.row>
					<@form.label></@form.label>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="displayPlagiarismNotice" />
							Show plagiarism notice
						</label>
					</@form.field>
				</@form.row>

				<#assign automaticallySubmitToTurnitinHelp>
					Automatically submit all submissions to Turnitin when the assignment closes.
					Any late submissions or submissions within an extension will be submitted when they are received.
				</#assign>
				<@form.row>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="automaticallySubmitToTurnitin" id="automaticallySubmitToTurnitin" />
							Automatically submit to Turnitin
							<@fmt.help_popover id="automaticallySubmitToTurnitinHelp" content="${automaticallySubmitToTurnitinHelp}" html=true />
						</label>
					</@form.field>
				</@form.row>

				<#if features.assignmentMembership>
					<@form.row>
						<@form.label></@form.label>
						<@form.field>
							<label class="radio">
								<@f.radiobutton path="restrictSubmissions" value="true" />
								Only allow students enrolled on this assignment to submit coursework
							</label>
							<label class="radio">
								<@f.radiobutton path="restrictSubmissions" value="false" />
								Allow anyone with a link to the assignment page to submit coursework
							</label>
							<div class="help-block">
								If you restrict submissions to students enrolled on the assignment,
								students who go to the page without access will be able to request it.
							</div>
						</@form.field>
					</@form.row>
				</#if>

				<@form.row cssClass="has-close-date">
					<@form.label></@form.label>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="allowLateSubmissions" />
							Allow new submissions after the close date
						</label>
					</@form.field>
				</@form.row>
				<@form.row>
					<@form.label></@form.label>
					<@form.field>
						<label class="checkbox">
							<@f.checkbox path="allowResubmission" />
							Allow students to re-submit work
						</label>
						<div class="help-block">
							Students will be able to submit new work, replacing any previous submission.
							Re-submission is <em>never</em> allowed after the close date.
						</div>
					</@form.field>
				</@form.row>

				<#if features.extensions>
					<div class="has-close-date">
						<@form.row>
							<@form.label></@form.label>
							<@form.field>
								<label class="checkbox">
									<@f.checkbox path="allowExtensions" id="allowExtensions" selector=".allows-extensions" />
									Allow extensions
								</label>
								<div class="help-block">
									<#if department.allowExtensionRequests>
										You will be able to grant extensions for an assignment to individual students,
										and students will be able to request extensions online.
									<#else>
										You will be able to grant extensions for an assignment to individual students.
										Students cannot currently request extensions online for assignments in ${department.name}.
									</#if>
									<#if can.do("Department.ManageExtensionSettings", department)>
										<a class="btn btn-mini use-tooltip" title="Department extension request settings (opens in a new window/tab)" href="<@routes.coursework.extensionsettings department />" target="_blank">Review</a>
									<#else>
										Departmental administrators control whether extension requests are allowed across a department.
									</#if>
								</div>
							</@form.field>
						</@form.row>

						<#if (assignment.countUnapprovedExtensions gt 0)!false>
							<script>
								jQuery(function($){
									$('#allowExtensions').change(function() {
										if ($(this).is(':checked')) {
											$('form.edit-assignment').confirmModal(false);
										} else {
											$('form.edit-assignment').confirmModal({
												message: '<@fmt.p assignment.countUnapprovedExtensions "extension request is" "extension requests are" /> awaiting review for this assignment. If you turn off extensions, all extension requests awaiting review will be rejected. Any extensions already granted will remain in place.',
												confirm: 'Continue, reject extension requests awaiting review'
											});
										}
									});
								});
							</script>
						</#if>

						<@form.row cssClass="allows-extensions">
							<@form.label></@form.label>
							<@form.field>
								<label class="checkbox">
									<@f.checkbox path="extensionAttachmentMandatory" id="extensionAttachmentMandatory" />
									Students required to attach at least 1 file to an extension request
								</label>
							</@form.field>
						</@form.row>

						<@form.row cssClass="allows-extensions">
							<@form.label></@form.label>
							<@form.field>
								<label class="checkbox">
									<@f.checkbox path="allowExtensionsAfterCloseDate" id="allowExtensionsAfterCloseDate" />
									Allow students to request extensions after the close date
								</label>
							</@form.field>
						</@form.row>
					</div>
				</#if>

				<@form.row>
					<@form.label path="fileAttachmentLimit">Max attachments per submission</@form.label>
					<@form.field>
						<@spring.bind path="maxFileAttachments">
							<#assign maxFileAttachments=status.actualValue />
						</@spring.bind>
						<@f.select path="fileAttachmentLimit" cssClass="span1">
							<@f.options items=1..maxFileAttachments />
						</@f.select>
					</@form.field>
				</@form.row>

				<@form.row>
					<@form.label path="fileAttachmentTypes">Accepted attachment file types</@form.label>
					<@form.field>
						<@f.errors path="fileAttachmentTypes" cssClass="error" />
						<@f.input path="fileAttachmentTypes"  type="hidden" />
						<script type="text/javascript" src="/static/js/textList.js"></script>
						<script type="text/javascript">
							jQuery(function($){
								var textListController = new TextListController('#fileExtensionList', '#fileAttachmentTypes');
								textListController.transformInput = function(text){
									var result = text.replace(new RegExp('\\.', 'g') , '');
									return result.toLowerCase();
								};
								textListController.preventDuplicates = true;
								textListController.init();
							});
						</script>
						<div id="fileExtensionList" class="textBoxListContainer">
							<ul>
								<li class="inputContainer"><input class="text" type="text"></li>
							</ul>
						</div>
						<div class="help-block">
							Enter the file types you would like to allow separated by spaces (e.g. "pdf doc docx"). Press the spacebar individually after each file type. Only attachments with the extensions specified will be permitted. Leave this field blank to accept attachments with any extension.
						</div>
					</@form.field>
				</@form.row>

				<@form.row>
					<@form.label path="individualFileSizeLimit">Max file size per file</@form.label>
					<@form.field>
						<@f.errors path="individualFileSizeLimit" cssClass="error" />
						<@f.input path="individualFileSizeLimit" cssClass="input-small" />
						<div class="help-block">
							Enter the maximum file size (in Megabytes) for a single uploaded file. If you wish to submit the file(s) to Turnitin each file can be no larger than ${turnitinFileSizeLimit}MB
						</div>
					</@form.field>
				</@form.row>

				<@form.row path="comment">
				  <@form.label for="assignmentComment">Text to show on submission form</@form.label>
					<@form.field>
						<@f.errors path="comment" cssClass="error" />
						<@f.textarea path="comment" id="assignmentComment" rows="6" cssClass="span6" />
						<div class="help-block">
							You can make a new paragraph by leaving a blank line (i.e. press Enter twice).
						</div>
					</@form.field>
				</@form.row>

				<@form.row>
					<@form.label path="wordCountMin">Minimum word count</@form.label>
					<@form.field>
						<@f.errors path="wordCountMin" cssClass="error" />
						<@f.input path="wordCountMin" cssClass="input-small" maxlength="${maxWordCount?c?length}" />
					</@form.field>
				</@form.row>

				<@form.row>
					<@form.label path="wordCountMax">Maximum word count</@form.label>
					<@form.field>
						<@f.errors path="wordCountMax" cssClass="error" />
						<@f.input path="wordCountMax" cssClass="input-small" maxlength="${maxWordCount?c?length}" />
						<div class="help-block">
							If you specify a minimum and/or maximum word count, students will be required to declare the word count for
							their submissions. They will not be able to submit unless their declaration is within your specified range.
							If you don't specify a minimum and/or maximum, students will not be asked to declare a word count. Note that Tabula
							does not actually check the number of words in submissions. Students can submit work with any number of words.
						</div>
					</@form.field>
				</@form.row>

				<@form.row>
				  <@form.label for="wordCountConventions">Word count conventions</@form.label>
					<@form.field>
						<@f.errors path="wordCountConventions" cssClass="error" />
						<@f.textarea path="wordCountConventions" id="wordCountConventions" rows="3" cssClass="span6" />
						<div class="help-block">
							<div class="enabled">
								Tell students if there are specific things which should be included or excluded from the word count.
							</div>
							<div class="disabled">
								This will only be displayed if a minimum and/or maximum word count is entered.
							</div>
						</div>
					</@form.field>
				</@form.row>

			</div>
		</details>
	</fieldset>

<script>
jQuery(function($) {

	var updateSubmissionsDetails = function() {
		var collectSubmissions = $('input[name=collectSubmissions]').is(':checked');
		if (collectSubmissions) {
			$('details.submissions').details('open');
		}  else {
			$('details.submissions').details('close');
		}
	};

	<#-- TAB-830 expand submission details when collectSubmissions checkbox checked -->
	$('input[name=collectSubmissions]').on('change click keypress',function(e) {
		e.stopPropagation();
		updateSubmissionsDetails();
	});

	updateSubmissionsDetails();

	var updateWordCountConventions = function() {
		if ($('input[name=wordCountMin]').val().length === 0 && $('input[name=wordCountMax]').val().length === 0) {
			$('textarea[name=wordCountConventions]').prop('disabled', true).closest('div')
				.find('.help-block .disabled').show()
				.end().find('.help-block .enabled').hide();
		} else {
			$('textarea[name=wordCountConventions]').prop('disabled', false).closest('div')
				.find('.help-block .enabled').show()
				.end().find('.help-block .disabled').hide();
		}
	};

	$('input[name=wordCountMin], input[name=wordCountMax]').on('change',function(e) {
		e.stopPropagation();
		updateWordCountConventions();
	});

	updateWordCountConventions();

	<#-- if summary supported, disable defaults for this summary when a contained label element is clicked -->
	$("summary").on("click", "label", function(e){
		e.stopPropagation();
		e.preventDefault();
		$('#collectSubmissions').attr('checked', !$('#collectSubmissions').attr('checked') );
		$('input[name=collectSubmissions]').triggerHandler("change");
	});


	$("input[name='allowExtensions']").radioControlled({mode: 'hidden'});

});
</script>

</#if>