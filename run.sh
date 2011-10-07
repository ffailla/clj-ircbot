#!/bin/sh

CLASSPATH=.:

for f in lib/*.jar; do
    CLASSPATH=$CLASSPATH:$f
done

java -server -Xmx1G -cp .:clj-ircbot-1.0-SNAPSHOT.jar:$CLASSPATH com.frankfailla.ircbot.app