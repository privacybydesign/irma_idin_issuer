$(function() {
    var translTable = {"zipcode": "Postcode", "address":"Adres" , "city":"Stad", "initials":"Initialen",
        "familyName":"Achternaam", "sex":"Geslacht", "dateOfBirth":"Geboortedatum"};

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


    function displayAttributes (attr, translator){
        $.each(attr,function(key, value){
            addTableLine(translator.hasOwnProperty(key)?translator[key]:key, value);
        })
    }

    //set issuing functionality to button
    $("#enroll").on("click", function() {
        // Clear errors
        $(".form-group").removeClass("has-error");
        $("#alert_box").empty();
        IRMA.issue(Cookies.get("jwt"),function() {
            showSuccess("iDIN data successfully issued");
        }, showWarning, showError);
    });

    //decode the issuing JWT and show the values in a table
    var decoded = jwt_decode(Cookies.get("jwt"));
    displayAttributes(decoded.iprequest.request.credentials[0].attributes, translTable)

});