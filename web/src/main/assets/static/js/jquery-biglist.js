(function ($) {
  'use strict';

  /*
      Provides functionality to lists/tables that have checkboxes,
      so that you can select some/all rows and do stuff with them.

      Options:
          setup($container): function called when the plugin is instantiated.
          onSomeChecked($container): callback when at least 1 is checked.
          onNoneChecked($container): callback when no rows are checked.
          onAllChecked($container): callback when all rows selected. defaults to behaviour of onSomeChecked.
          onChange($checkbox): callback when a row is selected/unselected.
          onBulkChange($container, $checkboxes): callback after rows have been selected/unselected in bulk.
  */
  $.fn.bigList = function (options) {

    options = options || {};

    this.each(function () {
      var $this = $(this);

      if (options === 'changed') {
        this.checkboxChangedFunction();
        return;
      }

      var checkboxClass = options.checkboxClass || 'collection-checkbox';
      var checkboxAllClass = options.checkboxClass || 'collection-check-all';

      var $checkboxes = $this.find('input.' + checkboxClass);
      var $selectAll = $this.find('input.' + checkboxAllClass);

      var doNothing = function () {
      };

      var onChange = options.onChange;
      var onBulkChange = options.onBulkChange || doNothing;
      var onSomeChecked = options.onSomeChecked || doNothing;
      var onNoneChecked = options.onNoneChecked || doNothing;
      var onAllChecked = options.onAllChecked || onSomeChecked;

      this.checkboxChangedFunction = function () {
        if (!!onChange) {
          onChange.call($(this)); // pass the checkbox as the context
        }
        var allChecked = $checkboxes.not(':checked').length === 0;
        $selectAll.prop('checked', allChecked);
        if (allChecked) {
          $this.data('checked', 'all');
          onAllChecked.call($this);
        } else if ($checkboxes.is(':checked')) {
          $this.data('checked', 'some');
          onSomeChecked.call($this);
        } else {
          $this.data('checked', 'none');
          onNoneChecked.call($this);
        }
        onBulkChange.call($this, $checkboxes);
      };
      $checkboxes.on('change', this.checkboxChangedFunction);

      $selectAll.on('change', function () {
        var checked = $(this).is(':checked');
        var $checkableCheckboxes = $checkboxes.filter(':not(:disabled)');
        $checkableCheckboxes.prop('checked', checked);
        if (!!onChange) {
          // Avoid looping if we can
          $checkableCheckboxes.each(function () {
            onChange.call(jQuery(this));
          });
        }
        onBulkChange.call($this, $checkableCheckboxes);
        if (checked) {
          $this.data('checked', 'all');
          onAllChecked.call($this);
        } else {
          $this.data('checked', 'none');
          onNoneChecked.call($this);
        }
      });

      var setupFunction = options.setup || doNothing;
      setupFunction.call($this);

      $(function () {
        $checkboxes.trigger('change');
        $this.closest('form.dirty-check').trigger('reinitialize.areYouSure');
      });
      //
      // // Returns an array of IDs.
      // var getCheckedFeedbacks = function() {
      //     return $checkboxes.filter(':checked').map(function(i,input){ return input.value; });
      // };
    });
    return this;
  }

})(jQuery);
