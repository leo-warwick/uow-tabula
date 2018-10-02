<#escape x as x?html>

<#if hasPermission>
<h1>Download my info</h1>

<p>
	You can download a zip file and pdf report(s) of your Tabula data for the academic year <strong>${academicYear.toString}</strong>, including:
</p>

<ul>
	<li>Profile</li>
	<li>Administrative notes</li>
	<li>Extenuating circumstances</li>
	<li>Meeting records</li>
	<li>Coursework submissions and files</li>
	<li>Marks, online feedback and feedback files</li>
	<li>Small group teaching attendance</li>
	<li>Monitoring point attendance</li>
</ul>

<form method="post" action="<@routes.profiles.download studentCourseDetails academicYear/>">
	<p>
		<button type="submit" class="btn btn-primary">Download report</button>
	</p>
</form>
<#else>
<div class="alert alert-danger">
	You don't have permission to download a profile report for this student.
</div>
</#if>

</#escape>