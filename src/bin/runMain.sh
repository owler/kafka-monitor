#!/bin/sh
#JAVA_HOME=/data/iris/utils/linux/java/jdk1.8.0_121/
#export JAVA_HOME

nohup java -Xms128m -Xmx756m -cp "../conf:../lib/*:../lib/ext/*" event.KafkaMonitor > /dev/null 2>&1 &


