$(function() {
    var attributes = [["stad","city"],["naam","name"]];

    $.each(attributes, function(){
        if (this !== null && this[0] !== null && this[1] !== null) {
            var value = Cookies.get(this[1]);
            if (value!==undefined) {
                console.log("??"+value);
                addTableLine(this[0], value);
            }
        }
    });



    function addTableLine(head, data){
        if (data !== null) {
            $('#attributeTable')
                .append($('<tr>')
                    .append($('<th>').text(head).attr("scope", "row"))
                    .append($('<td>').text(data)
                    )
                );
        }
    }

    function showError(message) {
        $("#alert_box").html("<div class=\"alert alert-danger\" role=\"alert\">"
            + "<strong>" + message + "</strong></div>");
    }

    var showWarning = function(msg) {
        $("#alert_box").html("<div class=\"alert alert-warning\" role=\"alert\">"
            + "<strong>Warning:</strong> " + msg + "</div>");
    };

    var showSuccess = function(msg) {
        $("#alert_box").html("<div class=\"alert alert-success\" role=\"alert\">"
            + msg + "</div>");
    };

    $("#enroll").on("click", function() {
        // Clear errors
        $(".form-group").removeClass("has-error");
        $("#alert_box").empty();
        IRMA.issue(Cookies.get("jwt"),function() {
            showSuccess("iDIN data successfully issued");
        }, showWarning, showError);
    });

    var token = Cookies.get("jwt");
    console.log(token);
    var decoded = jwt_decode(token);
    console.log(decoded.iprequest.request.credentials[0].attributes.address);

});