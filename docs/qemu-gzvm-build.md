# QEMU-GenieZone build and validation

The QEMU-GZVM implementation used for the SM-X930 must be built from a
pinned source revision. Do not replace DroidVM's bundled QEMU with an
unverified binary.

## Pinned source and artifact

- Source: https://github.com/IFindMe/qemu-gzvm
- Revision: `v26.8.4`
- Commit: `8eb96027d8298a4860a7f0d1a9cee0aea96d37b`
- Published ARM64 archive:
  `qemu-gzvm-v26.8.4-aarch64.tar.gz`
- Archive SHA-256:
  `d550de0bcbf6a6e328bf6b24e86aad5edabe756830a5e6b94ecd474cd38a53d2`

The source includes `linux-headers/linux/gzvm.h`, which defines the
GenieZone ioctl ABI (`GZVM_IOC_MAGIC` is `0x92`). The header is
SPDX `GPL-2.0 WITH Linux-syscall-note`; the QEMU tree is GPL-2.0 with
per-file licenses as described by its `LICENSE` and `COPYING` files.

## Reproducible build

Build on Linux x86_64 or WSL2. The upstream `build.sh` requires:

- Android NDK r30-beta2
- Meson 1.5.0 and Ninja
- CMake, Clang/LLVM tools, `patchelf`, `pkg-config`, `python3`, `zstd`,
  `curl`, `git`, `make`, `perl`, and `tar`

```sh
git clone https://github.com/IFindMe/qemu-gzvm.git
cd qemu-gzvm
git checkout 8eb96027d8298a4860a7f0d1a9cee0aea96d37b
mkdir -p /tmp/als/app/src/main
export ANDROID_NDK_ROOT=/path/to/android-ndk-r30-beta2
export alsDir=/tmp/als
./build.sh
```

The output is `build/out/qemu-gzvm/`. Keep its `lib/` directory beside
`qemu-system-aarch64`; the binary uses `$ORIGIN/lib`.

## Required verification

Before installing or selecting the binary:

```sh
sha256sum qemu-gzvm-v26.8.4-aarch64.tar.gz
llvm-readelf -h qemu-gzvm/qemu-system-aarch64
llvm-readelf -d qemu-gzvm/qemu-system-aarch64
adb push qemu-gzvm /data/local/tmp/qemu-gzvm-test
adb shell 'cd /data/local/tmp/qemu-gzvm-test &&
  LD_LIBRARY_PATH=$PWD/lib ./qemu-system-aarch64 -accel help'
```

The ELF must be AArch64, all libraries must resolve from the packaged
`lib/` directory or Android system libraries, and `-accel help` must
contain `gzvm`. On the SM-X930, also verify `/dev/gzvm` is readable by
the process. A no-disk control launch may use:

```sh
adb shell 'cd /data/local/tmp/qemu-gzvm-test &&
  LD_LIBRARY_PATH=$PWD/lib timeout 10 ./qemu-system-aarch64 \
  -M virt -accel gzvm -cpu host -smp 1 -m 128 \
  -display none -nodefaults -serial none -S'
```

This only creates a paused, disposable VM and does not open a guest disk.
The published artifact passed these checks on the connected SM-X930:
`-accel help` reported `gzvm`, and the no-disk control remained alive for
the timeout without the earlier `EFAULT`/`EINVAL` startup errors. This is
not yet evidence that Windows 11 ARM boots; guest firmware, drivers,
graphics, input, networking, and disk behavior remain unvalidated.
