/**
 * feedback rating widget
 */
(function ($) { "use strict";

var $rateFeedback;
var decorateFeedbackForm = function() {
    $rateFeedback = $('#feedback-rating');
    var $form = $rateFeedback.find('form'),
        action = $form.attr('action');

    if ($form.length > 0) {
        $form.find('.rating-question').button().each(function(){
            var $question = $(this);
            var $group = $('<div>').attr({'class':'btn-group',"data-toggle":"buttons-radio"});
            var $radios = $question.find('input[type=radio]');
            var $unsetter = $question.find('input[type=checkbox]');
            $radios.each(function(){
                var radio = this;
                var text = $(radio).parent('label').text();
                var $button = $('<a>').attr({'class':'btn'}).html(text);
                if (radio.checked) $button.addClass('active');
                $button.click(function(ev){
                    radio.checked = true;
                    $unsetter.attr('checked',false);
                });
                $group.append($button);
            });
            $question.find('label').hide();
            $question.append($group);
        });
        $form.on('submit', function(event){
            $form.find('input[type=submit]').button('loading');
            event.preventDefault();
            $.post(action, $form.serialize())
                .success(function(data){
                    if (data.indexOf('id="feedback-rating"') != -1) {
                        $rateFeedback.replaceWith(data);
                        decorateFeedbackForm();
                    } else { // returned some other HTML - error page or login page?
                        alert('Sorry, there was a problem saving the rating.');
                        $form.find('input[type=submit]').button('reset');
                    }
                })
                .error(function(){ alert('Sorry, that didn\'t seem to work.'); });
            return false;
        });
    }
};

$(function(){
    decorateFeedbackForm();    decorateFeedbackForm();
    $('#feedback-rating-container').each(function(){
        var $this = $(this);
        var action = $this.find('a').attr('href');
        $this.html('').load(action, function(){
            decorateFeedbackForm();
        });
    });
    $('#feedback-rating-container').each(function(){
        var $this = $(this);
        var action = $this.find('a').attr('href');
        $this.html('').load(action, function(){
            decorateFeedbackForm();
        });
    });
});

})(jQuery);
