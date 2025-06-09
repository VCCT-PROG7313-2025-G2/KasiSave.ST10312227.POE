Kasi Save - Smart Budgeting App
Icon:  
Kasi Save is an Android mobile application that empowers users to track and manage their finances in an engaging and user-friendly way. The app combines core expense and income tracking with innovative features like gamification, milestone goals, and smart voice/OCR input, all backed by Firebase.
Features
Authentication
•	Firebase Authentication for secure user registration and login.
•	Avatar selection during sign-up for personalized profiles.
Expense & Income Tracking
•	Log expenses and income with details like amount, category, date, time, description.
•	Supports recurring transactions (e.g., monthly rent or salary).
•	Attach and preview photos of receipts or bills, stored securely in Firebase Storage.
Smart Input
•	Voice input support for entering expense descriptions and amounts hands-free.
•	OCR scanning using the camera to extract text from physical receipts.
Date & Time Picker
•	Easy-to-use date, start time, and end time fields with built-in calendar and clock icons.
Milestones & Budget Alerts
•	Users can set minimum and maximum monthly spending goals.
•	App compares current monthly spending to these goals and provides visual alerts.
Gamified Rewards
•	Rewards are stored and displayed using Firebase Firestore.
Family & Friends Leaderboard
•	A leaderboard to compare savings progress with friends and family.
•	Avatars and usernames displayed for fun competition.
🔧 Tech Stack
•	Frontend: Java, XML (Android SDK)
•	Backend: Firebase Firestore, Firebase Storage, Firebase Auth
•	Libraries:
o	ML Kit for Text Recognition
o	Firebase UI
o	Lottie for animations

Setup Instructions
1.	Clone the repo:
git clone https://github.com/ST10312227/KasiSave.ST10312227.POE.git
2.	Open in Android Studio.
3.	Configure Firebase:
o	Go to Firebase Console
o	Create a new project and enable:
	Firebase Authentication
	Firestore Database
	Firebase Storage
o	Download the google-services.json file and place it in your app/ folder.
4.	Run the app on an emulator or physical device.


Advanced Features added in KasiSave
*Smart Input
•	Voice input support for entering expense descriptions and amounts hands-free.
•	OCR scanning using the camera to extract text from physical receipts.
*Family & Friends Leaderboard
•	A leaderboard to compare savings progress with friends and family.
•	Avatars and usernames displayed for fun competition.

Feature	Tech / Logic
Voice input	SpeechRecognizer API
OCR scanning	ML Kit Text Recognition
Recurring transactions	Stored with recurrence flags in Firestore
Image capture/upload	FileProvider + Firebase Storage
Leaderboard	Firebase queries + RecyclerView
Gamified rewards	Logic in Firestore based on conditions

UI Customizations
•	Colour scheme: Green-based for financial wellness.
•	Fonts: Modern sans-serif for readability.
•	Icons: Calendar, clock, microphone, camera — consistent throughout.

Security
•	Authentication and Firestore rules ensure data is private and tied to the user.
•	Only authenticated users can access or modify their data.


YouTube Link: https://youtu.be/KojSvU4qS-0

References 
https://developer.android.com/get-started/overview
https://developer.android.com/kotlin 
https://developer.android.com/develop/ui/views/layout/constraint-layout 
https://developer.android.com/training/data-storage/room 
https://developer.android.com/kotlin/coroutines#room 
https://developer.android.com/develop/ui/views/layout/recyclerview 
https://developer.android.com/media/camera/camerax 
https://developer.android.com/reference/androidx/core/content/FileProvider 
https://weeklycoding.com/mpandroidchart-documentation/ 
https://developer.android.com/reference/com/google/android/material/bottomnavigation/BottomNavigationView 
https://developer.android.com/training/data-storage/shared-preferences

*Disclaimer I have made use of AI chatbot namely ChatGPT to assist with code structure and a few errors
