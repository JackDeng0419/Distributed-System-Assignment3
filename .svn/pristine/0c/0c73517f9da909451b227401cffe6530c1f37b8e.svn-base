# Distributed System Assignment 2
## 1. Major design decision
This project implements a basic Paxos algorithm that works with a number n of councilors with four profiles of response times: immediate; medium; late; never. The proposers and accepters are running as threads and use socket to communicate. 

## 2. Compile the code
Run `./compile.sh`

## 3. Running the program
- Run `java PaxosElection <configurationFile>` 
(configurationFile is a txt file that defines the member id, respond profile and domain information)
- Example: 
    ```
    java PaxosElection test1_config.txt
    ```
- Format of the configuration file:
    MemberID-profile-accepterDomain-proposerDomain
    ```
    M1-immediate-127.0.0.1:9201-127.0.0.1:9101
    M2-immediate-127.0.0.1:9202-127.0.0.1:9102
    M3-immediate-127.0.0.1:9203
    M4-immediate-127.0.0.1:9204
    M5-immediate-127.0.0.1:9205
    M6-immediate-127.0.0.1:9206
    M7-immediate-127.0.0.1:9207
    M8-immediate-127.0.0.1:9208
    M9-immediate-127.0.0.1:9209
    ```
- The election result can be checked in `./voteResult.txt`

## 4. Test cases

*You can run an overall test that covers all the test cases below by running:  `./test_all.sh`.*

1. 2 proposers send proposal at the same time, with immediate respond time. 
    - Run: `./test1.sh`
        
2. 3 proposers send proposal at the same time. Accepter of M2 and M3 never respond. M4 to M9 have a medium (1-4 s) respond time.   
    - Run: `./test2.sh`

3. M1 to M5 never respond, which should cause a failed Paxos election. 
    - Run: `./test3.sh`

4. M1 to M4 never respond, and M5 to M9 have a late (3-6 s) respond time. This is the lowest requirement for a successful Paxos election. 
    - Run: `./test4.sh`
   
5. 30 hosts with 6 proposers. (immediate: 4, medium: 15, late: 6, never: 5)
    - Run: `./test5.sh`
