<#import "*/modal_macros.ftl" as modal />
<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
	<#if studentCourseDetails?has_content>
		<div class="clearfix">
			<p class="pull-right">
				<@fmt.bulk_email_students students=students />
			</p>
		</div>
		<@student_macros.tableWithMeetingsColumn items=studentCourseDetails meetingsMap=meetingsMap showSelectStudents=true/>
		<div id="meeting-modal" class="modal fade"></div>
		<div class="submit-buttons fix-footer">
			<section class="meetings">
				<a data-href="<@routes.profiles.create_bulk_meeting_record relationshipType />" class="btn btn-default new-meeting-record disabled">Record meeting for selected students</a>
			</section>
		</div>

	<#else>
		<p class="alert alert-info">No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
	</#if>

<script type="text/javascript">

	(function($) {
		var generateBulkRecordLink = function() {
			var $selectedCheckBoxes = $(".collection-checkbox:checkbox:checked");
			if ($selectedCheckBoxes.length > 0) {
				$meetingRecordLink = $('a.new-meeting-record');
				$meetingRecordLink.removeClass('disabled');
				var course = "";
				$selectedCheckBoxes.each(function() {
					var $checkbox = $(this);
					if(course.length > 0) {
						course = course + "," + $checkbox.data('student-course-details');
					} else {
						course = $checkbox.data('student-course-details');
					}
				});
				var baseLink = $meetingRecordLink.data("href");
				$meetingRecordLink.attr("href", baseLink + course);
			} else if (!($( "a.new-meeting-record" ).hasClass( "disabled" ))){
				$meetingRecordLink.addClass('disabled');
			}
		};

		$('.collection-check-all').on('change',function(e) {
			generateBulkRecordLink();
		});

		$('.collection-checkbox').on('change',function(e) {
			generateBulkRecordLink()
		});

	})(jQuery);
</script>
</#escape>
