package teleport;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.lang.reflect.Method;

/**
 * Waits for game classes to appear, then loads the teleport window
 * through the same loader that defined them. No game references here.
 */
public final class TeleportBootstrap {
    private static volatile boolean started;
    private static volatile boolean fired;
    private static volatile String savedArgs;
    private static volatile String logPath = "teleport.log";

    private TeleportBootstrap() { }

    public static void start(String arguments, java.lang.instrument.Instrumentation instrumentation) {
        if (started) return;
        synchronized (TeleportBootstrap.class) {
            if (started) return;
            started = true;
            savedArgs = arguments;
            resolveLogPath();
            note("BOOTSTRAP_START user.dir=" + safeUserDir());
            try {
                instrumentation.addTransformer(new GameClassWatcher());
            } catch (Throwable failure) {
                note("BOOTSTRAP_TRANSFORMER_ERROR " + failure);
            }
        }
    }

    private static final class GameClassWatcher implements ClassFileTransformer {
        private static final String[] TRIGGERS = {
            "players/Crew", "rnrscr/Helper", "rnrcore/eng"
        };

        public byte[] transform(ClassLoader loader, String className,
                Class redefiningClass, ProtectionDomain domain, byte[] bytes)
                throws IllegalClassFormatException {
            if (className == null || fired) return null;
            boolean hit = false;
            for (int i = 0; i < TRIGGERS.length; ++i) {
                if (className.equals(TRIGGERS[i])) { hit = true; break; }
            }
            if (!hit) return null;
            synchronized (TeleportBootstrap.class) {
                if (fired) return null;
                fired = true;
            }
            final ClassLoader gameLoader = loader;
            final String seen = className;
            Thread starter = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    try {
                        Class window;
                        if (gameLoader != null) {
                            window = Class.forName("teleport.TeleportWindow", true, gameLoader);
                        } else {
                            window = Class.forName("teleport.TeleportWindow");
                        }
                        Method m = window.getMethod("start", new Class[] { String.class });
                        m.invoke(null, new Object[] { savedArgs });
                        note("GUI_STARTED via=" + seen);
                    } catch (Throwable failure) {
                        note("GUI_START_ERROR " + failure);
                        Throwable cause = failure.getCause();
                        if (cause != null) note("GUI_START_CAUSE " + cause);
                    }
                }
            }, "Teleport-starter");
            starter.setDaemon(true);
            starter.start();
            return null;
        }
    }

    private static String safeUserDir() {
        try {
            return System.getProperty("user.dir");
        } catch (Throwable t) {
            return "?";
        }
    }

    private static void resolveLogPath() {
        logPath = resolve("teleport.log");
    }

    /** Writes UTF-8 explicitly: the game JVM default charset is not UTF-8. */
    static String resolve(String configured) {
        try {
            File cwd = new File(System.getProperty("user.dir"));
            File dir = new File(cwd, "Tools/Teleport");
            if (!dir.isDirectory()) dir = new File(cwd, "../Tools/Teleport");
            if (!dir.isDirectory()) dir = cwd;
            File direct = new File(dir, configured);
            File parent = direct.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            append(direct);
            return direct.getPath();
        } catch (Throwable ignored) { }
        return configured;
    }

    private static void append(File target) throws java.io.IOException {
        java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(target, true), "UTF-8");
        try { writer.write(""); } finally { writer.close(); }
    }

    static synchronized void note(String line) {
        try {
            System.err.println("Teleport: " + line);
        } catch (Throwable ignored) { }
        try {
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(logPath, true), "UTF-8");
            try { writer.write(new java.util.Date() + " " + line + "\r\n"); }
            finally { writer.close(); }
        } catch (Throwable ignored) { }
    }
}
