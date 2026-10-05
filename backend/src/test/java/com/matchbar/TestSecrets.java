package com.matchbar;

/** Valores ficticios compartidos por los tests. */
public final class TestSecrets {

    private TestSecrets() {}

    /** Secreto JWT para los tests web (mínimo 32 bytes). */
    public static final String JWT_SECRET = "secreto-de-test-con-mas-de-32-bytes-0123456789";
}
