# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v0.6.2] - (Feb 28, 2025)

**_Stable - requires android 5 or higher_**

## Added

- Add certificate comparison for faster checks ([159ba1c](https://github.com/aghontpi/ad-silence/commit/159ba1c))
- Add fastlane configuration ([159ba1c](https://github.com/aghontpi/ad-silence/commit/159ba1c))
- Support for Android 14 (SDK 34) ([d72783f](https://github.com/aghontpi/ad-silence/commit/d72783f))
- Support for Android 15 (SDK 35) ([f399fb3](https://github.com/aghontpi/ad-silence/commit/f399fb3))

## Fixes

- Fix Accuradio detection not working ([4c5a6c4](https://github.com/aghontpi/ad-silence/commit/4c5a6c4))

## Updates

- Update Gradle to 8.12 ([f5943a8](https://github.com/aghontpi/ad-silence/commit/f5943a8))
- Update Android build tools to 8.8.2 ([4486918](https://github.com/aghontpi/ad-silence/commit/4486918))
- Minor CI & CD improvements ([53b659d](https://github.com/aghontpi/ad-silence/commit/53b659d))

**You can get it on Google Playstore & F-Droid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'>
    <img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/>
</a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'>
    <img alt='Get it on F-Droid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/>
</a>
</p>


## [v0.6.1] - (Jan 26, 2024)

**_Stable - requires android 5 or higher_**

## Added

* add soundcloud Support thanks to @Xt3ns1s and several others,  https://github.com/aghontpi/ad-silence/pull/107, https://github.com/aghontpi/ad-silence/pull/108
* Update spotify detections, add spotify detection for "Catalan" lang and others , thanks to @bluegeekgh https://github.com/aghontpi/ad-silence/pull/106
* Add info on restricted settings, thanks to @barbe6 https://github.com/aghontpi/ad-silence/pull/117
* Handle Hibernation in https://github.com/aghontpi/ad-silence/pull/92

## Fixes

* Fix Accuradio not working on high dpi devices https://github.com/aghontpi/ad-silence/pull/102

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>




## [v0.6.0] - (Apr 2, 2023)

**_Stable - requires android 5 or higher_**

## Added

- Add support for android 13 (https://github.com/aghontpi/ad-silence/pull/86)
  - support for android 13 to disable notifications
  - update target sdks


## Updates

- Update CI (https://github.com/aghontpi/ad-silence/pull/88)

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>




## [v0.5.5] - (Jul 16,2022)

**_Stable - requires android 5 or higher_**

## Added

- Added Italian translation, thanks to @code-a1 in https://github.com/aghontpi/ad-silence/pull/46 (5e9c18f, 371198e)
- Added new way of `ad` detection for `Spotify`, thanks to @citizenserious in https://github.com/aghontpi/ad-silence/pull/64 (b25e915, cbf769a, 6bbbbe8, 58fa6f0, 0c19451)
  - for detailed infromation, refer comments in https://github.com/aghontpi/ad-silence/pull/64
- Added Keywords/triggers for `spotify`, thanks to @unseenlarks in https://github.com/aghontpi/ad-silence/pull/62 (47e5803, 2bc7ce6)

## Updates

- Update Gradle in https://github.com/aghontpi/ad-silence/pull/61 (217a666, 5ed14a7)
- Update kotlin & lib dependencies in https://github.com/aghontpi/ad-silence/pull/60 (c8cee29)

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

## [v0.5.4] - (Mar 30,2022)

**_Stable - requires android 5 or higher_**

## Updates

- fix ads not blocked in `Spotify` when the device language is not `english`, thanks to @unseenlarks & @famewolf in https://github.com/aghontpi/ad-silence/issues/29 (8f81cf0, e2a77c6)

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

## [v0.5.3] - (Mar 8,2022)

**_Stable - requires android 5 or higher_**

## Updates

- add `LiveOne` (0687498,,13bcfcd,695eb65)
- ui changes (50a9ea7, 2bfdc2d)

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

notes: `LiveOne` is not perfect, reason for this is not being `0.6.0`.

## [v0.5.2] - (Jan 22,2022)

**_Stable - requires android 5 or higher_**

### Updates

- update icon to comply with google playstore #19 (790389d, a221fff)

### Bugs

- android 5.x unmute fix, (bfbaa9a, 1751559, 89aaced) thanks to @wackydroid in #20

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

## [v0.5.0] - (Jan 17,2022)

**_Stable - requires android 5 or higher_**

### Updates

- add support for Pandora (cdc1b00, 0b9e74e, 93ad718, cb6c2f3, 82025b9)

**You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

## [v0.4.0] - (Jan 09,2022)

**_Stable - requires android 5 or higher_**

### Updates

- add support for spotify lite (11f63ef, 476ee21, e5cf0e1, 83d4937, 87ce921)

### Fixes

- bug: notification sound when updating notifications on some android devices. (899bd9c)

**Checking for app updates via github is tedious..., You can get it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

## [v0.3.0] - (Dec 19,2021)

**_Stable - requires android 5 or higher_**

### Updates

- add: support for TIDAL (bacde47, 3f85d1e, 493cd04, a28e8eb)

**Checking for app updates via github is tedious..., You can get now it on google playstore & fdroid**

<p align="left">
<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>
<a href='https://f-droid.org/packages/bluepie.ad_silence/'><img alt='Get it on Fdroid' height="72px" src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png'/></a>
</p>

## [v0.2.1] - (Dec 09,2021)

**_Stable - requires android 5 or higher_**

### Updates

- fix: block ads in Spotify-podcasts (thanks to @erawhctim in #5)

**_checking for app updates via github is tedious, thus posted it to playstore. From now, updates will be posted to both google play and github._**

<a href='https://play.google.com/store/apps/details?id=bluepie.ad_silence&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' height="72px" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a>

## [v0.2.0] - (Dec 03,2021)

**_Stable - requires android 5 or higher_**

### Added

- extend support to android 5 & up (previoius was 9 & up). (1020ddb)
- reduce build size to ~150KB

### Updates

- fix: spotify last word of ad is not muted(187b6c4)
- fix: Crash on android 12. (ec5df7e)
- update build, 0.1.1 -> 0.2.0 (459e41a, 7ce7c84)

## [v0.1.1] - (Nov 29,2021)

**_Stable - requires android 9 or higher_**

### Added

- add: spotify support(f545d83, fb777ef)
- further reduce app size to ~160KB (fe3ef59)
- additional ui (a18afb8, 996b72a, 9e8f0cb, 3ee1975, f5f27be, 49f1de5)
- add: access the app, through notification (a5ca656)
- add: fastlane configuration (00b541d, 8948070)

### Updates

- update: readme (2683404, 963ca81, dd9e7dd, 48af317, 00cca54)

## [v0.0.1] - (Nov 20,2021)

**_Stable - requires android 9 or higher_**

### Added

- add: exclude app from recents (361b7c9)
- add: check accuradio is installed (df3b115)
- add: Granting permission UI flow (a26a85d)
- add: Enable/Disable Toggle add Preference (d95166e)
- add: appIcon (d3db59d)
- add: UI App status (93afe2d)
- add: Persistent notification (0662bdd)
- add: enum to handle differnet apps (9360d85)
- Optimize app size, (down to 900kb) (adedaff)

### Update

- Update README.md (27864c4, 963ca81, ae41243)
- cleanup (a07cf7f, b4021bd, 04617b5, e9bbf7c)
- Update Notification Logic (257e6e8, 66ac45e)
- Update UI to new theme (ff9b67d)

## [0.0.0-prototype] - (Nov 09,2021)

**_Full working prototype - requires android 9 or higher_**
