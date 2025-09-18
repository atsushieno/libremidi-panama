import com.fizzed.jne.JNE;
import dev.atsushieno.panama.libremidi.libremidi_api_configuration;
import dev.atsushieno.panama.libremidi.libremidi_c_h;
import dev.atsushieno.panama.libremidi.libremidi_midi_configuration;
import dev.atsushieno.panama.libremidi.libremidi_observer_configuration;
import dev.atsushieno.panama.libremidi.libremidi_midi_observer_enumerate_input_ports$x0;
import dev.atsushieno.panama.libremidi.libremidi_midi_observer_enumerate_output_ports$x0;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

public class Driver {

    // copied from libremidi-javacpp
    enum DesktopPlatform {
        Linux,
        Windows,
        MacOS
    }

    static class API {
        static final int Unspecified = libremidi_c_h.UNSPECIFIED();
        static final int CoreMidi = libremidi_c_h.COREMIDI();
        static final int AlsaSeq = libremidi_c_h.ALSA_SEQ();
        static final int AlsaRaw = libremidi_c_h.ALSA_RAW();
        static final int JackMidi = libremidi_c_h.JACK_MIDI();
        static final int WindowsMM = libremidi_c_h.WINDOWS_MM();
        static final int WindowsUwp = libremidi_c_h.WINDOWS_UWP();
        static final int WebMidi = libremidi_c_h.WEBMIDI(); // ktmidi-jvm-desktop wouldn't run on Web platform though
        static final int PipeWire = libremidi_c_h.PIPEWIRE();
        static final int AlsaSeqUmp = libremidi_c_h.ALSA_SEQ_UMP();
        static final int AlsaRawUmp = libremidi_c_h.ALSA_RAW_UMP();
        static final int CoreMidiUmp = libremidi_c_h.COREMIDI_UMP();
        static final int WindowsMidiServices = libremidi_c_h.WINDOWS_MIDI_SERVICES();
        static final int Dummy = libremidi_c_h.DUMMY();

        static int getPlatformDefault(DesktopPlatform platform, int transportProtocol) {
            switch (platform) {
                case Linux:
                    return (transportProtocol == 2) ? AlsaSeqUmp : AlsaSeq;
                case Windows:
                    return (transportProtocol == 2) ? WindowsMidiServices : WindowsUwp;
                case MacOS:
                    return (transportProtocol == 2) ? CoreMidiUmp : CoreMidi;
                default:
                    throw new IllegalArgumentException("Unknown platform: " + platform);
            }
        }
    }

    private static DesktopPlatform guessPlatform() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("windows")) {
            return DesktopPlatform.Windows;
        } else if (os.toLowerCase().startsWith("mac")) {
            return DesktopPlatform.MacOS;
        } else {
            return DesktopPlatform.Linux;
        }
    }

    @FunctionalInterface
    interface PortNameGetter {
        int getName(MemorySegment port, MemorySegment nameBuf, MemorySegment size);
    }

    private static void processPort(Arena arena, MemorySegment port, PortNameGetter getName) {
        var namePtr = arena.allocate(ValueLayout.ADDRESS_UNALIGNED);
        var sizePtr = arena.allocate(ValueLayout.JAVA_LONG);
        getName.getName(port, namePtr, sizePtr);
        long size = sizePtr.get(ValueLayout.JAVA_LONG, 0);
        var nameSrc = namePtr.get(ValueLayout.ADDRESS_UNALIGNED, 0);
        var nameBuf = arena.allocate(size);
        MemorySegment.copy(nameSrc.reinterpret(size), 0, nameBuf, 0, size);
        System.out.println(StandardCharsets.UTF_8.decode(nameBuf.asByteBuffer()));
    }

    public static void main(String[] args) {
        System.getProperties().setProperty("jextract.trace.downcalls", "true");
        JNE.loadLibrary("libremidi");

        int api = API.getPlatformDefault(guessPlatform(), 1);

        var arena = Arena.global();
        var obsConf = libremidi_observer_configuration.allocate(arena);
        libremidi_c_h.libremidi_midi_observer_configuration_init(obsConf);
        libremidi_observer_configuration.track_virtual(obsConf, true);
        libremidi_observer_configuration.track_any(obsConf, true);

        var apiConf = libremidi_api_configuration.allocate(arena);
        libremidi_c_h.libremidi_midi_api_configuration_init(apiConf);
        libremidi_api_configuration.api(apiConf, api);

        var observerPtr = arena.allocate(ValueLayout.ADDRESS_UNALIGNED);
        System.out.println("libremidi_midi_observer_new: " + libremidi_c_h.libremidi_midi_observer_new(obsConf, apiConf, observerPtr));
        var observer = observerPtr.get(ValueLayout.ADDRESS_UNALIGNED, 0);

        // Input ports enumeration
        PortNameGetter inPortGetName = (port, nameBuf, size) ->
            libremidi_c_h.libremidi_midi_in_port_name(port, nameBuf, size);

        var inProc = libremidi_midi_observer_enumerate_input_ports$x0.allocate((ctx, port) -> {
            processPort(arena, port, inPortGetName);
        }, arena);

        libremidi_c_h.libremidi_midi_observer_enumerate_input_ports(observer, MemorySegment.NULL, inProc);

        // Output ports enumeration
        PortNameGetter outPortGetName = (port, nameBuf, size) ->
            libremidi_c_h.libremidi_midi_out_port_name(port, nameBuf, size);

        var outProc = libremidi_midi_observer_enumerate_output_ports$x0.allocate((ctx, port) -> {
            processPort(arena, port, outPortGetName);
        }, arena);

        libremidi_c_h.libremidi_midi_observer_enumerate_output_ports(observer, MemorySegment.NULL, outProc);

        var conf = libremidi_midi_configuration.allocate(arena);
        libremidi_c_h.libremidi_midi_configuration_init(conf);
    }
}