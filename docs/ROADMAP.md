# Roadmap

Where the port of Octo's own app features into Rhythm stands. Octo's apps are the reference
([winters27/octo-player](https://github.com/winters27/octo-player)); a feature is done when it works on a real phone.

## Done

| Feature | Notes |
|---|---|
| Preview detection and labelling | `isExternal` plus the shape Octo gives previews; "Not in library" in search |
| Heart a preview = get it | the landed song is hearted automatically |
| Rotating offline cache for Octo's radios, charts and mixes | follows what the server lists; Liked Songs excluded |
| Requests card | what Octo is fetching, with progress |
| Stations row and Start radio | stations from Octo's radio playlists |
| Radio mixer and tuning | port of octo-player's mixer; four dials |
| Find another copy | pick a different Soulseek/Lidarr copy; needs "Library actions" on the server |
| Imports card | lists, fetch-missing switch, add a link |
| Whole albums | the album heart stars by `albumId`, which Octo reads as "get the whole album" |
| Offline scrobble queue | plays made offline are sent later with their real time |
| Cover placeholder guard | a server's placeholder picture is not cached as a cover |

## Next

- **Import from a file or pasted text** (`importFile`, `importText`) and the services list (`getImportServices`).
- **Import list detail**: per-song state, ask for or skip single songs, keep a list as a playlist, remove a list.
- **Charts**: choose which country's charts become playlists (`getCharts`); artist top songs (`getArtistTopSongs`).
- **Live radio stations** (the stations the server keeps and ones near you).
- **Downloads drawer parity**: retry, pause and resume the trickle, clear finished.
- **Album pages for albums not in the library**: show the tracks, mark the ones already owned, one press adds the rest.
- **Lyrics by song id** (`getLyricsBySongId`).

## Later / depends on upstream

- Become a real extension once Rhythm has a source-hook extension API; until then this repo carries patches.
- Offer the generic pieces (offline playlists, the scrobble queue) to Rhythm as upstream pull requests.
