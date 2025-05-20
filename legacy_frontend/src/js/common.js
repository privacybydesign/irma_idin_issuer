var conf, strings, server, irma_server_conf;

function getSetupFromJson(successFunc) {
    console.log('Running getSetupFromJson');

    $.getJSON('conf.json', function(json) {
        conf = json;
        irma_server_conf = {
            language: conf.language,
            server: conf.irma_server_url,
        };
        server = `${conf.idin_server_url}/api/v1/idin`;

        console.log('Configuration:', conf);

        $.getJSON('languages/' + conf.language + '.json', function(text) {
            strings = text;
            console.log('Loaded language strings');
            successFunc();
        });
    });
}

function showError(message) {
    $('#alert_box').html('<div class="alert alert-danger" role="alert">'
        + '<strong>' + message + '</strong></div>');
}

var showWarning = function(msg) {
    $('#alert_box').html('<div class="alert alert-warning" role="alert">'
        + '<strong>Warning:</strong> ' + msg + '</div>');
};

var showSuccess = function(msg) {
    $('#alert_box').html('<div class="alert alert-success" role="alert">'
        + msg + '</div>');
};
