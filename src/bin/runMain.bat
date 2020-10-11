@echo off
echo Main started

java -Xms64m -Xmx1128m -cp "../web;../conf;../lib/*" event.KafkaMonitor
exit