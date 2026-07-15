package com.example.fittrack.utils

import com.example.fittrack.model.Exercise
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Seeds the Firestore 'exercises' collection with a richer starter catalogue.
 * The app also uses this list as an offline fallback for classroom demos.
 */
object ExerciseSeeder {

    private fun ex(
        id: String,
        name: String,
        description: String,
        muscleGroups: List<String>,
        equipment: String,
        animationUrl: String,
        difficulty: String,
        icon: String,
        howTo: List<String>,
        tips: List<String>,
        mistakes: List<String>,
        homeFriendly: Boolean = false
    ) = Exercise(
        id = id,
        name = name,
        description = description,
        muscleGroups = muscleGroups,
        equipment = equipment,
        animationUrl = animationUrl,
        difficulty = difficulty,
        iconEmoji = icon,
        howTo = howTo,
        tips = tips,
        commonMistakes = mistakes,
        homeFriendly = homeFriendly
    )

    val localExercises = listOf(
        ex(
            "bench_press", "Bench Press", "Compound chest press for strength and upper-body mass.",
            listOf("Chest", "Triceps", "Shoulders"), "Barbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bench-Press.gif", "intermediate", "💪",
            listOf("Set your eyes under the bar and keep feet planted.", "Lower the bar under control to mid/lower chest.", "Press up while keeping shoulder blades tight."),
            listOf("Use a spotter for heavy sets.", "Keep wrists stacked over elbows."),
            listOf("Bouncing the bar on the chest.", "Flaring elbows too aggressively.")
        ),
        ex(
            "incline_dumbbell_press", "Incline Dumbbell Press", "Upper-chest focused press with a larger range of motion.",
            listOf("Chest", "Shoulders", "Triceps"), "Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Incline-Dumbbell-Press.gif", "intermediate", "💪",
            listOf("Set bench to about 30–45 degrees.", "Start dumbbells near upper chest.", "Press up and slightly inward without clanking weights."),
            listOf("Control the eccentric phase.", "Use a moderate incline to avoid overusing shoulders."),
            listOf("Arching excessively.", "Cutting the depth short.")
        ),
        ex(
            "push_up", "Push-up", "Bodyweight chest exercise suitable for gym or home.",
            listOf("Chest", "Triceps", "Core"), "Bodyweight",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Push-Up.gif", "beginner", "🤸",
            listOf("Place hands slightly wider than shoulders.", "Keep body in a straight line.", "Lower chest near the floor, then push away."),
            listOf("Elevate hands to make it easier.", "Add a slow tempo to make it harder."),
            listOf("Sagging hips.", "Half reps."), true
        ),
        ex(
            "dumbbell_fly", "Dumbbell Fly", "Chest isolation movement focused on stretch and control.",
            listOf("Chest"), "Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Fly.gif", "beginner", "💪",
            listOf("Lie on a bench with a slight bend in the elbows.", "Open arms until you feel a chest stretch.", "Bring dumbbells together by squeezing the chest."),
            listOf("Use light weights.", "Think of hugging a barrel."),
            listOf("Turning it into a press.", "Going too heavy and stressing shoulders.")
        ),
        ex(
            "squat", "Barbell Squat", "Main lower-body strength lift for quads, glutes and trunk control.",
            listOf("Quadriceps", "Glutes", "Hamstrings", "Core"), "Barbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Squat.gif", "intermediate", "🦵",
            listOf("Brace your core before descending.", "Sit between your hips while keeping chest proud.", "Drive through mid-foot to stand up."),
            listOf("Film from the side to check depth.", "Warm up hips and ankles first."),
            listOf("Knees collapsing inward.", "Losing brace at the bottom.")
        ),
        ex(
            "leg_press", "Leg Press", "Machine-based lower-body press with stable torso support.",
            listOf("Quadriceps", "Glutes"), "Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Press.gif", "beginner", "🦵",
            listOf("Place feet shoulder-width on platform.", "Lower until knees are comfortably bent.", "Press without locking knees violently."),
            listOf("Great for high-rep leg volume.", "Adjust foot position to bias quads or glutes."),
            listOf("Letting lower back round off the pad.", "Using tiny range of motion.")
        ),
        ex(
            "romanian_deadlift", "Romanian Deadlift", "Hip-hinge movement for hamstrings, glutes and posterior chain.",
            listOf("Hamstrings", "Glutes", "Back"), "Barbell or Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Romanian-Deadlift.gif", "intermediate", "🦵",
            listOf("Hold weight close to thighs.", "Push hips back with soft knees.", "Return by driving hips forward."),
            listOf("Stop when hamstrings are stretched, not when plates touch floor.", "Keep lats tight."),
            listOf("Squatting instead of hinging.", "Rounding the back.")
        ),
        ex(
            "deadlift", "Deadlift", "Full-body pull from the floor emphasizing posterior chain strength.",
            listOf("Back", "Glutes", "Hamstrings", "Core"), "Barbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Deadlift.gif", "advanced", "🏋️",
            listOf("Stand with bar over mid-foot.", "Brace, pull slack out of the bar, and push floor away.", "Lock out with hips, not by leaning back."),
            listOf("Use progressive warm-up sets.", "Keep bar close to legs."),
            listOf("Jerking the bar from the floor.", "Overextending at lockout.")
        ),
        ex(
            "walking_lunge", "Walking Lunge", "Unilateral leg exercise for balance, glutes and quads.",
            listOf("Quadriceps", "Glutes", "Hamstrings"), "Bodyweight or Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lunges.gif", "beginner", "🦵",
            listOf("Step forward with control.", "Lower back knee toward the floor.", "Push through front foot and continue walking."),
            listOf("Start bodyweight before loading.", "Keep torso tall."),
            listOf("Short unstable steps.", "Front knee collapsing inward."), true
        ),
        ex(
            "leg_curl", "Leg Curl", "Hamstring isolation using a machine.",
            listOf("Hamstrings"), "Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Leg-Curl.gif", "beginner", "🦵",
            listOf("Align machine pad above heels.", "Curl smoothly toward glutes.", "Lower slowly until legs are extended."),
            listOf("Pause briefly at peak contraction.", "Avoid lifting hips off the pad."),
            listOf("Swinging the weight.", "Skipping the slow negative.")
        ),
        ex(
            "calf_raise", "Standing Calf Raise", "Calf exercise targeting ankle plantar flexion strength.",
            listOf("Calves"), "Machine or Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Standing-Calf-Raise.gif", "beginner", "🦵",
            listOf("Stand tall with balls of feet supported.", "Rise as high as possible.", "Lower until calves stretch."),
            listOf("Use full range of motion.", "Try pauses at top and bottom."),
            listOf("Bouncing quickly.", "Not reaching a full stretch.")
        ),
        ex(
            "pull_up", "Pull-up", "Bodyweight vertical pull for back and biceps.",
            listOf("Back", "Biceps"), "Pull-up Bar",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Pull-Up.gif", "intermediate", "🪽",
            listOf("Hang with active shoulders.", "Pull elbows down toward ribs.", "Lower under control to full extension."),
            listOf("Use bands if needed.", "Think chest toward bar."),
            listOf("Kipping unintentionally.", "Only doing half reps."), true
        ),
        ex(
            "lat_pulldown", "Lat Pulldown", "Vertical pulling exercise useful before progressing to pull-ups.",
            listOf("Back", "Biceps"), "Cable Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Lat-Pulldown.gif", "beginner", "🪽",
            listOf("Grip bar slightly wider than shoulders.", "Pull bar to upper chest.", "Control the bar back up."),
            listOf("Lean back slightly, not excessively.", "Lead with elbows."),
            listOf("Pulling behind the neck.", "Using body momentum.")
        ),
        ex(
            "barbell_row", "Barbell Row", "Horizontal pull for mid-back thickness and strength.",
            listOf("Back", "Biceps"), "Barbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Bent-Over-Row.gif", "intermediate", "🪽",
            listOf("Hinge forward with a braced torso.", "Pull bar toward lower ribs.", "Lower until arms are straight."),
            listOf("Keep torso angle consistent.", "Squeeze shoulder blades at the top."),
            listOf("Standing up each rep.", "Using too much hip drive.")
        ),
        ex(
            "seated_cable_row", "Seated Cable Row", "Controlled horizontal pull for back and posture.",
            listOf("Back", "Biceps"), "Cable Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Seated-Cable-Row.gif", "beginner", "🪽",
            listOf("Sit tall with knees slightly bent.", "Pull handle toward lower ribs.", "Return with shoulder blades moving forward under control."),
            listOf("Avoid shrugging.", "Pause at the squeeze."),
            listOf("Rounding the lower back.", "Yanking with momentum.")
        ),
        ex(
            "overhead_press", "Overhead Press", "Standing shoulder press for delts, triceps and core stability.",
            listOf("Shoulders", "Triceps", "Core"), "Barbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Overhead-Press.gif", "intermediate", "🏋️",
            listOf("Start bar at upper chest.", "Brace glutes and abs.", "Press overhead and move head through at the top."),
            listOf("Keep wrists neutral.", "Use small weight jumps."),
            listOf("Overarching the lower back.", "Pressing around the face instead of straight up.")
        ),
        ex(
            "lateral_raise", "Lateral Raise", "Shoulder isolation for side delts and wider-looking shoulders.",
            listOf("Shoulders"), "Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Lateral-Raise.gif", "beginner", "🏋️",
            listOf("Hold dumbbells by your sides.", "Raise arms to shoulder height.", "Lower slowly without swinging."),
            listOf("Slight elbow bend is fine.", "Use light weights and strict control."),
            listOf("Shrugging traps up.", "Swinging the torso.")
        ),
        ex(
            "face_pull", "Face Pull", "Rear-delt and upper-back movement for shoulder health.",
            listOf("Shoulders", "Back"), "Cable Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Face-Pull.gif", "beginner", "🏋️",
            listOf("Set cable around face height.", "Pull rope toward forehead.", "Finish with elbows high and shoulder blades squeezed."),
            listOf("Great warm-up or accessory.", "Control the return."),
            listOf("Turning it into a row.", "Using excessive load.")
        ),
        ex(
            "bicep_curl", "Bicep Curl", "Simple arm isolation movement for biceps.",
            listOf("Biceps"), "Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Curl.gif", "beginner", "💪",
            listOf("Stand tall with dumbbells by your sides.", "Curl without moving elbows forward.", "Lower until elbows are extended."),
            listOf("Squeeze at the top.", "Alternate arms if form breaks down."),
            listOf("Swinging the back.", "Letting wrists bend backward."), true
        ),
        ex(
            "hammer_curl", "Hammer Curl", "Neutral-grip curl emphasizing brachialis and forearms.",
            listOf("Biceps", "Forearms"), "Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hammer-Curl.gif", "beginner", "💪",
            listOf("Hold dumbbells with palms facing each other.", "Curl up while keeping elbows close.", "Lower with control."),
            listOf("Good complement to standard curls.", "Avoid rotating the wrist."),
            listOf("Using momentum.", "Shortening the bottom range."), true
        ),
        ex(
            "tricep_pushdown", "Tricep Pushdown", "Cable triceps isolation with easy load control.",
            listOf("Triceps"), "Cable Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Pushdown.gif", "beginner", "💪",
            listOf("Pin elbows at your sides.", "Push handle down until elbows lock softly.", "Return until forearms are near parallel."),
            listOf("Use rope or straight bar.", "Keep shoulders relaxed."),
            listOf("Letting elbows drift forward.", "Leaning body weight into the cable.")
        ),
        ex(
            "tricep_extension", "Overhead Tricep Extension", "Long-head triceps exercise using a dumbbell.",
            listOf("Triceps"), "Dumbbell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Dumbbell-Triceps-Extension.gif", "beginner", "💪",
            listOf("Hold dumbbell overhead with both hands.", "Lower behind your head.", "Extend elbows without flaring too much."),
            listOf("Keep ribs down.", "Use a comfortable elbow path."),
            listOf("Arching the back.", "Turning it into a shoulder movement."), true
        ),
        ex(
            "plank", "Plank", "Isometric core exercise for trunk stability.",
            listOf("Core"), "Bodyweight",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Plank.gif", "beginner", "🔥",
            listOf("Place elbows under shoulders.", "Squeeze glutes and brace abs.", "Hold a straight line from head to heels."),
            listOf("Start with short quality holds.", "Breathe while bracing."),
            listOf("Hips too high or too low.", "Holding breath."), true
        ),
        ex(
            "hanging_leg_raise", "Hanging Leg Raise", "Advanced core exercise emphasizing lower abs and hip flexors.",
            listOf("Core"), "Pull-up Bar",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Hanging-Leg-Raise.gif", "advanced", "🔥",
            listOf("Hang with active shoulders.", "Raise legs by curling pelvis upward.", "Lower slowly without swinging."),
            listOf("Start with knee raises if needed.", "Control the negative."),
            listOf("Swinging wildly.", "Only lifting with hip flexors.")
        ),
        ex(
            "mountain_climber", "Mountain Climber", "Dynamic bodyweight conditioning exercise for core and cardio.",
            listOf("Core", "Cardio"), "Bodyweight",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Mountain-Climber.gif", "beginner", "⚡",
            listOf("Start in a strong plank.", "Drive one knee toward chest.", "Alternate quickly while keeping hips stable."),
            listOf("Use it as a finisher.", "Slow it down to focus on core."),
            listOf("Hips bouncing too much.", "Hands too far forward."), true
        ),
        ex(
            "burpee", "Burpee", "Full-body conditioning movement for power and endurance.",
            listOf("Cardio", "Chest", "Legs"), "Bodyweight",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Burpee.gif", "intermediate", "⚡",
            listOf("Squat down and place hands on floor.", "Jump feet back to plank.", "Return feet forward and jump up."),
            listOf("Scale by stepping instead of jumping.", "Keep reps clean before speeding up."),
            listOf("Collapsing lower back in plank.", "Rushing sloppy reps."), true
        ),
        ex(
            "goblet_squat", "Goblet Squat", "Beginner-friendly squat pattern using a dumbbell or kettlebell.",
            listOf("Quadriceps", "Glutes", "Core"), "Dumbbell or Kettlebell",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Goblet-Squat.gif", "beginner", "🦵",
            listOf("Hold weight close to chest.", "Squat down between your knees.", "Stand tall by pushing through mid-foot."),
            listOf("Useful for learning squat depth.", "Keep elbows inside knees at the bottom."),
            listOf("Letting chest collapse.", "Heels lifting."), true
        ),
        ex(
            "hip_thrust", "Hip Thrust", "Glute-focused hip extension exercise.",
            listOf("Glutes", "Hamstrings"), "Barbell or Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Barbell-Hip-Thrust.gif", "intermediate", "🦵",
            listOf("Place upper back on bench.", "Drive hips upward by squeezing glutes.", "Lower until hips are close to floor."),
            listOf("Tuck chin slightly.", "Pause at full hip extension."),
            listOf("Overarching lower back.", "Feet too far away." )
        ),
        ex(
            "pec_deck", "Pec Deck", "Machine chest isolation with stable path.",
            listOf("Chest"), "Machine",
            "https://fitnessprogramer.com/wp-content/uploads/2021/02/Pec-Deck-Fly.gif", "beginner", "💪",
            listOf("Set handles at chest height.", "Bring arms together smoothly.", "Return until chest is stretched."),
            listOf("Great after pressing movements.", "Keep shoulder blades set."),
            listOf("Using too much weight.", "Letting shoulders roll forward.")
        )
    )

    suspend fun seedIfNeeded() {
        val db = FirebaseFirestore.getInstance()
        try {
            val snapshot = db.collection("exercises").limit(1).get().await()
            if (snapshot.isEmpty) {
                for (exercise in localExercises) {
                    db.collection("exercises").document(exercise.id).set(exercise).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
