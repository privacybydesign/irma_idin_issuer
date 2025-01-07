
# Generating a new keystore

Execute the following command while running in the devcontainer:

```bash
./gen-keystore.sh --alias <alias> --password <password> --priv-key-alias <private-key-alias> --priv-key-password <private-key-password>
```

The arguments should be based on what's defined in `config.xml`:
|argument|config key|
|---|---|
|`--alias` | `BankId.Acquirer.Certificate.Alias` |
|`--password` | `BankId.KeyStore.Password` |
|`--priv-key-alias` | `BankId.SAML.Certificate.Alias` |
|`--priv-key-password` | `BankId.SAML.Certificate.Password` |

The resulting keystore should be base64 encoded and passed to the KEYSTORE_JKS env var.
