package net.bankid.merchant.library;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for {@link AcquirerResponseVerifier}, which independently re-verifies the XML-DSIG
 * signature of an iDIN acquirer status response before its identity claims are trusted.
 *
 * <p>These tests exercise the real merchant-library verifier and assert that responses which
 * are not cryptographically vouched for &mdash; missing, blank, malformed or unsigned XML
 * &mdash; are always rejected. A correctly signed response can only be produced with the
 * acquirer's private key, so the positive path is covered indirectly through
 * {@code IdinResourceTest} (where the verifier is stubbed) rather than duplicated here.</p>
 */
public final class AcquirerResponseVerifierTest {

    @Test
    public void nullRawMessageIsRejected() {
        assertFalse(AcquirerResponseVerifier.isSignatureValid(null));
    }

    @Test
    public void blankRawMessageIsRejected() {
        assertFalse(AcquirerResponseVerifier.isSignatureValid("   "));
    }

    @Test
    public void malformedXmlIsRejected() {
        assertFalse(AcquirerResponseVerifier.isSignatureValid("this is not xml"));
    }

    @Test
    public void wellFormedButUnsignedResponseIsRejected() {
        final String unsigned =
                "<AcquirerStatusRes xmlns=\"http://www.betaalvereniging.nl/iDx/messages/Merchant-Acquirer/1.0.0\">"
                        + "<createDateTimestamp>2026-01-01T00:00:00.000Z</createDateTimestamp>"
                        + "<Transaction><transactionID>0000000000000001</transactionID><status>Success</status>"
                        + "</Transaction></AcquirerStatusRes>";
        assertFalse(AcquirerResponseVerifier.isSignatureValid(unsigned));
    }

    @Test
    public void unsignedSamlAssertionIsRejected() {
        // A response carrying identity attributes but no valid signature over them must not
        // be accepted.
        final String unsignedWithAssertion =
                "<AcquirerStatusRes xmlns=\"http://www.betaalvereniging.nl/iDx/messages/Merchant-Acquirer/1.0.0\">"
                        + "<Transaction><transactionID>0000000000000001</transactionID><status>Success</status>"
                        + "<container>"
                        + "<Response xmlns=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                        + "<Assertion xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
                        + "<AttributeStatement><Attribute Name=\"urn:nl:bvn:bankid:1.0:consumer.legallastname\">"
                        + "<AttributeValue>Attacker</AttributeValue></Attribute></AttributeStatement>"
                        + "</Assertion></Response>"
                        + "</container></Transaction></AcquirerStatusRes>";
        assertFalse(AcquirerResponseVerifier.isSignatureValid(unsignedWithAssertion));
    }
}
