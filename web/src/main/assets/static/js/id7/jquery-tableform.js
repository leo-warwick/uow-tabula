(function ($) { "use strict";

jQuery.fn.tableForm = function(options) {
    var $ = jQuery;
    this.each(function(){
        // this is the form
        var $this = $(this);

        var doNothing = function(){};
        var setupFunction = options.setup || doNothing;

        var addButtonClass = options.addButtonClass || 'add-button';
        var headerClass = options.headerClass || 'header-row';
        var rowClass = options.rowClass || 'table-row';
        var tableClass = options.tableClass || 'table-form';
        var listVariable = options.listVariable || 'items';

        var markupClass = options.markupClass || 'row-markup';
        var rowMarkup = $('.'+markupClass).html();

        var onAdd = options.onAdd || doNothing;

        var $table = $this.find('.'+tableClass);
        var $addButton = $this.find('.'+addButtonClass);
        var $header = $this.find('.'+headerClass);
        var $rows = $this.find('.'+rowClass);

        if($rows.size() === 0){
            $header.hide();
        }

        $addButton.on('click', function(e){
            e.preventDefault();
            $header.show();
            var newIndex = $this.find('.'+rowClass).size();
            var newRow = $(rowMarkup);
             // add items[index]. to the input names in the new row
            $("input", newRow).each(function(){
                var name = $(this).attr("name");
                $(this).attr("name", listVariable+"["+newIndex+"]."+name)
            });
            $table.append(newRow);
            onAdd.call(newRow);
			newRow.trigger('tableFormNewRow');
        });

        setupFunction.call($this);

    });
    return this;
}

})(jQuery);
