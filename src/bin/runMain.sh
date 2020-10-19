#!/bin/sh
#JAVA_HOME=/data/iris/utils/linux/java/jdk1.8.0_121/
#export JAVA_HOME

nohup java -Xms64m -Xmx1256m -cp "../conf:../lib/*:../lib/ext/*" event.KafkaMonitor &


