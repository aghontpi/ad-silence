# Ad-silence

> Remove ads in Accuradio & Spotify

<p align="left">
  <br/>
  <a href="https://github.com/aghontpi/ad-silence/releases"><img src="https://img.shields.io/github/v/release/aghontpi/ad-silence?include_prereleases&style=flat-square&label=github-release" alt="release"></a>
  <a href="https://github.com/aghontpi/ad-silence/blob/master/LICENSE"><img src="https://img.shields.io/github/license/aghontpi/ad-silence?style=flat-square" alt="license"></a>
</p>

## Features

- Block ads on `Accuradio android` & `Spotify android`
- Minimal UI
- App size **less than 150KB**
- Configure which apps to remove ads in
- Open source
- No inapp purchases or ads

## Supported apps?

|    app    | support |
| :-------: | :-----: |
| Accuradio |   yes   |
|  Spotify  |   yes   |

- If you want this to work on a different app/audio streaming service, [open an new issue](https://github.com/aghontpi/ad-silence/issues/new) mentioning its name.
- Since this is built with `Accuradio` in mind, ~~no other apps are supported for now, but Implementing other apps should be quite easy.~~
- `Spotify` is now supported.
- Easily extensible to other apps.

## Motivation

- Ad blocker for `Accuradio android` is not available.
- wanted a **lightweight** & **non bloat** app with size lessthan **_1mb_**
- wanted a minimal ui
  - below is the Total UI of the app.
  <p>
    <img src="./sample/1.png" alt="ad notification" height="400px" width="auto"/> 
    <img src="./sample/2.png" alt="configure apps" height="400px" width="auto"/> 
    <img src="./sample/3.png" alt="about" height="400px" width="auto"/> 
  </p>

## How this works

This is possible because of `NotificationListenerService` on `android`. Granted, the user gave permission to this setting.

- While music is playing, the following notification is present.

<p>
<img src="./sample/ad_playing.png" alt="ad notification" height="auto" width="480px"/> 
</p>

- I then parse the notification and stop it from playing.

## Track Project Status

- [Trello](https://trello.com/b/8XJDVbdo/ad-silence-android)

## Built with

- [Kotlin](https://kotlinlang.org/)
