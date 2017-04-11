<#compress>
<#escape x as x?html>

<@f.form action="/app/tell-us" method="post" commandName="appCommentCommand" id="app-comment-form">

	<p>
		Do you have a comment, complaint or suggestion related to this application? Let us know here.
		<#if (appCommentCommand.componentName!"") == 'courses'>
			Note that if you have a question about your course material or want to talk about some feedback/marks
			you received, you should talk to the person setting your coursework.
		</#if>
		If you are looking for instructions on how to use Tabula, you might like to look at <a class="btn btn-primary" href="http://warwick.ac.uk/tabula/manual/" target="_blank">the Tabula manual</a>
	</p>

	<p>
		We'll include some information below about you and your computer in order to help diagnose any problems you might be reporting.
	</p>

	<#-- DRY -->
	<#macro comment_input path title>
		<@bs3form.labelled_form_group path=path labelText=title>
			<@f.input cssClass="text form-control" path=path id="app-comment-${path}" />
		</@bs3form.labelled_form_group>
	</#macro>

	<h4>About you</h4>
	<@comment_input "name" "Your name" />
	<@comment_input "email" "Your email" />
	<@comment_input "usercode" "Usercode" />

	<@f.hidden path="currentPage" id="app-comment-currentPage" />
	<@f.hidden path="browser" id="app-comment-browser" />
	<@f.hidden path="os" id="app-comment-os" />
	<@f.hidden path="resolution" id="app-comment-resolution" />
	<@f.hidden path="ipAddress" id="app-comment-ipAddress" />

	<h4>Your message</h4>
	<@f.errors path="message" cssClass="error" />
	<div class="form-group">
		<@f.textarea path="message" cssClass="form-control" />
	</div>

	<#--
	<div>
	<#if user.loggedIn>
	<@f.checkbox path="pleaseRespond" id="app-comment-response" />
	<@f.label for="app-comment-response">I would like a response, please.</@f.label>
	<#else>
	<div class="disabled-zone">
	<input type="checkbox" disabled> If you would like a response, you'll need to sign in first.
	</div>
	</#if>
	</div>
	-->

	<div class="form-group"><input class="btn btn-primary" type="submit" value="Send"></div>
</@f.form>

<script>

jQuery('#app-comment-form').submit(function(event){

});

</script>

</#escape>
</#compress>
