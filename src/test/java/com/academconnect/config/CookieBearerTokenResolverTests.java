package com.academconnect.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;

public class CookieBearerTokenResolverTests {

    private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

    @Test
    void resolveShouldReturnTokenWhenCookieIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieBearerTokenResolver.COOKIE_NAME, "jwt-from-cookie"));

        Assertions.assertEquals("jwt-from-cookie", resolver.resolve(request));
    }

    @Test
    void resolveShouldFallbackToAuthorizationHeaderWhenCookieIsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer jwt-from-header");

        Assertions.assertEquals("jwt-from-header", resolver.resolve(request));
    }

    @Test
    void resolveShouldPreferCookieOverHeaderWhenBothArePresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieBearerTokenResolver.COOKIE_NAME, "jwt-from-cookie"));
        request.addHeader("Authorization", "Bearer jwt-from-header");

        Assertions.assertEquals("jwt-from-cookie", resolver.resolve(request));
    }

    @Test
    void resolveShouldReturnNullWhenNoCookieAndNoHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Assertions.assertNull(resolver.resolve(request));
    }

    @Test
    void resolveShouldIgnoreEmptyCookieValueAndFallbackToHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(CookieBearerTokenResolver.COOKIE_NAME, ""));
        request.addHeader("Authorization", "Bearer jwt-from-header");

        Assertions.assertEquals("jwt-from-header", resolver.resolve(request));
    }

    @Test
    void resolveShouldIgnoreCookiesWithDifferentNames() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("other_cookie", "irrelevant"));
        request.addHeader("Authorization", "Bearer jwt-from-header");

        Assertions.assertEquals("jwt-from-header", resolver.resolve(request));
    }
}
