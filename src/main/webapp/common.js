'use strict';

var API = '/tomcat/irma_ideal_server/api/v1/ideal/';

function loadBanks() {
    $.ajax({
        url: API + 'banks',
    }).done(function(data) {
        insertBanks(data);
    }).fail(function() {
        // TODO: show error on top? i18n?
        var select = $('#input-bank');
        select.empty();
        select.append($('<option selected disabled hidden>Failed to load bank list</option>'));
    });
}

function insertBanks(data) {
    // clear existing data ('Loading...')
    var select = $('#input-bank');
    select.empty();
    select.append($('<option selected disabled hidden>'));

    // create a list of countries
    var countries = [];
    for (var country in data) {
        countries.push(country);
    }
    countries.sort();
    if (countries.indexOf('Nederland') >= 0) {
        // set Nederland as first country
        countries.splice(countries.indexOf('Nederland'), 1);
        countries.unshift('Nederland');
    }

    // insert each country with it's banks
    for (var country of countries) {
        var optgroup = $('<optgroup>');
        optgroup.attr('label', country);
        select.append(optgroup);
        for (var bank of data[country]) {
            var option = $('<option>');
            option.text(bank.issuerName);
            option.val(bank.issuerID);
            optgroup.append(option);
        }
    }
}

function onsubmit(e) {
    e.preventDefault();

    $.ajax({
        url: API + 'request',
    }).done(function(data) {
        console.log('request:', data);
    }).fail(function(xhr) {
        console.error('request:', xhr.responseText);
    });
}

function requestEmail(e) {
    $('#result-alert').addClass('hidden');
    e.preventDefault();
    $.ajax({
        url: API + 'create-email-disclosure-req',
    }).done(function(jwt) {
        //showProgress('Creating email disclosure request...');
        IRMA.verify(jwt,
            function(disclosureJWT) { // success
                console.log('disclosure JWT:', disclosureJWT)
            }, function(message) { // cancel
                // The user explicitly cancelled the request, so do nothing.
                console.warn('user cancelled disclosure');
                requestEnd('cancel');
            }, function(errormsg) { // error
                console.error('could not disclose email attribute:', errormsg);
                requestEnd('danger', MESSAGES['disclosure-error'], errormsg);
            });
    }).fail(function(data) {
        requestEnd('danger', MESSAGES['api-fail']);
    });
}

// Copied from BIG server.
function requestEnd(result, message, errormsg) {
    console.log('user message: ' + result + ': ' + message);
    //$('#btn-request').prop('disabled', false);
    $('#progress').text('');

    if (result !== 'cancel') {
        $('#result-alert')
            .removeClass('alert-success') // remove all 4 alert types
            .removeClass('alert-info')
            .removeClass('alert-warning')
            .removeClass('alert-danger')
            .addClass('alert-' + result)
            .text(message)
            .removeClass('hidden')
            .append('<br>')
            .append($('<small></small>').text(errormsg))
    }
}

function onload() {
    loadBanks();
    var form = document.querySelector('#form-start');
    form.onsubmit = onsubmit;

    form.querySelector('#input-pick-email').onclick = requestEmail;
}

onload(); // script is deferred so DOM has been built
