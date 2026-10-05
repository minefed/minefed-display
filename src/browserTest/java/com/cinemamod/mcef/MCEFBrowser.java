package com.cinemamod.mcef;

import com.cinemamod.mcef.listeners.MCEFCursorChangeListener;

/**
 * Browser boundary fixture, without Minecraft, Chromium, or a native window. The default
 * callback models MCEF 2.1.6 changing GLFW cursor mode; native creation fires it immediately
 * so installing a replacement after createImmediately() cannot accidentally pass this test.
 */
public final class MCEFBrowser {
    public static final int DISABLED = 212995;
    public static final int NORMAL = 212993;
    public static final int HIDDEN = 212994;
    public static int windowCursorMode = DISABLED;
    public static int cursorWrites;
    public final MCEFClient client;
    public final boolean transparent;
    public String url;
    public boolean closeAllowed;
    public int creations;
    public int width;
    public int height;
    private MCEFCursorChangeListener listener = cursorType -> {
        windowCursorMode = cursorType == 43 ? HIDDEN : NORMAL;
        cursorWrites++;
    };

    public MCEFBrowser(MCEFClient client, String url, boolean transparent) {
        this.client = client;
        this.url = url;
        this.transparent = transparent;
    }

    public void setCursorChangeListener(MCEFCursorChangeListener listener) {
        this.listener = listener;
    }

    public void setCloseAllowed() {
        closeAllowed = true;
    }

    public void createImmediately() {
        if (!closeAllowed) throw new AssertionError("browser must retain MCEF close behavior");
        creations++;
        emitCursor(0);
    }

    public void emitCursor(int cursorType) {
        listener.onCursorChange(cursorType);
    }

    public void loadURL(String url) {
        this.url = url;
        emitCursor(43);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        emitCursor(2);
    }
}
