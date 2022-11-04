#!/bin/bash

rm experimentResult.txt >> /dev/null 2>&1

sum=0

for (( i=1; i<=10; i++))
do

java PaxosElection $1

sleep 1

done

experiment_result="experimentResult.txt"

while read -r line; 
do

sum=$(( $sum + $line ))

done < "$experiment_result"

avg=$(( $sum / 10 ))

echo $avg 

