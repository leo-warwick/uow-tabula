<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#macro webTeamForm>
	<@bs3form.labelled_form_group path="name" labelText="Your name">
		<@f.input path="name" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="email" labelText="Your email">
		<@f.input path="email" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="usercode" labelText="Usercode">
		<@f.input path="usercode" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="url" labelText="URL (where did you encounter a problem?)">
		<@f.input path="url" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="message" labelText="Your message">
		<@f.textarea path="message" cssClass="form-control"/>
	</@bs3form.labelled_form_group>

	<input type="hidden" name="browser" value="" />
	<input type="hidden" name="os" value="" />
	<input type="hidden" name="resolution" value="" />
	<input type="hidden" name="ipAddress" value="" />
</#macro>

<h1>Problems, questions?</h1>

<p>If you are in need of help using Tabula there are three ways to find an answer.</p>

<div class="row">
	<div class="col-md-8">
			<h2>1</h2>

			<p>Use our quick search box on the right.</p>

			<p>Or you can access the full Tabula manual below.</p>

			<p><a href="http://go.warwick.ac.uk/tabula/manual" class="btn btn-primary" target="_blank">The Tabula Manual</a></p>

			<h2>2</h2>

			<#if hasDeptAdmin>

				<@f.form commandName="command" action="${url('/comments')}" cssClass="comment-form">

					<p>Use the form below to contact your department administrator:</p>

					<@bs3form.labelled_form_group path="name" labelText="Your name">
						<@f.input path="name" cssClass="form-control"/>
					</@bs3form.labelled_form_group>

					<@bs3form.labelled_form_group path="email" labelText="Your email">
						<@f.input path="email" cssClass="form-control"/>
					</@bs3form.labelled_form_group>

					<@bs3form.labelled_form_group path="usercode" labelText="Usercode">
						<@f.input path="usercode" cssClass="form-control"/>
					</@bs3form.labelled_form_group>

					<@bs3form.labelled_form_group path="message" labelText="Your message">
						<@f.textarea path="message" cssClass="form-control"/>
					</@bs3form.labelled_form_group>

					<button type="submit" class="btn btn-primary" name="recipient" value="${Recipients.DeptAdmin}">Send</button>

				</@f.form>

				<h2>3</h2>

				<p>If you still need an answer you can <a href="#comment-modal" data-toggle="modal">contact the web team</a> who can help with your query.</p>

				<@f.form commandName="command" action="${url('/comments')}" cssClass="comment-form">

					<div class="modal fade" id="comment-modal">
						<@modal.wrapper>
							<@modal.header><h3 class="modal-title">Email the Web Team</h3></@modal.header>
							<@modal.body>

								<@webTeamForm />

							</@modal.body>
							<@modal.footer>
								<button type="submit" class="btn btn-primary" name="recipient" value="${Recipients.WebTeam}">Send message</button>
								<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
							</@modal.footer>
						</@modal.wrapper>
					</div>

				</@f.form>

			<#else>

				<p>Use the form below to contact the web team who can help with your query.</p>

				<@f.form commandName="command" action="${url('/comments')}" cssClass="comment-form">

					<@webTeamForm />

					<button type="submit" class="btn btn-primary" name="recipient" value="${Recipients.WebTeam}">Send</button>

				</@f.form>

			</#if>


	</div>
	<div class="col-md-4">
		<div class="well well-sm">
			<h3>Quick Search</h3>
			<form class="search-manual" action="//search.warwick.ac.uk/sitebuilder" method="get">
				<input name="urlPrefix" value="http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual" type="hidden">
				<input name="indexSection" value="sitebuilder" type="hidden">
				<div class="form-group">
					<div class="input-group input-group-lg">
						<input type="search" class="form-control" name="q" autocomplete="off" spellcheck="false" placeholder="Search manual" maxlength="640" />
						<span class="input-group-btn">
							<button class="btn btn-default" type="submit"><i class="fa fa-search"></i></button>
						</span>
					</div>
				</div>
				<ul class="results links"></ul>
			</form>
		</div>
	</div>
</div>

<script>
	jQuery(function($){
		BrowserDetect.init();
		var $form = $('.comment-form');
		$form.find('[name=os]').val(BrowserDetect.OS);
		$form.find('[name=browser]').val(BrowserDetect.browser + ' ' + BrowserDetect.version);
		$form.find('[name=resolution]').val(BrowserDetect.resolution);
		BrowserDetect.findIP(function(ip){
			$form.find('[name=ipAddress]').val(ip);
		});
		var $url = $form.find('[name=url]');
		if ($url.val() == '') {
			var currentPageArg = $.grep((window.location.search || '').substr(1).split('&'), function(pair) {
				return pair.split('=')[0] === 'currentPage';
			});
			if (currentPageArg.length > 0) {
				$url.val(currentPageArg[0].replace('currentPage=', ''));
			}
		}

		var manualSearch = null, $manualForm = $('form.search-manual');
		var manualSearchTypeahead = $manualForm.find('input[type=search]').tabulaTypeahead({
			source: function(query, process){
				// Abort any existing search
				if (manualSearch) {
					manualSearch.abort();
					manualSearch = null;
				}
				manualSearch = $.ajax({
					url: '//search.warwick.ac.uk/search/query.json?callback=?',
					dataType: 'json',
					data: {
						q: query + '*',
						urlPrefix: 'http://www2.warwick.ac.uk/services/its/servicessupport/web/tabula/manual',
						indexSection: 'sitebuilder'
					},
					success: function(data) {
						process(data.results)
					}
				});
			}
		}).data('tabula-typeahead');
		manualSearchTypeahead.render = function (items) {
			var that = this;
			$.each(items, function (i, item) {
				that.$menu.append(
					$('<li />').append(
						$('<a />').prop({
							'href': item.link,
							'target': '_blank'
						}).html(item.title)
					)
				);
			});
			return this;
		};
		manualSearchTypeahead.show = function () {
			var $results = $manualForm.find('ul.results').empty();
			this.$menu.find('li').appendTo($results);
			this.shown = true;
			return this;
		};
	});
</script>

</#escape>