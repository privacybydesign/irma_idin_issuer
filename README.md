# iDIN server

Add personal data received from Dutch banks via iDIN for use in your
[IRMA app](https://github.com/privacybydesign/irmamobile).

## Building new version of iDIN library
The iDIN library is included in this project as library in `/libs`. This library is built ourselves
based on the code of the [iDIN library of Currence](https://github.com/Currence-Online/iDIN-libraries-java/tree/master/Java/library).
Please check the documentation of this library how the JAR can be re-generated in case
you want to update this library.


## Docker secrets
To run this issuer in a Docker container, you need to specify some secrets.
- `config.json` needs to contain the settings for this issuer (see `config.EXAMPLE.json` for options).
- `config.xml` needs to contain the iDIN config (see `config.EXAMPLE.xml` for options).
- `sk.pem`: the RSA privare key.
- `pk.pem`: the RSA public key.

These secrets should be mounted in a directory. You can then point the server to this by setting the `CONFIG_DIR` environment variable.

Additionally, we need a secret for the iDIN credential id.
This can be passed in at runtime via the `IDIN_ISSUER_ID` environment variable.
The last thing we need is a `keystore.jks` file. This is a binary file that should be passed as a base64 encoded string to `KEYSTORE_JKS`.

### Generating JWT keys
```
mkdir -p .secrets
openssl genrsa 4096 > .secrets/sk.pem
openssl rsa -in .secrets/sk.pem -pubout > .secrets/pk.pem
```

Note: these keys will be transformed to `der` keys at runtime. 
They're expected as `pem` files because that's easier to store in secret managers.
In the `config.json` they should be named `sk.der` and `pk.der`.
