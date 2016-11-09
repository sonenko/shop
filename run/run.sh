#!/usr/bin/env bash
cp local.conf ../target/scala-2.11/local.conf
cd ../target/scala-2.11/
java -Dconfig.file=local.conf -jar ShoppingBasket.jar