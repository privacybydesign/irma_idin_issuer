#! /bin/bash

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --password) PASSWORD="$2"; shift ;;
        --alias) ALIAS="$2"; shift ;;
        --priv-key-alias) PRIVATE_KEY_ALIAS="$2"; shift ;;
        --priv-key-password) PRIVATE_KEY_PASSWORD="$2"; shift ;;
        *) echo "Unknown parameter: $1"; exit 1 ;;
    esac
    shift
done

PRIVATE_KEY_FILE="private.pem"
PUB_CERT_FILE="public.cer"
PKCS12_FILE="pkcs12.p12"
KEYSTORE_FILE="keystore.jks"


# Generate RSA Private Key with AES-128 encryption
openssl genrsa -aes128 -out $PRIVATE_KEY_FILE -passout pass:$PASSWORD 2048

# Create Self-Signed Certificate
openssl req -x509 -sha256 -new -key $PRIVATE_KEY_FILE -passin pass:$PASSWORD -days 1825 -out $PUB_CERT_FILE

# Export Private Key and Certificate to PKCS12 Format
openssl pkcs12 -export -out $PKCS12_FILE -inkey $PRIVATE_KEY_FILE -password pass:$PASSWORD -in $PUB_CERT_FILE

# Import Certificate to Java Keystore (JKS) for the public certificate
keytool -import -alias "$ALIAS" -file $PUB_CERT_FILE -keystore $KEYSTORE_FILE -storepass $PASSWORD -noprompt

# Import PKCS12 into Java Keystore with a new alias and different password for private key
keytool -importkeystore -destkeystore $KEYSTORE_FILE -srckeystore $PKCS12_FILE \
-srcstoretype pkcs12 -srcstorepass $PASSWORD -deststorepass $PASSWORD \
-destalias "$PRIVATE_KEY_ALIAS" -srcalias "1" -keypass $PRIVATE_KEY_PASSWORD

# Remove unused artifacts
rm $PRIVATE_KEY_FILE $PUB_CERT_FILE $PKCS12_FILE
