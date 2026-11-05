# rhythm-octo-extension

Octo support for [Rhythm](https://github.com/cromaguy/Rhythm) (Android), as winters27's [Octo](https://github.com/winters27/octo)
server exposes it. Planned as a Rhythm extension; **today it is app code**, because Rhythm has no source-hook
extension API yet. Wire shapes for Octo's extra calls follow winters27/octo-player (GPL-3.0).

## What is here
- `src/` - the new files (client, parsers, requests card, grab tracking, preview signature, tests), at their app paths.
- `patches/` - the 7 commits (`git format-patch`) from the app that add them and hook them in
  (isExternal, preview signature, rotating radio-playlist cache, heart-to-keep, Octo client + Home card).

## Features so far
- Octo previews recognised (isExternal and a path/bitrate signature) and kept out of downloads and server playlists.
- Octo's generated radio playlists (cover `octo-radio`) kept downloaded as a rotating temporary cache; ones the server stops listing are released.
- Hearting a preview records a grab; the landed library song is hearted once it appears.
- Client for `getAcquisitions`, `findSongs`, `getFoundSongs`, `pickFoundSong`; Home card listing what Octo is fetching.

## Not yet
Radio from a song and stations, add whole albums, import, find-songs UI, charts.

Licence: GPL-3.0-or-later.
