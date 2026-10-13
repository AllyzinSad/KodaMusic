# Koda Music Android 3.11.1

Based on the Android final 3.11.0 branch (version code 31104). Package remains com.kodamusic; new version code is 31110.

- Anime-style icon geometry with white audio bars and a purple K. Density-specific, adaptive, notification and monochrome assets are included; the old mark bitmap was removed.
- Purple/charcoal mobile Home, real featured track, solid card surfaces and larger mini-player touch targets. Existing low-cost image cache and reduced-effects defaults are retained.
- Track IDs are read only from the item's own fields; album/artist/playlist navigation is kept separate.
- Alternate sources cannot substitute another named artist based only on a similar title and runtime. Old alternate-source cache entries are invalidated once; downloaded tracks are not removed by that migration.
- Playback engine, audio quality controls, downloads and offline playback are preserved.

The dedicated GitHub Actions workflow builds a universal unsigned production APK and a source archive. Final distribution APKs must be signed with the existing release key. Never commit the keystore or passwords.

Local verification: Kotlin 2.3.20 / JVM 17 compilation of models, identity reader, Innertube parser and matcher; 32 JUnit tests passed, including search paging, playlist ownership and account channels. CI also runs SourcesTest and compiles the Android application. Verify startup, icon masks, offline playback and the previously affected songs on a real device.
