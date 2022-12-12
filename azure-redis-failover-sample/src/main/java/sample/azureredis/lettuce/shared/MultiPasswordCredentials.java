package sample.azureredis.lettuce.shared;

import io.lettuce.core.*;

public class MultiPasswordCredentials implements RedisCredentials {
    private final char[] password;
    private final char[] secondaryPassword;
    private char[] currentPassword;

    public MultiPasswordCredentials(String password, String secondaryPassword) {
        this.password = password.toCharArray();
        this.secondaryPassword = secondaryPassword.toCharArray();
        this.currentPassword = this.password;
    }

    public synchronized void swapPassword() {
        if (this.currentPassword == this.password) {
            this.currentPassword = this.secondaryPassword;
        }
        else {
            this.currentPassword = this.password;
        }
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public boolean hasUsername() {
        return false;
    }

    @Override
    public char[] getPassword() {
        return currentPassword;
    }

    @Override
    public boolean hasPassword() {
        return true;
    }
}