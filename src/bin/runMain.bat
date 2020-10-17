@echo off
echo Main started

java -Xms64m -Xmx1128m -cp "../conf;../lib/*;../lib/ext/*" event.KafkaMonitor
exit