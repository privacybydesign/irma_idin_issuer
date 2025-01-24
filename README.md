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
We need enviroment variables for the `ISSUER_HOST_NAME` and the `IRMA_SERVER_HOST_NAME`.
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

### Docker Compose
It's easiest to run this repo locally using Docker Compose.
The `docker-compose.yml` file expects a `.secrets` directory with the config files and jwt keys specified above.
To pass the required environment variables, it's recommended to create a `.env` file too and place that in `.secrets`.
The `.env` file should look something like the following:
```
ISSUER_HOST_NAME="http://localhost:8080"
IRMA_SERVER_HOST_NAME="http://localhost:8088"
CONFIG_DIR=/config
IDIN_ISSUER_ID=irma-demo.idin
KEYSTORE_JKS=<idin_keystore_base64_encoded>
```

You can then spin up the docker containers using:
```bash
docker compose --env-file .secrets/.env up --build
```
The `--build` flag is optional, but is recommended during development.
