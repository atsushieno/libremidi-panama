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

<del>
Currently I do not run jextract at build time because it cannot generate
valid code for unnamed union within unnamed struct (which is actually used
in `libremidi-c.h`). They are manually rewritten.
</del>

We run `jextract.libraries()` which I think runs jextract at run time, but
I did not verify precisely.

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
