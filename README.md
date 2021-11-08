# Ad-silence

> Remove ads in Accuradio

## What apps are supported?

|    app    | support |
| :-------: | :-----: |
| Accuradio |   yes   |

Since this is built with `Accuradio` in mind, no other apps are supported for now, but Implementing other apps should be quite easy.

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
