# Final Project - FitTrack


Course: Computação Móvel (Mobile Computing)


Student(s): Tomás Santos


Date: 14/07/2026


Repository URL: https://github.com/tomassantos2025/fittrack

---

## 1. Introduction

This project is an Android application, **FitTrack**, developed for the Mobile Computing course. The goal is to apply mobile development concepts (MVVM architecture, remote persistence, authentication, multimedia, multilingual support) in a complete application built around a real business model (freemium).

The problem the app addresses is the lack of structure and motivation for people who work out at the gym: there is no single place to plan workouts, log what was done, track progress, and stay motivated through a social component.

## 2. System Overview

FitTrack is a gym workout tracking app with a social layer. Main features:

- **Authentication**: sign-up and login with email/password and Google Sign-In.
- **Workout plans**: create your own plans or import community plans, organized by goal (strength, hypertrophy, fat loss, home workout).
- **Smart Planner (Pro)**: automatically generates a plan based on the selected goal.
- **Workout sessions**: real-time session tracking, logging sets, reps and weight.
- **AI Coach**: an AI-generated tip shown in the post-workout summary, with a local fallback.
- **Progress**: charts showing training volume over time.
- **Social**: friend requests, friends, training groups and real-time group chat.
- **Business model**: Free (up to 2 plans) vs Pro (1€/month, unlimited plans + Smart Planner + extended history), with Pro state simulated via Firebase Remote Config + an `is_pro` field.

Typical use case: a user creates an account, sets a goal, creates or generates a workout plan, runs it while logging each set, receives an AI-generated summary at the end, and tracks progress over the following weeks — optionally training together with friends in a group.

## 3. Architecture and Design

The app follows the **MVVM (Model-View-ViewModel) + Repository** pattern:

```
Activity / Fragment (UI)  →  ViewModel (state)  →  Repository (Firebase)
```

- **UI**: XML layouts + View Binding (not Jetpack Compose), with Fragments organized through the **Navigation Component** (`nav_graph.xml`) and a main Activity with bottom navigation.
- **ViewModel**: exposes state to the UI through `LiveData`, launching async work with `viewModelScope.launch`.
- **Repository**: the only layer that talks to Firebase, using `suspend` functions + `.await()`, always returning `Result<T>` (success/error).
- **Persistence**: Firebase Firestore (structured data), Firebase Authentication (login), Firebase Storage (images), Firebase Remote Config (global business parameters).

### Folder structure (summary)

```
app/src/main/java/com/example/fittrack/
├── model/          # Data classes (User, WorkoutPlan, Session, ...)
├── repository/      # Firebase access (AuthRepository, SessionRepository, ...)
├── viewmodel/        # State and logic for each screen
├── ui/               # Activities and Fragments
└── utils/            # Helpers (language, appearance, data seeding)
```

### Justification of key decisions

- **XML instead of Compose**: chosen as the more traditional pattern, more direct given the time available, with View Binding removing manual `findViewById` calls.
- **Firestore instead of Room/SQLite**: the app needs to sync data across users (friends, groups, public plans), which a local-only database cannot provide.
- **LiveData instead of Kotlin Flow**: LiveData is lifecycle-aware by default, which fits the scope of the project without needing manual cancellation handling.

## 4. Implementation

Main modules and responsibilities:

| Module | Responsibility |
|---|---|
| `AuthRepository` | Login, sign-up, Google Sign-In |
| `WorkoutPlanRepository` | CRUD for workout plans in Firestore |
| `SessionRepository` | Logging and reading workout sessions and stats |
| `SocialRepository` | Friend requests, friendships and groups (with *batch writes*) |
| `AiSuggestionRepository` | Calls the Anthropic API for the AI Coach, with a local fallback |
| `RemoteConfigRepository` | Global parameters (e.g., history-length limit) |

**Relevant example — atomic operation (batch) when accepting a friend request**, ensuring the friendship creation and the request status update happen together:

