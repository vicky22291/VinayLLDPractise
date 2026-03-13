---
title: "Netflix at AWS re:Invent 2018"
author: "Netflix Technology Blog"
date: "Nov 26, 2018"
url: "https://netflixtechblog.com/netflix-at-aws-re-invent-2018-4884a292835"
tags: ['Netflixsecurity', 'Cloud Computing', 'AWS', 'Netflixoss', 'Cloud']
---

# Netflix at AWS re:Invent 2018

_by _[_Shaun Blackburn_](https://www.linkedin.com/in/shaunblackburn/)

[AWS re:Invent](https://reinvent.awsevents.com/) is back in Las Vegas this week! Many Netflix engineers and leaders will be among the 40,000 attending the conference to connect with fellow cloud and OSS enthusiasts. You can **find** us at our booth on the expo floor, speaking on a variety of subjects, and at meetups and events around the re:Invent campus. We have listed all our talks below to make it easy to hear what we have been up to. Please say hello — we would love to hear from you!

**Monday — November 26**

**10:45am ARC334 — Scaling Push Messaging for Millions of Netflix Devices**

[Susheel Aroskar](https://www.linkedin.com/in/susheel-aroskar-0418631/), Senior Software Engineer

**Abstract: **Netflix built Zuul Push, a massively scalable push messaging service that handles millions of always-on, persistent connections to proactively push time-sensitive data, like personalized movie recommendations, from the AWS Cloud to devices. This helped reduce Netflix’s notification latency and the Amazon EC2 footprint by eliminating wasteful polling requests. It also powers Netflix’s integration with Amazon Alexa. Zuul push is a high-performance async WebSocket/SSE server. In this session, we cover its design and how it delivers push notifications globally across AWS Regions. Key takeaways include how to scale to large numbers of persistent connections, differences between operating this type of service versus traditional request/response-style stateless services, and how push messaging can be used to add new, exciting features to your application.

**11:30am NET204 — ALB User Authentication: Identity Management at Scale with Netflix**

[Will Rose](https://www.linkedin.com/in/william-rose-67196399/), Senior Security Engineer

**Abstract:** In the zero-trust security environment at Netflix, identity management has historically been a challenge due to the reliance on its VPN for all application access. About one year ago, Netflix began exploring various identity solutions to alleviate the operational burden of maintaining its VPN. Additionally, it was looking for ways to provide a superior user experience. Join this chalk talk to learn how Netflix solved identity management at scale.

**12:15pm NET312 — Another Day in the Life of a Cloud Network Engineer at Netflix**

[Donavan Fritz](https://www.linkedin.com/in/donavanfritz/), Senior Network SRE and [Joel Kodama](https://www.linkedin.com/in/joelkodama/), Senior Network SRE

**Abstract: **Making decisions today for tomorrow’s technology — from DNS to AWS Direct Connect, ELBs to ENIs, VPCs to VPNs, the Cloud Network Engineering team at Netflix are resident subject matter experts for a myriad of AWS resources. Learn how a cross-functional team automates and manages an infrastructure that services over 125 million customers while evaluating new features that enable us to continue to grow through our next 100 million customers and beyond.

**1:45pm NET404-R — Elastic Load Balancing: Deep Dive and Best Practices**

Will Rose, Senior Security Engineer and Pratibha Suryadevara of AWS

Abstract: Elastic Load Balancing (ALB & NLB) automatically distributes incoming application traffic across multiple Amazon EC2 instances for fault tolerance and load distribution. In this session, we go into detail on ELB configuration and day-to-day management. We also discuss its use with Auto Scaling, and we explain how to make decisions about the service and share best practices and useful tips for success. Finally, Netflix joins this session to share how it leveraged the authentication functionality on Application Load Balancer to help solve its workforce identity management at scale.

**Tuesday — November 27**

**3:15pm CMP377 — Capacity Management Made Easy with Amazon EC2 Auto Scaling**

[Vadim Filanovsky](https://www.linkedin.com/in/vfilanovsky/), Senior Performance Engineer and [Anoop Kapoor](https://www.linkedin.com/in/anoopkapoor/) of AWS

**Abstract: **Amazon EC2 Auto Scaling removes the complexity of capacity planning to help customers improve application availability and reduce costs. In this session, we will deep dive on how EC2 Auto Scaling works to simplify health checking, security patching, continuous deployments, and automatic scaling with changing load. Netflix is spending over $8 billion on programming this year, with shows like Lost In Space, Altered Carbon and Money Heist, and plenty more in the future. They will share how Auto Scaling allows their infrastructure to automatically adapt to changing traffic patterns in order to keep their audience entertained and their costs on target.

**Wednesday — November 28**

**11:30am ARC336 — Sleeping on the Edge: Running and Operating Netflix’s Cloud Edge, Zuul**

[Mikey Cohen](https://twitter.com/moldfarm), Manager, Cloud Gateway and [Gayathri Varadarajan](https://www.linkedin.com/in/gvaradarajan/), Senior Software Engineer

**Abstract: **Zuul, Netflix’s cloud gateway, is the front door for all requests coming into the Netflix cloud infrastructure. It handles more than one million requests per second. How do we efficiently operate Zuul with a minimal operational and support burden and a happy team of six? In this chalk talk, learn how we achieve this by taking a DevOps first approach and a desire to sleep at night.

**11:30am NET404-R1 — Elastic Load Balancing: Deep Dive and Best Practices (Repeat)**

[Will Rose](https://www.linkedin.com/in/william-rose-67196399/), Senior Security Engineer and [Pratibha Suryadevara](https://www.linkedin.com/in/pratibha-suryadevara-25459964/) of AWS

**Abstract: **Elastic Load Balancing (ALB & NLB) automatically distributes incoming application traffic across multiple Amazon EC2 instances for fault tolerance and load distribution. In this session, we go into detail on ELB configuration and day-to-day management. We also discuss its use with Auto Scaling, and we explain how to make decisions about the service and share best practices and useful tips for success. Finally, Netflix joins this session to share how it leveraged the authentication functionality on Application Load Balancer to help solve its workforce identity management at scale.

**2:30pm SEC389 — Detecting Credential Compromise in AWS**

[Will Bengtson](https://twitter.com/__muscles), Senior Security Engineer

**Abstract: **Credential compromise in the cloud is not a threat that a single company faces. Rather, it is a widespread concern as more and more companies operate in the cloud. Credential compromise can lead to many different outcomes, depending on the motive of the attacker. In certain cases, this has led to erroneous AWS service usage for bitcoin mining or other nondestructive yet costly abuse. In other cases, it has led to companies shutting down due to the loss of data and infrastructure.

**3:15pm** **DAT406 — Netflix: Iterating on Stateful Services in the Cloud**

[Joey Lynch](https://www.linkedin.com/in/joseph-lynch-9976a431/), Senior Software Engineer

**Abstract:** While stateless services are suitable for many architectures, stateful services are also useful and sometimes overlooked. In this session, we hear from Netflix about the unique challenges of upgrading stateful services in the cloud, architectural advice to make iterating on stateful services easy, and concrete tools and infrastructure you can use on AWS to make upgrading easy.

**Thursday — November 29**

**1:00pm NET324 — Load Balancing the World: A Lesson in Adopting New Technology**

[Joel Kodama](https://www.linkedin.com/in/joelkodama/), Senior Network SRE

**Abstract: **Classic elastic load balancers have serviced Netflix since 2010, but with our ever-increasing subscriber growth, moving to the next generation of elastic load balancing was the key to continued success. Migrating elastic load balancers at Netflix came with some big challenges and several lessons learned. In this chalk talk, we discuss the Netflix journey from canarying to productionalizing network load balancers for over 125 million customers.

**1:45pm CMP376 — Another Week, Another Million Containers on Amazon EC2**

[Andrew Spyker](https://twitter.com/aspyker), Software Engineering Manager and [Joe Hsieh](https://www.linkedin.com/in/joe-hsieh-2474251/) of AWS

**Abstract: **Netflix’s container management platform, Titus, powers critical aspects of the Netflix business, including video streaming, recommendations, machine learning, big data, content encoding, studio technology, internal engineering tools, and other Netflix workloads. Titus offers a convenient model for managing compute resources, enables developers to maintain just their application artifacts, and provides a consistent developer experience from a developer’s laptop to production by leveraging Netflix container-focused engineering tools.

**1:45pm SEC391 — Inventory, Track, and Respond to AWS Asset Changes within Seconds at Scale**

[Mike Grima](https://www.linkedin.com/in/mikegrima/), Senior Security Engineer

**Abstract: **Large AWS environments have assets distributed across many accounts and regions. Ideally, asset inventory should be timely and provide an audit trail to document who made the changes and when. This is required for security teams to quickly react to insecure configurations and for DevOps tooling to manage infrastructure effectively. The traditional means of obtaining the timely and current state of AWS assets is to very frequently poll over the entire infrastructure, often tens of times per minute. This becomes increasingly difficult as AWS infrastructures grow in complexity. Additionally, polling for infrastructure changes provides no auditability context. In this session, learn how to inventory, track, and respond to AWS asset changes with seconds at scale.

**1:45pm STG391 — Post-Production Media Delivery at Scale with AWS**

[Zile Liao](https://www.linkedin.com/in/zile-l-1a3a308/), Senior Software Engineer and [Brandon Bussinger](https://www.linkedin.com/in/brandon-bussinger-a659148/), Product Manager

**Abstract: **Netflix is using AWS Snowball Edge to deliver post-production content to our asset management system, called Content Hub, in the AWS Cloud. Production companies have been historically using LTO tapes to move data around, and that has well-known complications. In order to accelerate and secure our media workflows Netflix has shifted to using Snowball Edge devices for data migration. Please join us to learn how Netflix is using the Snowball Edge service at scale.

**3:15pm DEV370 — Role of Central Teams in DevOps Organizations**

[Ruslan Meshenberg](https://twitter.com/rusmeshenberg), Vice President, Platform Engineering

**Abstract: **You’ve migrated your business to the cloud. You’ve embraced DevOps. All your engineering teams operate the systems they write. You don’t need central teams any longer … or do you? In this talk, we discuss how Netflix balances the need for product teams to stay loosely coupled yet how it maximizes the leverage for productivity and velocity that healthy central teams provide.

---
**Tags:** Netflixsecurity · Cloud Computing · AWS · Netflixoss · Cloud
