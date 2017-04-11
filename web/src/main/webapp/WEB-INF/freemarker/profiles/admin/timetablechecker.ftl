<h3>Timetable Checker</h3>

<@f.form method="post" enctype="multipart/form-data" commandName="command">

<@bs3form.labelled_form_group path="warwickUniId" labelText="Enter student university id to check timetable feeds">
	<@f.input type="text" path="warwickUniId" cssClass="form-control" maxlength="255" placeholder="University id" />
</@bs3form.labelled_form_group>

<button title="submit" class="btn btn-primary spinnable spinner-auto" type="submit" name="submit">
	Submit
</button>

</@f.form>