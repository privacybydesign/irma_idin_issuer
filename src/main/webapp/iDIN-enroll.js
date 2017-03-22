$(function() {
    //TODO: make an array over which to loop
    addTableLine("stad",Cookies.get("city"));

    function addTableLine(head, data){
        console.log("addLine");
        $('#attributeTable')
            .append($('<tr>')
                .append($('<th>').text(head).attr("scope","row"))
                    .append($('<td>').text(data)
                    )
            );

    }
});