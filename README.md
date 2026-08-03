# Secure Distributed File System 

A secure distributed file system built with Java RMI, developed as part of the 
Distributed Systems Security course at Arab Academy for Science, Technology and 
Maritime Transport.

**Team:** Saifeldin Sherif, Nourien Mohammed

## Overview
Multiple clients perform file operations (upload, download, delete, rename, search, 
list) across 3 replicated storage nodes. The system enforces file consistency using 
Totally Ordered Multicast (TO-Multicast) and is hardened against four security 
vulnerabilities.

## Features
- 3 replica nodes with full file replication
- Totally Ordered Multicast for consistency across concurrent writes
- Leader-based read operations (randomly selected)
- User registration and authentication

## Security Fixes
| # | Vulnerability | Fix |
|---|---|---|
| 1 | Insecure Object Deserialization (RCE) | Whitelist-based `ValidatingObjectInputStream` |
| 2 | Dynamic Class Loading (RCI) | `useCodebaseOnly=true`, codebase cleared |
| 3 | Plaintext Transport (MitM) | Mutual TLS via RMI SSL socket factories |
| 4 | Missing Authentication | Salted SHA-256 passwords + expiring session tokens |

## Bonus
- Replay attack protection using nonce + 5-minute timestamp window

## Structure
- `Vulnerable/` — vulnerable codebase with `// VULNERABILITY:` markers + attacker demos
- `Secured/` — fixed codebase with all 4 vulnerabilities resolved

## Tech Stack
Java, Java RMI, SSL/TLS, JKS Keystores
