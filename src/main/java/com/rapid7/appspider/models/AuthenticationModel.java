/*
 * Copyright © 2003 - 2021 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider.models;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Storage class containing all the information need to authenticate
 */
public class AuthenticationModel {

    private final String username;
    private final String password;
    private final Optional<String> clientId;

    /**
     * instantiates a new instance of the {@code AuthenticationModel} class with no client Id
     */
    public AuthenticationModel(String username, String password) {
        this(username, password, Optional.empty());
    }

    /**
     * instantiates a new instance of the {@code AuthenticationModel} class ensuring that
     * username and password are both non-null and non-empty
     */
    public AuthenticationModel(String username, String password, Optional<String> clientId) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.username = username;
        this.password = password;
        this.clientId = clientId;
    }

    /**
     * gets the password value
     * @return password as a {@code Secret}
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get the username value
     * @return username as a String
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns whether this object contains a clientId or not
     * 
     * <p>
     * should be called prior to {@code getClientId} to ensure a value is present,
     * otherwise an exception may occur
     * </p>
     * @return
     */
    public boolean hasClientId() {
        return clientId.isPresent();
    }

    /**
     * Gets the client id if present; otherwise throws a {@code NoSuchElementException}
     * @return the client Id if present
     * @throws NoSuchElementException if this instance does not have a clientId
     */
    public String getClientId() throws NoSuchElementException {
        return clientId.orElseThrow(NoSuchElementException::new);
    }


}
