# **TechWay - Online Learning Platform**

## **Team Members**
**Rasha Alzyadi** - **Reem Aljasser** - **Bayan Alkhashlan**

---

## **Project Overview**
TechWay is an advanced online learning platform that enables users to:
- Browse and enroll in technical courses
- Watch video lessons
- Take quizzes and view results
- Manage favorite courses
- Admins can monitor the platform through a powerful dashboard

---

## **Tools and Technologies**
**Backend:** Spring Boot (REST APIs)  
**Frontend:** Vue.js (Web), JavaFX (Desktop)  
**Database:** PostgreSQL  
**Build Tool:** Maven  
**Containerization:** Docker  
**Testing:** JUnit, Postman  
**API Documentation:** Swagger  

---

## **System Requirements**

### **Functional Requirements**
- User authentication (Login, Logout, Registration)  
- Browse and view course details  
- Enroll in courses  
- Watch video lessons  
- Take quizzes and view results  
- Manage favorite courses  
- Add reviews and ratings  
- Admin dashboard to manage users, courses, and platform statistics  

### **Non-Functional Requirements**
- **Security:** JWT-based authentication  
- **Performance:** Responsive UI, non-blocking API calls  
- **Scalability:** Easily extendable to support large user base  
- **Usability:** Clean and intuitive interface with proper error handling  

---

## **System Design**
**Architecture:** 3-Tier  
- **Presentation Layer:** Vue.js (Web), JavaFX (Desktop)  
- **Application Layer:** Spring Boot (REST APIs)  
- **Data Layer:** PostgreSQL  

**Main Entities:**  
- Users  
- Courses  
- Lessons  
- Quizzes  
- Favorites  

**Use Case Flow (Example):**  
Login → View Courses → Course Details → Enroll → Quiz → Favorites → Dashboard  

---

## **Use Cases**
### **Login**
**Actor:** Student  
**Precondition:** Account exists  
**Flow:** Enter credentials → Validate → Redirect to Courses  
**Alternative:** Invalid login → Show error  

### **View Courses**
**Actor:** Student  
**Precondition:** Logged in  
**Flow:** Open Courses screen → Fetch list → Display with search filter  

### **Course Details and Enrollment**
**Actor:** Student  
**Precondition:** Course list displayed  
**Flow:** Click course → View details → Enroll if desired  

### **Favorites**
**Actor:** Student  
**Precondition:** Logged in  
**Flow:** Add or remove from favorites → Updated favorites screen  

### **Quiz**
**Actor:** Student  
**Precondition:** Logged in and enrolled  
**Flow:** Open quiz → Load questions → Submit → Show score  

### **Admin Dashboard**
**Actor:** Admin  
**Precondition:** Logged in as admin  
**Flow:** Open dashboard → View stats (Users, Courses, Reviews, Favorites, Statistics)  
**Alternative:** Non-admin users → Dashboard not accessible  

---

## **Implementation**
**Backend Endpoints (Spring Boot):**  
- /api/auth/login  
- /api/courses  
- /api/favorites  
- /api/quizzes  
- /api/reviews  

**Frontend (JavaFX):**  
- Login  
- Courses  
- Course Details  
- Favorites  
- Quiz  
- Dashboard  

**Frontend (Vue.js):**  
- Same screen structure as JavaFX with responsive design  

**Token Handling:**  
JWT stored in TokenStore for authenticated requests  

---

## **Testing**
**Unit Testing (JUnit):**  
- AuthService  
- CourseService  
- QuizService  

**Integration Testing:**  
- API tested using Postman collections  

**UI Testing:**  
- Manual testing for login, navigation, error handling, and data updates  

**Example Test Case – Login:**  
- Input: Correct email and password → Expected: Redirect to courses  
- Input: Wrong credentials → Expected: Show error and stay on login screen  

---

## **Features and Advantages**
- Secure authentication using JWT  
- Web and desktop support  
- Clean and user-friendly navigation  
- Personalized learning with favorites  
- Quizzes for self-assessment  
- Admin dashboard for monitoring platform usage  

---

## **Future Work**
- Expand course library to cover tech, business, and creative topics  
- Launch mobile apps for iOS and Android  
- Add AI-powered course recommendations  
- Support multilingual content for global reach  

---

## **Conclusion**
Learning is the first step toward transformation.  
Our platform brings together expert instructors, engaging content, and flexible learning paths to help every learner unlock their full potential.  
Whether you're looking to start a career, improve your skills, or explore new knowledge areas — TechWay is your trusted companion in lifelong learning.  
Together, let’s shape a future full of growth, opportunity, and success.  

---

## **License**
Please refer to the LICENSE file for more information.  

## **Contact**
**Email:** support@techway
Please refer to the LICENSE file for more information.
Contact
Email: support@techway.
