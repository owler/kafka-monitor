@echo off
echo Main started

java -Xms512m -Xmx1536m -XX:+UseG1GC -Djava.security.auth.login.config=..\conf\jaas.conf -cp "../conf;../lib/*;../lib/ext/*" event.KafkaMonitor
exit