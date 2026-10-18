package teleport;

import java.lang.instrument.Instrumentation;

/** Bootstrap agent. Touches no game classes here by design. */
public final class TeleportAgent {
    private TeleportAgent() { }

    public static void premain(String arguments, Instrumentation instrumentation) {
        TeleportBootstrap.start(arguments, instrumentation);
        System.err.println("Teleport: bootstrap active; waiting for game classes");
    }
}