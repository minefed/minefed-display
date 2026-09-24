package team.minefed.mods.display.client.renderers;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

import java.nio.file.Files;
import java.nio.file.Path;

/** Applies the same access fixes and mappings used by Fabric before the check. */
public final class DisplayOcclusionTestLauncher {
    public static void main(String[] args) throws Exception {
        // Gradle shortens long Windows classpaths to a manifest JAR; Fabric's
        // discovery needs the expanded paths rather than that wrapper JAR.
        System.setProperty("java.class.path", Files.readString(Path.of(System.getProperty("minefed.testClasspath"))));
        ClassLoader loader = new Knot(EnvType.CLIENT).init(new String[0]);
        Class.forName("team.minefed.mods.display.client.renderers.DisplayOcclusionTest", true, loader)
                .getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
