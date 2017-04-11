<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#macro reportLoader reportUrl commandName="command" hasDatePicker=true>
	<script>
		window.ReportBuilder = {};
		window.ReportBuilder.rowKey = 'students';
	</script>
	<#if hasDatePicker>
		<@f.form method="get" action="" commandName="${commandName}" cssClass="form-inline double-submit-protection">
			<div class="form-group">
				<label>Start date</label>
				<@f.input id="startDate" path="startDate" cssClass="date-picker input-small form-control" />
			</div>
			<div class="form-group">
				<label>End date</label>
				<@f.input id="endDate" path="endDate" cssClass="date-picker input-small form-control" />
			</div>
			<div class="form-group">
				<button type="submit" class="btn btn-default">Submit</button>
			</div>
		</@f.form>
	</#if>
	<div class="loading">
		<p><em>Building report&hellip;</em></p>

		<div class="progress ">
			<div class="progress-bar progress-bar-striped active" style="width: 10%;"></div>
		</div>
	</div>

	<div class="complete alert alert-info" style="display: none; margin-top: 16px;">
		<p>Report complete</p>
		<div class="btn-toolbar">
			<a href="#" class="show-data btn btn-default" data-loading-text="Building table, please wait&hellip;">Show</a>
			<div class="download btn-group">
				<a href="#" class="btn  btn-default dropdown-toggle" data-toggle="dropdown">Download&hellip;<span class="caret"></span></a>
				<#nested />
			</div>
		</div>
	</div>

	<div class="alert alert-danger" style="display: none;">
		<p>There was a problem generating the report. If the problem persists, please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a>.</p>
	</div>

	<div class="report-target"></div>
	<form class="report-target-form" style="display: none;" method="POST" action="" enctype="multipart/form-data"></form>

	<script>
		var progress = 10, stepProgress = function() {
			progress = progress + 5;
			jQuery('.id7-main-content .loading .bar').width(progress + '%');
			if (progress < 90) {
				progressStepperTimeout = setTimeout(stepProgress, 10 * 1000);
			}
		}, progressStepperTimeout = setTimeout(stepProgress, 10 * 1000), pleaseWaitTimeout = setTimeout(function(){
			jQuery('.id7-main-content .loading p em').html('Still building report&hellip; please be patient');
		}, 60 * 1000);
		jQuery(function($){
			$.ajax('${reportUrl}', {
				type: 'POST',
				<#if hasDatePicker>
					data: {
						'startDate' : $('#startDate').val(),
						'endDate' : $('#endDate').val()
					},
				</#if>
				success: function(data) {
					clearTimeout(progressStepperTimeout);
					var $mainContent = $('.id7-main-content');
					$mainContent.find('.loading p em').html('Downloading data&hellip;');
					$mainContent.find('.loading .bar').width('100%');
					setTimeout(function(){
						var key, key1, key2;
						window.ReportBuilder.reportData = data;
						$('form.report-target-form').append(
							$('<input/>').prop({
								'type': 'hidden',
								'name': 'data',
								'value': JSON.stringify(data)
							})
						);

						$mainContent.find('div.loading').hide();
						$mainContent.find('div.complete').show();

						$mainContent.find('div.complete div.download ul.dropdown-menu a').on('click', function(e) {
							e.preventDefault();
							$('form.report-target-form').prop('action', $(this).data('href')).submit();
						});

						var showReport = function(){
							var table = $('<table/>')
								.addClass('table table-condensed table-striped table-sortable')
								.css({
									'width' : 'auto',
									'max-width' : 'none'
								})
								.append($('<thead/>').append(window.ReportBuilder.buildHeader()));

							var rows = $.map(window.ReportBuilder.reportData[window.ReportBuilder.rowKey], function(student) {
								return window.ReportBuilder.buildRow(student);
							});
							table.append($('<tbody />').append(rows));

							if (window.ReportBuilder.buildFooter != undefined) {
								table.append($('<tfoot/>').append(window.ReportBuilder.buildFooter()));
							}

							$('.report-target').append(table);
							table.sortableTable({
								textExtraction: function(node) {
									var $el = $(node);
									if ($el.data('sortby')) {
										return $el.data('sortby');
									} else {
										return $el.text().trim();
									}
								}
							});
							$(window).trigger('load'); // Wrap the wide table

							$('th.rotated').each(function() {
								var $this = $(this),
									width = $this.find('.rotate').width(),
									height = $this.find('.rotate').height();
								$this.css('height', width + 20).css('width', height + 5);
								$this.find('.rotate').css('margin-top', -(width + 25));
								if ($this.is('.sortable')) {
									$this.css('height', width + 35).css('width', 'auto');
									$this.find('.rotate').css('margin-top', -(width + 40));
								}
							});
							$('td.rotated').each(function() {
								var width = $(this).find('.rotate').width();
								var height = $(this).find('.rotate').height();
								$(this).css('height', width).css('width', height + 5);
								$(this).find('.rotate').css('margin-top', -(height));
							});
						};

						if (window.ReportBuilder.buildHeader().find('th').length > 300) {
							$('.id7-main-content div.complete a.show-data').attr({
								'disabled': true,
								'title': 'The report is too large to display in the page'
							});
						} else {
							$('.id7-main-content div.complete a.show-data').on('click', function(e) {
								e.preventDefault();
								var $this = $(this);
								setTimeout(function(){
									showReport();
									$this.hide();
								}, 500);
							});
						}

					}, 500);
				},
				error: function() {
					$('.id7-main-content div.loading').hide().end().find('div.alert-error').show();
				}
			});
		});
	</script>
