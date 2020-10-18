@echo off
echo Main started

java -Xms64m -Xmx512m -Djava.security.auth.login.config=C:\tools\kafkatool2\jaas.conf -cp "../conf;../lib/*;../lib/ext/*" event.KafkaMonitor
exit