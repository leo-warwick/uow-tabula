<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#assign isSelf = member.universityId == user.universityId />

<#if !isSelf>
	<details class="indent">
		<summary>${member.officialName}</summary>
		<#if member.userId??>
		${member.userId}<br/>
		</#if>
		<#if member.email??>
			<a href="mailto:${member.email}">${member.email}</a><br/>
		</#if>
		<#if member.phoneNumber??>
		${phoneNumberFormatter(member.phoneNumber)}<br/>
		</#if>
		<#if member.mobileNumber??>
		${phoneNumberFormatter(member.mobileNumber)}<br/>
		</#if>
	</details>
</#if>

<div class="pull-right">
	<a class="btn btn-default hidden-xs" href="http://warwick.ac.uk/tabula/manual/profiles/timetables" target="_blank">Timetable help</a>
	<a class="btn btn-primary" href="<@routes.profiles.department_timetables member.homeDepartment />">Show all timetables</a>
</div>
<h1 class="with-settings">Timetable</h1>

<div class="calendar-outer">
	<div class="calendar-loading hidden-print">
		<i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em>
	</div>
	<div class="calendar hidden-xs" data-viewname="month" data-calendar-download-button=".calendar-download" data-timetable-download-button=".timetable-download"></div>
</div>

<div class="calendar-smallscreen-outer visible-xs-block">
	<div class="calendar-smallscreen"></div>
	<div class="calendar-smallscreen-loading">
		<i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em>
	</div>
</div>

<p>
	<a class="btn btn-default calendar-download hidden-xs" href="<@routes.profiles.timetable_calendar_download member />" data-href="<@routes.profiles.timetable_calendar_download member />">
		Download calendar as PDF
	</a>
	<#if academicYear??>
		<a class="btn btn-default timetable-download" href="<@routes.profiles.timetable_download member academicYear />" data-href="<@routes.profiles.timetable_download member academicYear />">
			Download timetable as PDF (${academicYear.toString})
		</a>
	<#elseif academicYears?has_content>
		<#list academicYears as academicYear>
			<a class="btn btn-default timetable-download" href="<@routes.profiles.timetable_download member academicYear />" data-href="<@routes.profiles.timetable_download member academicYear />">
				Download timetable as PDF (${academicYear.toString})
			</a>
		</#list>
	</#if>
	<#if member.timetableHash?has_content>
		<a class="btn btn-default timetable-ical-link" href="<@routes.profiles.timetable_ical member />">Export as iCal</a>
	</#if>
</p>

<#if member.timetableHash?has_content>
	<div class="modal fade" id="timetable-ical-modal">
		<@modal.wrapper>
			<@modal.header>
				<h3 class="modal-title">Subscribe to your timetable</h3>
			</@modal.header>
			<@modal.body>
				<#if isSelf>
					<div class="alert alert-info">
						<p>Tabula provides your timetable as a calendar feed with a "private address". Private Addresses are designed for your use only. They don't require any further authentication to get information from your timetable, so they're useful for getting your timetable into another calendar or application, or your mobile phone.</p>
						<p>If you accidentally share the address with others, you can change the address by clicking the button below. All of the existing clients using this private address will break, and you will have to give them the new private address.</p>
						<form class="form-inline double-submit-protection" method="POST" action="<@routes.profiles.timetable_ical_regenerate />">
							<div class="submit-buttons">
								<button type="submit" class="btn btn-primary">Generate a new private address</button>
							</div>
						</form>
					</div>
				</#if>

				<p>You can <a href="<@routes.profiles.timetable_ical member />">click this link</a> to subscribe to your timetable in your default calendar application.</p>

				<p>You can also copy the link and paste it into an external application, e.g. Google Calendar:</p>

				<p><a href="<@routes.profiles.timetable_ical member />"><@routes.profiles.timetable_ical member false /></a></p>
			</@modal.body>
		</@modal.wrapper>
	</div>
</#if>

<style type="text/css">
	@import url("<@url resource="/static/css/fullcalendar.css" />");
	@import url("<@url resource="/static/css/fullcalendar-custom.css" />");

	.fc-event.allday {
		font-weight: bold;
		color: white !important;
		border-color: #185c54 !important;
		font-size: .95em;
	}

	.calendar-outer {
		position: relative;
		margin-bottom: 16px;
	}
	.calendar {
		background: white;
		position: relative;
		z-index: 1;
	}
	.calendar-loading {
		position: absolute;
		top: 50%;
		font-size: 4em;
		line-height: 4em;
		margin-top: -2em;
		left: 50%;
		width: 400px;
		margin-left: -200px;
		text-align: center;
		display: none;
	}
</style>

<script type="text/javascript">
	jQuery(function($) {
		$(function(){
			$('.timetable-ical-link').on('click', function(e){
				e.preventDefault();
				$('#timetable-ical-modal').modal('show');
			});
		});

		var weeks = ${weekRangesDumper()};

		var $calendar = $('.calendar');
		if ($calendar.is(':visible')) {
			Profiles.createCalendar(
				$calendar,
				$calendar.data('viewname'),
				weeks,
				Profiles.getCalendarEvents(
					$calendar,
					$('.calendar-loading'),
					'/api/v1/member/${member.universityId}/timetable/calendar',
					function (start, end) {
						var startToSend = new Date(start.getTime());
						startToSend.setDate(startToSend.getDate() - 1);
						var endToSend = new Date(end.getTime());
						endToSend.setDate(endToSend.getDate() + 1);
						return {
							'from':startToSend.getTime(),
							'to':endToSend.getTime(),
							'cb':new Date().valueOf() // Break the IE cache
						};
					},
					'GET'
				)<#if startDate??>,
					true,
					${startDate.getYear()?c},
					${(startDate.getMonthOfYear() - 1)?c},
					${startDate.getDayOfMonth()?c},
					'${startDate.toString("YYYY-MM-dd")}' // This is here for FullCalendar 2 support or if it's ever backported to 1.6.x
				</#if>
			);
			$calendar.find('table').attr('role', 'presentation');
		} else {
			Profiles.createSmallScreenCalender(
				$('.calendar-smallscreen'),
				$('.calendar-smallscreen-loading'),
				'/api/v1/member/${member.universityId}/timetable/calendar',
				function(startDate, endDate) {
					return {
						'from':startDate.getTime(),
						'to':endDate.getTime(),
						'cb':new Date().valueOf() // Break the IE cache
					};
				},
				'GET'
			)
		}
	});
</script>

</#escape>