package com.matchbar.util;

import java.util.Locale;

public final class Emails {

    private Emails() {}

    /**
     * Forma canónica con la que se guardan y buscan los emails. Sin esto,
     * "Mario@test.com" y "mario@test.com" serían cuentas distintas y el login
     * fallaría según cómo escriba el usuario su email.
     */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
