# irma_idin_webclient
The IRMA iDIN attribute issuance webapp can be used as a front-end for the [IRMA iDIN issuer](https://github.com/privacybydesign/irma_idin_issuer).

## Building
The webapp can be either fully build and configured using Grunt or you can specify the configuration after having run Grunt by editing the `conf.json` file in the build directory.

In case of fully building the webapp using Grunt, the following command can be used: 

    grunt --language="en" --idin_server_url="<URL1>" --irma_server_url="<URL2>"
    
If you want to set the configuration manually, you only have to run:

    grunt --language="en"
    
The configuration can then be edited in the `build/conf.json` file.
