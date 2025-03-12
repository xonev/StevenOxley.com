---
title: 'Nudgsicle Development Environment'
date: 2025-02-25T21:44:25-08:00
layout: microblog
tags:
- nudgsicle
- programming
---

As part of my work on [Nudgsicle](https://nudgsicle.com/), I've created a pretty neat development environment where I can work locally with a similar architecture to the production environment, which includes Lambda, an S3 website, etc. One of the main pieces that makes it work is a little [AWS Lambda Function URL](https://docs.aws.amazon.com/lambda/latest/dg/urls-configuration.html) emulating dev server that I wrote. You can see the code in [this gist]: https://gist.github.com/xonev/0aa0078f9e5d19ac63c8fd0adb8b4a4b. Let me know what you think!
