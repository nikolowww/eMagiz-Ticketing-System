# eMagiz Ticketing System

## About the Project

The eMagiz Ticketing System is a web application developed as part of a university Software Engineering project. Its purpose is to provide a simple platform where customers can submit support requests and track their progress, while support staff can manage and resolve them.

The application follows a role-based access model, meaning that each type of user has different permissions and functionality within the system.

---

## Features

### Customer

- Create new support tickets
- View personal tickets
- Track ticket status
- Add comments to tickets

### Support Engineer

- View all tickets
- Create customer accounts
- Assign tickets to consultants
- Change ticket priority and status

### Consultant

- View assigned tickets
- Comment on tickets
- Update ticket progress
- Mark tickets as resolved

### Administrator

- Manage users
- Manage roles
- View audit logs
- Access all system functionality

---

## Technologies Used

### Backend

- Java
- Jersey (JAX-RS)
- Maven
- REST API

### Frontend

- HTML
- CSS
- JavaScript
- Bootstrap

### Database

- PostgreSQL

### Tools

- IntelliJ IDEA
- Git
- GitLab
- Trello

---

## Project Structure

```text
Frontend
    │
    ▼
REST API
    │
    ▼
Business Logic
    │
    ▼
PostgreSQL Database
```

---

## Database

The application uses the following main entities:

- User
- Customer
- Support Engineer
- Consultant
- Ticket
- Comment
- Audit Log

The database was designed to maintain data integrity and keep a complete history of important user actions.

---

## Security

The application includes:

- User authentication
- Password hashing
- Role-Based Access Control (RBAC)
- Protected API endpoints
- Input validation
- Audit logging

---

## Agile Development

The project was developed using Agile Scrum practices.

During development we worked with:

- User Stories
- Sprint Planning
- Daily Stand-ups
- Sprint Reviews
- Sprint Retrospectives
- MoSCoW Prioritization

GitLab was used for version control and Trello for task management.

---

## What I Learned

Working on this project helped me improve my understanding of:

- REST API development
- Java backend development
- Database design with PostgreSQL
- Authentication and authorization
- Git and version control
- Team collaboration
- Agile software development

---

## Demo Accounts

| Role | Username | Password |
|------|----------|----------|
| Support Engineer | support | support123 |
| Consultant | consultant | consultant123 |
| Customer | customer | customer123 |

---

## Status

Completed

---

## Author

Aleksandar Nikolov
