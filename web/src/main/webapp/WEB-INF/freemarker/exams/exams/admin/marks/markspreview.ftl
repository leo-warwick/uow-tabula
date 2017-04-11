<#escape x as x?html>

<#assign commandName="adminAddMarksCommand" />
<#assign verbed_your_noun="received your files"/>

<@spring.bind path=commandName>
<#assign hasErrors=status.errors.allErrors?size gt 0 />
</@spring.bind>

<#if marker??>
	<#assign formUrl><@routes.exams.markerAddMarks exam marker /></#assign>
<#else>
	<#assign formUrl><@routes.exams.addMarks exam /></#assign>
</#if>

<div class="fix-area">
	<@f.form method="post" action="${formUrl}" commandName=commandName>

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


	<h1>Submit marks for ${exam.name}</h1>
	<#assign verbed_your_noun="received your files"/>

	<@spring.bind path="marks">
	<#assign itemsList=status.actualValue />
	<#assign modifiedCount = 0 />
	<#list itemsList as item>
		<#if item.valid><#assign modifiedCount = modifiedCount + 1 /></#if>
	</#list>
	<p>
		<#if itemsList?size gt 0>
			${text_acknowledge} <@fmt.p modifiedCount "student"/>.
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
			<table class="table table-hover">
				<tr>
					<th>University ID</th>
					<th>Marks</th>
					<th>Grade</th>
				</tr>
				<#list itemList as item>
					<@spring.nestedPath path="marks[${item_index}]">
						<#if !item.valid>
							<#assign errorClass="danger" />
						<#elseif item.modified>
							<#assign errorClass="warning" />
						<#else>
							<#assign errorClass="success" />
						</#if>

						<tr class="${errorClass}">
							<@f.hidden path="universityId" />
							<@f.hidden path="actualMark" />
							<@f.hidden path="actualGrade" />
							<@f.hidden path="valid" />
							<td>
								<@spring.bind path="universityId">
									${status.value}
								</@spring.bind>
								<@f.errors path="universityId" cssClass="error" />

								<#if item.modified>
									<br />
									Mark for this student already uploaded - previous mark will be overwritten when you click Confirm.
								</#if>
								<#if item.published>
									<br />
									Feedback for this student has already been published. They will be notified that their mark has changed.
								</#if>
								<#if item.hasAdjustment>
									<br />
									This student's mark has already been adjusted. The adjusted mark may need to be amended.
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
		<#if marker??>
			<a class="btn btn-default" href="<@routes.exams.examsHome />">Cancel</a>
		<#else>
			<a class="btn btn-default" href="<@routes.exams.moduleHomeWithYear module=exam.module academicYear=exam.academicYear/>">Cancel</a>
		</#if>

	</div>
	</@f.form>
</div>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>