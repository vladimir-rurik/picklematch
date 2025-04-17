
# PickleMatch — Re-frame + Firebase 

This project started as a minimal **ClojureScript + Re-frame** application demonstrating **Google sign-in** and **logout** using **Firebase v9**. It has now been expanded to include:

- **Sign in with Google or Facebook**  
- **Role-based** access (ordinary vs. admin)  
- **Admin features** for scheduling games, setting times, and storing them in Firestore  
- **Date selector** to view scheduled games for a particular day  
- **Join/registration** flow for users to sign up for a specific team  
- **Rating logic** to recalculate player ratings after each game  
- **Printing** the daily game list

---

## Features

1. **Multi-provider Auth**  
   - Google **and** Facebook login flows via Firebase Authentication.  
2. **Role Management**  
   - Each user has a `role` stored in Firestore (`"ordinary"` or `"admin"`). Admins can toggle their role, schedule games, and see admin-only panels.  
3. **Scheduling & Viewing Games**  
   - Admin users can create new game slots, picking a date and time.  
   - Ordinary users can pick a date to see scheduled games and register for a team.  
4. **Score Entry & Ratings**  
   - Once a game finishes, users enter scores in the UI.  
   - The system recalculates each player’s rating (`+5` for winners, `-5` for losers), storing updated ratings in Firestore.  
5. **Printing**  
   - A “Print” button quickly generates a printable daily game list.

---

## Prerequisites

- [Node.js](https://nodejs.org/) (v14+ recommended)  
- [npm](https://www.npmjs.com/)  
- A [Firebase](https://firebase.google.com/) project with:  
  - **Google** and/or **Facebook** sign-in enabled (Firebase Console → **Build** → **Authentication** → **Sign-in method**).  
  - **Cloud Firestore** enabled (Firebase Console → **Build** → **Firestore**).  

Make sure you’ve created a **Web App** in Firebase (Project Settings → General → “Your apps”) and have added your **Authorized domains** (like `localhost`) under **Sign-in methods**.

---

## Installation & Setup

1. **Clone** (or download) this repository to your machine.
2. In your project folder, install dependencies:
   ```bash
   npm install
   ```
3. Open `src/picklematch/config.cljs` (or similar config file) and update **Firebase config** with your project details:
   ```clojure
   (defonce firebase-config
     {:apiKey            "YOUR-API-KEY"
      :authDomain        "your-app.firebaseapp.com"
      :projectId         "your-app"
      :storageBucket     "your-app.appspot.com"
      :messagingSenderId "123456789"
      :appId             "1:123456789:web:abcdefg"})
   ```
4. Ensure **Google Sign-in** and **Facebook Sign-in** are **enabled** in the Firebase Console:
   - **Build → Authentication → Sign-in method**.

5. Verify **Authorized domains** (like `localhost`) under the same console section.

6. Check your **Firestore security rules** to allow your use case. For development, you can temporarily allow open access or restrict it so that:
   - Admins can schedule games.  
   - Ordinary users can read games and update only their own user doc, etc.

---

## Running the App (Development)

1. Start the local dev server with Shadow-CLJS:
   ```bash
   npx shadow-cljs watch app
   ```
2. Open [http://localhost:3000](http://localhost:3000) in your browser.
3. **Sign in** with Google or Facebook:
   - A popup appears for OAuth.
   - On success, you’re taken to the main “home” panel (if you’re admin, you’ll also see the scheduling panel).
4. You can:
   - **Toggle** admin/ordinary role (if you’re testing) to see the different UI.
   - **Pick a date** to view scheduled games or add new ones (admin).
   - **Join** a game on Team1 or Team2 (ordinary user).
   - **Enter scores** to trigger rating updates.

---

## Scheduling & Viewing Games

1. **Admin**:
   - Chooses a date, enters time (e.g., `8:00 AM`), and clicks “Add Game.”
   - The new game is stored in Firestore with `date: "YYYY-MM-DD"`.
2. **Ordinary** users:
   - Select the same date to see the scheduled games, click “Join Team1” or “Join Team2.”
   - Once four players are assigned, a game is effectively full (though the code can be expanded for more advanced logic).
3. **Scores**:
   - After playing, any user can enter the final score and click “Save.”
   - The system calculates rating changes (`+5` winners, `-5` losers) and updates Firestore.

---

## Project Structure

```
my-picklematch/
├── shadow-cljs.edn         ; Shadow-CLJS config (build targets, output paths, etc.)
├── package.json            ; npm dependencies and scripts
├── resources/
│   └── public/
│       └── index.html      ; HTML entry point
└── src/
    └── picklematch/
        ├── config.cljs     ; Firebase config (apiKey, etc.)
        ├── firebase.cljs   ; Firebase auth/firestore logic
        ├── events.cljs     ; Re-frame event handlers & effects
        ├── subs.cljs       ; Re-frame subscriptions
        ├── views.cljs      ; UI components (login panel, admin panel, etc.)
        └── core.cljs       ; App entry point (mount root, initialization)
```

- **`config.cljs`** holds your **Firebase config** object.  
- **`firebase.cljs`** manages sign-in with Google/Facebook, Firestore reads/writes, etc.  
- **`events.cljs`** handles the main application logic, like scheduling games, logging in, toggling roles, and updating ratings.  
- **`views.cljs`** defines the Reagent components for login, admin scheduling, game list, etc.  
- **`core.cljs`** is where the app initializes Re-frame state and React rendering.

---

## Production Build

To create an optimized build:

```bash
npx shadow-cljs release app
```

This writes optimized output to `resources/public/js/main.js`. You can deploy the entire `resources/public` folder to hosting services like **Firebase Hosting**, **Vercel**, or **Netlify**.

---

## Troubleshooting

- **User sees “Empty” instead of names**: Make sure you load all relevant user docs in your `:players` map so you can map UID → email or display name.  
- **Query requires an index**: If you do `(where "date" "==" ...)` + `(orderBy "time")`, create a Firestore composite index in the console.  
- **No games appear**: Ensure the date format in your scheduling code matches the format in your load query. Also check security rules.  
- **API key invalid**: Double-check the Firebase config.  
- **Popup blocked**: Some browsers block popups by default; allow popups for `localhost:3000`.  
- **Role doesn’t change**: Confirm your Firestore rules or `update-user-role!` logic in `firebase.cljs` allows writing `:role`.  

---

## License

This project is provided **as is** without warranty. Feel free to use and modify for your own application needs.
```