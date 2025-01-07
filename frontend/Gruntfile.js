module.exports = function(grunt) {
    // Setup urls for the keyshare server, api server, and irma_js
    // these are used to configure the webclient
    if ((typeof (grunt.option("irma_server_url")) === "undefined")) {
        console.log("INFO: don't forget setting irma_server_url in conf.json or specify as grunt parameter");
    }
    if ((typeof (grunt.option("idin_server_url")) === "undefined")) {
        console.log("INFO: don't forget setting idin_server_url in conf.json or specify as grunt parameter");
    }
    if ((typeof (grunt.option("language")) === "undefined")) {
        console.log("INFO: No language chosen, assuming nl");
    }
    if ((typeof (grunt.option("idin_credential_id")) === "undefined")) {
        console.log("INFO: No idin_credential_id chosen, assuming pbdf.pbdf.idin");
    }

    var conf = {
        idin_server_url: grunt.option("idin_server_url") || "<IDIN_SERVER_URL>",
        irma_server_url: grunt.option("irma_server_url") || "<IRMA_SERVER_URL>",
        language: grunt.option("language") || "nl",
        idin_credential_id: grunt.option("idin_credential_id") || "pbdf.pbdf.idin",
    };
    conf.irma_server_url += "/api/v2";

    console.log("Configuration:", conf);

    grunt.initConfig({
        connect: {
            server: {
                options: {
                    port: 9000, // Port to run the server
                    base: 'build', // Serve files from the 'dist' directory
                    livereload: true, // Enable LiveReload
                    open: true, // Automatically open in the browser
                },
            },
        },
        copy: {
            node_modules: {
                cwd: "node_modules",
                src: [
                    "@privacybydesign/irma-frontend/dist/irma.js",
                    "bootstrap/dist/**",
                    "jquery/dist/jquery.min.js",
                    "js-cookie/src/js.cookie.js",
                    "jwt-decode/build/jwt-decode.js"
                ],
                dest: "build/node_modules",
                expand: "true",
            },
            non_html: {
                cwd: "src",
                src: ["**/*", "!**/*.html"],
                dest: "build/",
                expand: "true",
            },
            translated: {
                cwd: "translated/" + conf.language,
                src: ["**/*.html"],
                dest: "build/",
                expand: "true",
            },
        },
        watch: {
            webfiles: {
                files: [
                    "./src/**/*",
                    "!./src/**/*.html",
                ],
                tasks: ["copy:non_html"],
            },
            htmlfiles: {
                files: [
                    "./src/**/*.html",
                ],
                tasks: ["translate"],
            },
            translationfiles: {
                files: [
                    "./src/languages/*",
                ],
                tasks: ["translate"],
            },
        },
        multi_lang_site_generator: {
            default: {
                options: {
                    vocabs: ["en", "nl"],
                    vocab_directory: "src/languages",
                    output_directory: "translated",
                },
                files: {
                    "index.html": ["src/index.html"],
                    "done.html": ["src/done.html"],
                    "error.html": ["src/error.html"],
                    "enroll.html": ["src/enroll.html"],
                },
            },
        },
        json_generator: {
            configuration: {
                dest: "build/conf.json",
                options: conf,
            },
        }
    });

    grunt.loadNpmTasks('grunt-contrib-connect');
    grunt.loadNpmTasks("grunt-contrib-watch");
    grunt.loadNpmTasks("grunt-contrib-copy");
    grunt.loadNpmTasks("grunt-multi-lang-site-generator");
    grunt.loadNpmTasks("grunt-json-generator");

    grunt.registerTask("default", [
        "connect",
        "copy:non_html",
        "json_generator",
        "copy:node_modules",
        "multi_lang_site_generator",
        "copy:translated",
        "watch",
    ]);
    grunt.registerTask("build", [
        "copy:non_html",
        "json_generator",
        "copy:node_modules",
        "multi_lang_site_generator",
        "copy:translated",
    ]);
    grunt.registerTask("translate", [
        "multi_lang_site_generator",
        "copy:translated",
    ]);
};
