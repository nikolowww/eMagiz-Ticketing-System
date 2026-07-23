# 🎫 eMagiz Ticketing System

A modern, role-based ticket management system that streamlines communication between customers and support teams. The platform enables efficient incident reporting, request tracking, collaboration, and complete auditability through a secure web application.

---

# 📖 Overview

The **eMagiz Ticketing System** was developed as a university Software Engineering project following **Agile Scrum** methodologies.

The system provides a centralized platform where:

- Customers can submit and track support requests.
- Support Engineers manage and assign tickets.
- Consultants resolve assigned issues.
- Administrators oversee users, roles, and system activity.

The application focuses on:

- Secure authentication
- Role-Based Access Control (RBAC)
- RESTful API architecture
- Audit logging
- Responsive user interface
- Scalable backend design

---

# ✨ Features

## 👤 Customer

- Create support tickets
- View personal tickets
- Track ticket status in real time
- Add comments to tickets
- Receive updates from support staff

---

## 🛠 Support Engineer

- Create customer accounts
- View all submitted tickets
- Assign tickets to consultants
- Update ticket priority and status
- Manage customer requests
- Monitor ticket workflow

---

## 💼 Consultant

- View assigned tickets
- Communicate via ticket comments
- Update ticket progress
- Resolve assigned issues

---

## 👑 Administrator

- Full system access
- User management
- Role management
- Access audit logs
- Monitor overall system activity

---

# 🛠 Tech Stack

## Frontend

- HTML5
- CSS3
- JavaScript (ES6)
- Bootstrap

## Backend

- Java
- Jersey (JAX-RS)
- RESTful APIs

## Database

- PostgreSQL

## Authentication & Security

- Password Hashing
- Role-Based Access Control (RBAC)
- Session Authentication
- Audit Logging

## Development Tools

- IntelliJ IDEA
- Maven
- Git
- GitLab
- Trello

---

# 🏗 System Architecture

```text
Frontend
    │
    ▼
REST API (Jersey)
    │
    ▼
Business Logic
    │
    ▼
PostgreSQL Database
```

---

# 🗄 Database Design

The system is built around the following core entities:

- User
- Customer
- Support Engineer
- Consultant
- Ticket
- Comment
- Audit Log

### Database Design Principles

- Data consistency
- Referential integrity
- Efficient querying
- Complete action history

---

# 🔐 Security

Security features implemented include:

- Secure password hashing
- Role-Based Authorization
- Protected REST API endpoints
- Session authentication
- Input validation
- Audit logging
- Authentication required for protected resources

---

# ⚙ Agile Development

The project was developed using **Agile Scrum** practices.

### Scrum Activities

- Sprint Planning
- Daily Stand-ups
- Sprint Reviews
- Sprint Retrospectives

### Project Management

- User Stories
- MoSCoW Prioritization
- Trello Board
- GitLab Version Control

---

# 📚 Learning Outcomes

This project provided practical experience in:

- RESTful API development
- Java backend development
- Software architecture
- PostgreSQL database design
- Authentication & Authorization
- Secure web application development
- Agile Software Engineering
- Git collaboration workflows
- Full-stack application development

---

# 🔑 Demo Login Credentials

| Role | Username | Password |
|------|----------|----------|
| Support Engineer | `support` | `support123` |
| Consultant | `consultant` | `consultant123` |
| Customer | `customer` | `customer123` |

---

# 🧰 Project Tools

- GitLab
- Trello
- IntelliJ IDEA
- Maven
- PostgreSQL

---

# 📌 Project Status

✅ **Completed**

Developed as part of a university Software Engineering course using Agile Scrum methodologies.

---

# 👨‍💻 Author

**Aleksandar Nikolov**