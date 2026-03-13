---
title: "Bringing AV1 Streaming to Netflix Members’ TVs"
author: "Netflix Technology Blog"
date: "Nov 9, 2021"
url: "https://netflixtechblog.com/bringing-av1-streaming-to-netflix-members-tvs-b7fc88e42320"
tags: ['Av1', 'Netflix', 'Encoding', 'Streaming']
---

# Bringing AV1 Streaming to Netflix Members’ TVs

by_ _[_Liwei Guo_](https://www.linkedin.com/in/liwei-guo-a5aa6311/)_, _[_Ashwin Kumar Gopi Valliammal_](https://www.linkedin.com/in/gvashwin/)_, _[_Raymond Tam_](https://www.linkedin.com/in/raymond-tam-66a17910/)_, _[_Chris Pham_](https://www.linkedin.com/in/phamchristopher/)_, _[_Agata Opalach_](https://www.linkedin.com/in/agataopalach/)_, _[_Weibo Ni_](https://www.linkedin.com/in/niweibo/)

[AV1](https://en.wikipedia.org/wiki/AV1) is the first high-efficiency video codec format with a royalty-free license from [Alliance of Open Media ](https://aomedia.org/)(AOMedia), made possible by wide-ranging industry commitment of expertise and resources. Netflix is proud to be a founding member of AOMedia and a key contributor to the development of AV1. The specification of AV1 was published in 2018. Since then, we have been working hard to bring AV1 streaming to Netflix members.

In February 2020, Netflix started [streaming AV1 to the Android mobile app](./netflix-now-streaming-av1-on-android-d5264a515202.md). The Android launch leveraged the open-source software decoder [dav1d](https://code.videolan.org/videolan/dav1d) built by the VideoLAN, VLC, and FFmpeg communities and sponsored by AOMedia. We were very pleased to see that AV1 streaming improved members’ viewing experience, particularly under challenging network conditions.

While software decoders enable AV1 playback for more powerful devices, a majority of Netflix members enjoy their favorite shows on TVs. AV1 playback on TV platforms relies on hardware solutions, which generally take longer to be deployed.

Throughout 2020 the industry made impressive progress on AV1 hardware solutions. Semiconductor companies announced [decoder SoCs](https://www.mediatek.com/blog/mediatek-dimensity-1000-hardware-av1-ready-for-netflix-av1-streams-on-android) for a range of consumer electronics applications. TV manufacturers released [TVs ready for AV1 streaming](http://aomedia.org/av1%20adoption/av1-enabled-2020-qled-8k-tvs/). Netflix has also partnered with YouTube to develop an [open-source solution](https://aomedia.org/in%20the%20news/youtube-and-netflix-announce-availability-reference-av1-decoder-for-xbox/) for an AV1 decoder on game consoles that utilizes the additional power of GPUs. It is amazing to witness the rapid growth of the ecosystem in such a short time.

Today we are excited to announce that Netflix has started streaming AV1 to TVs. With this advanced encoding format, we are confident that Netflix can deliver an even more amazing experience to our members. In this techblog, we share some details about our efforts for this launch as well as the benefits we foresee for our members.

## Enabling Netflix AV1 Streaming on TVs

Launching a new streaming format on TV platforms is not an easy job. In this section, we list a number of challenges we faced for this launch and share how they have been solved. As you will see, our [“highly aligned, loosely coupled”](https://jobs.netflix.com/culture) culture played a key role in the success of this cross-functional project. The high alignment guides all teams to work towards the same goals, while the loose coupling keeps each team agile and fast paced.

### Challenge 1: What is the best AV1 encoding recipe for Netflix streaming?

AV1 targets a wide range of applications with numerous encoding tools defined in the specification. This leads to unlimited possibilities of encoding recipes and we needed to find the one that works best for Netflix streaming.

Netflix serves movies and TV shows. Production teams spend tremendous effort creating this art, and it is critical that we faithfully preserve the original creative intent when streaming to our members. To achieve this goal, the Encoding Technologies team made the following design decisions about AV1 encoding recipes:

- We always encode at the highest available source resolution and frame rate. For example, for titles where the source is 4K and high frame rate (HFR) such as [“Formula 1: Drive to Survive”](https://en.wikipedia.org/wiki/Formula_1:_Drive_to_Survive), we produce AV1 streams in 4K and HFR. This allows us to present the content exactly as creatively envisioned on devices and plans which support such high resolution and frame-rate playback.
- All AV1 streams are encoded with 10 bit-depth even if AV1 Main Profile allows both 8 and 10 bit-depth. Almost all movies and TV shows are delivered to Netflix at 10 or higher bit-depth. Using 10-bit encoding can better preserve the creative intent and reduce the chances of artifacts (e.g., [banding](./cambi-a-banding-artifact-detector-96777ae12fe2.md)).
- [Dynamic optimization](https://netflixtechblog.com/dynamic-optimizer-a-perceptual-video-encoding-optimization-framework-e19f1e3a277f) is used to adapt the recipe at the shot level and intelligently allocate bits. Streams on the Netflix service can easily be watched millions of times, and thus the optimization on the encoding side goes a long way in improving member experience. With dynamic optimization, we allocate more bits to more complex shots to meet Netflix’s high bar of visual quality, while encoding simple shots at the same high quality but with much fewer bits.

### Challenge 2: How do we guarantee smooth AV1 playback on TVs?

We have a stream analyzer embedded in our encoding pipeline which ensures that all deployed Netflix AV1 streams are spec-compliant. TVs with an AV1 decoder also need to have decoding capabilities that meet the spec requirement to guarantee smooth playback of AV1 streams.

To evaluate decoder capabilities on these devices, the Encoding Technologies team crafted a set of special certification streams. These streams use the same production encoding recipes so they are representative of production streams, but have the addition of extreme cases to stress test the decoder. For example, some streams have a peak bitrate close to the [upper limit](https://en.wikipedia.org/wiki/AV1#Levels) allowed by the spec. The [Client and UI Engineering](https://jobs.netflix.com/teams/client-and-ui-engineering) team built a certification test with these streams to analyze both the device logs as well as the pictures rendered on the screen. Any issues observed in the test are flagged on a report, and if a gap in the decoding capability was identified, we worked with vendors to bring the decoder up to specification.

### Challenge 3: How do we roll out AV1 encoding at Netflix scale?

Video encoding is essentially a search problem — the encoder searches the parameter space allowed by all encoding tools and finds the one that yields the best result. With a larger encoding tool set than previous codecs, it was no surprise that AV1 encoding takes more CPU hours. At the scale that Netflix operates, it is imperative that we use our computational resources efficiently; maximizing the impact of the CPU usage is a key part of AV1 encoding, as is the case with every other codec format.

The Encoding Technologies team took a first stab at this problem by fine-tuning the encoding recipe. To do so, the team evaluated different tools provided by the encoder, with the goal of optimizing the tradeoff between compression efficiency and computational efficiency. With multiple iterations, the team arrived at a recipe that significantly speeds up the encoding with negligible compression efficiency changes.

Besides speeding up the encoder, the total CPU hours could also be reduced if we can use compute resources more efficiently. The Performance Engineering team specializes in optimizing resource utilization at Netflix. Encoding Technologies teamed up with Performance Engineering to analyze the CPU usage pattern of AV1 encoding and based on our findings, Performance Engineering recommended an improved [CPU scheduling strategy](https://www.infoq.com/presentations/video-encoding-netflix/?itm_source=infoq&itm_campaign=user_page&itm_medium=link). This strategy improves encoding throughput by right-sizing jobs based on instance types.

Even with the above improvements, encoding the entire catalog still takes time. One aspect of the Netflix catalog is that not all titles are equally popular. Some titles (e.g., [La Casa de Papel](https://gadgets.ndtv.com/entertainment/news/money-heist-season-5-69-million-best-maid-67-queens-gambit-netflix-viewership-metric-change-2581469)) have more viewing than others, and thus AV1 streams of these titles can reach more members. To maximize the impact of AV1 encoding while minimizing associated costs, the [Data Science and Engineering](https://jobs.netflix.com/teams/data-science-and-engineering) team devised a catalog rollout strategy for AV1 that took into consideration title popularity and a number of other factors.

### Challenge 4: How do we continuously monitor AV1 streaming?

With this launch, AV1 streaming reaches tens of millions of Netflix members. Having a suite of tools that can provide summarized metrics for these streaming sessions is critical to the success of Netflix AV1 streaming.

The Data Science and Engineering team built a number of dashboards for AV1 streaming, covering a wide range of metrics from streaming quality of experience (“QoE”) to device performance. These dashboards allow us to monitor and analyze trends over time as members stream AV1. Additionally, the Data Science and Engineering team built a dedicated AV1 alerting system which detects early signs of issues in key metrics and automatically sends alerts to teams for further investigation. Given AV1 streaming is at a relatively early stage, these tools help us be extra careful to avoid any negative member experience.

## Quality of Experience Improvements

We compared AV1 to other codecs over thousands of Netflix titles, and saw significant compression efficiency improvements from AV1. While the result of this offline analysis was very exciting, what really matters to us is our members’ streaming experience.

To evaluate how the improved compression efficiency from AV1 impacts the quality of experience (QoE) of member streaming, [A/B testing](./decision-making-at-netflix-33065fa06481.md) was conducted before the launch. Netflix encodes content into multiple formats and selects the best format for a given streaming session by considering factors such as device capabilities and content selection. Therefore, multiple A/B tests were created to compare AV1 with each of the applicable codec formats. In each of these tests, members with eligible TVs were randomly allocated to one of two cells, “control” and “treatment”. Those allocated to the “treatment” cell received AV1 streams while those allocated to the “control” cell received streams of the same codec format as before.

In all of these A/B tests, we observed improvements across many metrics for members in the “treatment” cell, in-line with our expectations:

### Higher VMAF scores across the full spectrum of streaming sessions

- [VMAF](https://netflixtechblog.com/toward-a-practical-perceptual-video-quality-metric-653f208b9652) is a video quality metric developed and open-sourced by Netflix, and is highly correlated to visual quality. Being more efficient, AV1 delivers videos with improved visual quality at the same bitrate, and thus higher VMAF scores.
- The improvement is particularly significant among sessions that experience serious network congestion and the lowest visual quality. For these sessions, AV1 streaming improves quality by up to 10 VMAF without impacting the rebuffer rate.

### More streaming at the highest resolution

- With higher compression efficiency, the bandwidth needed for streaming is reduced and thus it is easier for playback to reach the highest resolution for that session.
- For 4K eligible sessions, on average, the duration of 4K videos being streamed increased by about** **5%.

### Fewer noticeable drops in quality during playback

- We want our members to have brilliant playback experiences, and our players are designed to adapt to the changing network conditions. When the current condition cannot sustain the current video quality, our players can switch to a lower bitrate stream to reduce the chance of a playback interruption. Given AV1 consumes less bandwidth for any given quality level, our players are able to sustain the video quality for a longer period of time and do not need to switch to a lower bitrate stream as much as before.
- On some TVs, noticeable drops in quality were reduced by as much as 38%.

### Reduced start play delay

- On some TVs, with the reduced bitrate, the player can reach the target buffer level sooner to start the playback.
- On average, we observed a 2% reduction in play delay with AV1 streaming.

## Next Steps

Our initial launch includes a number of AV1 capable TVs as well as TVs connected with PS4 Pro. We are working with external partners to enable more and more devices for AV1 streaming. Another exciting direction we are exploring is AV1 with HDR. Again, the teams at Netflix are committed to delivering the best picture quality possible to our members. Stay tuned!

## Acknowledgments

This is a collective effort with contributions from many of our colleagues at Netflix. We would like to thank

- [Andrey Norkin](https://www.linkedin.com/in/andreynorkin/) and [Cyril Concolato](https://www.linkedin.com/in/cyril-concolato-567a522/) for providing their insights about AV1 specifications.
- Kyle Swanson for the work on reducing AV1 encoding complexity.
- [Anush Moorthy](https://www.linkedin.com/in/anush-moorthy-b8451142/) and [Aditya Mavlankar](https://www.linkedin.com/in/aditya-mavlankar-7139791/) for fruitful discussions about encoding recipes.
- [Frederic Turmel](https://www.linkedin.com/in/frederic-turmel-b5b7874/) and his team for managing AV1 certification tests and building tools to automate device verification.
- [Susie Xia](https://www.linkedin.com/in/susie-xia/) for helping improve resource utilization of AV1 encoding.
- Client teams for integrating AV1 playback support and optimizing the experience.
- The Partner Engineering team for coordinating with device vendors and investigating playback issues.
- The Media Cloud Engineering team for accommodating the computing resources for the AV1 rollout.
- The Media Content Playback team for providing tools for AV1 rollout management.
- The Data Science and Engineering team for A/B test analysis, and for providing data to help us continuously monitor AV1.

![image](../images/7c22d32915ac7e3a.png)

If you are passionate about video technologies and interested in what we are doing at Netflix, come and chat with us! The Encoding Technologies team currently has a number of openings, and we can’t wait to have more stunning engineers joining us.

[Senior Software Engineer, Encoding Technologies](https://jobs.netflix.com/jobs/126802582)

[Senior Software Engineer, Video & Image Encoding](https://jobs.netflix.com/jobs/101109705)

[Senior Software Engineer, Media Systems](https://jobs.netflix.com/jobs/127695186)

---
**Tags:** Av1 · Netflix · Encoding · Streaming
