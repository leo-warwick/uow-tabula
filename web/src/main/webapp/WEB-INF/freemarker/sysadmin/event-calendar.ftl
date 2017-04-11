<#escape x as x?html>
	<h1>Event calendar</h1>

	<div class="fullCalendar" data-viewname="month"></div>

	<style type="text/css">
		@import url("<@url resource="/static/css/fullcalendar.css" />");
		@import url("<@url resource="/static/css/fullcalendar-custom.css" />");

		.fc-event.allday {
			font-weight: bold;
			color: white !important;
			border-color: #185c54 !important;
			font-size: .95em;
		}
	</style>

	<@script "/static/js/fullcalendar.js" />
	<script type="text/javascript">
		// TIMETABLE STUFF
		jQuery(function($) {
			function getEvents($container){
				return function (start, end, callback){
					var complete = false;
					setTimeout(function() {
						if (!complete) $container.fadeTo('fast', 0.3);
					}, 300);
					$.ajax({url:'<@url context="/" page="/sysadmin/event-calendar/events" />',
						// make the from/to params compatible with what FullCalendar sends if you just specify a URL
						// as an eventSource, rather than a function. i.e. use seconds-since-the-epoch.
						data:{'from':start.getTime()/1000,
							'to':end.getTime()/1000
						},
						success:function(data){
							//
							// TODO
							//
							// insert code here to look through the events and magically display weekends if a weekend event is found
							// (cal.fullCalendar('option','weekends',true); doesn't work, although some other options do)
							// https://code.google.com/p/fullcalendar/issues/detail?id=293 has some discussion and patches
							//
							// TAB-3008 - Change times to Europe/London
							$.each(data, function(i, event){
								event.start = moment(moment.unix(event.start).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
								event.end = moment(moment.unix(event.end).tz('Europe/London').format('YYYY-MM-DDTHH:mm:ss')).unix();
							});
							callback(data);
						},
						complete: function() {
							complete = true;
							$container.fadeTo('fast', 1);
						}
					});
				};
			}
			function onViewUpdate(view,element){
				$('.popover').hide();
			}

			function createCalendar(container, defaultViewName){
				var showWeekends = (defaultViewName == "month");
				var cal = $(container).fullCalendar({
					events: getEvents($(container)),
					defaultView: defaultViewName,
					allDaySlot: true,
					slotMinutes: 60,
					firstHour: 0,
					firstDay: 1, // monday
					timeFormat: {
						agendaWeek: '', // don't display time on event
						// for all other views
						'': 'HH:mm{ - HH:mm}'   //  17:00 - 18:30
					},
					defaultEventMinutes: 30,
					weekends: showWeekends,
					viewRender: onViewUpdate,
					header: {
						left:   'title',
						center: 'month,agendaWeek,agendaDay',
						right:  'today prev,next'
					},
					eventAfterRender: function(event, element, view) {
						if (event.allDay) $(element).addClass('allday');
						$(element).tabulaPopover({html:true, container:"#container", placement: 'bottom', title:event.title, content:event.description.replace(/\n/g, '<br>')})
					}
				});
			}

			$(".fullCalendar").each(function(index){
				createCalendar($(this), $(this).data('viewname'));
			});
		});
	</script>
</#escape>