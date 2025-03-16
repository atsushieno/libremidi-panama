# libremidi-panama

It is a Panama binding for [libremidi](https://github.com/celtera/libremidi).

It is primarily intended to be used by [ktmidi](https://github.com/atsushieno/ktmidi/).

It is still experimental as I am rather just evaluating Panama to see if it is
viable enough to handle libremidi and brings in acceptable development
experience. Once I get it working in ktmidi, it will be used as a replacement
to libremidi-javacpp as a better alternative. Unlike libremidi-javacpp, it can
resolve native libraries without hassle.
We use [fizzed/jne](https://github.com/fizzed/jne) to achieve that.

## Bundling libremidi into Jar

JNE can embed native dependency libraries into the library Jar, and you can load it
using JNE API:

```
JNE.loadLibrary("libremidi");
```

This repository does NOT bundle `(lib)libremidi(.dll/.dylib/.so)` binaries, but
our CI build is configured to build libremidi from its submodule and put the
resulting binaries into
`libremidi-panama/src/main/resources/jne/{platform}/{ABI}/` .
When they are released via Maven Central, it will contain those prebuilt binaries
for the supported platforms (as of writing this, Windows x86_64, Linux x86_64, and
Darwin arm64, and Darwin x86_64 will be built).

## jextract issue and workarounds

Currently I do not run jextract at build time because

(1) we do not have to iterate that part every time (and from what I learned
from KSP1 Gradle brings in more annoyance with respect to build script
backward incompatibility), and
(2) we make changes to libremidi-c.h which is kind of cautious (jextract
generates invalid code for unnamed union and struct that are actually used
in `libremidi-c.h`).
(3) we also fix some generated code parts (`findOrThrow`).

When (and only when) we run `jextract`, we apply `libremidi-jextractable.patch`
in `external/libremidi` submodule.
The patch should not affect ABI compatibility so we do not apply them when
building `(lib)libremidi.(so/dylib/dll)`.

There are some command line instructions to build and run `jextract` tool from
the submodule sources, in `libremidi-panama/build.gradle`.

## Licenses

libremidi-panama is released under the MIT license.

Note that apps that use libremidi-paname will contain libraries that are
released under the Apache V2 License.

libremidi is released under the BSD 2-clause license, yet it includes

- RtMidi which is released under the (modified) MIT license
- ModernMIDI which is released under the BSD 2-clause license, which includes
  - LabMidi which is released under the BSD 3-clause license
  - midifile which is released under the BSD 2-clause license
  - ofxmidi which is released under the 3-clause BSD license
- fizzed/jne is released under the Apache V2 License.
