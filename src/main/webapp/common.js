'use strict';

var API = '/tomcat/irma_ideal_server/api/v1/ideal/';

var emailJWT = null;

function init() {
    var params = parseURLParams();
    if (!('trxid' in params)) {
        // phase 1: request email + bank
        $(document.body).addClass('phase1')
        loadBanks();
        var form = document.querySelector('#form-start');
        form.onsubmit = startTransaction;
        form.querySelector('#input-pick-email').onclick = requestEmail;
    } else {
        // phase 2: get the result and issue the credential
        $(document.body).addClass('phase2')
        finishTransaction(params);
    }
}

function loadBanks() {
    $.ajax({
        url: API + 'banks',
    }).done(function(data) {
        insertBanksIntoForm(data);
    }).fail(function() {
        // TODO: show error on top? i18n?
        var select = $('#input-bank');
        select.empty();
        select.append($('<option selected disabled hidden>Failed to load bank list</option>'));
    });
}

function insertBanksIntoForm(data) {
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
                setStatus('cancel');
            }, function(errormsg) { // error
                console.error('could not disclose email attribute:', errormsg);
                setStatus('danger', MESSAGES['disclosure-error'], errormsg);
            });
    }).fail(function(data) {
        setStatus('danger', MESSAGES['api-fail']);
    });
}

// With the name and email, start a transaction.
function startTransaction(e) {
    e.preventDefault();
    setStatus('info', MESSAGES['start-transaction']);
    $('#btn-request').prop('disabled', true);
    $('#result-alert').addClass('hidden');

    var data = {
        bank: $('#input-bank').val(),
    };
    $.ajax({
        method: 'POST',
        url:    API + 'start',
        data:   data,
    }).done(function(data) {
        setStatus('info', MESSAGES['redirect-to-bank']);
        location.href = data;
    }).fail(function(xhr) {
        setStatus('danger', MESSAGES['api-fail'], xhr);
        $('#btn-request').prop('disabled', false);
    });
}

function finishTransaction(params) {
    setStatus('info', MESSAGES['loading-return']);
    $.ajax({
        method: 'POST',
        url: API + 'return',
        data: {
            trxid: params.trxid,
            ec:    params.ec,
        },
    }).done(function(jwt) {
        setStatus('info', MESSAGES['issuing-credential']);
        console.log('issuing JWT:', jwt);
        IRMA.issue(jwt, function(e) {
            console.log('iDeal credential issued:', e);
            setStatus('success', MESSAGES['issue-success']);
        }, function(e) {
            console.warn('cancelled:', e);
            setStatus('cancel');
        }, function(e) {
            console.error('issue failed:', e);
            setStatus('danger', MESSAGES['failed-to-issue'], e);
        });
    }).fail(function(xhr) {
        if (xhr.status == 502 && xhr.responseText.substr(0, 7) == 'status:') {
            if (xhr.responseText in MESSAGES) {
                if (xhr.responseText == 'status:Cancelled') {
                    setStatus('warning', MESSAGES[xhr.responseText]);
                } else {
                    setStatus('danger', MESSAGES[xhr.responseText]);
                }
            } else {
                setStatus('danger', MESSAGES['status:other'], xhr.responseText);
            }
        } else if (xhr.status == 502 && xhr.responseText.substr(0, 12) == 'consumermsg:') {
            setStatus('danger', MESSAGES['status:consumermsg'], xhr.responseText.substr(12));
        } else {
            setStatus('danger', MESSAGES['failed-to-verify'], xhr);
            console.error('failed to finish transaction:', xhr.responseText);
        }
    });
}

// Show progress in the alert box.
function setStatus(alertType, message, errormsg) {
    console.log('user message: ' + alertType + ': ' + message);
    message = message || '???'; // make sure it's not undefined
    if (errormsg && errormsg.statusText) { // is this an XMLHttpRequest?
        errormsg = errormsg.status + ' ' + errormsg.statusText;
    }

    var alert = $('#result-alert')
    alert.removeClass('alert-success'); // remove all 4 alert types
    alert.removeClass('alert-info');
    alert.removeClass('alert-warning');
    alert.removeClass('alert-danger');
    alert.addClass('alert-' + alertType);
    alert.text(message);
    alert.removeClass('hidden');
    if (errormsg) {
        alert.append('<br>');
        alert.append($('<small></small>').text(errormsg));
    }
}

// https://stackoverflow.com/a/8486188/559350
function parseURLParams() {
  var query = location.search.substr(1);
  var result = {};
  query.split("&").forEach(function(part) {
    var item = part.split("=");
    result[item[0]] = decodeURIComponent(item[1]);
  });
  return result;
}

init(); // script is deferred so DOM has been built
