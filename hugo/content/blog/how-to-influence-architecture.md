---
title: 'How to Influence Architecture'
date: 2025-10-09T12:16:37-07:00
hero_image: /images/scaling-architecture.png
categories:
- small platform
email_preface: |
  Hi there {{FirstName}},

  I know many of you (on my mailing list) are involved in architecture, but you may not be. I encourage you to give this a read either way, since I think there are some general principles that apply to decision-making in general. It's not exclusive to architecture. I hope you find it helpful.

  Steve Oxley

  You can also read this [on my website]({{ .Page.Permalink }}).
---

A colleague of mine asked me, "How do you get a shared technical style across teams and also between teams?" He specifically mentioned, "[I] often find people come up with functional solutions to problems that break core abstractions of our platform, I can usually unwind things myself but I can't be in all places at the same time."

I had a couple of thoughts immediately, but I didn't think they were very helpful. After mulling it over a few months, I think the heart of his question is, "How do I influence good software architecture when I can't make every technical decision myself (and don't have the authority to anyway)?"

A few years ago I read the article "[Scaling the Practice of Architecture, Conversationally](https://martinfowler.com/articles/scaling-architecture-conversationally.html)." I remember thinking at the time that it was a very good article, and I shared it with the technical leadership at my company, Seeq. Every so often this article comes to mind, and I realized that it directly related to my colleague's question.

I think that the article directly answers the question on what you can put in place at your company to make sure that good decisions are made throughout the organization.
As I went back to re-read the article, the first paragraph had language that was exactly parallel to my colleague's commentary regarding being in all places at the same time:

> ...while using ["traditional" approaches] in the world of continuously delivering autonomous teams I’ve repeatedly found myself faced with an impossible task: to be everywhere, tolerating significant contextual variance, and blocking no-one.

I highly recommend reading the article for yourself, but I'll summarize the main points:
- The traditional way of "doing architecture" (lol and 🤢) has a centralized few that make the big technical decisions
- The traditional way doesn't scale
- Instead, you can implement the "Advice Process" which has two rules:
    - Anyone can make architectural decisions
    - Everyone making a decision should seek out advice from everyone who will be meaningfully affected by the decision and everyone with expertise in the area the decision is being taken.
- The "Advice Process" has some supporting tools that help make this happen:
    - Decision records - published and tracked documents that provide a history of architectural decisions (and a place to record advice received regarding the decision)
    - Advisory forum - an *open* forum for discussing architectural considerations and soliciting and receiving advice
    - Team-sourced principles - architectural principles that can help make architectural decisions
    - Technology radar - an internal list of technologies along with their maturity or level of acceptance within your organization

So if that article has all that you need to make sure good architectural decisions are consistently made, then why am I writing an article rather than just sharing a link? Well, that article was clearly written for those who were in the traditional Software Architecture roles and have authority to put certain processes in place and mandate that people need to seek advice from other people. I don't think my colleague is in that position, and few people are. Even in my position at Seeq as a Senior Principal Software Engineer, I don't have the authority to put in place practices like this across the organization. So what do you do?

Here are my ideas for how to go about spreading these ideas:
- Send "[Scaling the Practice of Architecture, Conversationally](https://martinfowler.com/articles/scaling-architecture-conversationally.html)" to someone at your organization who does have authority to implement a program like this.
- Document the conventions, patterns, and principles that you have seen in your organization.
- Start writing your own architecture decision records for the decisions you make - recommend the practice to others.
- Seek advice from those who would be affected by your decisions or who have expertise in the area you're making a decision - recommend the practice to others.
- Create a technology radar for your organization - like the [Thoughtworks one](https://www.thoughtworks.com/radar) but focused on tech and techniques specific to your organization.
- Write an internal blog post about all of this.

These are just some ideas to get started. I'm sure you'll come up with your own ideas from reading this article and the ones I've linked, and combining that with your own knowledge of how your organization works. If you come up with some ideas of your own, please share them. As an example of one of these ideas actually working, I shared "[Scaling the Practice of Architecture, Conversationally](https://martinfowler.com/articles/scaling-architecture-conversationally.html)" around my org and it has had positive effects like influencing our head architect to institute a truly open "Advisory forum" where anyone can propose topics and contribute. I will continue to try to influence my own sphere to make everyone responsible for (and capable of) making good architectural decisions, and I challenge you to do the same
