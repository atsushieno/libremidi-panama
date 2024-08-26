# libremidi-panama

It is a Panama binding for [libremidi](https://github.com/celtera/libremidi).

It is primarily intended to be used by [ktmidi](https://github.com/atsushieno/ktmidi/).

It is still experimental as I am rather just evaluating Panama to see if it is
viable enough to handle libremidi and brings in acceptable development
experience as I already have libremidi-javacpp (which is also under evaluation)
and basically prefer JavaCPP as it can resolve native libraries without hassle.

Currently it is at unverified state as I'm only getting UnsatisfiedLinkError
for missing `liblibremidi.so`/`.dylib`/`.dll` as it is not automatically (built
and) resolved.
If apps based on panama and jextract can resolve this annoyance, then I would
probably pick this out.

## jextract issue and workarounds

Currently I do not run jextract at build time because it cannot generate
valid code for unnamed union within unnamed struct (which is actually used
in `libremidi-c.h`). They are manually rewritten.

## Licenses

libremidi-panama is released under the MIT license.

libremidi is released under the BSD 2-clause license, yet it includes

- RtMidi which is released under the (modified) MIT license
- ModernMIDI which is released under the BSD 2-clause license, which includes
  - LabMidi which is released under the BSD 3-clause license
  - midifile which is released under the BSD 2-clause license
  - ofxmidi which is released under the 3-clause BSD license

