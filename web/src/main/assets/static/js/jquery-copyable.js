(function ($) { "use strict";

/**
 * Replace links with a textbox that selects itself on focus, and
 * gives you a hint to press Ctrl+C (or Apple+C)
 *
 * options:
 *   preselect: true to preselect item. Only works when working on a single element.
 */
jQuery.fn.copyable = function(options) {
  options = options || {};
  var Mac = -1!=(window.navigator&&navigator.platform||"").indexOf("Mac");
  var PressCtrlC = 'Now press '+(Mac?'\u2318':'Ctrl')+"-C to copy.";

  var preselect = (this.length == 1) && (!!options['preselect'] || false);
  var prefixLinkText = (!!options['prefixLinkText'] || false);

  this.each(function(){
    var $this = $(this),
        url = this.href,
        title = this.title,
        text = $this.html();
    var $container = $('<span class=copyable-url-container></span>').attr('title',title);
    var $explanation = $('<span class=press-ctrl-c></span>').html(PressCtrlC).hide();
    var $input = $('<input class="copyable-url" rel="tooltip"></span>')
        .attr('readonly', true)
        .attr('value',url)
        .click(function(){
          this.select();
          $explanation.slideDown('fast');
        }).blur(function(){
          $explanation.fadeOut('fast');
        });
    $container.append($input).append($explanation);
    $this.after($container).remove();
    if (prefixLinkText) {
        $container.before(text);
    }
    if (preselect) {
        $input.click();
    }
  });

  return this;
}

}(jQuery));