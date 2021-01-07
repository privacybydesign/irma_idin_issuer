
# iDIN server

Add personal data received from Dutch banks via iDIN for use in your
[IRMA app](https://github.com/privacybydesign/irmamobile).


## Setting up the server

 1. Add the server config file `src/main/resources/config.json`. As an example you can check
    `config.EXAMPLE.json` in the root of this repository.
 2. Add the iDIN library config file `src/main/resources/config.xml`. As an example you can
    check `config.EXAMPLE.xml` in the root of this repository.
 3. Supply the Java keystore on the location as specified in the iDIN library config. Make sure
    that this file is available in the Java classpath. For example, include it in `src/main/resources/...`
    or in `webapp/WEB-INF/classes`.
 3. Run `gradle appRun` in the root directory of this project.

## Building new version of iDIN library

The iDIN library is included in this project as library in `/libs`. This library is built ourselves
based on the code of the [iDIN library of Currence](https://github.com/Currence-Online/iDIN-libraries-java/tree/master/Java/library).
Please check the documentation of this library how the JAR can be re-generated in case
you want to update this library.
