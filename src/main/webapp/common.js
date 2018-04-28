'use strict';

var API = '/tomcat/irma_ideal_server/api/v1/ideal/';

function onsubmit() {
    // TODO
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
    var form = document.querySelector('#form-start');
    form.onsubmit = onsubmit;

    form.querySelector('#input-pick-email').onclick = requestEmail;
}

onload(); // script is deferred so DOM has been built