</#macro>

<#macro attendanceMonitoringReportScript>
	<script>
		jQuery(function($){
			if (window.ReportBuilder == undefined)
				return false;

			window.ReportBuilder.buildHeader = function(){
				var container = $('<tr/>');
				container.append(
					$('<th/>').addClass('sortable').html('First name')
				).append(
					$('<th/>').addClass('sortable').html('Last name')
				).append(
					$('<th/>').addClass('sortable').html('University ID')
				);
				$.each(window.ReportBuilder.reportData.points, function(i, point){
					container.append(
						$('<th/>').addClass('point rotated').append(
							$('<div/>').addClass('rotate').html(point.name + ' (' + point.intervalString + ')')
						)
					)
				});
				container.append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('fa fa-exclamation-triangle fa-fw late').prop('title', 'Unrecorded'))
				).append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('fa fa-times fa-fw unauthorised').prop('title', 'Missed monitoring points'))
				);
				return container;
			};

			window.ReportBuilder.buildRow = function(student) {
				var container = $('<tr/>');
				container.append(
					$('<td/>').html(student.firstName)
				).append(
					$('<td/>').html(student.lastName)
				).append(
					$('<td/>').append(
						$('<a/>').prop({
							'href' : '/profiles/view/' + student.universityId,
							'target' : '_blank'
						}).html(student.universityId)
					)
				);
				var attendance = window.ReportBuilder.reportData.attendance[student.universityId], unrecordedCount = 0, missedCount = 0;
				$.each(window.ReportBuilder.reportData.points, function(i, point){
					if (attendance[point.id] == undefined) {
						container.append($('<td/>').append($('<i/>').addClass('fa fa-fw')));
					} else {
						if (attendance[point.id] === 'attended') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-check attended')));
						} else if (attendance[point.id] === 'authorised') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-times-circle-o authorised')));
						} else if (attendance[point.id] === 'unauthorised') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-times unauthorised')));
							missedCount++;
						} else if (point.late == 'true') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-exclamation-triangle late')));
							unrecordedCount++;
						} else {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-minus unrecorded')));
						}
					}
				});
				container.append(
					$('<td/>').addClass('unrecorded').append(
						$('<span/>').addClass('badge progress-bar-' + ((unrecordedCount > 2) ? 'danger' : ((unrecordedCount > 0) ? 'warning' : 'success'))).html(unrecordedCount)
					)
				).append(
					$('<td/>').addClass('missed').append(
						$('<span/>').addClass('badge progress-bar-' + ((missedCount > 2) ? 'danger' : ((missedCount > 0) ? 'warning' : 'success'))).html(missedCount)
					)
				);
				return container;
			};
		});
	</script>
</#macro>

