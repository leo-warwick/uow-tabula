<#assign help>
	The number of words in the attachment you are submitting.
	${field.conventions!""}
	<#if ((field.min)!0) gt 0 && ((field.max)!1000000) lt 1000000>
		It must be between ${(field.min)!0} and ${(field.max)!1000000} words.
	<#elseif ((field.min)!0) == 0>
		It must be <@fmt.p (field.max)!1000000 "word" /> or less.
	<#else>
		It must be <@fmt.p (field.min)!0 "word" /> or more.
	</#if>
</#assign>

<@form.labelled_row "fields[${field.id}].value" "Word count" "" help>
	<@f.input id="fields[${field.id}].value" path="fields[${field.id}].value" cssClass="input-small" maxlength="${1000000?c?length}" />
</@form.labelled_row>