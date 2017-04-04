
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


