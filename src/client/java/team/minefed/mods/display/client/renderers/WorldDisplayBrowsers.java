package team.minefed.mods.display.client.renderers;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;

/** Creates passive world displays without giving their pages control of the game cursor. */
final class WorldDisplayBrowsers {
    private WorldDisplayBrowsers() {
    }

    static MCEFBrowser create(String url) {
        // MCEF's default cursor listener changes the game window to CURSOR_NORMAL/HIDDEN,
        // overriding Minecraft's CURSOR_DISABLED mouse lock even for a passive world display.
        // Install our listener before native creation can deliver the first page cursor event.
        // getClient() retains MCEF's initialization check (and the optional lazy-init hook).
        MCEFBrowser browser = new MCEFBrowser(MCEF.getClient(), url, false);
        browser.setCursorChangeListener(cursorType -> { });
        browser.setCloseAllowed();
        browser.createImmediately();
        return browser;
    }
}
