/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

public interface InstanceIdentity {
	int $$_hibernate_getInstanceId();
	void $$_hibernate_setInstanceId(int instanceId);
}
