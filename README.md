<h1 align="center">
  Infinity For Reddit Fork </a>
  
</h1>

# There have been multiple reports of Reddit banning users of the app.

# If you still want to use the app, update to at least version 5.9.2 and log out of all accounts to wipe the session cookies associated with older versions and login again.

## ⚠️ Warning

Asking users to input their credentials directly in the app is a big **security risk**. As a user you are also **violating the Reddit User Agreement**.

I implore everyone who uses this fork and similar projects to look at the commits yourself and ensure that the credentials are not being stolen.

The releases are automated.


## Changes

This Fork enables a user to login via the official Reddit Accounts endpoint (no 3P authorize). The App will make requests that look like they are coming from the official Reddit App.

## TODO (First Party Features)

-   [x] Chat
    - [x] basic messaging 
    - [ ] render all types of messages
    - [x] enable sending images and emotes
    - [ ] implement chat requests
-   [x] Inline Subreddit Emotes, Gifs and Images in Comments
-   [x] use GQL API for Home posts
-   [x] use GQL API for Subreddit/User/Search posts
    -   [x] use the 1P Reddit DASH streams for external videos, i.e. REDGifs will load without a REDGifs API-Key.
    -   [x] fix video downloads missing audio
-   [x] update to new federated GQL API
-   [x] Giphy SDK
    -   [x] integrate SDK
    -   [x] use comment endpoint to post with RichtextJSON
    -   [ ] Gif loading in Comment preview

---

#### External Media

-   External videos like _REDGifs/Imgur_ will be loaded from the Reddit servers and not through an external API if possible.
-   You will be able to see the content of broken links, if they were mirrored to Reddit's servers before breaking.
-   This only works in the Subreddit, User, Search and Home Feeds. Support for Popular/All will be on hold, to not break compatibility.
-   Might break if the GQL API changes
-   old Gfycat links might not work

## Want to input your own Client-ID?

