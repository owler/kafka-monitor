# kafka-monitor
[![Build](https://travis-ci.com/owler/kafka-monitor.svg?token=q8PUaXxtZ8UBGympJ7ap&branch=master)](https://travis-ci.org/owler/kafka-monitor#)

<em>Kafka monitor is a web UI for viewing Kafka topics and messages.</em> 
# Features
* **View topics** — partitions with start and end offsets
* **Browse messages** — UTF8 or custom format
* **Download** - raw bytes, UTF8 or custom format 
* **Plugins** - supports custom plugins written in Java to view/download messages

# Requirements
* Java 8 or newer
* Kafka (version 0.10.0 or newer)

# Build
sbt pack

