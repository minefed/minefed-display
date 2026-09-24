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

Run `./gradlew build` for the normal build. On a desktop with an OpenGL driver,
`./gradlew verifyDisplayOcclusion` additionally creates a hidden GLFW window and
checks the real depth buffer: a wall occludes a rear display, a front display
remains visible, and the display occludes geometry drawn behind it later. It also
reproduces the old missing-depth-test failure as a control. This optional check
does not start Minecraft or MCEF and is not part of the headless build.

For an in-game check on Minecraft 1.20.4 Fabric with MCEF, load content on both
monitor types, place an opaque wall between the camera and each screen, and view
them from all four horizontal facings. Check a custom display spanning several
blocks, its bezel with no URL, and overlapping displays. Repeat with the client
modpack's rendering mods and graphics modes; the isolated OpenGL check does not
cover those integrations.
