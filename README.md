# minefed-display

Minefed Display provides monitor blocks with web content rendered by MCEF.

The Minecraft 1.20.4 Fabric server only needs this mod and Fabric API. MCEF is
used by the client renderer and is not required on a dedicated server.

To view web content, install the Fabric release of MCEF 2.1.6 or later for
Minecraft 1.20.4 on the client. Without MCEF, display blocks and their URL/size
settings remain available, but web rendering is disabled and the client log
explains the missing dependency. Older MCEF versions are rejected by Fabric
Loader instead of being used with an incompatible browser API.
