/**

jQuery.draganddrop

A configurable plugin to define multiple sets of items, which can
be dragged between sets, either one at a time or in batch by making
a drag selection.

Run the plugin on an element that contains all the sets:

    $('#tutee-widget').dragAndDrop();

Each set must be at least a .drag-target containing a ul.drag-list.

The list must have a data-bindpath attribute relating to the collection
that this list will be bound to on the server. Each list item must then
contain a hidden field relating to its value. The script will use the
bindpath value to rename fields as they are moved about.

If you actually don't want to bind any items in a particular list, give it
data-bindpath="true" instead and it will set the name to blank when moved
into this list.

Example (showing some optional extras as below)

  <div id=tutee-widget>
    <a class="btn return-items">Unallocate students</a>
    <div class=drag-target>
      <h3>Students</h3>
      <ul class="drag-list return-list" data-bindpath=command.unsorted>
        <li>0001 <input type=hidden name="command.unsorted[0]" value=0001>
        <li>0002 <input type=hidden name="command.unsorted[1]" value=0002>
      </ul>
    </div>
    <div class=drag-target>
      <h3>Students</h3>
      <span class=drag-count></span>
      <a href=# class="btn show-list">List</a>
      <ul class="drag-list hide" data-bindpath=command.tutor></ul>
    </div>
  </div>

Optional extras:
 - Counter: add a .drag-count element and it will be kept up to date
       with the number of items inside that .drag-target.
 - Popup list: Add a .show-list button and it will trigger a popout
       listing all the items. Use this in conjunction with hiding the
       list itself (by adding .hide to .drag-list)
 - Return items: Add .return-list to ONE .drag-list then add a
       .return-items button; it will be wired to move all items into
       that list.

Method calls (after initialising):

 - $('#tutee-widget').dragAndDrop('return')
        Returns items, same as .return-items button.

TODO: More options; Random allocation function.

*/
(function($){ "use strict";

    var DataName = "tabula-dnd";

    var DragAndDrop = function(element, options) {
        var sortables = '.drag-list';
        var $el = $(element);
        var self = this;
        var first_rows = {};
        
        var $returnList = $el.find('.return-list');
        var hasReturnList = $returnList.length > 0;

        // randomly allocate items from .return-list into all the other lists.
        this.randomise = function() {
            var $sourceList = $returnList;
            var $targets = $el.find(sortables).not('.return-list');

            // shuffle the items
            var items = $sourceList.find(".student").sort(function(){
                return Math.random() > 0.5 ? 1 : -1;
            });

            var itemsPerTarget =  Math.floor(items.length / $targets.length);
            var remainder = items.slice(items.length - (items.length % $targets.length));
            $targets.each(function(index, target){
                var $target = $(target);
                var from = (index*itemsPerTarget);
                var to = ((index+1)*itemsPerTarget);
                var itemsForTarget = items.slice(from, to);
                // If any left, add one to this list.
                if(remainder.length > 0)
                    itemsForTarget = itemsForTarget.add(remainder.splice(0,1));

                self.batchMove([{
                    target: $target,
                    items: itemsForTarget,
                    sources: [] // don't trigger change for source every time
                }]);
            });

            // trigger change event for source now since we didn't do it inside the loop.
            $sourceList.trigger('changed.tabula');

            return false;
        };

        // Move a bunch of items. Mappings is a list of objects. Each object contains:
        //   'target', the $ul to move items to;
        //   'items', an array of list items to move;
        //   'sources', array of lists where the items came from 
        //             (just used to trigger an event on the list);
        // This function powers most of the other item moving functions.
        this.batchMove = function(mappings) {
            $.each(mappings, function( i, entry ) {
                var $target = entry.target;
                var $sources = entry.sources;
                if (!$sources.jquery) $sources = $($sources);
                $.each(entry.items, function(i, li) {
                    $target.append(li);
                });
                $target.trigger('changed.tabula');
                $sources.trigger('changed.tabula');
            });
        };

        // called on a $(ul) when its content changes.
        $(sortables).on('changed.tabula', function() {
            var $list = $(this);
            renameFields($list);
            var $target = $list.closest('.drag-target');
            if ($target.length) {
                updateCount($target);
            }
        });

        // Returns all items to the .return-list.drag-list
        // assuming there is one.
        this.returnItems = function() {
            if (!hasReturnList) throw new Error ('No .return-list list to return items to');
            self.batchMove([{
                target: $returnList,
                items: $el.find(sortables).find('li'),
                sources: $el.find('ul:not(.return-list)')
            }]);
        };

        var returnItem = function($listItem) {
            var $sourceList = $listItem.closest('ul');
            self.batchMove([{
                target: $returnList,
                items: $listItem,
                sources: $sourceList
            }]);
        };

        // Wire button to trigger returnItems
        $el.find('.return-items').click(function() {
            self.returnItems();
        });

        var deleteLinkHtml = ' <a href=# class="delete btn btn-mini"><i class="icon-remove"></i> Remove</a>';

		var popoverGenerator = function() {
            var customHeader = $(this).data('pre') || ''; // data-pre attribute
            var lis = $(this)
                .closest('.drag-target')
                .find(sortables)
                .find('li')
                .map(function(i, li){
                    var $li = $(li);
                    var id = $li.find('input').val();
                    var text = (hasReturnList)? ($li.text()+deleteLinkHtml) : ($li.text());
                    return '<li data-item-id="'+id+'">'+text+'</li>';
                })
                .toArray();
            return customHeader + '<ul>'+lis.join('')+'</ul>';
        };

        // A button to show the list in a popover.
        $el.find('.show-list').tabulaPopover({
            html: true,
            content: popoverGenerator
        }).click(function(e){
            return false;
        }).each(function(i, link) {
            var $link = $(link);
            var $sourceList = $link.closest('.drag-target').find(sortables);
            // When the underlying list changes...
            $sourceList.on('changed.tabula', function() {
                // Update the popover contents, if it's visible.
                if ($sourceList.find('li').length === 0) {
                    $link.addClass('disabled');
                    $link.popover('hide');
                } else {
                    $link.removeClass('disabled');
                    var popover = $link.data('popover');
                    if (popover.$tip) {
                        var $content = popover.$tip.find('.popover-content');
                        if ($content.is(':visible')) {
                            $content.html( popoverGenerator.call( $link[0] ) );
                        }
                    }
                }
            });
        });

        // Handle buttons inside the .show-list popover by attaching it to .drag-target,
        // so we don't have to remember to bind events to popovers as they come and go.
        $el.find('.drag-target').on('click', '.delete', function(e) {
            var $link = $(this);
            // the popover list item
            var $li = $link.closest('li');
            var id = $li.data('item-id');
            // the underlying list item
            var $realLi = $li
                .closest('.drag-target')
                .find('input')
                .filter(function(){ return this.value === id; })
                .closest('li');
            returnItem($realLi);
            return false;
        });

        var $sortables = $el.find(sortables);

        var draggableOptions = {
            scroll: false,
            revert: 'invalid',
            handle: '.handle',
            containment: $el,

            start: function(event, ui) {
                var $li = $(this);
                var $dragTarget = $li.closest('.drag-target');
                $li.data('source-target', $dragTarget);

                var $selectedItems = $dragTarget.find('.ui-selected');

                if ($li.hasClass('ui-selected') && $selectedItems.length > 1) {
                    first_rows = $selectedItems.map(function(i, e) {
                        var $tr = $(e);
                        return {
                            tr : $tr.clone(true),
                            id : $tr.attr('id')
                        };
                    }).get();
                    $selectedItems.addClass('cloned');
                }
            },

            // helper returns the HTML item that follows the mouse
            helper: function(event) {
                var $element = $(event.currentTarget);
                var multidrag = $element.hasClass('ui-selected');
                var msg = $element.text();
                if (multidrag) msg = $element.closest('ul').find('.ui-selected').length + " items";
                return $('<div>')
                    .addClass('label')
                    .addClass('multiple-items-drag-placeholder')
                    .html(msg);
            },

            stop : function(event, ui) {
                // Unhighlight stuff else it gets messy-looking
                $el.find('.ui-selected').removeClass('.ui-selected');
            }

        };

        // Drag any list item by its handle
        $sortables.find('li')
            .draggable(draggableOptions)
            .prepend('<i class="icon-th icon-white handle"></i> ');

        // Drag-select
        $sortables.selectable({
                filter: 'li',
                cancel: '.handle'
            });

        var updateAllCounts = function() {
            $el.find('.drag-target').each(function(i, dragTarget){
                updateCount($(dragTarget));
            });
        };

        // Dropping onto any .drag-target
        $el.find('.drag-target').droppable({
            hoverClass: "drop-hover",
            activate: function(event, ui) {
                //$(event.target).addClass('droponme-highlight');
            },
            deactivate: function(event, ui) {
                //$(event.target).removeClass('droponme-highlight');
            },
            drop: function(event, ui) {
                var $target = $(this);
                var $source = $(ui.draggable).data('source-target');
                var $sourceDragList = $source.find(sortables);
                var $dragList = $target.find(sortables);

                if (first_rows.length > 1) {
                    // multi-ball!
                    // have to re-draggable() these as they
                    // lost their senses during cloning.
                    $.each(first_rows, function(i, item) {
                        $(item.tr)
                        .removeAttr('style')
                        .removeClass('ui-draggable')
                        .data('draggable', null)
                        .data('ui-draggable', null)
                        .draggable(draggableOptions)
                        .appendTo($dragList);
                    });
                    $el.find('.cloned').remove();
                    first_rows = {};
                } else {
                    $dragList.append(ui.draggable);
                }

                $el.find('.ui-selected').removeClass('ui-selected');

                // update counts, lists, popups
                $dragList.trigger('changed.tabula');
                $sourceDragList.trigger('changed.tabula');
            }
        });

        // Initialise all dependent widgets.
        $el.find(sortables).trigger('changed.tabula');

    };

    // The jQ plugin itself is a basic adapter around DragAndDrop
    $.fn.dragAndDrop = function(options) {
        var dnd = this.data(DataName);
        if (options === 'return') {
            dnd.returnItems();
        } else if (options === 'randomise') {
            dnd.randomise();
        } else {
            this.each(function(i, element) {
                dnd = new DragAndDrop(element, options);
                $(element).data(DataName, dnd);
            });
            return this;
        }
    };

    var updateCount = function($dragTarget) {
        // setTimeout is a silly hack to work around the fact that an object
        // just added or removed won't be reflected in the list straight away.
        // This is neater than the mathemagical alternative.
        setTimeout(function() {
            var $dragList = $dragTarget.find('.drag-list');
            $dragTarget.find('.drag-count').html($dragList.find('li').length);
        }, 10);
    };

    // Rename all form input for this department to represent the ordered list
    // NOTE only works if exactly 1 input in each li
    var renameFields = function($list) {
        var bindpath = $list.data('bindpath');
        var nobind = $list.data('nobind') === true;
        if (bindpath || nobind) {
            $list.find('li input').each(function(i, field) {
                var path = "";
                if (!nobind) path = bindpath + '[' + i + ']';
                field.name = path;
            });
        } else {
            throw new Error("No data-bindpath on ul: " + $list);
        }
    };

})(jQuery);
