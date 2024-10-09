---
layout: post
title: "Why No Touchie: Upgrading to Ubuntu 11.10"
date: 2011-11-02 01:33
comments: true
categories: [linux]
---
I upgraded to Ubuntu 11.10 this evening and the first thing I noticed was that my touchpad no longer worked... Of course, I immediately suspected that "touchpad-indicator", the application I installed to disable my touchpad when I have a normal mouse plugged in, was to blame.  However, I decided to do some searching around first and found a couple of forum posts about similar problems.  After reading about their woes, I decided to just try removing touchpad-indicator (`sudo apt-get remove touchpad-indicator`).  After running that command line and restarting, everything worked fine.  I don't know what changed in the upgrade.  Perhaps it would work again after reinstalling, but I'll probably wait until having the touchpad enabled annoys me again before trying.

I also ran into a problem with the "ffi" Ruby gem.  As I mentioned in [the previous post](/blog/2011/09/30/first/), I am running this blog on Octopress, which is built on Ruby.  When I would try to run `rake generate` ("generate" is the Octopress Rakefile target that builds all of the HTML from the markdown that I write these posts in), I would get something like the following error:

<pre><code>in `require': libffi.so.5: cannot open shared object file: No such file or directory</code></pre>

After some investigation, I found that this was easily fixed by running `sudo gem pristine ffi`.

Other than these two little issues, Ubuntu 11.10 has been pretty good to me so far.  Of course, I've only used it for an hour or so, but it appears that it hasn't totally hosed my system, so that's a start.  I have noticed that the `alt-tab` behavior between workspaces is now different.  It will now switch to the next most recently used application no matter what workspace that application is in whereas before it would restrict `alt-tab` switching to the current workspace.  I'll have to see if I get used to that.  If I don't, I may end up looking around for a setting to change it (I would imagine it would be configurable).  I guess we'll see what other issues I run up against in the following weeks.
