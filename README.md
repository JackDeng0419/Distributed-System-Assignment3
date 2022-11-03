# Distributed System Assignment 2
## 1. Major design decision
This project implements a basic Paxos algorithm that works with a number n of councilors with four profiles of response times: immediate; medium; late; never. The proposers and accepters are running as threads and use socket to communicate. 

## 2. Compile the code
Run `./compile.sh`

## 3. Running the program
- Run `java PaxosElection <configurationFile>`
- Example: 
    ```
    java PaxosElection test1_config.txt
    ```

## 4. Test cases

*You can run an overall test that covers all the test cases below by running:  `./test_all.sh`.*

1. One GC getting feed from the AG
    - Run: `./test1.sh`
        
2. Multiple GCs getting feed from the AG
    - Run: `./test2.sh`

3. One CS putting feed to the AG
    - Run: `./test3.sh`

4. Multiple CSs putting feed to the AG
    - Run: `./test4.sh`
   
5. CS heartbeat signal
    - Run: `./test5.sh`
