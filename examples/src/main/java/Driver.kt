import com.fizzed.jne.JNE
import dev.atsushieno.panama.libremidi.libremidi_api_configuration
import dev.atsushieno.panama.libremidi.libremidi_c_h
import dev.atsushieno.panama.libremidi.libremidi_observer_configuration
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
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
    System.getProperties().setProperty("jextract.trace.downcalls", "true")
    JNE.loadLibrary("libremidi")

    val api = API.getPlatformDefault(guessPlatform(), 1)

    val arena = Arena.global()
    val obsConf = libremidi_observer_configuration.allocate(arena)
    library.libremidi_midi_observer_configuration_init(obsConf)
    libremidi_observer_configuration.track_virtual(obsConf, true)
    libremidi_observer_configuration.track_any(obsConf, true)

    val apiConf = libremidi_api_configuration.allocate(arena)
    library.libremidi_midi_api_configuration_init(apiConf)
    libremidi_api_configuration.api(apiConf, api)

    val observerPtr = arena.allocate(ValueLayout.ADDRESS_UNALIGNED)
    println("libremidi_midi_observer_new: " + library.libremidi_midi_observer_new(obsConf, apiConf, observerPtr))
    val observer = observerPtr.get(ValueLayout.ADDRESS_UNALIGNED, 0)

    val commonProc = { port: MemorySegment, getName: (observer: MemorySegment, context: MemorySegment, size: MemorySegment)->Int ->
        val namePtr = arena.allocate(ValueLayout.ADDRESS_UNALIGNED)
        val sizePtr = arena.allocate(ValueLayout.JAVA_LONG)
        getName(port, namePtr, sizePtr)
        val size = sizePtr.get(ValueLayout.JAVA_LONG, 0)
        val nameSrc = namePtr.get(ValueLayout.ADDRESS_UNALIGNED, 0)
        val nameBuf = arena.allocate(size)
        MemorySegment.copy(nameSrc.reinterpret(size), 0, nameBuf, 0, size)
        println(StandardCharsets.UTF_8.decode(nameBuf.asByteBuffer()))
    }

    val inPortGetName = { port: MemorySegment, nameBuf: MemorySegment, size: MemorySegment -> libremidi_c_h.libremidi_midi_in_port_name(port, nameBuf, size) }
    val inProc = InputEnumerationCallback.allocate({ _, port -> commonProc(port, inPortGetName) }, arena)
    library.libremidi_midi_observer_enumerate_input_ports(observer, MemorySegment.NULL, inProc)

    val outPortGetName = { port: MemorySegment, nameBuf: MemorySegment, size: MemorySegment -> libremidi_c_h.libremidi_midi_out_port_name(port, nameBuf, size) }
    val outProc = OutputEnumerationCallback.allocate({ _, port -> commonProc(port, outPortGetName) }, arena)
    library.libremidi_midi_observer_enumerate_output_ports(observer, MemorySegment.NULL, outProc)
}