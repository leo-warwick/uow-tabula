<#escape x as x?html>

<h1>Check Upload to SITS status</h1>

<#if errors?has_content>
	<div class="alert alert-danger">
		<@spring.hasBindErrors name="command">
			<#list errors.globalErrors as e>
				<div><@spring.message message=e /></div>
			</#list>
		</@spring.hasBindErrors>
	</div>
<#else>

	<#assign context><#if assignment?has_content!false>assignment<#else>exam</#if></#assign>

	<div class="status hidden <#if !result.hasAssessmentGroups>error</#if>">
		<div class="alert alert-info">
			Checking for SITS links&hellip;
			<div class="error-message hidden">
				<p>This ${context} has no SITS links so this feedback cannot be uploaded to SITS.</p>
				<p>Check whether a SITS link has been accidentally removed or if the student has been moved to an alternate assessment group.</p>
			</div>
		</div>
	</div>

	<div class="status hidden <#if !result.hasAssignmentRow>error</#if>">
		<div class="alert alert-info">
			Checking for matching row in SITS&hellip;
			<div class="error-message hidden">
				<p>Could not find a row to add the marks.</p>
				<p>There should be a row in the table CAM_SAS with the following details, based on the current SITS links of this ${context}:</p>
				<ul>
					<li>SPR_CODE: <#list sprCodes as sprCode>${sprCode}<#if sprCode_has_next> or </#if></#list></li>
					<li>MOD_CODE that starts with: ${module.code?upper_case}</li>
					<li>AYR_CODE: <#if assignment?has_content>${assignment.academicYear.toString}<#else>${exam.academicYear.toString}</#if></li>
					<li>PSL_CODE: Y</li>
					<li>MAV_OCCUR: <#list assessmentGroupPairs as pair>${pair._1()}<#if pair_has_next> or </#if></#list></li>
					<li>MAB_SEQ: <#list assessmentGroupPairs as pair>${pair._2()}<#if pair_has_next> or </#if></#list></li>
				</ul>
			</div>
		</div>
	</div>

	<div class="status hidden <#if !result.hasWritableMark>error</#if>">
		<div class="alert alert-info">
			Checking for writable mark&hellip;
			<div class="error-message hidden">
				<p>Could not find a row with either an empty actual mark (CAM_SAS.SAS_ACTM), or with a previous mark uploaded by Tabula.</p>
				<p>Tabula will not overwrite a mark that has been uploaded outside of Tabula.</p>
			</div>
		</div>
	</div>

	<div class="status hidden <#if !result.hasWritableGrade>error</#if>">
		<div class="alert alert-info">
			Checking for writable grade&hellip;
			<div class="error-message hidden">
				<p>Could not find a row with either an empty actual grade (CAM_SAS.SAS_ACTG), or with a previous grade uploaded by Tabula.</p>
				<p>Tabula will not overwrite a grade that has been uploaded outside of Tabula.</p>
			</div>
		</div>
	</div>

	<div class="final-status hidden">
		<p>We couldn't find a reason why this mark could not be uploaded.
			Try uploading to SITS again and if the problem persists contact the Web Team using the "Problems, questions?" button above.</p>
	</div>

	<script>
		jQuery(function($){
			var showNextStatus = function(){
				var $status = $('div.status.hidden:first').removeClass('hidden');
				setTimeout(function(){
					if ($status.hasClass('error')) {
						$status.find('.alert').removeClass('alert-info').addClass('alert-danger').prepend(' ').prepend($('<i/>').addClass('icon-remove'));
						$status.find('.error-message').removeClass('hidden');
					} else {
						$status.find('.alert').removeClass('alert-info').addClass('alert-success').prepend(' ').prepend($('<i/>').addClass('icon-ok'));
						var $nextStatus = $('div.status.hidden:first');
						if ($nextStatus.length > 0) {
							setTimeout(showNextStatus, 500);
						} else {
							$('div.final-status').removeClass('hidden');
						}
					}
				}, 1000);
			};

			showNextStatus();
		});
	</script>
</#if>

</#escape>