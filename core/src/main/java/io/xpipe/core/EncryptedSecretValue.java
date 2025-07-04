package io.xpipe.core;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@SuperBuilder
@EqualsAndHashCode
public abstract class EncryptedSecretValue implements SecretValue {

    String encryptedValue;

    public EncryptedSecretValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public EncryptedSecretValue(byte[] b) {
        encryptedValue = SecretValue.toBase64e(encrypt(b));
    }

    public EncryptedSecretValue(char[] c) {
        var utf8 = StandardCharsets.UTF_8.encode(CharBuffer.wrap(c));
        var bytes = new byte[utf8.limit()];
        utf8.get(bytes);
        encryptedValue = SecretValue.toBase64e(encrypt(bytes));
    }

    @Override
    public String toString() {
        return "<encrypted secret>";
    }

    @Override
    public byte[] getSecretRaw() {
        try {
            var bytes = SecretValue.fromBase64e(getEncryptedValue());
            bytes = decrypt(bytes);
            return bytes;
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    @Override
    public char[] getSecret() {
        try {
            var bytes = SecretValue.fromBase64e(getEncryptedValue());
            bytes = decrypt(bytes);
            var charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
            var chars = new char[charBuffer.limit()];
            charBuffer.get(chars);
            return chars;
        } catch (Exception ex) {
            return new char[0];
        }
    }

    public byte[] encrypt(byte[] c) {
        throw new UnsupportedOperationException();
    }

    public byte[] decrypt(byte[] c) {
        throw new UnsupportedOperationException();
    }
}
