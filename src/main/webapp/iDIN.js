$(function() {

    var server = "http://localhost:8080/irma_idin_server/api/v1/idin";

    function getBanks (){
        $.ajax({
            type: "GET",
            contentType: "application/json;charset=utf-8",
            url: server + "/banks",
            success: populateBankSelect,
            error: error,
        });
    }

    function populateBankSelect(data) {
        $.each(data, function(countryName, issuers) {
            var optgroup = $('<optgroup>').attr('label', countryName);
            $.each(issuers, function(i, issuer) {
                    optgroup.append($('<option>').text(issuer["issuerName"]).attr('value', issuer["issuerID"]));
            });
            $('#bank-select').append(optgroup);
        });
    }

    function error(jqXHR, status, error) {
        console.log(jqXHR, status, error);
    }

    getBanks()
});