<#macro smallGroupReportScript>
	<script>
		jQuery(function($){
			if (window.ReportBuilder == undefined)
				return false;

			window.ReportBuilder.buildHeader = function(){
				var container = $('<tr/>');
				container.append(
					$('<th/>').addClass('sortable').html('First name')
				).append(
					$('<th/>').addClass('sortable').html('Last name')
				).append(
					$('<th/>').addClass('sortable').html('University ID')
				);
				$.each(window.ReportBuilder.reportData.events, function(i, event){
					container.append(
						$('<th/>').addClass('event rotated').append(
							$('<div/>').addClass('rotate').html(
								event.moduleCode + ' '
								+ event.setName + ' '
								+ event.format + ' '
								+ event.groupName + ' '
								+ event.dayString + '  Week ' + event.week
							)
						)
					)
				});
				container.append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('fa fa-exclamation-triangle fa-fw late').prop('title', 'Unrecorded'))
				).append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('fa fa-times fa-fw unauthorised').prop('title', 'Missed (unauthorised) events'))
				).append(
					$('<th/>').addClass('sortable').append($('<i/>').addClass('fa fa-times-circle-o fa-fw authorised').prop('title', 'Missed (authorised) events'))
				);
				return container;
			};

			window.ReportBuilder.buildRow = function(student) {
				var container = $('<tr/>');
				container.append(
					$('<td/>').html(student.firstName)
				).append(
					$('<td/>').html(student.lastName)
				).append(
					$('<td/>').append(
						$('<a/>').prop({
							'href' : '/profiles/view/' + student.universityId,
							'target' : '_blank'
						}).html(student.universityId)
					)
				);
				var attendance = window.ReportBuilder.reportData.attendance[student.universityId], unrecordedCount = 0, missedCount = 0, authorisedCount = 0;
				$.each(window.ReportBuilder.reportData.events, function(i, event){
					if (attendance[event.id] == undefined) {
						container.append($('<td/>').append($('<i/>').addClass('fa fa-fw')));
					} else {
						if (attendance[event.id] === 'attended') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-ok attended')));
						} else if (attendance[event.id] === 'authorised') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-times-circle-o authorised')));
							authorisedCount++;
						} else if (attendance[event.id] === 'unauthorised') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-times unauthorised')));
							missedCount++;
						} else if (event.late == 'true') {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-exclamation-triangle late')));
							unrecordedCount++;
						} else {
							container.append($('<td/>').append($('<i/>').addClass('fa fa-fw fa-minus unrecorded')));
						}
					}
				});
				container.append(
					$('<td/>').addClass('unrecorded').append(
						$('<span/>').addClass('badge progress-bar-' + ((unrecordedCount > 2) ? 'danger' : ((unrecordedCount > 0) ? 'warning' : 'success'))).html(unrecordedCount)
					)
				).append(
					$('<td/>').addClass('missed').append(
						$('<span/>').addClass('badge progress-bar-' + ((missedCount > 2) ? 'danger' : ((missedCount > 0) ? 'warning' : 'success'))).html(missedCount)
					)
				).append(
					$('<td/>').addClass('authorised').append(
						$('<span/>').addClass('badge badge-' + ((authorisedCount > 2) ? 'important' : ((authorisedCount > 0) ? 'warning' : 'success'))).html(authorisedCount)
					)
				);
				return container;
			};

			window.ReportBuilder.buildFooter = function() {
				var container = $('<tr/>');
				container.append(
					$('<th/>').prop('colspan', 2)
				).append(
					$('<th/>').html('Tutor/s')
				);
				$.each(window.ReportBuilder.reportData.events, function(i, event){
					container.append(
						$('<td/>').addClass('tutors rotated').append(
							$('<div/>').addClass('rotate').html(event.tutors)
						)
					)
				});
				container.append($('<td/>')).append($('<td/>'));
				return container;
			}
		});
	</script>
</#macro>

<#macro smallGroupByModuleReportScript>
<script>
	jQuery(function($){
		if (window.ReportBuilder == undefined)
			return false;

		window.ReportBuilder.buildHeader = function(){
			var container = $('<tr/>');
			container.append(
				$('<th/>').addClass('sortable').html('First name')
			).append(
				$('<th/>').addClass('sortable').html('Last name')
			).append(
				$('<th/>').addClass('sortable').html('University ID')
			);
			$.each(window.ReportBuilder.reportData.modules, function(i, module){
				container.append(
					$('<th/>').addClass('module rotated sortable').append(
						$('<div/>').addClass('rotate').html(module.code.toUpperCase() + ' ' + module.name)
					)
				)
			});
			return container;
		};

		window.ReportBuilder.buildRow = function(student) {
			var container = $('<tr/>');
			container.append(
				$('<td/>').html(student.firstName)
			).append(
				$('<td/>').html(student.lastName)
			).append(
				$('<td/>').append(
					$('<a/>').prop({
						'href' : '/profiles/view/' + student.universityId,
						'target' : '_blank'
						}).html(student.universityId)
				)
			);
			var counts = window.ReportBuilder.reportData.counts[student.universityId];
			$.each(window.ReportBuilder.reportData.modules, function(i, module){
				if (counts[module.id] == undefined) {
					container.append($('<td/>').append($('<i/>').addClass('fa fa-fw')));
				} else {
					container.append(
						$('<td/>').addClass('unrecorded').append(
							$('<span/>').addClass('badge progress-bar-' + ((counts[module.id] > 2) ? 'danger' : ((counts[module.id] > 0) ? 'warning' : 'success'))).html(counts[module.id])
						)
					);
				}
			});
			return container;
		};
	});
</script>
</#macro>