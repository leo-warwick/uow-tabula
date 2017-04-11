(function($) {
        var tables = $("table.sortable-table");
        tables.each(function(i,t){
            var table = $(t);
            var sortableHeaders =[];
            var unsortedHeaders = {};

            table.find("th").each(function(index, h){
                var header=$(h);
                if (header.hasClass("sortable")){
                    sortableHeaders.push([index,0]);
                }else{
                    unsortedHeaders[index]={sorter:false};
                }
            });
            table.sortableTable({

                sortList:sortableHeaders,
                headers:unsortedHeaders
            });
        });
 })(jQuery);
