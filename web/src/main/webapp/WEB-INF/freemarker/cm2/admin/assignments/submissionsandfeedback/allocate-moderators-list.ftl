<#import "*/cm2_macros.ftl" as cm2 />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>
	<@cm2.assignmentHeader "Choose moderation sample for" assignment />

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<a class="btn btn-primary must-have-selected form-post" href="<@routes.cm2.moderationSamplingAllocation assignment />">
		Send selected for moderation
	</a>
	<a class="btn btn-primary must-have-selected form-post" href="<@routes.cm2.moderationSamplingFinalise assignment />">
		Finalise selected feedback
	</a>

	<table class="table table-striped marking-table preview-marks">
		<thead>
		<tr>
				<th class="check-col"><@bs3form.selector_check_all /></th>
				<th class="student-col sortable">University ID</th>
				<th class="student-col sortable">First name</th>
				<th class="student-col sortable">Last name</th>
				<th class="sortable">Marker</th>
				<th class="sortable">Mark</th>
				<th class="sortable">Grade</th>
				<th class="sortable">Moderator</th>
		</tr>
		</thead>
		<tbody>
			<#list infos as info>
				<tr data-toggle="collapse" data-target="#${info.student.userId}" class="clickable collapsed expandable-row">
					<td class="check-col">
						<@bs3form.selector_check_row name="students" value="${info.markerFeedback.feedback.usercode}" />
					</td>
					<td class="toggle-icon-large student-col">${info.markerFeedback.feedback.studentIdentifier!""}</td>
					<td class="student-col">${info.student.firstName}</td>
					<td class="student-col">${info.student.lastName}&nbsp;
						<#if info.student.warwickId??>
							<@pl.profile_link info.student.warwickId />
						<#else>
							<@pl.profile_link info.student.userId />
						</#if>
					</td>
					<td>${info.marker.fullName}</td>
					<td>${info.mark!""}</td>
					<td>${info.grade!""}</td>
					<td><#if info.moderator??>${info.moderator.fullName}</#if></td>
				</tr>
				<#assign detailUrl><@routes.cm2.markerOnlineFeedback assignment stage info.marker info.student /></#assign>
				<tr id="${info.student.userId}" data-detailurl="${detailUrl}" class="collapse detail-row">
					<td colspan="8" class="detailrow-container"><i class="fa fa-spinner fa-spin"></i> Loading</td>
				</tr>
			</#list>
		</tbody>
	</table>
</#escape>

<script type="text/javascript">
	(function($) {
		var $body = $('body');

		// prevent rows from expanding when selecting the checkbox column
		$body.on('click', '.check-col', function(e){
			e.stopPropagation()
		});

		var bigListOptions = {

			setup: function(){

				var $container = this;

				$('.form-post').click(function(event){
					event.preventDefault();
					var $this = $(this);
					if(!$this.hasClass("disabled")) {
						var action = this.href;
						if ($this.data('href')) {
							action = $this.data('href')
						}

						var $form = $('<form></form>').attr({method: 'POST', action: action}).hide();
						var doFormSubmit = false;

						if ($container.data('checked') !== 'none' || $this.closest('.must-have-selected').length === 0) {
							var $checkedBoxes = $(".collection-checkbox:checked", $container);
							$form.append($checkedBoxes.clone());
							doFormSubmit = true;
						}

						if (doFormSubmit) {
							$(document.body).append($form);
							$form.submit();
						} else {
							return false;
						}
					}
				});
			},

			onSomeChecked : function() {
				if (this.find('input:checked').length) {
					$body.find('.must-have-selected').removeClass('disabled');
				} else {
					$body.find('.must-have-selected').addClass('disabled');
				}

				if (this.find('.ready-next-stage input:checked').length) {
					$body.find('.must-have-ready-next-stage').removeClass('disabled');
				} else {
					$body.find('.must-have-ready-next-stage').addClass('disabled');
				}
			},

			onNoneChecked : function() {
				$body.find('.must-have-selected').addClass('disabled');
			}
		};

		$('.marking-table')
			.bigList(bigListOptions)
			.sortableTable()
			.on('sortEnd', function(){
				// reposition detail rows after the sort
				var $table = $(this);
				$table.find('tr.clickable').each(function(){
					var $row = $(this);
					$($row.data('target')).detach().insertAfter($row);
				});
			});

	})(jQuery);
</script>