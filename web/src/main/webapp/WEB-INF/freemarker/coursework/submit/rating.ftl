<#escape x as x?html>
<div id="feedback-rating">

<#assign command=rateFeedbackCommand/>
<#assign topstars=command.maximumStars/><#--

Interface for students to leave a rating about the feedback they've received.

-->
<h3>Rate your feedback</h3>

<p>
Rating feedback is anonymous and will help us to give you better feedback
in future. Please try to rate solely on the quality of the feedback.
</p>


<#if rating??>
<#-- Has a rating -->
<!-- Current rating ${rating} -->
<#else>
<#-- No rating set yet -->
<!-- No current rating -->
</#if>

<#macro yesnocheckbox path value text current>
	<#local checked=(current.value?? && current.value?string=value) />
	<label><input type="radio" name="${path}.value" value="${value}" <#if checked>checked</#if>>${text}</label>
</#macro>

<#macro yesnonone path>
	<@f.errors path=path cssClass="error"/>
	<div class="radio-button-group">
	<@spring.bind path=path>
		<#local current=status.actualValue>
		<@yesnocheckbox path "true" "Agree" current />
		<@yesnocheckbox path "false" "Do not agree" current />
		<#--
		<#if current.value??>
		<label><@f.checkbox path="${path}.unset" /> Remove rating</label>
		</#if>
		-->
	</@spring.bind>
	</div>
</#macro>

<form id="feedback-rating-form" method="POST" action="<@routes.coursework.ratefeedback command.feedback />">

	<@spring.nestedPath path="rateFeedbackCommand">

	<p><strong>Feedback on my work&hellip;</strong></p>

	<div class="rating-question">
    <div>&hellip;was returned in the agreed timeframe</div>
    <@yesnonone "wasPrompt" />
    </div>

    <div class="rating-question">
    <div>&hellip;has been helpful</div>
    <@yesnonone "wasHelpful" />
    </div>
<#--
	<noscript>1</noscript>
	<#list 1..topstars as stars>
	    <input type="radio" name="rating" value="${stars}" <#if rating?? && rating = stars>checked</#if> />
	</#list>

	<noscript>${topstars}
	<div><input type="checkbox" name="unset"> withdraw your rating</div>
	<input type="submit" value="Rate" />
	</noscript>
-->

<input type="submit" class="btn btn-primary" value="Rate" data-loading-text="Saving..." />

	</@spring.nestedPath>

</form>
<div class="end-floats"></div>
<#if rated??>
<p class="subtle">Thanks for rating.</p>
</#if>

</div>
</#escape>