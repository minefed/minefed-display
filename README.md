# minefed-display

Minefed Display provides monitor blocks with web content rendered by MCEF.

The Minecraft 1.20.4 Fabric server only needs this mod and Fabric API. MCEF is
used by the client renderer and is not required on a dedicated server.

To view web content, install the Fabric release of MCEF 2.1.6 or later for
Minecraft 1.20.4 on the client. Without MCEF, display blocks and their URL/size
settings remain available, but web rendering is disabled and the client log
explains the missing dependency. Older MCEF versions are rejected by Fabric
Loader instead of being used with an incompatible browser API.

Display content and the custom display bezel use world render layers with depth
testing and depth writes. Opaque blocks in front of a screen hide it, including
when another block entity was rendered immediately before the screen.

Passive world displays ignore page cursor changes. MCEF's default cursor listener
changes the Minecraft window's cursor mode, which could release the captured mouse
or request an invalid platform cursor while a page loads or changes. Both monitor
types now install their passive listener before native browser creation. Browser
initialization, navigation and resizing are unchanged, and interactive browsers
created by other mods retain their own cursor handling.

Run `./gradlew build` for the normal build, including the headless
`verifyDisplayBezelTrace` regression. It invokes the public production renderer
and compares its complete ordered layer/vertex/UV stream against the preserved
pre-optimization renderer, including raw float bits, the final pending layer,
and matrix-stack restoration. Coverage includes all sizes 0–40, larger NBT-loaded
sizes, nonpositive dimensions, all four horizontal facings, and transformed parent
matrices. Small direct-emitter probes also cover integer/float precision boundaries
without attempting to render impractically large full grids.

The build also runs `verifyDisplayCursor`. It checks both compiled renderers use
the passive browser factory, confirms the cursor side effect in the pinned MCEF
bytecode, and runs the production factory with test-only browser boundary fixtures.
The fixtures deliver cursor events during creation, resize and navigation, before
checking every MCEF cursor ID with captured, normal and hidden mouse modes. They
also preserve initialization errors and demonstrate that a separate interactive
browser still handles its own cursor. This check does not launch Chromium or a game.

On a desktop with an OpenGL driver,
`./gradlew verifyDisplayOcclusion` additionally creates a hidden GLFW window and
checks the real depth buffer: a wall occludes a rear display, a front display
remains visible, and the display occludes geometry drawn behind it later. It also
reproduces the old missing-depth-test failure as a control. It also compares the
entire baseline/production framebuffer using an alpha-texture resource fixture,
including transparent, partially transparent, and opaque texels. This optional check
does not start Minecraft or MCEF and is not part of the headless build.

For an in-game check on Minecraft 1.20.4 Fabric with MCEF, load content on both
monitor types, place an opaque wall between the camera and each screen, and view
them from all four horizontal facings. Check a custom display spanning several
blocks, its bezel with no URL, and overlapping displays. Repeat with the client
modpack's rendering mods and graphics modes; the isolated OpenGL check does not
cover those integrations.

For the cursor fix, stay in first-person gameplay while both monitor types load,
reload or navigate their web pages. The mouse must remain captured; opening a menu
or switching applications must still release it normally. The complete client pack
and native CEF callbacks require this additional in-game check.
