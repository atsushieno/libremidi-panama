import dev.atsushieno.panama.libremidi.libremidi_api_configuration
import dev.atsushieno.panama.libremidi.libremidi_c_h
import dev.atsushieno.panama.libremidi.libremidi_observer_configuration
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import dev.atsushieno.panama.libremidi.libremidi_c_h as library
import dev.atsushieno.panama.libremidi.`libremidi_midi_observer_enumerate_input_ports$x0` as InputEnumerationCallback
import dev.atsushieno.panama.libremidi.`libremidi_midi_observer_enumerate_output_ports$x0` as OutputEnumerationCallback

// copied from libremidi-javacpp
enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
}
object API {
    val Unspecified = library.UNSPECIFIED()
    val CoreMidi = library.COREMIDI()
    val AlsaSeq = library.ALSA_SEQ()
    val AlsaRaw = library.ALSA_RAW()
    val JackMidi = library.JACK_MIDI()
    val WindowsMM = library.WINDOWS_MM()
    val WindowsUwp = library.WINDOWS_UWP()
    val WebMidi = library.WEBMIDI() // ktmidi-jvm-desktop wouldn't run on Web platform though
    val PipeWire = library.PIPEWIRE()
    val AlsaSeqUmp = library.ALSA_SEQ_UMP()
    val AlsaRawUmp = library.ALSA_RAW_UMP()
    val CoreMidiUmp = library.COREMIDI_UMP()
    val WindowsMidiServices = library.WINDOWS_MIDI_SERVICES()
    val Dummy = library.DUMMY()

    fun getPlatformDefault(platform: DesktopPlatform, transportProtocol: Int) =
        when(platform) {
            DesktopPlatform.Linux ->
                if (transportProtocol == 2) AlsaSeqUmp
                else AlsaSeq
            DesktopPlatform.Windows ->
                if (transportProtocol == 2) WindowsMidiServices
                else WindowsUwp
            DesktopPlatform.MacOS ->
                if (transportProtocol == 2) CoreMidiUmp
                else CoreMidi
        }
}
private fun guessPlatform(): DesktopPlatform {
    val os = System.getProperty("os.name")
    return when {
        os.startsWith("windows", true) -> DesktopPlatform.Windows
        os.startsWith("mac", true) -> DesktopPlatform.MacOS
        else -> DesktopPlatform.Linux
    }
}


fun main(args: Array<String>) {

    val api = API.getPlatformDefault(guessPlatform(), 1)

    val arena = Arena.global()
    val obsConf = libremidi_observer_configuration.allocate(arena)
    library.libremidi_midi_observer_configuration_init(obsConf)
    libremidi_observer_configuration.track_virtual(obsConf, true)
    libremidi_observer_configuration.track_any(obsConf, true)

    val apiConf = libremidi_api_configuration.allocate(arena)
    library.libremidi_midi_api_configuration_init(apiConf)
    libremidi_api_configuration.api(apiConf, api)

    val observer = arena.allocate(ValueLayout.ADDRESS)
    library.libremidi_midi_observer_new(obsConf, apiConf, observer)

    val commonProc = { port: MemorySegment, func: (MemorySegment, MemorySegment, MemorySegment)->Int ->
        val nameBuf = arena.allocate(1024)
        val sizePtr = arena.allocateFrom(ValueLayout.ADDRESS, MemorySegment.NULL)
        println(func(port, nameBuf, sizePtr))
        val size = sizePtr[ValueLayout.JAVA_LONG, 0]
        println(size)
        println(nameBuf.asByteBuffer().array().decodeToString())
    }

    val inGetName = { port: MemorySegment, ms1: MemorySegment, ms2: MemorySegment -> libremidi_c_h.libremidi_midi_in_port_name(port, ms1, ms2) }
    val inProc = InputEnumerationCallback.allocate({ _, port -> commonProc(port, inGetName) }, arena)
    library.libremidi_midi_observer_enumerate_input_ports(observer, null, inProc)

    val outGetName = { port: MemorySegment, ms1: MemorySegment, ms2: MemorySegment -> libremidi_c_h.libremidi_midi_out_port_name(port, ms1, ms2) }
    val outProc = OutputEnumerationCallback.allocate({ _, port -> commonProc(port, outGetName) }, arena)
    library.libremidi_midi_observer_enumerate_output_ports(observer, null, outProc)
}