<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign commandName="adminAddMarksCommand" />
<#assign verbed_your_noun="received your files"/>

<@spring.bind path=commandName>
<#assign hasErrors=status.errors.allErrors?size gt 0 />
</@spring.bind>

<#if features.disabilityOnSubmission>
	<#assign disabilityMap = adminAddMarksCommand.fetchDisabilities />
</#if>

<div class="fix-area">
	<@f.form method="post" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/marks')}" commandName=commandName>

	<#assign isfile=RequestParameters.isfile/>

	<#if isfile = "true">
		<#assign text_acknowledge="I've ${verbed_your_noun} and I found marks for"/>
		<#assign text_problems="However, there were some problems with its contents, which are shown below.
				You'll need to correct these problems with the spreadsheet and try again.
				If you choose to confirm without fixing the spreadsheet any rows with errors
				will be ignored."/>
		<#assign column_headings_warning="Remember that the first row in all spreadsheets is assumed to be column headings and ignored."/>
	<#else>
		<#assign text_acknowledge="You are submitting marks for "/>
		<#assign text_problems="However, there were some problems, which are shown below.
				You'll need to return to the previous page, correct these problems and try again.
				If you choose to confirm without fixing the data any rows with errors
				will be ignored."/>
		<#assign column_headings_warning=""/>

	</#if>


	<h1>Submit marks for ${assignment.name}</h1>
	<#assign verbed_your_noun="received your files"/>

	<@spring.bind path="marks">
	<#assign itemsList=status.actualValue />
	<#assign modifiedCount = 0 />
	<#list itemsList as item>
		<#if item.valid><#assign modifiedCount = modifiedCount + 1 /></#if>
	</#list>
	<p>
		<#if itemsList?size gt 0>
			${text_acknowledge} ${modifiedCount} students.
			<#if hasErrors>
				${text_problems}
			</#if>
		<#else>
			I've ${verbed_your_noun} but I couldn't find any rows that looked like marks. ${column_headings_warning}
		</#if>
	</p>
	</@spring.bind>

	<@spring.bind path="marks">
		<#assign itemList=status.actualValue />
		<#if itemList?size gt 0>
			<table class="marksUploadTable">
				<tr>
					<th>University ID</th>
					<th>Marks</th>
					<th>Grade</th>
				</tr>
				<#list itemList as item>
					<@spring.nestedPath path="marks[${item_index}]">
						<#if !item.valid>
							<#assign errorClass="alert-error" />
						<#elseif item.modified>
							<#assign errorClass="alert" />
						<#else>
							<#assign errorClass="alert-success" />
						</#if>

						<tr class="${errorClass}">
							<@f.hidden path="universityId" />
							<@f.hidden path="user" />
							<@f.hidden path="actualMark" />
							<@f.hidden path="actualGrade" />
							<@f.hidden path="valid" />
							<td>
								<@spring.bind path="universityId">
									${status.value}
								</@spring.bind>
								<@f.errors path="universityId" cssClass="error" />

								<#if item.modified>
									<div class="warning">
										Mark for this student already uploaded - previous mark will be overwritten when you click Confirm.
									</div>
								</#if>
								<#if item.published>
									<div class="warning">
										Feedback for this student has already been published. They will be notified that their mark has changed.
									</div>
								</#if>
								<#if item.hasAdjustment>
									<span class="warning">This student's mark has already been adjusted. The adjusted mark may need to be amended.</span>
								</#if>

								<#if features.disabilityOnSubmission && disabilityMap[item.universityId]??>
									<#assign disability = disabilityMap[item.universityId] />
									<a class="use-popover cue-popover" id="popover-disability" data-html="true"
									   data-original-title="Disability disclosed"
									   data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${disability.code}</h6><small>${(disability.sitsDefinition)!}</small></div>"
											>
										<span class="label label-info">Disability disclosed</span>
									</a>
								</#if>

							</td>
							<td>
								<@spring.bind path="actualMark">
									${status.value}
								</@spring.bind>
								<@f.errors path="actualMark" cssClass="error" />
							</td>
							<td>
								<@spring.bind path="actualGrade">
									${status.value}
								</@spring.bind>
								<@f.errors path="actualGrade" cssClass="error" />
							</td>
						</tr>
					</@spring.nestedPath>
				</#list>
			</table>
		</#if>
	</@spring.bind>

	<div class="submit-buttons fix-footer">
		<input type="hidden" name="confirm" value="true">
		<input class="btn btn-primary" type="submit" value="Confirm">
		<a class="btn" href="<@routes.coursework.depthome module=assignment.module />">Cancel</a>
	</div>
	</@f.form>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>