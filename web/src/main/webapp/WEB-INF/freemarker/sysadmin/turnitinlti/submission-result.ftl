<h1>Turnitin LTI - submission details</h1>

<#if response.success>
	<ul>
		<li>similarity: ${submission_info.similarity}</li>
		<li>student overlap: ${submission_info.student_overlap}</li>
		<li>web overlap: ${submission_info.web_overlap}</li>
		<li>publications overlap: ${submission_info.publication_overlap}</li>
	</ul>
<#else>
	Something went wrong
</#if>