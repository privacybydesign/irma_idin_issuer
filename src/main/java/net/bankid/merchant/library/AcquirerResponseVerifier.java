package net.bankid.merchant.library;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge into the merchant library's package-private {@link XmlProcessor} so that our own
 * application code can independently verify the XML-DSIG signature of an iDIN acquirer
 * status response before trusting the identity attributes it carries.
 *
 * <p>Background: {@link Communicator#getResponse(StatusRequest)} already verifies the
 * response signature internally (its private {@code performRequest} helper calls
 * {@link XmlProcessor#VerifySignature(Configuration, String)} and turns a failure into an
 * error {@link StatusResponse}). This class exposes that exact same verification &mdash;
 * using the same {@link Configuration#defaultInstance() default configuration} and
 * certificates &mdash; as an explicit, defence-in-depth check that does not rely on the
 * library's internal {@code getIsError()} flag. That way the identity claims are only
 * trusted after we have positively re-confirmed both the SAML (BankID) assertion signature
 * and the iDx acquirer signature over the raw response message ourselves.</p>
 *
 * <p>Because it re-uses the very same verifier and configuration the library used to accept
 * the response, this check never rejects a response the library already accepted; it only
 * adds a fail-closed guard for the case where a response reaches attribute processing
 * without having been cryptographically verified.</p>
 *
 * <p>This class lives in the {@code net.bankid.merchant.library} package on purpose:
 * {@link XmlProcessor} is package-private, so this is the only way to invoke the library's
 * own verifier without resorting to reflection.</p>
 */
public final class AcquirerResponseVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquirerResponseVerifier.class);

    private AcquirerResponseVerifier() {
    }

    /**
     * Verifies the XML-DSIG signature(s) of a raw acquirer status response message.
     *
     * <p>Verification is performed against the {@link Configuration#defaultInstance()
     * default configuration} (i.e. the same acquirer/BankID certificates the
     * {@link Communicator} uses). Any missing signature, tampered content or error during
     * verification results in {@code false}; this method never throws.</p>
     *
     * @param rawResponseXml the raw response XML as returned by
     *                       {@link StatusResponse#getRawMessage()}
     * @return {@code true} if and only if the response carries a valid signature
     */
    public static boolean isSignatureValid(final String rawResponseXml) {
        if (rawResponseXml == null || rawResponseXml.isBlank()) {
            LOGGER.error("Refusing iDIN acquirer response: raw message is missing, cannot verify signature");
            return false;
        }
        try {
            final Configuration config = Configuration.defaultInstance();
            final boolean valid = new XmlProcessor(config).VerifySignature(config, rawResponseXml);
            if (!valid) {
                LOGGER.error("iDIN acquirer response signature is invalid");
            }
            return valid;
        } catch (final Exception e) {
            LOGGER.error("Error while verifying iDIN acquirer response signature: {}", e.getMessage(), e);
            return false;
        }
    }
}
