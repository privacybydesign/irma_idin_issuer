
# this is the startup script, doing a bunch of configuration based on secrets
# before starting the actual server

set -e # make sure the script immediately ends when one of its commands fails

echo "creating the idin-fontend-config json file based on template"
echo "issuer id: $IDIN_ISSUER_ID"
cat idin-fontend-config-template.txt
mkdir -p /var/www/
envsubst < idin-fontend-config-template.txt > /var/www/conf.json



KEYSTORE_DIR=/usr/local/tomcat/webapps/irma_idin_server/WEB-INF/classes/

echo "creating $KEYSTORE_DIR"

mkdir -p $KEYSTORE_DIR

echo "base64 decoding KEYSTORE_JKS and putting it in $KEYSTORE_DIR/keystore.jks"
echo $KEYSTORE_JKS | base64 -d > $KEYSTORE_DIR/keystore.jks

export IRMA_CONF="/irma-idin-conf"

echo "creating directory $IRMA_CONF"
mkdir -p $IRMA_CONF

echo "copying config from $CONFIG_DIR to $IRMA_CONF"
echo "contents of $CONFIG_DIR:\n$(ls -la $CONFIG_DIR)"
cp $CONFIG_DIR/sk.pem $IRMA_CONF/sk.pem
cp $CONFIG_DIR/pk.pem $IRMA_CONF/pk.pem
cp $CONFIG_DIR/config.json $IRMA_CONF/config.json
cp $CONFIG_DIR/config.xml $IRMA_CONF/config.xml


pushd $IRMA_CONF
echo "contents of $IRMA_CONF:\n$(ls -la)"
echo "converting JWT public and private keys to 'der' format"
openssl pkcs8 -topk8 -inform PEM -outform DER -in sk.pem -out sk.der -nocrypt
openssl pkey -pubin -inform PEM -outform DER -in pk.pem -out pk.der
popd


echo "starting Java server"
catalina.sh run
