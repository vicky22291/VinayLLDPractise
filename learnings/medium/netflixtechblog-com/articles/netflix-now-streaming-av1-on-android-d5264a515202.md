---
title: "Netflix Now Streaming AV1 on Android"
author: "Netflix Technology Blog"
date: "Feb 5, 2020"
url: "https://netflixtechblog.com/netflix-now-streaming-av1-on-android-d5264a515202"
tags: ['Android', 'Av1', 'Aom', 'Video Codec']
---

# Netflix Now Streaming AV1 on Android

_By _[_Liwei Guo_](https://www.linkedin.com/in/liwei-guo-a5aa6311/)_, _[_Vivian Li_](https://www.linkedin.com/in/wei-vivian-li/)_, _[_Julie Beckley_](https://www.linkedin.com/in/julie-novak1/)_, _[_Venkatesh Selvaraj_](https://www.linkedin.com/in/venkatesh-selvaraj-88824137/)_, and _[_Jeff Watts_](https://www.linkedin.com/in/jeffrwatts/)

Today we are excited to announce that Netflix has started streaming AV1 to our Android mobile app. AV1 is a high performance, royalty-free video codec that provides 20% improved compression efficiency over our VP9† encodes. AV1 is made possible by the wide-ranging industry commitment of expertise and intellectual property within the [Alliance for Open Media](https://aomedia.org/) (AOMedia), of which Netflix is a founding member.

Our support for AV1 represents Netflix’s continued investment in delivering the most efficient and highest quality video streams. For our mobile environment, AV1 follows on our work with VP9, which we released as part of our [mobile encodes](https://medium.com/netflix-techblog/more-efficient-mobile-encodes-for-netflix-downloads-625d7b082909) in 2016 and further optimized with [shot-based encodes](https://medium.com/netflix-techblog/optimized-shot-based-encodes-now-streaming-4b9464204830) in 2018.

While our goal is to roll out AV1 on all of our platforms, we see a good fit for AV1’s compression efficiency in the mobile space where cellular networks can be unreliable, and our members have limited data plans. Selected titles are now available to stream in AV1 for customers who wish to reduce their cellular data usage by enabling the “Save Data” feature.

Our AV1 support on Android leverages the open-source [dav1d decoder](https://code.videolan.org/videolan/dav1d) built by the VideoLAN, VLC, and FFmpeg communities and sponsored by the Alliance for Open Media. Here we have optimized dav1d so that it can play Netflix content, which is 10-bit color. In the spirit of making AV1 widely available, we are sponsoring an open-source effort to optimize 10-bit performance further and make these gains available to all.

As codec performance improves over time, we plan to expand our AV1 usage to more use cases and are now also working with device and chipset partners to extend this into hardware.


---

† _AV1-libaom compression efficiency as measured against VP9-libvpx._

---
**Tags:** Android · Av1 · Aom · Video Codec
