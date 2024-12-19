
# this is the startup script, doing a bunch of configuration based on secrets
# before starting the actual server

set -e # make sure the script immediately ends when one of its commands fails

echo "creating the idin-fontend-config json file based on template"
echo "issuer id: $IDIN_ISSUER_ID"
cat idin-fontend-config-template.txt
mkdir -p /var/www/pbdf-website/uitgifte/idin/
envsubst < idin-fontend-config-template.txt > /var/www/pbdf-website/uitgifte/idin/conf.json


echo "creating /usr/local/tomee/webapps/irma_idin_server/WEB-INF/classes/"
mkdir -p /usr/local/tomee/webapps/irma_idin_server/WEB-INF/classes/

echo "base64 decoding KEYSTORE_JKS and putting it in /usr/local/tomee/webapps/irma_idin_server/WEB-INF/classes"
echo $KEYSTORE_JKS | base64 -d > /usr/local/tomee/webapps/irma_idin_server/WEB-INF/classes/keystore.jks


export IRMA_CONF="/irma-idin-conf"

echo "creating directory $IRMA_CONF"
mkdir -p $IRMA_CONF

echo "copying config from $CONFIG_DIR to $IRMA_CONF"
cp -R $CONFIG_DIR $IRMA_CONF


pushd $IRMA_CONF
echo "converting JWT public and private keys to 'der' format"
openssl pkcs8 -topk8 -inform PEM -outform DER -in sk.pem -out sk.der -nocrypt
openssl pkey -pubin -inform PEM -outform DER -in pk.pem -out pk.der
popd


echo "starting Java server"
catalina.sh run
