package dev.atsushieno.panama.libremidi;

import com.fizzed.jne.JNE;

public class Initializer {
    public static void initialize() {
        JNE.loadLibrary("libremidi");
    }
}
