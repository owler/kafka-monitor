#!/bin/sh
JAVA_HOME=/data/iris/utils/linux/java/jdk1.8.0_121/
export JAVA_HOME

nohup $JAVA_HOME/bin/java -Xms64m -Xmx1256m -cp "../web:../conf:../lib/*:../lib/ext/*" event.KafkaMonitor &


