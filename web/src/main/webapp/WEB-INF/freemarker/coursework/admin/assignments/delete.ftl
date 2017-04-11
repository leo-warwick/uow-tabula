<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<h1>Delete assignment</h1>

<@f.form method="post" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/delete')}" commandName="deleteAssignmentCommand">

<h2>${assignment.name} (${assignment.academicYear.label})</h2>

<!-- global errors -->
<@f.errors cssClass="error" />

<p>
You can delete an assignment if it's been created in error.
</p>

<@f.errors path="confirm" cssClass="error" />
<@form.label checkbox=true>
<@f.checkbox path="confirm" id="confirmCheck" />
<strong> I definitely will not need this assignment again and wish to delete it entirely.</strong>
</@form.label>

<div class="submit-buttons">
<input type="submit" value="Delete" class="btn btn-danger">
<a href="<@routes.coursework.assignmentedit assignment=assignment />" class="btn">Cancel</a>
</div>
</@f.form>

<script>
jQuery(function($){
	$('#confirmCheck').change(function(){
		$('.submit-buttons input[type=submit]').attr('disabled', !this.checked).toggleClass('disabled', !this.checked);
	});
	$('.submit-buttons input[type=submit]').attr('disabled',true).addClass('disabled');
})
</script>

</#escape>