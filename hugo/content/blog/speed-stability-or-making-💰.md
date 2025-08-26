---
title: 'Speed? Stability? or Making 💰? How to clarify software priorities'
date: 2025-08-08T14:09:22-07:00
categories:
  - small platform
email_preface: |
  Hi there {{FirstName}},
  In this installment of my small platform mailing list I enjoyed discussing some responses that I received from readers. I'd like to hear from you, too. So email me. 😄

  You can also read this [on my website]({{ .Page.Permalink }}).
---

In [The Principle of Production Purpose](https://www.stevenoxley.com/blog/2025/07/06/the-principle-of-production-purpose/) (PPP), I described a principle that encourages applying common sense to software. That common sense can have some pretty far-reaching implications. In the article, I gave the application of the principle unfortunately brief treatment. After some correspondence and chatting with readers about it, I think it's time to further flesh out how the principle of production purpose can be applied to your work. As a reminder the principle states,

> When developing software, the only thing that really matters is whether your software is accomplishing its intended purpose in its production environment.

## Applying the Principle of Production Purpose

In order to make PPP effective, you first need to identify what the purpose of your software is. My colleague and former manager at Groupon, [Chris Powers](https://www.linkedin.com/in/chris-powers-b415a4122/), commented:

> I'm curious what you would see as opposition to [your] "PPP" -- what values/decisions would a team have if they were not using the Principle of Production Purpose? You mentioned that PPP can clarify things like this line: 
>
> "Do we actually want to wait four hours for that build before every merge? Or should we run it prior to release? Or once a week? Is it more important to our purpose to catch the bugs this build might uncover or to ship more frequently?"
>
> Though after reading it, I'm not 💯 which answers PPP would push a team towards. Does PPP favor speed of deployment, or stability of deployment, or something else? Given your PPP definition I'm assuming "stability of deployment" would be the top consideration, but you also discuss speed of development a lot in the article.

He's right, it isn't clear which thing you should favor without some more information. The point of the principle of production purpose is that none of stability of deployment, speed of development, passing tests, or similar is a goal *in itself*. You first need to decide what the purpose of the software is. Then you can deduce whether stability of deployment, speed of development, or something else is the most important consideration.

The purpose of most software that I've worked on is to make my company money in some way. Though making money is often the ultimate purpose, you need to reason further to determine the specific purpose of the piece of software you are working on.

For example, when I worked at Groupon, I had the opportunity to work on the checkout page, including adding the first feature to persist users' addresses (we called it Address Book Lite). An e-commerce site's checkout page has a very specific purpose:  make it as easy as possible for customers to give you money in exchange for what you're selling. Reasoning one step further from that, availability of the checkout page is paramount to its purpose. If the checkout page is unavailable then customers can't give you money at all. I still remember my team being lectured (not by Chris) about how every minute (or second?) the Groupon checkout page was down we'd lose enough money to pay one of our yearly salaries. (This was after some incident involving the checkout page, but I honestly don't remember what it was - hopefully it wasn't my fault heh.) So for making changes to Groupon's checkout page, I would say that the principle of production purpose would guide you to test changes thoroughly before deployment, favor availability and stability over speed, slowly roll out changes, and measure things in detail.

On the other hand, another project I worked on at Groupon was a rewards system called Points. This was a completely new feature developed by a new team of which I was a founding member. For that, again, the general purpose was to make Groupon money, but the specific purpose of Points was quite different. At the beginning, we had no users, and we needed to get some users as soon as possible to start measuring whether this rewards system would lead to more usage of Groupon and therefore more money made for Groupon. Points's purpose at that point in time was to determine whether it was even a good idea. So in this case, I think the principle of production purpose would guide you to ship the first version as soon as possible and start getting some data on whether the idea actually worked. Testing changes excessively and focusing on availability of the Points page would be a mistake in this case since those considerations would be secondary to getting something shipped to learn as quickly as possible. (Of course, it's critical that unavailability of the Points page wouldn't cause some other issue like a user not being able to get to or use the checkout page.) As it turned out, the product manager and I left Groupon shortly after we started making Points available to users, and I hear it was never rolled out to more than 1% of Groupon users 😢.

So, as is hopefully clear from those examples, the principle of production purpose will lead you to emphasize different things depending on the specific purpose of your software. Even at the same company, different pieces of software can have different specific purposes though they may be working towards the same overall goal (making money in the Groupon examples).

Of course for some software, the purpose isn't to make money at all. Mastodon for instance started as an open source project in reaction to social media like Twitter that attempt to make money from users via advertising and control of users' attention. Mastodon's explicit goal is not to make money but instead to give its users ultimate control over their social media and data. As evidence, this is directly from [Mastodon's homepage](https://joinmastodon.org/):

> Social networking that's not for sale.
> Your home feed should be filled with what matters to you most, not what a corporation thinks you should see. Radically different social media, back in the hands of the people.

As you can see, the purpose of Mastodon's software is going to be different and lead to different decisions than X would make. Many goals will likely be the same - Mastodon users still want their social network to be available. But other objectives will be different - Mastodon isn't likely to try to maximize time spent scrolling feeds.

I am not surprised that other people have identified the principle of production purpose even though they have called it something different. After a bit of back and forth on the topic, Chris mentioned that he has expressed a similar idea but called it, "aligning engineering practices with business value." In his words:

> Got it, makes sense, I agree that the most meaningful idea here is aligning on a decision-making/prioritization process. I have generally used the words "aligning engineering practices with business value" to describe this in the past. You remove the idea of any particular engineering practice having intrinsic value in and of itself, and instead ask how that practice creates business value for the end user or the organization. Unsurprisingly, very few things in engineering create business value without going to production or having an impact on production, which I think is where your PPP comes from.

And he also shared a great example:

>  I think [Test Driven Development (TDD)] is a great way to write a lot of code, but not just because the Church of Bob Martin told me so -- rather, because TDD gives me fast feedback and certainty the code does what it says, which reduces defects in production and might even get me to prod faster. When I want to use Language X or Framework Y or Tool Z to build something, it's not because that's the "hip new tech" or because I have devoted a lot of my career to it -- rather, it's because I genuinely believe that tool will help us ship faster and more reliably.

Another reader, Charles Palen, mentioned that customer feedback relates to PPP:

> The teams I work with often have to incorporate direct customer feedback. This goes straight to production purpose; you are being told what is needed for production.

I agree with Charles that customer feedback is a very strong signal that can inform your software's specific purpose. But you still need to reason about whether the customer feedback actually aligns with your goals or not. Customer feedback is not generally equivalent to your software's production purpose. As an example, at Seeq some of our customers have asked to be able to download the application logs for their Seeq instance. When considering this customer request, we have had to ask ourselves if adding that feature fulfills our purpose. Since we've been moving our customers to our SaaS offering from on-premise installations, we should be managing their Seeq instance for them, and therefore they shouldn't be - and we probably don't want them - worrying about what is in the application logs. Instead, in this case, we'd probably want to dig further into why customers are requesting these logs. Perhaps they want to audit logins and access to Seeq. In that case, we could probably provide them with a more targeted feature around that specific use case, and therefore fulfill their requirement while not undermining our purpose of providing a service to them.

## Shipping is Critical

Once you have evaluated priorities based on the software's specific purpose, it's critical that you actually ship it to production. Both my manager at Seeq ([Andres Barbaro](https://www.linkedin.com/in/afbarbaro/)) and Chris Powers pointed out that shipping is extremely important. It's the principle of **production** purpose. If the software is not shipped then it's not fulfilling any purpose yet. Perhaps it will when it's done or when you apply the learnings from building it (in the case of prototype software), but if you haven't shipped to production you're not done yet.

Chris described this as a pet peeve of his when we worked at Groupon:

> I remember back in Groupon I started developing a strong distaste for engineers saying code was "dev complete", or for letting [Pull Requests] sit around while they focused on their own thing. If we believe that our work only has value once it's in production, then "dev complete" may as well be replaced with "having zero value", and you would do everything you can to move small units of value into production rapidly to create the value. Conveniently, that's also a great engineering practice for other reasons, but it aligns very well with business value.

## Conclusion

With these additional thoughts from readers and me, I believe you'll find the principle of production purpose will clarify your thinking about what your software should do and how you should build it. First, determine your software's specific purpose in its context. Then, identify the engineering practices that will best help you meet that specific purpose. Finally, make sure to ship to production, so you can fulfill your software's specific purpose.

## Want to Get More Like This in Your Inbox?
I have a mailing list where I write about topics that would be relevant to people who are just getting started creating or evolving a [platform](https://platformengineering.org/blog/what-is-platform-engineering) for their organization, or those that handle the platform responsibilities at companies that don't have a dedicated platform team. If that sounds interesting to you, sign up for my mailing list below!

<script async src="https://eomail6.com/form/969cad3c-5ae6-11f0-97e8-b7e8ac832f27.js" data-form="969cad3c-5ae6-11f0-97e8-b7e8ac832f27"></script>
