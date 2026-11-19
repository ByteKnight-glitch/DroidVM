// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright DroidVM contributors
// Additional permissions apply; see ADDITIONAL-PERMISSIONS in the repository root.
package cn.classfun.droidvm.lib.store.vm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QemuGenieZoneCapabilityTest {
    @Test
    public void qemuAndCrosvmBothRecognizeGenieZone() {
        assertTrue(VMHypervisor.isBackendSupported(VMBackend.QEMU, VMHypervisor.GENIEZONE));
        assertTrue(VMHypervisor.isBackendSupported(VMBackend.CROSVM, VMHypervisor.GENIEZONE));
    }

    @Test
    public void acceleratorHelpMustAdvertiseGzvm() {
        assertTrue(QemuGenieZoneCapability.acceleratorIsAvailable(
            "Accelerators supported in QEMU binary:\ngzvm\nkvm\ntcg"));
        assertFalse(QemuGenieZoneCapability.acceleratorIsAvailable(
            "Accelerators supported in QEMU binary:\ngunyah\nkvm\ntcg"));
        assertFalse(QemuGenieZoneCapability.acceleratorIsAvailable(""));
    }

    @Test
    public void preflightRequiresNodeAndAccelerator() {
        assertTrue(QemuGenieZoneCapability.preflight(true, true));
        assertFalse(QemuGenieZoneCapability.preflight(false, true));
        assertFalse(QemuGenieZoneCapability.preflight(true, false));
        assertFalse(QemuGenieZoneCapability.preflight(false, false));
    }

    @Test
    public void diagnoseSeparatesDeviceAndQemuFailures() {
        assertEquals(QemuGenieZoneCapability.Status.DEVICE_NODE_ABSENT,
            QemuGenieZoneCapability.classify(false, false, false, false, false, ""));
        assertEquals(QemuGenieZoneCapability.Status.DEVICE_NODE_INACCESSIBLE,
            QemuGenieZoneCapability.classify(true, false, true, false, false, ""));
        assertEquals(QemuGenieZoneCapability.Status.QEMU_BINARY_ABSENT,
            QemuGenieZoneCapability.classify(true, true, false, false, false, ""));
        assertEquals(QemuGenieZoneCapability.Status.QEMU_ACCELERATOR_MISSING,
            QemuGenieZoneCapability.classify(true, true, true, true, false, "kvm\ntcg"));
        assertEquals(QemuGenieZoneCapability.Status.QEMU_ABI_MISMATCH,
            QemuGenieZoneCapability.classify(true, true, true, false, false, "Exec format error"));
    }
}
