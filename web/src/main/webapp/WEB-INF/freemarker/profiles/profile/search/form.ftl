<#if searchProfilesCommand?has_content>
	<section class="profile-search profile-search-results">
		<@f.form method="get" action="${url('/profiles/search')}" commandName="searchProfilesCommand">
			<div class="input-group">
				<@f.input path="query" placeholder="Search for a student..." cssClass="form-control" />
				<span
					class="input-group-btn use-tooltip"
					data-placement="right"
					data-container="body"
					data-trigger="manual"
					data-title="Start typing a student's name, or put their University ID in, and we'll show you a list of results. Any student who studies in your department should be included."
				>
					<button class="btn btn-default" type="submit"><i class="fa fa-search"></i></button>
				</span>
			</div>
			<@bs3form.form_group>
				<@bs3form.radio>
					<input type="radio" name="searchAllDepts" checked value="false">
					Departmental students & staff
				</@bs3form.radio>
				<@bs3form.radio>
					<input type="radio" name="searchAllDepts" value="true">
					All students & staff
				</@bs3form.radio>
			</@bs3form.form_group>
		</@f.form>
	</section>
</#if>
