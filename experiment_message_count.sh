#!/bin/bash

rm experimentResultMessageCount.txt >> /dev/null 2>&1

sum=0
experiment_result="experimentResultMessageCount.txt"

for (( i=1; i<=10; i++))
do

java PaxosElection $1 > temp_message_count.txt

wc -l temp_message_count.txt >> $experiment_result

sleep 1

done


# while read -r line; 
# do

# sum=$(( $sum + $line ))

# done < "$experiment_result"

# avg=$(( $sum / 10 ))

# echo $avg 

