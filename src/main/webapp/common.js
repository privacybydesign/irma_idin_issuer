'use strict';

var API = '/tomcat/irma_ideal_server/api/v1/';

var emailJWT = null;

function init() {
    document.querySelector('#form-ideal').onsubmit = startIDealTransaction;
    document.querySelector('#input-pick-email').onclick = requestEmail;
    document.querySelector('#form-idin').onsubmit = startIDINTransaction;

    var params = parseURLParams();
    if (params.trxid && params.ec == 'ideal') {
        // phase 2: get the result and issue the iDeal credential
        $(document.body).addClass('phase2')
        finishIDealTransaction(params);
        loadIDINBanks();
    } else if (params.trxid) {
        // phase 3: get the result and issue the iDIN credential
        $(document.body).addClass('phase3')
        finishIDINTransaction(params);
    } else {
        // phase 1: request email + bank
        $(document.body).addClass('phase1')
        loadIDealBanks();
    }
}

function loadIDealBanks() {
    var select = $('#input-ideal-bank');
    $.ajax({
        url: API + 'ideal/banks',
    }).done(function(data) {
        insertBanksIntoForm(data, select);
    }).fail(function() {
        // TODO: show error on top? i18n?
        select.empty();
        select.append($('<option selected disabled hidden>Failed to load bank list</option>'));
    });
}

function loadIDINBanks() {
    var select = $('#input-idin-bank');
    $.ajax({
        url: API + 'idin/banks',
    }).done(function(data) {
        insertBanksIntoForm(data, select);
    }).fail(function() {
        // TODO: show error on top? i18n?
        select.empty();
        select.append($('<option selected disabled hidden>Failed to load bank list</option>'));
    });
}

function insertBanksIntoForm(data, select) {
    // clear existing data ('Loading...')
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

    select.val(sessionStorage.idx_selectedBank);
}

function requestEmail(e) {
    $('#result-alert').addClass('hidden');
    e.preventDefault();
    $.ajax({
        url: API + 'ideal/create-email-disclosure-req',
    }).done(function(jwt) {
        //showProgress('Creating email disclosure request...');
        IRMA.verify(jwt,
            function(disclosureJWT) { // success
                console.log('disclosure JWT:', disclosureJWT)
                emailJWT = disclosureJWT;
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
function startIDealTransaction(e) {
    e.preventDefault();
    setStatus('info', MESSAGES['start-ideal-transaction']);
    $('#btn-ideal-request').prop('disabled', true);
    $('#result-alert').addClass('hidden');

    var selectedBank = $('#input-ideal-bank').val();
    sessionStorage.idx_selectedBank = selectedBank;
    var data = {
        bank: selectedBank,
    };
    $.ajax({
        method: 'POST',
        url:    API + 'ideal/start',
        data:   data,
    }).done(function(data) {
        setStatus('info', MESSAGES['redirect-to-ideal-bank']);
        location.href = data;
    }).fail(function(xhr) {
        setStatus('danger', MESSAGES['api-fail'], xhr);
        $('#btn-ideal-request').prop('disabled', false);
    });
}

function finishIDealTransaction(params) {
    setStatus('info', MESSAGES['loading-return']);
    $.ajax({
        method: 'POST',
        url: API + 'ideal/return',
        data: {
            trxid: params.trxid,
            ec:    params.ec,
        },
    }).done(function(response) {
        setStatus('info', MESSAGES['issuing-ideal-credential']);
        console.log('issuing JWT:', response.jwt);
        localStorage.idx_token = response.token;
        IRMA.issue(response.jwt, function(e) {
            console.log('iDeal credential issued:', e);
            setStatus('success', MESSAGES['issue-success']);
        }, function(e) {
            console.warn('cancelled:', e);
            setStatus('cancel');
        }, function(e) {
            console.error('issue failed:', e);
            setStatus('danger', MESSAGES['failed-to-issue-ideal'], e);
        });
    }).fail(function(xhr) {
        if (xhr.status == 502 && xhr.responseText.substr(0, 13) == 'ideal-status:') {
            if (xhr.responseText in MESSAGES) {
                if (xhr.responseText == 'ideal-status:Cancelled') {
                    setStatus('warning', MESSAGES[xhr.responseText]);
                } else {
                    setStatus('danger', MESSAGES[xhr.responseText]);
                }
            } else {
                setStatus('danger', MESSAGES['ideal-status:other'], xhr.responseText);
            }
        } else if (xhr.status == 502 && xhr.responseText.substr(0, 12) == 'consumermsg:') {
            setStatus('danger', MESSAGES['ideal-status:consumermsg'], xhr.responseText.substr(12));
        } else {
            setStatus('danger', MESSAGES['failed-to-verify'], xhr);
            console.error('failed to finish iDeal transaction:', xhr.responseText);
        }
    });
}

function startIDINTransaction(e) {
    e.preventDefault();
    setStatus('info', MESSAGES['start-idin-transaction']);
    $('#btn-ideal-request').prop('disabled', true);
    $('#result-alert').addClass('hidden');

    var selectedBank = $('#input-idin-bank').val();
    sessionStorage.idx_selectedBank = selectedBank;
    var data = {
        bank: selectedBank,
        token: localStorage.idx_token,
    };
    $.ajax({
        method: 'POST',
        url:    API + 'idin/start',
        data:   data,
    }).done(function(data) {
        setStatus('info', MESSAGES['redirect-to-idin-bank']);
        location.href = data;
    }).fail(function(xhr) {
        setStatus('danger', MESSAGES['api-fail'], xhr);
        $('#btn-idin-request').prop('disabled', false);
    });
}

function finishIDINTransaction(params) {
    setStatus('info', MESSAGES['loading-return']);
    $.ajax({
        method: 'POST',
        url: API + 'idin/return',
        data: {
            trxid: params.trxid,
            ec:    params.ec,
            token: localStorage.idx_token,
        },
    }).done(function(response) {
        delete localStorage.idx_token; // removed on the server
        setStatus('info', MESSAGES['issuing-idin-credential']);
        console.log('issuing JWT:', response.jwt);
        IRMA.issue(response.jwt, function(e) {
            console.log('iDeal credential issued:', e);
            setStatus('success', MESSAGES['issue-success']);
        }, function(e) {
            console.warn('cancelled:', e);
            setStatus('cancel');
        }, function(e) {
            console.error('issue failed:', e);
            setStatus('danger', MESSAGES['failed-to-issue-idin'], e);
        });
    }).fail(function(xhr) {
        if (xhr.status == 502 && xhr.responseText.substr(0, 7) == 'idin-status:') {
            if (xhr.responseText in MESSAGES) {
                if (xhr.responseText == 'idin-status:Cancelled') {
                    setStatus('warning', MESSAGES[xhr.responseText]);
                } else {
                    setStatus('danger', MESSAGES[xhr.responseText]);
                }
            } else {
                setStatus('danger', MESSAGES['idin-status:other'], xhr.responseText);
            }
        } else if (xhr.status == 502 && xhr.responseText.substr(0, 12) == 'consumermsg:') {
            setStatus('danger', MESSAGES['idin-status:consumermsg'], xhr.responseText.substr(12));
        } else {
            setStatus('danger', MESSAGES['failed-to-verify-idin'], xhr);
            console.error('failed to finish iDIN transaction:', xhr.responseText);
        }
    });
}

// Show progress in the alert box.
function setStatus(alertType, message, errormsg) {
    console.log('user message: ' + alertType + ': ' + message);
    message = message || MESSAGES['unknown-error']; // make sure it's not undefined
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
