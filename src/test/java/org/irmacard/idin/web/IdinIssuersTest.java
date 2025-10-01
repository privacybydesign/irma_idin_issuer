package org.irmacard.idin.web;

import net.bankid.merchant.library.internal.DirectoryResponseBase;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IdinIssuersTest {

    public static final String BANK_CODE = "INGBNL2A";
    public static final String NL_KEY = "NL";

    @Test
    public void getIssuers() {
        final Map<String, List<DirectoryResponseBase.Issuer>> issuers = new HashMap<>();
        final IdinIssuers idinIssuers = new IdinIssuers(issuers);

        assertEquals(issuers, idinIssuers.getIssuers());
    }

    @Test
    void containsBankCode_returnsTrueWhenPresent() {
        final DirectoryResponseBase.Issuer issuer = mock(DirectoryResponseBase.Issuer.class);
        when(issuer.getIssuerID()).thenReturn(BANK_CODE);

        final Map<String, List<DirectoryResponseBase.Issuer>> map = new HashMap<>();
        map.put(NL_KEY, Collections.singletonList(issuer));

        final IdinIssuers issuers = new IdinIssuers(map);

        assertTrue(issuers.containsBankCode(BANK_CODE));
    }

    @Test
    void containsBankCode_returnsFalseWhenAbsent() {
        final DirectoryResponseBase.Issuer rabobank = mock(DirectoryResponseBase.Issuer.class);
        when(rabobank.getIssuerID()).thenReturn("RABONL2U");

        final Map<String, List<DirectoryResponseBase.Issuer>> map = new HashMap<>();
        map.put(NL_KEY, Collections.singletonList(rabobank));

        final IdinIssuers issuers = new IdinIssuers(map);

        assertFalse(issuers.containsBankCode(BANK_CODE));
    }

    @Test
    void shouldUpdate_isFalseImmediatelyAfterConstruction() {
        final Map<String, List<DirectoryResponseBase.Issuer>> map = new HashMap<>();
        final IdinIssuers issuers = new IdinIssuers(map);

        assertFalse(issuers.shouldUpdate());
    }

    @Test
    void shouldUpdate_isFalseExactlyAtSevenDays() throws Exception {
        final Map<String, List<DirectoryResponseBase.Issuer>> map = new HashMap<>();
        final IdinIssuers issuers = new IdinIssuers(map);

        // Move 'updated' to exactly 7 days ago (boundary). Comparison is '>' so it should still be false.
        final long nowSec = System.currentTimeMillis() / 1000;
        setUpdatedSeconds(issuers, nowSec - 7L * 24 * 60 * 60);

        assertFalse(issuers.shouldUpdate());
    }

    @Test
    void shouldUpdate_isTrueWhenOlderThanSevenDays() throws Exception {
        final Map<String, List<DirectoryResponseBase.Issuer>> map = new HashMap<>();
        final IdinIssuers issuers = new IdinIssuers(map);

        final long nowSec = System.currentTimeMillis() / 1000;
        setUpdatedSeconds(issuers, nowSec - (7L * 24 * 60 * 60 + 1));

        assertTrue(issuers.shouldUpdate());
    }

    private static void setUpdatedSeconds(final IdinIssuers obj, final long value) throws Exception {
        final Field f = IdinIssuers.class.getDeclaredField("updated");
        f.setAccessible(true);
        f.setLong(obj, value);
    }

}