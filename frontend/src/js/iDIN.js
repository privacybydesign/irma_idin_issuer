getSetupFromJson(function() {


    function getBanks (){
        $.ajax({
            type: 'GET',
            contentType: 'application/json;charset=utf-8',
            url: server + '/banks',
            success: populateBankSelect,
            error: error,
        });
    }

    function populateBankSelect(data) {
        $.each(data, function(countryName, issuers) {
            var optgroup = $('<optgroup>').attr('label', countryName);
            $.each(issuers, function(i, issuer) {
                    optgroup.append($('<option>').text(issuer['issuerName']).attr('value', issuer['issuerID']));
            });
            $('#bank-select').append(optgroup);
        });
    }

    function error(jqXHR, status, error) {
        console.log(jqXHR, status, error);
    }

    //retrieve banks and populate drop down
    getBanks();

    //enable continue button when a bank is selected
    $('#bank-select').on('change', function () {
        $('#continue').prop('disabled', this.value==='default');
    });

    //set correct URL for continue button
    $('#form').submit(function(event) {
        console.log('submit pressed');
        event.preventDefault();

        //clear all errors
        $('.form-group').removeClass('has-error');
        $('#alert_box').empty();

        var issuerID = $('#bank-select').prop('value');

        $.ajax({
            type: 'POST',
            url: server + '/start',
            data: issuerID,
            contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
            success: function(data){
                console.log('redirect to: ' + data);
                window.location.replace(data);
            },
            error: function (jqXHR, status, error) {
                console.log(jqXHR, status, error);
                showError(jqXHR.responseJSON['description']);
            }

        });
    });
});


