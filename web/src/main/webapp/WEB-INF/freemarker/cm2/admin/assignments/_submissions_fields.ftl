<#escape x as x?html>
	<@bs3form.labelled_form_group path="" labelText="Set submission options">
		<@bs3form.checkbox path="collectSubmissions">
			<@f.checkbox path="collectSubmissions" id="collectSubmissions" /> Collect submissions
		</@bs3form.checkbox>
	<span class="help-block">When checked assignments which are open for submissions will be collected.</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Plagiarism check">
		<@bs3form.checkbox path="automaticallySubmitToTurnitin">
			<@f.checkbox path="automaticallySubmitToTurnitin" id="automaticallySubmitToTurnitin" /> Automatically check submissions for plagiarism
		</@bs3form.checkbox>
	<span class="help-block">
		When checked all submissions will be checked for plagiarism when the assignment closes.
		Any late submissions or submissions within an extension will be submitted when they are	received.
	</span>
		<@bs3form.checkbox path="displayPlagiarismNotice">
			<@f.checkbox path="displayPlagiarismNotice" id="displayPlagiarismNotice" /> Show plagiarism notice
		</@bs3form.checkbox>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Submission scope">
		<@bs3form.radio>
			<@f.radiobutton path="restrictSubmissions" value="true" />
		Only allow students enrolled on this assignment to submit coursework
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="restrictSubmissions" value="false" />
		Allow anyone with a link to the assignment page to submit coursework
		</@bs3form.radio>
	<span class="help-block">If you restrict submissions to students enrolled on the assignment, students
		who go to the page without access will be able to request it.
	</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="">
		<@bs3form.checkbox path="allowResubmission">
			<@f.checkbox path="allowResubmission" id="allowResubmission" />Allow students to re-submit work
		</@bs3form.checkbox>
	<span class="help-block">Students will be able to submit new work, replacing any previous submission.
		Re-submission is never allowed after the close date.
	</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Late submission">
		<@bs3form.checkbox path="allowLateSubmissions">
			<@f.checkbox path="allowLateSubmissions" id="allowLateSubmissions" />Allow new submissions after the
		close date
		</@bs3form.checkbox>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="" labelText="Extensions">
		<@bs3form.checkbox path="allowExtensions">
			<@f.checkbox path="allowExtensions" id="allowExtensions" />Allow extensions
		</@bs3form.checkbox>
	<span class="help-block">You will be able to grant extensions for an assignment to individual students, and
		students will be able to request extensions online.
	</span>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="">
		<@bs3form.checkbox path="extensionAttachmentMandatory">
			<@f.checkbox path="extensionAttachmentMandatory" id="extensionAttachmentMandatory" />Students required to attach at least 1 file to an extension request
		</@bs3form.checkbox>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="">
		<@bs3form.checkbox path="allowExtensionsAfterCloseDate">
			<@f.checkbox path="allowExtensionsAfterCloseDate"  id="allowExtensionsAfterCloseDate" />Allow extensions after close date
		</@bs3form.checkbox>
	<span class="help-block">When selected students will be able to request extensions on late or unsubmitted submission.</span>

	</@bs3form.labelled_form_group>
</#escape>