Check out the [Sub-Branch](https://github.com/KhoalaS/Infinity-For-Reddit/tree/sub). You will be prompted to enter a Client-ID during login. The User-Agent will be determined by the two additional inputs, App name and Username.

#### Known Issues Version 5.8.0

-   Devices with >= Android 13 login error

#### Known Issues Version 5.7.3

-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   ~~User related Feeds under "Post" section display the posts of the user~~

#### Known Issues Version 5.7.2

-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   ~~Home feed does not load with "Save Scrolled Position in HOME" enabled~~
-   User related Feeds under "Post" section display the posts of the user

#### Known Issues Version 5.7.0

-   ~~Images in text posts not loading correctly~~
    -   ~~probably due to the way the GQL API delivers the markdown text~~
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   Home feed does not load with "Save Scrolled Position in HOME" enabled

#### Known Issues Version 5.6.4

-   Images in text posts not loading correctly
    -   probably due to the way the GQL API delivers the markdown text
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   ~~Reddit GIFs cant be loaded~~

#### Known Issues Version 5.6.3

-   Images in text posts not loading correctly
    -   probably due to the way the GQL API delivers the markdown text
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   Reddit GIFs cant be loaded

#### Known Issues Version 5.6.2

-   Images in text posts not loading correctly
    -   probably due to the way the GQL API delivers the markdown text
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   ~~The App refreshes the Access Token incorrectly resulting in logged in Users browsing with an anonymous account~~

#### Known Issues Version 5.6.1

-   Images in text posts not loading correctly
    -   probably due to the way the GQL API delivers the markdown text
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   ~~The App refreshes the Access Token incorrectly resulting in logged in Users browsing with an anonymous account~~
-   The App refreshes the Access Token incorrectly during a pull refresh, resulting in logged in Users browsing with an anonymous account

#### Known Issues Version 5.6.0

-   Images in text posts not loading correctly
    -   probably due to the way the GQL API delivers the markdown text
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   The App refreshes the Access Token incorrectly resulting in logged in Users browsing with an anonymous account
-   ~~the app uses the latest session cookie (i.e. the one from the last logged in account) for all access token refreshes~~

#### Known Issues Version 5.5.1

-   Images in text posts not loading correctly
    -   probably due to the way the GQL API delivers the markdown text
-   ~~external Videos being displayed as links~~
-   Devices with >= Android 13 login error
    -   maybe some HMAC related Crypto APIs changed?
-   ~~Youtube links not being displayed~~
-   the app uses the latest session cookie (i.e. the one from the last logged in account) for all access token refreshes

#### Known Issues Version 5.5.0

-   Images in text posts not loading correctly
-   external Videos being displayed as links
-   Devices with >= Android 13 login error
-   Youtube links not being displayed

<div align="center">

A Reddit client on Android written in Java. It does not have any ads and it features a clean UI and smooth browsing experience

<img align="right" src="https://raw.githubusercontent.com/Docile-Alligator/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/icon.png" width=200>

</div>

<br>

<div align="center">

Infinity for Reddit is available on Google Play and F-Droid

  <a href="https://play.google.com/store/apps/details?id=ml.docilealligator.infinityforreddit">
      <img alt="Get it on Google Play" height="80" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
      </a>  
      <a href="https://f-droid.org/packages/ml.docilealligator.infinityforreddit/">
          <img alt="Get it on F-Droid" height="80" src="https://f-droid.org/badge/get-it-on.png" />
  </a>

</div>

<div align="center">

<br>
    <a href="https://github.com/Docile-Alligator/Infinity-For-Reddit/wiki"><strong>Explore the docs »</strong></a>
<br>

<a href="https://github.com/Docile-Alligator/Infinity-For-Reddit/issues">Report a Bug</a>
·
<a href="https://github.com/Docile-Alligator/Infinity-For-Reddit/discussions/categories/ideas">Request a Feature</a>
·
<a href="https://github.com/Docile-Alligator/Infinity-For-Reddit/discussions/categories/q-a">Ask a Question</a>

</div>

<br>

<div align="center">

[![release](https://img.shields.io/github/v/release/Docile-Alligator/Infinity-For-Reddit)](https://github.com/Docile-Alligator/Infinity-For-Reddit/releases)
[![license](https://img.shields.io/github/license/Docile-Alligator/Infinity-For-Reddit)](LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/Docile-Alligator/Infinity-For-Reddit)](https://github.com/Docile-Alligator/Infinity-For-Reddit/issues)

</div>

## Donation

<p>Infinity for Reddit+:</p>
<a href="https://play.google.com/store/apps/details?id=ml.docilealligator.infinityforreddit.plus">
    <img alt="Get it on Google Play" height="80" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" />
</a>

Patreon: https://www.patreon.com/docile_alligator

Bitcoin: bc1qxtkd5ap9na7uy8nr9qpt6jny6tdwaj4v43ddle

<details>
  <summary>Table of Contents</summary>

-   [About](#about)
    -   [Built With](#built-with)
-   [Getting Started](#getting-started)
    -   [Prerequisites](#prerequisites)
    -   [Usage](#usage)
        -   [Cookiecutter template](#cookiecutter-template)
        -   [Manual setup](#manual-setup)
        -   [Variables reference](#variables-reference)
-   [Roadmap](#roadmap)
-   [Contributing](#contributing)
-   [Support](#support)
-   [License](#license)
-   [Acknowledgements](#acknowledgements)

</details>

---

## About The Project

<table>
<tr>
<td>

Key features of **Infinity For Reddit**:

-   Lazy mode: Automatic scrolling of posts enables you to enjoy amazing posts without moving your thumb.
-   Browsing posts
-   View comments
-   Expand and collapse comments section
-   Vote posts and comments
-   Save posts
-   Write comments
-   Edit comments and delete comments
-   Submit posts (text, link, image and video)
-   Edit posts (mark and unmark NSFW and spoiler and edit flair) and delete posts
-   See all the subscribed subreddits and followed users
-   View the messages
-   Get notifications of unread messages
-   etc...

</td>
</tr>
</table>

<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" 
  alt="Screenshot 1"
  height="200" >
<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" 
  alt="Screenshot 2"
  height="200" >
<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" 
  alt="Screenshot 3"
  height="200" >
<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" 
  alt="Screenshot 4"
  height="200" >
<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" 
  alt="Screenshot 5"
  height="200" >
<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" 
  alt="Screenshot 6"
  height="200" >
<img 
  src="https://raw.githubusercontent.com/Wladefant/Infinity-For-Reddit/master/fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" 
  alt="Screenshot 7"
  height="200" >

<p align="right">(<a href="#top">back to top</a>)</p>

## Contributing

First off, thanks for taking the time to contribute! Contributions are what makes the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request.
It's better to also open an issue describing the issue you want to fix. But it is not required.

Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

Here are other ways you can help:

-   [Report Bugs](https://github.com/Docile-Alligator/Infinity-For-Reddit/issues)
-   [Make Suggestions](https://github.com/Docile-Alligator/Infinity-For-Reddit/discussions)
-   [Translate The App](https://poeditor.com/join/project?hash=b2IRyfaJv6)

<p align="right">(<a href="#top">back to top</a>)</p>

## License

Distributed under the AGPL-3.0 License. See <a href="https://github.com/Docile-Alligator/Infinity-For-Reddit/blob/master/LICENSE">LICENSE</a> for more information.

<p align="right">(<a href="#top">back to top</a>)</p>

## Contact

[u/Hostilenemy](https://www.reddit.com/user/Hostilenemy) -
docilealligator.app@gmail.com (Owner)

or [u/Wladefant](https://www.reddit.com/user/Wladefant) - wladefant@gmail.com (Collaborator)

Project Link: [https://github.com/Docile-Alligator/Infinity-For-Reddit](https://github.com/Docile-Alligator/Infinity-For-Reddit)

<p align="right">(<a href="#top">back to top</a>)</p>
