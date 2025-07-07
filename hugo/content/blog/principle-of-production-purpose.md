---
title: 'The Principle of Production Purpose'
date: 2025-07-06T21:46:22-06:00
---

“We want you to be the manager,” my VP of engineering told me.

“Oh… ok,” I responded.

Earlier that week I had volunteered to be part of a newly forming infrastructure team - also responsible for developer experience, software delivery, production observability, and operations tooling - at my company, Seeq.

I was very interested in this new team for a couple of reasons. First, I had always been curious about how systems I worked on functioned from end to end. Rather than being satisfied with pushing some code and trusting the deploy pipeline to take it from there, I wanted to inspect the nodes, network connections, and other components of the production system. Second, I saw big opportunities for Seeq in particular for improved productivity via more automation of our deployments, faster builds, better observability of the system, etc.

Because of my interest in these areas, I had made my interest clear in this new team. But it hadn’t even crossed my mind that they would want me to lead it.
Leading the team meant that very quickly I had a lot of stuff to ramp up and decide on. I had never used any configuration management technology like Terraform or Chef. I’d used Kubernetes but was far from an expert. I had experience with the Elasticsearch, Logstash, Kibana (ELK) stack, but we were considering Grafana... But before picking technologies we first had to determine our approach to developer productivity, operations, deployments, and maintaining our old systems while developing their replacements.

I'd like to dig into all of these decisions - both technical and process - in future installments of this mailing list, but first I want to share the number one principle I learned throughout this journey, which shaped our approach:

> When developing software, the only thing that really matters is whether your software is accomplishing its intended purpose in its production environment.

For convenience, let's call this the principle of production purpose. Everything else serves to support that intended purpose. It doesn’t matter if the latest change passes all the pre-production tests if it then breaks production. It doesn’t matter if your software is up 99.999% of the time if it’s not actually helping your users. Even security must bend to this principle. One might argue that a perfectly secure system wouldn't allow anyone to access it: as soon as you allow one person access you have a security hole. But unless you have a very strange goal your software isn't accomplishing its purpose if it disallows all access.

The principle of production purpose seems obvious at first, but try telling someone that the only thing that *actually* matters is how the software functions in production the next time you're having a discussion about builds or tests. And please report back to me how people respond. In my experience it leads to more pointed discussions about the value of the builds or tests in question. For example, "Do we actually want to wait four hours for that build before every merge? Or should we run it prior to release? Or once a week? Is it more important to our purpose to catch the bugs this build might uncover or to ship more frequently?"

The best part of this principle of production purpose is how it can clarify decisions. As a (real) example, let's consider running your system tests on a production-like Kubernetes cluster vs. running them on a single-node install of [k3s](https://github.com/k3s-io/k3s) (in other words, a single-node Kubernetes cluster). In our case, we had production-like Kubernetes clusters ready to go, and we had never run our software on a single-node cluster. We would have required strong justifications (cost or speed, for example) to pick k3s, since the production-like cluster would always be more likely to catch real defects and avoid spurious test failures.

Of course, when we started this infrastructure team at Seeq, I couldn't fully articulate the principle of production purpose. Over the next few weeks, we started forming the team. A few excellent engineers joined me - some I recruited, some volunteered. They mostly had resumes similar to mine: doing their assigned work well while also building and improving the systems that made our production software work well and our developers productive. It was a good time, but there were lots of challenges to be met. And it was through working with these great people, discussing design choices and tradeoffs that I realized that the principle of production purpose could often cut through some of the noise and help us move forward.

And now I have a few questions for you:

- What is a principle that you've (re-)discovered during your work? Bonus points if it's related to DevOps or Platform Engineering.
- How have you noticed the principle of production purpose crop up in your work?
- What are problems you've had with your production systems that you've struggled to get other people to care about? If you have some, maybe you could try using the principle of production purpose to overcome some of the friction you're experiencing. If you do, please let me know how it goes.

By the way, none of these questions are rhetorical. Email me with your thoughts, I'd love to feature them in future articles. You can reach me at: "me at the domain name of this website."
Also, subscribe to receive emails with articles like this one in your inbox:

<script async src="https://eomail6.com/form/969cad3c-5ae6-11f0-97e8-b7e8ac832f27.js" data-form="969cad3c-5ae6-11f0-97e8-b7e8ac832f27"></script>
