# Library Management System

A lightweight Java backend and HTML/JS frontend library management system.

## Setup Locally

1. Install JDK 17 or higher.
2. Compile the Java files:
   ```bash
   javac *.java
   ```
3. Run the server:
   ```bash
   java LibraryServer
   ```
4. Open `http://localhost:8080` in your web browser.

## Deployment to Render 🚀

This project is completely configured to run seamlessly on [Render](https://render.com) using Docker. 
Render perfectly hosts containerized Java applications like this one using the provided Dockerfile. Vercel isn't suitable for this kind of persistent Java TCP server because it only hosts static files and serverless functions directly. Therefore, Render is highly recommended.

1. **Push** this directory to a new repository on GitHub.
2. Visit **Render.com** and create a new **Web Service**.
3. Point it to your GitHub repository.
4. Keep the environment as **Docker** (Render will automatically detect your `Dockerfile`).
5. Complete setup and deploy. The Server binds perfectly onto Render's default `PORT` environment var.
