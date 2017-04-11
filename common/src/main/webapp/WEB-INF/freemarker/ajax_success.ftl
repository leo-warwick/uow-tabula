<#--

What is this?

It is something a controller can return as a response to an AJAX request, when the
page is mainly working with HTML.

The script using AJAX can look for the matching element, and do the appropriate thing
(usually close a popup) if it is found.

    if ($element.find('.ajax-response').data('status') == 'success') {
      // close popup or whatever
    }

If you add a model attribute called data to the Mav that returns this view then it will be exposed as a data attribute
called data. This allows you to perform actions based on the state of the response.

 var $response = $element.find('.ajax-response');
 if ($response.data('status') == 'success') {
   if($response.data('data') == 'true')
     // do some conditional stuff
   else
     // do other fallback stuff
 }

-->
<div class="ajax-response" data-status="success" data-data="${(data!'')?html}">
<#-- Some text just in case close doesn't work. -->
Successful. You can close this popup now.
</div>