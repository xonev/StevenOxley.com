---
title: 'Robust Software, Clear Thinking, and Production Excellence in Release It!'
date: 2025-08-26T06:53:39-07:00
hero_image: "/images/thinking-jet-engines.png"
categories:
- small platform
email_preface: |
  Hi there {{FirstName}},

  I hope your summer is going well! Mine has been great, though it has been a bit of a challenge balancing camping, boating, and backpacking with all the other things I want to do - like sending out these articles on a regular cadence! I've also been reading some good books over the summer, and *Release It!* was particularly relevant to this mailing list. Read on for my thoughts on the book - it's a good one.

  Steve Oxley

  You can also read this [on my website]({{ .Page.Permalink }}).
---

I recently ran a book club at Seeq for the book *Release It!* by Michael T. Nygard. It's a new entry on my list of recommended books for all software engineers - platform engineers included. There are a bunch of great, practical insights to be harvested. Throughout the book, I found myself reflecting on the systems I work on at Seeq or even checking code to verify some detail that Nygard had brought to mind.

To [borrow an idea](https://jamesclear.com/book-summaries) from James Clear, here is my summary of the book in three sentences or less:

> Once your software is shipped to production your work is just beginning. Shift your mindset from ensuring your code behaves correctly to ensuring your code behaves and can handle other components or systems it integrates with not behaving. Take your system from design to implementation to deployment to retirement by taking an evolutionary approach in each step of the lifecycle.

If I had one criticism of the book, it would probably be that it covers too much ground. Writing that three sentence summary was really hard! Entire books have been written on many of the topics that Nygard covers, including observability (Nygard calls it *transparency*), networking, configuration, and continuous delivery - just to name a few. *Release It!* can be seen as a quick primer on these topics and a good jumping off point to dig deeper. So I won't bore you with a full summary of the book - you should read it yourself - instead I'll call out some of the things from the book that resonated with me on my first read through and explain why.

## Who Is Release It! For?

First of all, I like that most technical books explicitly describe the intended audience. Nygard states this book is for people who write important software. That's me! And I bet it's most of you, too. "If anybody has to go home for the day because your software stops working than this book is for you." I thought that this quote best captures the situation we have at my company, Seeq, though he mentioned some other cases like money lost if your software stops working, which would better apply to ecommerce sites. We at Seeq make a software product for analyzing industrial process data. It's hard to quantify the impact of Seeq no longer working, since it really depends on what kind of data our users' are analyzing and how critical that data analysis is to their processes. But more of our customers are using our software for more critical use cases over time. So Seeq not working could make someone go home for the day, or it could lose one of our customers money. Either way, this book is for developers at Seeq.

Once Nygard establishes this book is for those working on important software, he stresses that software's success is measured in production. He specifically contrasts designing and writing software for production with passing the quality assurance (QA) department's tests. I think that contrast may be a little antiquated - most of the places I've worked haven't had a QA department - but this resonated regardless because his point is to focus on your software's long term suitability. As he states it, "Simply passing QA tells us little about the system's suitability for the next three to ten years of life." To extend that, I think it's also important not to write your software so that the ops team can just barely keep things running in production. Ideally, developers own and are responsible for the operation of their software until it comes *out* of production. As [Adrian Cockcroft](https://fosstodon.org/@xonev/111285599676131221) likes to frame that idea, you want to evolve "from 'done' is when you give your code to QA, to when your code is running, to owning the whole life cycle." All of these ideas resonate nicely with the [Principle of Production Purpose](https://www.stevenoxley.com/blog/2025/07/06/the-principle-of-production-purpose/).

Nygard's comment on writing software to pass QA also resonated with me because Seeq recently retired Quality Engineer as a title at Seeq (which I [wrote briefly about](https://www.stevenoxley.com/blog/2025/07/24/conquer-your-main-quest/#abandon-the-quest) before). Even prior to retiring Quality Engineer as a title, our quality engineers were either embedded on our other teams or were generally functioning as an [enabling team](https://teamtopologies.com/key-concepts). So we at Seeq have fully embraced that the goal isn't to pass QA. I think we are still working on developers owning the whole life cycle of their code as Cockcroft advocates, but that evolution is happening.

How well are you and your organization adhering to these principles of writing code for production? Here is a quick assessment you can do:

1. Does the way I track my work (Jira, Linear, etc.) consider something done only once it has been released to production?
2. Is monitoring a change's success in production part of my normal development process?
3. Am I the first person to respond to an incident if the software I work on is unavailable?

If you answer "yes" to all of these questions then that's awesome! Otherwise, you may want to take a closer look and consider changing some of your development and/or operational practices. For me, I can generally answer "yes" to 1 and 3, but if I'm honest I'm still growing towards "yes" for number 2.

## Systems Thinking and Stability

Once Nygard has established why you should care about your software's production stability, he gets into how to make it more stable. To that end, a major theme throughout *Release It!* is that you need to think of your software not just as a piece of code that you're writing, but holistically as a system of all the components it integrates with or depends on. This could be networking components, computer hardware, libraries, HTTP APIs, or other parts of your system. A user of your system isn't going to care that you didn't write the line of code that caused your whole system to be unavailable: they will simply care that the system is unavailable. In particular, there are a couple of chapters dedicated to stability patterns and antipatterns. These chapters alone are worth the price of the book.

The most amusing stability antipattern to me Nygard named "Users." If you don't have any users then your software probably won't have stability issues. I brought this same idea up with regard to security [in a previous article](https://www.stevenoxley.com/blog/2025/07/06/the-principle-of-production-purpose/) - if no one can access your system, then no one can compromise it. Similarly, if no one is using the system, then it will probably run without any issues. This whole concept reminds me of [a joke](https://jorts.horse/@Z/111741405518199218), "As we all know, the only secure computer is one not connected at all, which is why I recommend comcast." Of course, you can't simply eliminate users (that's illegal!) or - in many cases - entirely disconnect your systems from the Internet, but Nygard does have some great advice in this section about how to handle user traffic, including identifying "unwanted" users and protecting your application from their requests.

The stability patterns catalog is probably the most complete I've seen, and I appreciated Nygard's direct and clear descriptions of patterns like [Circuit Breaker](https://en.wikipedia.org/wiki/Circuit_breaker_design_pattern) and [Bulkhead](https://www.geeksforgeeks.org/system-design/bulkhead-pattern/). I am sure I will be reaching for the book as a reference to these patterns.

Nygard is very skilled at taking this systems thinking and breaking down big problems into simple principles. In more than one case, he clarified my thinking on a topic. For example, he pointed out that long-term stability of a system is usually interrupted by one of two things: memory leaks or data growth:

> The major dangers to your system's longevity are memory leaks and data growth. Both kinds of sludge will kill your system in production. Both are rarely caught during testing.

At first, I had to pause - is that true? - but thinking about it further, if we eliminate instability caused by new defects released, hardware failures, and that type of thing, it seems that degradation in performance of a system would most often be caused by some form of data accumulation. Either more data to process or memory growing without bound could easily cause a system that was working just fine to grind to a halt or crash.

Nygard caused another "aha" moment for me in his treatment of API versioning. Nygard points out that, "We can always accept more than we accepted before, but we cannot accept less or require more. We can always return more than we returned before, but we cannot return less." Anything we "cannot" do is a breaking change to the API. It's that simple, and all of the API versioning considerations are about how we can best manage those constraints.

This clarity of thinking that Nygard has makes me evaluate how well I reason about the systems I work on. I suggest you reflect on your own clarity of thinking. One pitfall that can be easy to fall into is blaming an outage of your software on some other piece of software it integrates with. In these cases, rather than chalking that up as a on-off occurrence and moving on, you can ask, "Did that really need to be an outage? Can we make our system more robust next time that dependency has issues?" I have asked myself these questions with regard to entire AWS regions going down. Certainly, there are some organizations that are at the system-level maturity that they can handle outages like that. We are not there yet, but one day I plan to be.
## Platforms for Production Excellence

Of course, I must include Nygard's comments on platforms and DevOps. Platform engineering is my main squeeze, after all! Nygard gives platforms and DevOps a good - though relatively brief - treatment. Chapter 10, "Control Plane," discusses how your control plane can be anything from nothing (you log into the server to view the logs, and deploy things manually) to a couple of one-off tools for monitoring and deployments to a full-blown platform like Kubernetes with CI/CD pipelines and automated everything. I agree with Nygard:  not every company and not every software project needs a platform team and the services they provide. In fact, I've been thinking about a series of articles that covers recommended approaches to delivery and operations for different team sizes and maturity levels. Nygard approaches this range of needs by discussing the building blocks that can be used to put together a useful control plane, including more piecemeal tools like deployment automation and more holistic solutions such as Kubernetes.

In the control plane chapter, Nygard specifically discusses platform teams, and includes one of the quotes from the book that resonates with me the most:

> Keep in mind that the goal for the platform team is to enable their customers. The team should be trying to take themselves out of the loop on every day-to-day process and focus on building safety and performance into the platform itself.

This really captures what platform teams are supposed to be about. They provide a service to the rest of the development teams. A platform team is not an operations team, but rather provides services that allow the other teams to operate the software themselves.

In Chapter 16, "Adaptation," Nygard brings up platform teams again, and specifically contrasts them with "DevOps teams." In a callout panel called "The Fallacy of the 'DevOps Team'" Nygard doesn't mince words,

> It's common these days, typically in larger enterprises, to find a group called the DevOps team. . . This is an antipattern. . . When a company creates a DevOps team, it has one of two objectives. One possibility is that it's really either a platform team or a tools team. . . The other possibility is that the team is there to promote the adoption of DevOps by others.

I couldn't agree more. The whole point of DevOps is to combine development and operations. As Nygard says, "It should soften the interface between different teams." I would go even further in saying that DevOps in its purest form is where developers own and are responsible for the operation of their software until it comes *out* of production, as I mentioned earlier. In that case a team (the ops team) is either eliminated entirely, embedded in the development teams, or replaced by a supporting platform team. DevOps was never supposed to need another team. Nygard's two objectives for so-called DevOps teams anticipates what would later be codified in [Team Topologies](https://teamtopologies.com/key-concepts):  these DevOps teams are typically either platform teams (and should simply be called that) or enabling teams, in which case it should be made explicit that they don't actually create any software or perform any function other than helping other teams adopt DevOps practices.

I recommend considering the teams where you work. If you have a platform team, are they doing what platform teams should do, building tools and services to support developers? Or are they handling alerts themselves and manually deploying things? If you have a DevOps team, is it actually a platform team or something else? If you don't have any of these teams, do you think you need one? What kinds of problems exist at your company that might be solved by a platform team?

## A Nice Sampling From a Smorgasbord

This article covered a small sampling of the topics that resonated with me as I read the book. I think different topics from the book will probably resonate with you depending on what you're working on and where you are in your career. I may even read through it again in the future, since I think different topics will jump out to future me. I very highly recommend this one; I'm sure I'll be referencing and quoting it regularly. You should go read it - you can buy it from [The Pragmatic Bookshelf](https://pragprog.com/titles/mnee2/release-it-second-edition/) with discount code `stevenoxley` for 40% off until the end of the year (I don't get any kickback from this, I'm just trying to help you!). And in the meantime, I recommend making sure you're owning the entire lifecycle of your software, clarifying and expanding your systems thinking, and considering your organization's team topologies.
