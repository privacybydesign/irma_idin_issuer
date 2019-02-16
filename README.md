# iDIN server and frontend

## Configure MySQL

 1. Install MySQL/MariaDB.
 2. Create a new user:
    
        create user ideal@localhost identified by 'password';
    
 3. Create a new database:
    
        create database ideal;
    
 4. Allow the ideal user access to this database:
    
        grant all privileges on *.ideal to ideal@localhost;
    
 5. Create a new table to store tokens:
    
        use ideal;
        CREATE TABLE idin_tokens (id INTEGER PRIMARY KEY AUTO_INCREMENT, hashedToken text);
    
 6. Modify src/test/resources/jetty-env.xml and modify the properties `url`,
    `username`, and `password`.

## Configure iDIN

All paths here are relative to the resources directory: `src/main/resources`.

  * Set up `config.xml`. The bank should provide this file. There is an example
    file called `bankid-EXAMPLE-config.xml`.
    These fields should probably be configured manually:
      * `BankId.Merchant.MerchantID`: the merchant ID for your connection.
      * `BankId.Merchant.SubID`: usually 0.
      * `BankId.Merchant.ReturnUrl`: the URL to return to after an iDIN
        transaction. It should return back to the frontend.
      * `BankId.KeyStore.Location`: the name of the keystore file (usually with
        the .jks extension).
      * `BankId.Merchant.Certificate.Alias`: the name of the private key in the
        keystore file.
      * `BankId.Merchant.Certificate.Password`: the password on the private key
        inside the keystore.
  * Create a local keypair of which the public part should be shared with your
    bank and the private part should be put in the keystore (with the name set
    in `BankId.Merchant.Certificate.Alias`). The password of the private key is
    set in `BankId.Merchant.Certificate.Password`.
  * Copy `iDIN-config.example.json` to `iDIN-config.json`. Set the following
    fields as appropriate:
      * `server_name`: should be `testip` or the actual server name used by the
        API server.
      * `static_salt` and `token_hmac_key`: must match the values configured in
        [go-ideal-issuer](https://github.com/privacybydesign/go-ideal-issuer/blob/master/README.md),
        see that README for details.
      * The rest of the file contains many values related to the exact
        configuration of IRMA credentials.
  * Put the public key of the API server you're using (test or production) in
    the resources folder, named `apiserver-pk.der` (or a different place
    depending on `api_server_public_key`).
  * Put the application-specific secret key for signing issuing JWTs in the
    resources folder, named `sk.der`. The location can be configured with
    `jwt_privatekey`.
  * Put the keystore file provided by your bank (but modified to store a
    keypair) in the resources folder, with the name from `config.xml`
    (`BankId.KeyStore.Location`), for example `iis.jks`.
