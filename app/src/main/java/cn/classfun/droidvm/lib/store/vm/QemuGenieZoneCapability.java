// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright DroidVM contributors
// Additional permissions apply; see ADDITIONAL-PERMISSIONS in the repository root.
package cn.classfun.droidvm.lib.store.vm;

import static cn.classfun.droidvm.lib.utils.AssetUtils.getPrebuiltBinaryPath;
import static cn.classfun.droidvm.lib.utils.FileUtils.shellCheckExists;
import static cn.classfun.droidvm.lib.utils.RunUtils.runListQuiet;

import androidx.annotation.NonNull;

public final class QemuGenieZoneCapability {
    public static final String DEVICE = "/dev/gzvm";
    public static final String ACCELERATOR = "gzvm";

    private QemuGenieZoneCapability() {
    }

    public enum Status {
        AVAILABLE,
        DEVICE_NODE_ABSENT,
        DEVICE_NODE_INACCESSIBLE,
        QEMU_BINARY_ABSENT,
        QEMU_ACCELERATOR_MISSING,
        QEMU_ABI_MISMATCH
    }

    @NonNull
    public static String describe(@NonNull Status status) {
        switch (status) {
            case AVAILABLE:
                return "available";
            case DEVICE_NODE_ABSENT:
                return "/dev/gzvm is missing";
            case DEVICE_NODE_INACCESSIBLE:
                return "/dev/gzvm exists but is not readable";
            case QEMU_BINARY_ABSENT:
                return "QEMU is not installed; run the setup extraction step";
            case QEMU_ACCELERATOR_MISSING:
                return "installed QEMU does not advertise the gzvm accelerator";
            case QEMU_ABI_MISMATCH:
                return "installed QEMU cannot execute on this device ABI";
            default:
                throw new AssertionError(status);
        }
    }

    public static boolean acceleratorIsAvailable(@NonNull String acceleratorHelp) {
        for (var line : acceleratorHelp.toLowerCase().split("\\R")) {
            var token = line.trim();
            if (token.equals(ACCELERATOR)
                || token.startsWith(ACCELERATOR + " ")
                || token.startsWith(ACCELERATOR + "\t")) return true;
        }
        return false;
    }

    public static boolean preflight(boolean deviceExists, boolean acceleratorExists) {
        return deviceExists && acceleratorExists;
    }

    public static Status classify(
        boolean deviceExists,
        boolean deviceAccessible,
        boolean binaryExists,
        boolean commandSucceeded,
        boolean acceleratorExists,
        @NonNull String commandOutput
    ) {
        if (!deviceExists) return Status.DEVICE_NODE_ABSENT;
        if (!deviceAccessible) return Status.DEVICE_NODE_INACCESSIBLE;
        if (!binaryExists) return Status.QEMU_BINARY_ABSENT;
        if (!commandSucceeded) {
            var output = commandOutput.toLowerCase();
            if (output.contains("exec format")
                || output.contains("bad elf")
                || output.contains("wrong architecture")
                || output.contains("not executable")) return Status.QEMU_ABI_MISMATCH;
            return Status.QEMU_ACCELERATOR_MISSING;
        }
        return acceleratorExists ? Status.AVAILABLE : Status.QEMU_ACCELERATOR_MISSING;
    }

    public static boolean isAvailable() {
        return diagnose() == Status.AVAILABLE;
    }

    @NonNull
    public static Status diagnose() {
        boolean deviceExists = shellCheckExists(DEVICE);
        if (!deviceExists) return Status.DEVICE_NODE_ABSENT;
        boolean deviceAccessible = runListQuiet("test", "-r", DEVICE).isSuccess();
        var binary = getPrebuiltBinaryPath("qemu-system-aarch64");
        boolean binaryExists = shellCheckExists(binary);
        if (!binaryExists)
            return classify(true, deviceAccessible, false, false, false, "");
        try {
            var result = runListQuiet(binary, "-accel", "help");
            var help = result.getOutString() + "\n" + result.getErrString();
            return classify(
                true, deviceAccessible, true, result.isSuccess(),
                acceleratorIsAvailable(help), help);
        } catch (Throwable t) {
            return classify(true, deviceAccessible, true, false, false, t.toString());
        }
    }
}