```kotlin
suspend fun acceptRequest(request: FriendRequest): Result<Unit> {
    val batch = firestore.batch()
    val friendshipRef = firestore.collection("friendships").document()
    batch.set(friendshipRef, mapOf("memberUids" to listOf(request.fromUid, request.toUid)))
    batch.update(requestRef, "status", "accepted")
    return try {
        batch.commit().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

The **Smart Planner** filters the exercise library by muscle group according to the goal selected by the user, and assembles a new `WorkoutPlan` saved to Firestore — plain Kotlin logic, with no AI involved at runtime.

## 5. Testing and Validation

- Manual navigation testing across all screens (login → dashboard → plans → active session → summary → progress → social → profile).
- Test cases: creating/editing/deleting plans, Free plan limit (2 plans) and Pro unlock, sending/accepting/declining friend requests, logging a session with sets and total volume calculation.
- Edge cases considered: a user with no plans (empty Dashboard state), loss of internet connection during a Firestore call (handled via `Result.failure`), duplicate friend requests.
- **Known limitations**: no automated tests (unit/instrumented); profile picture upload works as a prototype with a local fallback; Pro status is simulated (no real payment/Google Play Billing integration).

## 6. Usage Instructions

**Requirements**: an up-to-date Android Studio installation, the Android SDK configured, your own Firebase project (a `google-services.json` file).

1. Clone/extract the repository.
2. Open the project folder in Android Studio (`File > Open`).
3. `File > Sync Project with Gradle Files`.
4. Place your Firebase project's `google-services.json` in `app/`.
5. Run the app on an emulator or physical device.

If Windows shows a "file locked" build error:
```bat
gradlew --stop
rmdir /S /Q app\build
rmdir /S /Q build
```

---

# Autonomous Software Engineering Sections - only for [AC OK, AI OK] sections

## 7. Prompting Strategy

AI (a Claude-based assistant) was used from the start of development, in two distinct stages with two distinct prompting goals:

- **Build stage**: prompts described the desired feature set at a functional level — e.g. "build an Android gym-tracking app with Firebase Auth, Firestore-backed workout plans with a Free/Pro limit, a workout session flow with set logging, a social system with friend requests and groups, and a Free/Pro business model around a 'Smart Planner' feature." Follow-up prompts refined individual features once the base was in place (e.g. adding the AI Coach post-workout suggestion, adding Remote Config for global parameters, adding multilingual support).
- **Comprehension stage** (after the app already existed): prompts shifted from "build X" to "explain X" — asking the assistant to inspect the actual generated code and explain, in plain terms, how specific parts worked: the overall architecture, how Firebase data is structured, whether Compose or Flow were actually used (verified directly against the code rather than assumed), what a Firestore batch write is, what `Result<T>` is for, and what ViewBinding/Fragments/Navigation do. This stage was essential because the app had been generated faster than it had been understood, and the oral defense required being able to explain the code, not just present it.

## 8. Autonomous Agent Workflow

The AI assistant was responsible for the large majority of the coding work: architecture setup (MVVM + Repository), Firebase integration (Auth, Firestore, Storage, Remote Config), XML layouts, ViewModels, and the AI Coach integration itself. It also assisted with debugging when features didn't behave as expected, and with producing project documentation (this report, the concept/design documents, and a personal study guide used to prepare for the oral defense).

The human role was to define what the app should do at a feature/business level, to review the resulting code and documentation, to ask targeted questions until each part was understood well enough to explain out loud, and to make the final calls on what to keep, cut, or simplify.

## 9. Verification of AI-Generated Artifacts

Verification was done by directly inspecting the generated codebase rather than trusting summaries at face value — for example, searching the code for actual Compose or Kotlin Flow usage before answering "did you use X?" questions, tracing the friend-request flow end to end in `SocialRepository`, and confirming that the `AI_API_KEY` used for the AI Coach was not hardcoded. The app was also run and manually tested on the intended navigation paths (login, plan creation, active session, progress, social) to confirm the generated code actually behaves as described. No automated test suite was added, which is listed as a known limitation in Section 5.

## 10. Human vs AI Contribution

- **AI-assisted**: the large majority of the Kotlin/XML implementation (architecture, repositories, ViewModels, layouts, Firebase integration, AI Coach integration), the initial drafts of this report and the concept/design documents.
- **Human-driven**: the choice of app concept and business model, the decision of which features to include or drop, review and verification of the AI-generated code and its claims, all Firebase Security Rules configuration decisions in the console, preparation for and delivery of the oral defense, and the final review/edit pass over every document submitted.

This project relied on AI for a large share of the implementation. The comprehension work — going back through the code, verifying what was and wasn't actually used, and being able to explain each part — was done by the student, specifically to be able to answer questions in the oral discussion rather than just submit a working app.

## 11. Ethical and Responsible Use

The main risk identified was **over-reliance**: it is possible to submit AI-generated code without understanding it, which would defeat the purpose of the assignment and could constitute misrepresentation of authorship if not disclosed. This risk was addressed directly by dedicating a separate phase of the work to understanding the existing codebase (Section 7), and by disclosing this process transparently in this report rather than presenting the app as if it had been hand-written from scratch.

A secondary risk was the AI overstating what the code actually did (e.g., initially assuming Kotlin Flow or Compose might have been used, before checking). This was mitigated by verifying claims against the actual source files rather than accepting them as given.

---

# Development Process

## 12. Version Control and Commit History

This work was pushed as a single commit rather than incremental commits over time:
- "FitTrack: aplicação Android de treino com Firebase, social e AI Coach"

## 13. Difficulties and Lessons Learned

- Understanding the difference between LiveData and Kotlin Flow, and recognizing that this project ended up using only the former.
- Correctly configuring Firestore Security Rules, and understanding that they are separate from Firebase Authentication rather than a substitute for it.
- Understanding atomic (batch) writes in Firestore and why they matter for operations like accepting a friend request.
- Learning to verify AI-generated explanations against the actual source code instead of accepting them uncritically.

## 14. Future Improvements

- Add automated tests (unit and instrumented).
- Migrate the UI to Jetpack Compose.
- Replace the simulated Pro subscription with real Google Play Billing integration.
- Add push notifications for workout reminders and group messages.

---

## 15. AI Usage Disclosure (Mandatory)

- **Tool(s) used**: Claude (Anthropic AI assistant).
- **How it was used**: generation of the majority of the application's Kotlin/XML code and Firebase integration; generation and iteration of project documentation (concept document, design document, this report); explanation of the existing codebase to the student, used to prepare for the oral discussion; generation of diagrams (ER diagram, UML class diagram, database schema, navigation map) included in the project documents.
- **Accountability statement**: I confirm that I reviewed the AI-generated code and documentation, verified its behavior directly against the running application and source files, and I take full responsibility for the code and documentation submitted as part of this project.
