CREATE DATABASE IF NOT EXISTS student_management1;
USE student_management1;

CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    role ENUM('Admin', 'Teacher', 'Student') NOT NULL
);

CREATE TABLE students (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    grade VARCHAR(10),
    contact VARCHAR(15),
    class_name VARCHAR(50),
    section VARCHAR(10),
    admission_date DATE,
    academic_year VARCHAR(10)
);

CREATE TABLE attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50),
    date DATE,
    status ENUM('Present', 'Tardy','Left Early'),
    FOREIGN KEY (student_id) REFERENCES students(id)
);

CREATE TABLE fees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id VARCHAR(50),
    class_name VARCHAR(50),
    amount DOUBLE,
    status VARCHAR(20),
    due_date DATE,
    FOREIGN KEY (student_id) REFERENCES students(id)
);
INSERT INTO users (user_id, username, password, role) VALUES
('admin1', 'admin', 'admin123', 'Admin'),
('teacher1', 'teacher', 'teacher123', 'Teacher'),
('student1', 'student', 'student123', 'Student');

ALTER TABLE students ADD class_name VARCHAR(50);
ALTER TABLE fees ADD class_name VARCHAR(50);
ALTER TABLE students ADD Section VARCHAR(50);
ALTER TABLE students ADD Admission_date date;
ALTER TABLE students ADD Academic_year VARCHAR(50);


select * from attendance;