#!/usr/bin/env bash
shopt -s expand_aliases
source ~/.bash_aliases
cp local.conf ../rest/target/scala-2.11/local.conf
cd ../rest/target/scala-2.11/
java -Dconfig.file=local.conf -jar ShoppingBasketRest.jar