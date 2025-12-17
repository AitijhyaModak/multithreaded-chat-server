# multithreaded-chat-server

A realtime application built from scratch using Java sockets and multithreading. This project demonstrates networking TCP concepts and concurrency concepts.

## features

-   **multiple clients:** supports multiple clients connecting together, creating threads for handling each client.
-   **command based interaction:** parsing of user intent to execute various commands like blocking, exiting, listing online users, etc.
-   **thread-safe architecture:** utilizes synchronized hash map and synchronized sets to manage global state without race condition.
-   **private messaging:** clients can send direct messages using `/msg <username> <message>` command.
-   **user blocking:** clients can block specific user using `/block <username>` command.
-   **ANSI colored UI:** distinct color coding for Server, Private, and Global messages and various information for better readability in terminal.


## architecture
// todo


## getting started
// todo

## todo
- [ ] implement a file transfer method
