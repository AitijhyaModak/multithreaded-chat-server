# Multi-Threaded Chat System 💬

A real-time chat application built using Java Sockets. This project features a client-server architecture capable of handling multiple simultaneous users with private messaging, blocking capabilities, and colored terminal UI.

## Features
* **Multi-Client Support:** Handle many users at once using a thread-per-client model.
* **Private Messaging:** Send direct messages to specific users.
* **User Blocking:** Feature to ignore messages from specific participants.
* **File Transfer** Basic functionality of sending files to server.

## Concepts Used
* **Networking:** TCP/IP Sockets, Input/Output Streams, and Port Binding.
* **Concurrency:** Multi-threading (`Runnable`), `volatile` variables.
* **Synchronization:** Thread-safe collections (`SynchronizedSet`) and `synchronized` blocks for race-condition prevention.
* **Protocol Design:** Custom command parsing and state management.


## ⌨️ List of Commands
| Command | Action |
| :--- | :--- |
| `/list` | Show all online users |
| `/msg <user> <text>` | Send a private message |
| `/msgall <text>` | Broadcast message to everyone |
| `/block <user>` | Ignore a specific user |
| `/unblock <user>` | Resume seeing messages from a user |
| `/help` | Show command menu |
| `/exit` | Safely disconnect from server |
| `@ <filename>` | Sends and store files in server directory |

## How to Run

1. **Compile the files:**
   
   ```bash
   javac *.java
3. **Run the server:**
   
   ```bash
   java ChatServer <port_no>
5. **Run the clients in seperate terminals:**
   
   ```bash
   java ChatClient localhost <port_no>
   
