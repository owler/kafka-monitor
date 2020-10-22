#!/bin/sh
#JAVA_HOME=/data/iris/utils/linux/java/jdk1.8.0_121/
#export JAVA_HOME

nohup java -Xms512m -Xmx1536m -XX:+UseG1GC -cp "../conf:../lib/*:../lib/ext/*" event.KafkaMonitor > /dev/null 2>&1 &


