/*
 *
 *
 * Copyright (C) 2008 Pingtel Corp., certain elements licensed under a Contributor Agreement.
 * Contributors retain copyright to elements licensed under a Contributor Agreement.
 * Licensed to the User under the LGPL license.
 *
 * $
 */

package org.sipfoundry.sipxconfig.admin.update;

import java.io.Serializable;

import org.sipfoundry.sipxconfig.admin.WaitingListener;

/**
 * Runs a package update completely synchronously, while the user is presented with the
 * "waiting page".
 */
public class SynchronousPackageUpdate implements Serializable, WaitingListener {

    private final PackageUpdateManager m_packageUpdateManager;

    public SynchronousPackageUpdate(PackageUpdateManager packageUpdateManager) {
        m_packageUpdateManager = packageUpdateManager;
    }

    public void afterResponseSent() {
        m_packageUpdateManager.installUpdates();
    }
}